package com.bank.admin.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bank.admin.module.ai.client.CloudBaseAiClient;
import com.bank.admin.module.ai.dto.AiAnalyzeRequest;
import com.bank.admin.module.ai.dto.AiChatRequest;
import com.bank.admin.module.ai.service.AiService;
import com.bank.admin.module.ai.vo.AiResultVO;
import com.bank.admin.module.calendar.entity.CalendarEvent;
import com.bank.admin.module.calendar.mapper.CalendarEventMapper;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.hunyuan.v20230901.models.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 服务实现 - 支持多业务模块数据查询与智能问答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final CardTransactionMapper transactionMapper;
    private final CalendarEventMapper calendarEventMapper;

    @Autowired(required = false)
    private CloudBaseAiClient aiClient;

    @Value("${cloudbase.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${cloudbase.ai.model:hunyuan-lite}")
    private String model;

    /** 总配额（从配置读取，单位 tokens） */
    @Value("${cloudbase.ai.quota:1000000}")
    private long totalQuota;

    /** 本次服务启动后的累计已用 Token（内存计数，重启归零） */
    private static final AtomicLong usedTokens = new AtomicLong(0);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ==================== 系统提示词（深度强化版） ====================

    private static final String SYSTEM_PROMPT =
        "# 角色\n" +
        "你是「奶龙」，一个聪明、幽默、有温度的AI助手。你的智商很高，能处理复杂问题。\n\n" +
        "## 性格特征\n" +
        "- 像一个博学又有趣的朋友，不是客服机器人\n" +
        "- 回答精准但不死板，适当用轻松的语气\n" +
        "- 会主动追问细节以便给出更好的回答\n" +
        "- 不知道的事情诚实承认，不编造\n\n" +
        "## 核心能力\n" +
        "1. **通用问答**：天气、新闻、常识、生活技巧、翻译、写作、代码、数学、科学知识\n" +
        "2. **闲聊**：讲笑话、讲故事、推荐电影/音乐/美食、情感陪伴、哲学思考\n" +
        "3. **业务数据**：日程待办分析、银行卡流水分析、账单管理、财务建议\n" +
        "4. **推理计算**：数学题、逻辑推理、数据分析、方案对比\n" +
        "5. **创意写作**：文案、邮件、总结、报告、各种文体\n\n" +
        "## 回答规范\n" +
        "- 先理解问题，再组织答案。复杂问题先拆解再逐一回答\n" +
        "**重要**：涉及数据时必须引用具体数字和事实，不能笼统概括\n" +
        "- 用列表/分段组织长答案，方便阅读\n" +
        "- 适度使用emoji增加可读性（不要每句话都加）\n" +
        "- 如果用户的问题有歧义或信息不足，主动询问澄清\n" +
        "- 默认中文回答，用户用其他语言则跟随用户的语言\n" +
        "- 回答长度根据问题复杂度灵活调整，简单问题简答，复杂问题详答\n\n" +
        "## 示例\n" +
        "\n" +
        "用户：「今天天气怎么样？」\n" +
        "回复思路：我没有实时天气数据，但可以基于常识给出建议。如果用户说了城市，我可以给出更具体的建议。同时可以聊一些天气相关的话题。\n" +
        "\n" +
        "用户：「帮我算一下，如果每个月存2000元，年化4%，5年后有多少钱？」\n" +
        "回复思路：这是复利计算问题。需要用复利公式 FV = PMT × (((1+r)^n - 1) / r) 来计算。给出公式、过程步骤、最终结果。\n" +
        "\n" +
        "用户：「我最近花钱有点多怎么办」\n" +
        "回复思路：结合流水数据给出具体分析，然后给出实用建议（记账、设定预算、延迟消费等），语气要温暖鼓励而不是说教。";

    @Override
    public AiResultVO chat(AiChatRequest request) {
        if (!checkEnabled()) return fail("AI服务未启用");

        String userMessage = request.getMessage();

        // 智能判断是否需要注入上下文数据
        String contextData = buildContextData(userMessage);

        // 构建消息列表：system + 历史记录 + 当前用户消息
        List<Message> msgList = new java.util.ArrayList<>();

        // 1. 系统提示词
        msgList.add(buildMessage("system", SYSTEM_PROMPT));

        // 2. 历史对话（多轮记忆，最多保留最近20条）
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            int start = Math.max(0, request.getHistory().size() - 20);
            for (int i = start; i < request.getHistory().size(); i++) {
                AiChatRequest.ChatMessage hist = request.getHistory().get(i);
                if ("user".equals(hist.getRole()) || "assistant".equals(hist.getRole())) {
                    msgList.add(buildMessage(hist.getRole(), hist.getContent()));
                }
            }
        }

        // 3. 当前用户消息（有业务数据时拼接）
        if (contextData != null && !contextData.isEmpty()) {
            msgList.add(buildMessage("user", "[以下是相关业务数据]\n" + contextData + "\n\n[用户问题] " + userMessage));
        } else {
            msgList.add(buildMessage("user", userMessage));
        }

        Message[] messages = msgList.toArray(new Message[0]);
        log.debug("[AI] 发送消息数={}, 含历史={}", messages.length, request.getHistory() != null ? request.getHistory().size() : 0);

        try {
            ChatCompletionsResponse response = aiClient.chat(messages);
            Usage usage = response.getUsage();
            int totalTokens = usage != null ? usage.getTotalTokens().intValue() : 0;
            int inputTokens = usage != null ? usage.getPromptTokens().intValue() : 0;
            int outputTokens = usage != null ? usage.getCompletionTokens().intValue() : 0;
            String reply = response.getChoices()[0].getMessage().getContent();
            usedTokens.addAndGet(totalTokens);
            return AiResultVO.success(reply, totalTokens, inputTokens, outputTokens, model)
                    .withQuota(totalQuota, usedTokens.get());
        } catch (TencentCloudSDKException e) {
            log.error("[AI] 聊天请求失败: {}", e.getMessage());
            return fail("AI调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("[AI] 聊天异常", e);
            return fail("系统内部错误");
        }
    }

    @Override
    public AiResultVO analyze(AiAnalyzeRequest request) {
        if (!checkEnabled()) return fail("AI服务未启用");

        LambdaQueryWrapper<CardTransaction> wrapper = new LambdaQueryWrapper<>();
        if (request.getStartDate() != null) {
            wrapper.ge(CardTransaction::getTxDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(CardTransaction::getTxDate, request.getEndDate());
        }
        if (request.getCardId() != null) {
            wrapper.eq(CardTransaction::getCardId, request.getCardId());
        }
        if (request.getOwnerId() != null) {
            wrapper.eq(CardTransaction::getOwnerId, request.getOwnerId());
        }
        wrapper.orderByDesc(CardTransaction::getTxDate)
               .last("LIMIT 500");

        List<CardTransaction> transactions = transactionMapper.selectList(wrapper);
        if (transactions.isEmpty()) {
            return fail("指定范围内没有找到流水数据");
        }

        String dataText = buildTransactionText(transactions);
        String analyzeType = getAnalyzeTypeLabel(request.getType());
        String startStr = request.getStartDate() != null ? request.getStartDate().format(DATE_FMT) : "(不限)";
        String endStr = request.getEndDate() != null ? request.getEndDate().format(DATE_FMT) : "(不限)";

        String prompt = String.format(
            "你是金融数据分析助手。请根据以下银行卡交易流水数据进行%s分析。\n\n" +
            "【时间范围】%s ~ %s\n\n" +
            "【流水数据】\n%s",
            analyzeType, startStr, endStr, dataText
        );

        Message[] messages = new Message[]{
            buildMessage("system", SYSTEM_PROMPT),
            buildMessage("user", prompt)
        };

        try {
            ChatCompletionsResponse response = aiClient.chat(messages);
            Usage usage = response.getUsage();
            int totalTokens = usage != null ? usage.getTotalTokens().intValue() : 0;
            int inputTokens = usage != null ? usage.getPromptTokens().intValue() : 0;
            int outputTokens = usage != null ? usage.getCompletionTokens().intValue() : 0;
            String reply = response.getChoices()[0].getMessage().getContent();
            usedTokens.addAndGet(totalTokens);
            return AiResultVO.success(reply, totalTokens, inputTokens, outputTokens, model)
                    .withQuota(totalQuota, usedTokens.get());
        } catch (TencentCloudSDKException e) {
            log.error("[AI] 分析请求失败: {}", e.getMessage());
            return fail("AI调用失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("[AI] 分析异常", e);
            return fail("系统内部错误");
        }
    }

    @Override
    public AiResultVO getQuota() {
        if (!checkEnabled()) return fail("AI服务未启用");
        long used = usedTokens.get();
        AiResultVO vo = new AiResultVO();
        vo.setSuccess(true);
        vo.withQuota(totalQuota, used);
        vo.setModel(model);
        log.debug("[AI] 配额查询: 总额={}, 已用={}, 剩余={}", totalQuota, used, Math.max(0, totalQuota - used));
        return vo;
    }

    // ==================== 上下文数据构建 ====================

    /**
     * 根据用户问题智能匹配需要查询的业务数据，返回格式化的文本
     * 返回 null 表示不需要额外数据
     */
    private String buildContextData(String userMessage) {
        if (userMessage == null) return null;

        String msg = userMessage.toLowerCase();
        StringBuilder context = new StringBuilder();
        LocalDate today = LocalDate.now();

        // ---- 日程/待办 相关 ----
        if (containsAny(msg, "待办", "todo", "日程", "计划", "今天", "今日", "要做", "安排")) {
            String todoData = queryTodayTodos(today);
            if (todoData != null) context.append(todoData).append("\n");
        }

        // ---- 流水/交易/收支 相关 ----
        if (containsAny(msg, "流水", "交易", "支出", "收入", "消费", "花了", "账单", "金额")) {
            String txData = queryRecentTransactions(today);
            if (txData != null) context.append(txData).append("\n");
        }

        return context.length() > 0 ? context.toString() : null;
    }

    /** 查询今日待办事项 */
    private String queryTodayTodos(LocalDate date) {
        LambdaQueryWrapper<CalendarEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarEvent::getEventDate, date)
               .ne(CalendarEvent::getStatus, 2) // 排除已完成
               .orderByAsc(CalendarEvent::getStartTime);

        List<CalendarEvent> events = calendarEventMapper.selectList(wrapper);

        if (events == null || events.isEmpty()) {
            return String.format("【今日待办 - %s】\n暂无待办事项，今天可以休息一下！", date.format(DATE_FMT));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【今日待办 - %s】共 %d 项\n", date.format(DATE_FMT), events.size()));

        int pending = 0, inProgress = 0, highPriority = 0;
        for (int i = 0; i < events.size(); i++) {
            CalendarEvent ev = events.get(i);
            String statusLabel = switch (ev.getStatus()) {
                case 0 -> "⬜ 待办";
                case 1 -> "🔄 进行中";
                case 3 -> "❌ 已取消";
                default -> "";
            };
            String priorityLabel = switch (ev.getPriority()) {
                case 2 -> "🔴 高优先级";
                case 1 -> "🟡 中优先级";
                default -> "⚪ 低优先级";
            };
            String categoryLabel = switch (ev.getCategory()) {
                case 0 -> "工作"; case 1 -> "生活"; case 2 -> "学习";
                case 3 -> "健康"; default -> "其他";
            };
            String timeStr = ev.getStartTime() != null
                ? ev.getStartTime().format(TIME_FMT)
                    + (ev.getEndTime() != null ? "-" + ev.getEndTime().format(TIME_FMT) : "")
                : "全天";

            sb.append(String.format("%d. [%s] %s | 分类:%s | %s | %s\n",
                i + 1, statusLabel, ev.getTitle(), categoryLabel, priorityLabel, timeStr));

            if (ev.getStatus() == 0) pending++;
            else if (ev.getStatus() == 1) inProgress++;
            if (ev.getPriority() == 2) highPriority++;
        }

        sb.append(String.format("\n统计: 待办%d项, 进行中%d项, 高优先级%d项", pending, inProgress, highPriority));

        // 再查一下近7天的事项作为补充
        LocalDate weekLater = date.plusDays(7);
        LambdaQueryWrapper<CalendarEvent> futureWrapper = new LambdaQueryWrapper<>();
        futureWrapper.gt(CalendarEvent::getEventDate, date)
                     .le(CalendarEvent::getEventDate, weekLater)
                     .ne(CalendarEvent::getStatus, 2)
                     .orderByAsc(CalendarEvent::getEventDate)
                     .last("LIMIT 10");

        List<CalendarEvent> futureEvents = calendarEventMapper.selectList(futureWrapper);
        if (futureEvents != null && !futureEvents.isEmpty()) {
            sb.append(String.format("\n\n【近7天即将到来】共 %d 项\n", futureEvents.size()));
            for (CalendarEvent ev : futureEvents) {
                sb.append(String.format("- %s: %s (%s)\n",
                    ev.getEventDate().format(DATE_FMT), ev.getTitle(),
                    ev.getCategory() == 0 ? "工作"
                        : ev.getCategory() == 1 ? "生活"
                            : ev.getCategory() == 2 ? "学习"
                                : ev.getCategory() == 3 ? "健康"
                                    : ev.getCategory() == 4 ? "其他" : "日记"));
            }
        }

        return sb.toString();
    }

    /** 查询最近的交易记录 */
    private String queryRecentTransactions(LocalDate date) {
        LocalDate weekAgo = date.minusDays(7);

        LambdaQueryWrapper<CardTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CardTransaction::getTxDate, weekAgo)
               .le(CardTransaction::getTxDate, date)
               .orderByDesc(CardTransaction::getTxDate)
               .last("LIMIT 30");

        List<CardTransaction> transactions = transactionMapper.selectList(wrapper);

        if (transactions == null || transactions.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【最近7天流水 - %s ~ %s】共 %d 条记录\n",
            weekAgo.format(DATE_FMT), date.format(DATE_FMT), transactions.size()));

        double totalIncome = 0, totalExpense = 0;
        for (CardTransaction tx : transactions) {
            sb.append(String.format("- %s | %s %.2f元 | %s\n",
                tx.getTxDate(),
                tx.getTxType() == 1 ? "收入+" : "支出-",
                tx.getAmount(),
                tx.getDescription()));
            if (tx.getTxType() == 1) {
                totalIncome += tx.getAmount().doubleValue();
            } else {
                totalExpense += tx.getAmount().doubleValue();
            }
        }
        sb.append(String.format("\n总收入: %.2f元, 总支出: %.2f元, 净额: %+.2f元",
            totalIncome, totalExpense, totalIncome - totalExpense));

        return sb.toString();
    }

    // ==================== 内部方法 ====================

    private boolean checkEnabled() {
        if (!aiEnabled || aiClient == null) {
            log.warn("[AI] AI服务未启用或客户端未初始化");
            return false;
        }
        return true;
    }

    private AiResultVO fail(String message) {
        return AiResultVO.fail(message);
    }

    private Message buildMessage(String role, String content) {
        Message msg = new Message();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    /**
     * 将流水列表转换为文本格式供 AI 分析
     */
    private String buildTransactionText(List<CardTransaction> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("共 %d 条记录\n", list.size()));

        double totalIncome = 0, totalExpense = 0;
        for (CardTransaction tx : list) {
            sb.append(String.format("- [%s] %s | 类型:%s | 金额:%.2f元 | %s\n",
                tx.getTxDate(),
                tx.getDescription(),
                tx.getTxType() == 1 ? "收入" : "支出",
                tx.getAmount(),
                tx.getRemark() != null ? tx.getRemark() : ""
            ));
            if (tx.getTxType() == 1) {
                totalIncome += tx.getAmount().doubleValue();
            } else {
                totalExpense += tx.getAmount().doubleValue();
            }
        }
        sb.append(String.format("\n总收入: %.2f元, 总支出: %.2f元, 净额: %.2f元",
            totalIncome, totalExpense, totalIncome - totalExpense));
        return sb.toString();
    }

    private String getAnalyzeTypeLabel(String type) {
        return switch (type) {
            case "reconcile" -> "对账";
            case "anomaly" -> "异常检测";
            case "summary" -> "收支总结";
            default -> type;
        };
    }

    /** 判断文本是否包含任意关键词 */
    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
