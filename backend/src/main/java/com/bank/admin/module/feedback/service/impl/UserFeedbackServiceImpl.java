package com.bank.admin.module.feedback.service.impl;

import com.bank.admin.common.result.PageResult;
import com.bank.admin.module.feedback.dto.FeedbackCreateDTO;
import com.bank.admin.module.feedback.dto.FeedbackQueryDTO;
import com.bank.admin.module.feedback.dto.FeedbackRemarkDTO;
import com.bank.admin.module.feedback.dto.FeedbackStatusUpdateDTO;
import com.bank.admin.module.feedback.dto.FeedbackUpdateDTO;
import com.bank.admin.module.feedback.entity.UserFeedback;
import com.bank.admin.module.feedback.entity.UserFeedbackAttachment;
import com.bank.admin.module.feedback.entity.UserFeedbackProcessLog;
import com.bank.admin.module.feedback.enums.FeedbackPriorityEnum;
import com.bank.admin.module.feedback.enums.FeedbackStatusEnum;
import com.bank.admin.module.feedback.enums.FeedbackTypeEnum;
import com.bank.admin.module.feedback.mapper.UserFeedbackAttachmentMapper;
import com.bank.admin.module.feedback.mapper.UserFeedbackMapper;
import com.bank.admin.module.feedback.mapper.UserFeedbackProcessLogMapper;
import com.bank.admin.module.feedback.service.UserFeedbackService;
import com.bank.admin.module.feedback.vo.FeedbackAttachmentVO;
import com.bank.admin.module.feedback.vo.FeedbackProcessLogVO;
import com.bank.admin.module.feedback.vo.FeedbackStatsVO;
import com.bank.admin.module.feedback.vo.FeedbackVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户反馈 ServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFeedbackServiceImpl implements UserFeedbackService {

    private final UserFeedbackMapper feedbackMapper;
    private final UserFeedbackAttachmentMapper attachmentMapper;
    private final UserFeedbackProcessLogMapper processLogMapper;

    /** 上传根目录（开发环境）*/
    private static final String UPLOAD_BASE_DIR = System.getProperty("user.home") + "/bank-uploads/feedback/";

    /** 允许的文件后缀 */
    private static final Set<String> ALLOWED_SUFFIX = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx", "xls", "xlsx", "txt", "zip", "rar"
    ));

    /** 单文件最大：10MB */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** 单次最多上传文件数 */
    private static final int MAX_FILE_COUNT = 5;

    // ========================= 分页查询 =========================

    @Override
    public PageResult<FeedbackVO> page(FeedbackQueryDTO query) {
        Page<UserFeedback> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<UserFeedback>()
            .like(StringUtils.hasText(query.getTitleKeyword()),
                UserFeedback::getTitle, query.getTitleKeyword())
            .eq(query.getStatus() != null, UserFeedback::getStatus, query.getStatus())
            .eq(query.getPriority() != null, UserFeedback::getPriority, query.getPriority())
            .eq(query.getFeedbackType() != null, UserFeedback::getFeedbackType, query.getFeedbackType())
            .like(StringUtils.hasText(query.getSubmitter()),
                UserFeedback::getSubmitter, query.getSubmitter())
            .ge(StringUtils.hasText(query.getStartTime()),
                UserFeedback::getCreateTime, parseDateTime(query.getStartTime()))
            .le(StringUtils.hasText(query.getEndTime()),
                UserFeedback::getCreateTime, parseDateTime(query.getEndTime()))
            .orderByDesc(UserFeedback::getUpdateTime);

        Page<UserFeedback> result = feedbackMapper.selectPage(page, wrapper);

        List<FeedbackVO> voList = result.getRecords().stream()
            .map(this::toVO)
            .collect(Collectors.toList());
        fillAttachmentList(voList);

        Page<FeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return PageResult.of(voPage);
    }

    // ========================= 详情 =========================

    @Override
    public FeedbackVO detail(Long id) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getIsDeleted() == 1) {
            throw new RuntimeException("反馈记录不存在");
        }
        FeedbackVO vo = toVO(feedback);
        fillAttachmentList(List.of(vo));

        // 处理轨迹（正序）
        List<UserFeedbackProcessLog> logs = processLogMapper.selectList(
            new LambdaQueryWrapper<UserFeedbackProcessLog>()
                .eq(UserFeedbackProcessLog::getFeedbackId, id)
                .orderByAsc(UserFeedbackProcessLog::getOperateTime)
        );
        vo.setProcessLogs(logs.stream().map(this::toProcessLogVO).collect(Collectors.toList()));

        return vo;
    }

    // ========================= 提交反馈 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeedbackCreateDTO dto, List<MultipartFile> files) {
        // 文件数量校验
        if (files != null && files.size() > MAX_FILE_COUNT) {
            throw new RuntimeException("最多上传 " + MAX_FILE_COUNT + " 个文件");
        }

        String currentUser = currentUsername();

        // 保存主表
        UserFeedback feedback = new UserFeedback();
        feedback.setFeedbackNo(generateFeedbackNo());
        feedback.setTitle(dto.getTitle());
        feedback.setContent(dto.getContent());
        feedback.setFeedbackType(dto.getFeedbackType());
        feedback.setPriority(dto.getPriority());
        feedback.setStatus(FeedbackStatusEnum.PENDING.getCode());
        feedback.setSubmitter(currentUser);
        feedback.setAttachmentCount(0);
        feedbackMapper.insert(feedback);

        Long feedbackId = feedback.getId();

        // 处理附件
        int attachmentCount = 0;
        if (files != null && !files.isEmpty()) {
            List<UserFeedbackAttachment> attachmentList = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) continue;
                UserFeedbackAttachment attachment = saveFile(file, feedbackId, i);
                attachmentList.add(attachment);
                attachmentCount++;
            }
            if (!attachmentList.isEmpty()) {
                attachmentList.forEach(attachmentMapper::insert);
            }
        }

        // 更新附件数量
        if (attachmentCount > 0) {
            UserFeedback update = new UserFeedback();
            update.setId(feedbackId);
            update.setAttachmentCount(attachmentCount);
            feedbackMapper.updateById(update);
        }

        // 写入处理轨迹
        saveProcessLog(feedbackId, "SUBMIT", null, FeedbackStatusEnum.PENDING.getCode(),
            "提交反馈", currentUser);

        return feedbackId;
    }

    // ========================= 编辑反馈 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FeedbackUpdateDTO dto, List<MultipartFile> files) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || feedback.getIsDeleted() == 1) {
            throw new RuntimeException("反馈记录不存在");
        }

        List<Long> deleteAttachmentIds = dto.getDeleteAttachmentIds() == null
            ? new ArrayList<>()
            : dto.getDeleteAttachmentIds();
        List<UserFeedbackAttachment> deleteAttachments = deleteAttachmentIds.isEmpty()
            ? new ArrayList<>()
            : attachmentMapper.selectList(
                new LambdaQueryWrapper<UserFeedbackAttachment>()
                    .eq(UserFeedbackAttachment::getFeedbackId, id)
                    .in(UserFeedbackAttachment::getId, deleteAttachmentIds)
            );

        long remainAttachmentCount = attachmentMapper.selectCount(
            new LambdaQueryWrapper<UserFeedbackAttachment>()
                .eq(UserFeedbackAttachment::getFeedbackId, id)
        ) - deleteAttachments.size();
        int newFileCount = files == null ? 0 : (int) files.stream().filter(file -> file != null && !file.isEmpty()).count();
        if (remainAttachmentCount + newFileCount > MAX_FILE_COUNT) {
            throw new RuntimeException("最多上传 " + MAX_FILE_COUNT + " 个文件");
        }

        Integer fromStatus = feedback.getStatus();
        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setTitle(dto.getTitle());
        update.setContent(dto.getContent());
        update.setFeedbackType(dto.getFeedbackType());
        update.setPriority(dto.getPriority());
        update.setStatus(dto.getStatus());
        update.setSubmitter(dto.getSubmitter().trim());
        update.setAssignee(emptyToNull(dto.getAssignee()));
        update.setLatestRemark(emptyToNull(dto.getLatestRemark()));
        update.setResolvedTime(parseDateTime(dto.getResolvedTime()));
        update.setClosedTime(parseDateTime(dto.getClosedTime()));
        feedbackMapper.updateById(update);

        for (UserFeedbackAttachment attachment : deleteAttachments) {
            attachmentMapper.deleteById(attachment.getId());
        }

        if (files != null && !files.isEmpty()) {
            int sortNo = (int) Math.max(0, remainAttachmentCount);
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                attachmentMapper.insert(saveFile(file, id, sortNo++));
            }
        }

        int attachmentCount = Math.toIntExact(attachmentMapper.selectCount(
            new LambdaQueryWrapper<UserFeedbackAttachment>()
                .eq(UserFeedbackAttachment::getFeedbackId, id)
        ));
        UserFeedback attachmentUpdate = new UserFeedback();
        attachmentUpdate.setId(id);
        attachmentUpdate.setAttachmentCount(attachmentCount);
        feedbackMapper.updateById(attachmentUpdate);

        saveProcessLog(id, "UPDATE", fromStatus, dto.getStatus(),
            StringUtils.hasText(dto.getLatestRemark()) ? dto.getLatestRemark() : "编辑反馈信息", currentUsername());

        if (!deleteAttachments.isEmpty() || newFileCount > 0) {
            saveProcessLog(id, "ATTACHMENT", null, null,
                "编辑附件：删除 " + deleteAttachments.size() + " 个，新增 " + newFileCount + " 个", currentUsername());
        }
    }

    // ========================= 修改状态 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, FeedbackStatusUpdateDTO dto) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) throw new RuntimeException("反馈记录不存在");

        Integer fromStatus = feedback.getStatus();
        Integer toStatus = dto.getStatus();

        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setStatus(toStatus);
        update.setLatestRemark(dto.getRemark());

        if (FeedbackStatusEnum.RESOLVED.getCode().equals(toStatus)) {
            update.setResolvedTime(LocalDateTime.now());
        } else if (FeedbackStatusEnum.CLOSED.getCode().equals(toStatus)) {
            update.setClosedTime(LocalDateTime.now());
        }
        feedbackMapper.updateById(update);

        // 写入处理轨迹
        saveProcessLog(id, "STATUS_CHANGE", fromStatus, toStatus, dto.getRemark(), currentUsername());
    }

    // ========================= 追加备注 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRemark(Long id, FeedbackRemarkDTO dto) {
        UserFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) throw new RuntimeException("反馈记录不存在");

        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setLatestRemark(dto.getRemark());
        feedbackMapper.updateById(update);

        saveProcessLog(id, "REMARK", null, null, dto.getRemark(), currentUsername());
    }

    // ========================= 统计 =========================

    @Override
    public FeedbackStatsVO stats() {
        FeedbackStatsVO vo = new FeedbackStatsVO();
        vo.setPendingCount(countByStatus(FeedbackStatusEnum.PENDING.getCode()));
        vo.setInProgressCount(countByStatus(FeedbackStatusEnum.IN_PROGRESS.getCode()));
        vo.setResolvedCount(countByStatus(FeedbackStatusEnum.RESOLVED.getCode()));
        vo.setTotalCount(feedbackMapper.selectCount(new LambdaQueryWrapper<>()));

        // 本周新增
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        vo.setWeekNewCount(feedbackMapper.selectCount(
            new LambdaQueryWrapper<UserFeedback>().ge(UserFeedback::getCreateTime, weekStart)
        ));
        return vo;
    }

    // ========================= 附件路径 =========================

    @Override
    public String getAttachmentFilePath(Long attachmentId) {
        UserFeedbackAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) throw new RuntimeException("附件不存在");
        return attachment.getFilePath();
    }

    // ========================= 删除附件 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId) {
        UserFeedbackAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) throw new RuntimeException("附件不存在");

        // 逻辑删除
        attachmentMapper.deleteById(attachmentId);

        // 更新主表附件数
        UserFeedback feedback = feedbackMapper.selectById(attachment.getFeedbackId());
        if (feedback != null) {
            int newCount = Math.max(0, feedback.getAttachmentCount() - 1);
            UserFeedback update = new UserFeedback();
            update.setId(feedback.getId());
            update.setAttachmentCount(newCount);
            feedbackMapper.updateById(update);
        }

        saveProcessLog(attachment.getFeedbackId(), "ATTACHMENT",
            null, null, "删除附件：" + attachment.getFileName(), currentUsername());
    }

    // ========================= 逻辑删除 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        feedbackMapper.deleteById(id);
    }

    // ========================= 私有方法 =========================

    private void fillAttachmentList(List<FeedbackVO> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            return;
        }

        List<Long> feedbackIds = feedbackList.stream()
            .map(FeedbackVO::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        if (feedbackIds.isEmpty()) {
            return;
        }

        List<UserFeedbackAttachment> attachments = attachmentMapper.selectList(
            new LambdaQueryWrapper<UserFeedbackAttachment>()
                .in(UserFeedbackAttachment::getFeedbackId, feedbackIds)
                .orderByAsc(UserFeedbackAttachment::getFeedbackId)
                .orderByAsc(UserFeedbackAttachment::getSortNo)
        );
        Map<Long, List<FeedbackAttachmentVO>> attachmentMap = new HashMap<>();
        for (UserFeedbackAttachment attachment : attachments) {
            attachmentMap.computeIfAbsent(attachment.getFeedbackId(), key -> new ArrayList<>())
                .add(toAttachmentVO(attachment));
        }

        feedbackList.forEach(item -> item.setAttachments(
            attachmentMap.getOrDefault(item.getId(), new ArrayList<>())
        ));
    }

    private FeedbackVO toVO(UserFeedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        vo.setFeedbackTypeDesc(FeedbackTypeEnum.getDesc(feedback.getFeedbackType()));
        vo.setPriorityDesc(FeedbackPriorityEnum.getDesc(feedback.getPriority()));
        vo.setStatusDesc(FeedbackStatusEnum.getDesc(feedback.getStatus()));
        return vo;
    }

    private FeedbackAttachmentVO toAttachmentVO(UserFeedbackAttachment a) {
        FeedbackAttachmentVO vo = new FeedbackAttachmentVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    private FeedbackProcessLogVO toProcessLogVO(UserFeedbackProcessLog log) {
        FeedbackProcessLogVO vo = new FeedbackProcessLogVO();
        BeanUtils.copyProperties(log, vo);
        vo.setFromStatusDesc(FeedbackStatusEnum.getDesc(log.getFromStatus()));
        vo.setToStatusDesc(FeedbackStatusEnum.getDesc(log.getToStatus()));
        vo.setActionDesc(buildActionDesc(log.getActionType()));
        return vo;
    }

    private String buildActionDesc(String actionType) {
        return switch (actionType) {
            case "SUBMIT" -> "提交反馈";
            case "UPDATE" -> "编辑反馈";
            case "STATUS_CHANGE" -> "状态变更";
            case "REMARK" -> "添加备注";
            case "ATTACHMENT" -> "附件操作";
            case "ASSIGN" -> "指派处理人";
            case "CLOSE" -> "关闭反馈";
            default -> actionType;
        };
    }

    private void saveProcessLog(Long feedbackId, String actionType,
                                Integer fromStatus, Integer toStatus,
                                String remark, String operator) {
        UserFeedbackProcessLog logEntry = new UserFeedbackProcessLog();
        logEntry.setFeedbackId(feedbackId);
        logEntry.setActionType(actionType);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setRemark(remark);
        logEntry.setOperator(operator);
        logEntry.setOperateTime(LocalDateTime.now());
        processLogMapper.insert(logEntry);
    }

    private UserFeedbackAttachment saveFile(MultipartFile file, Long feedbackId, int index) {
        String originalName = file.getOriginalFilename();
        String suffix = getSuffix(originalName);

        // 后缀校验
        if (!ALLOWED_SUFFIX.contains(suffix.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型：" + suffix);
        }
        // 大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件 " + originalName + " 超过10MB限制");
        }

        // 生成存储路径
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        String relativePath = dateDir + "/" + storedName;
        String fullPath = UPLOAD_BASE_DIR + relativePath;

        // 确保目录存在
        File dir = new File(UPLOAD_BASE_DIR + dateDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 写文件
        try {
            file.transferTo(new File(fullPath));
        } catch (IOException e) {
            log.error("文件上传失败: {}", fullPath, e);
            throw new RuntimeException("文件上传失败：" + originalName);
        }

        UserFeedbackAttachment attachment = new UserFeedbackAttachment();
        attachment.setFeedbackId(feedbackId);
        attachment.setFileName(originalName);
        attachment.setFilePath(fullPath);
        attachment.setFileUrl("/api/feedbacks/attachments/file?path=" + relativePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileSuffix(suffix.toLowerCase());
        attachment.setContentType(file.getContentType());
        attachment.setSortNo(index);
        return attachment;
    }

    private String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private long countByStatus(Integer status) {
        return feedbackMapper.selectCount(
            new LambdaQueryWrapper<UserFeedback>().eq(UserFeedback::getStatus, status)
        );
    }

    private String currentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) return auth.getName();
        } catch (Exception ignored) {}
        return "system";
    }

    private String generateFeedbackNo() {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = String.valueOf((int) (Math.random() * 9000) + 1000);
        return "FB" + timeStr + rand;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (!StringUtils.hasText(dateTimeStr)) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }
}
