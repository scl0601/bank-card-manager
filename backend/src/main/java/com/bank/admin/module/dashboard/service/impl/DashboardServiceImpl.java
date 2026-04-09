package com.bank.admin.module.dashboard.service.impl;

import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.dashboard.service.DashboardService;
import com.bank.admin.module.dashboard.vo.DashboardVO;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.mapper.OperationLogMapper;
import com.bank.admin.module.owner.mapper.CardOwnerMapper;
import com.bank.admin.module.reminder.mapper.ReminderTaskMapper;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页看板 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CardOwnerMapper cardOwnerMapper;
    private final BankCardMapper bankCardMapper;
    private final CardTransactionMapper transactionMapper;
    private final CardBillMapper cardBillMapper;
    private final ReminderTaskMapper reminderTaskMapper;
    private final OperationLogMapper operationLogMapper;

    @Override
    public DashboardVO getStats() {
        DashboardVO vo = new DashboardVO();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        // 持卡人总数
        vo.setTotalOwners(cardOwnerMapper.selectCount(
                new LambdaQueryWrapper<>()));

        // 银行卡总数
        vo.setTotalCards(bankCardMapper.selectCount(
                new LambdaQueryWrapper<>()));

        // 信用卡数量
        vo.setCreditCardCount(bankCardMapper.selectCount(
                new LambdaQueryWrapper<BankCard>().eq(BankCard::getCardType, 2)));

        // 借记卡数量
        vo.setDebitCardCount(bankCardMapper.selectCount(
                new LambdaQueryWrapper<BankCard>().eq(BankCard::getCardType, 1)));

        // 待处理提醒数
        vo.setPendingReminderCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)));

        // 逾期账单数
        vo.setOverdueBillCount(cardBillMapper.selectCount(
                new LambdaQueryWrapper<CardBill>().eq(CardBill::getStatus, 3)));

        // 本月收入
        BigDecimal income = transactionMapper.selectList(
                        new LambdaQueryWrapper<CardTransaction>()
                                .eq(CardTransaction::getTxType, 1)
                                .ge(CardTransaction::getTxDate, monthStart)
                                .le(CardTransaction::getTxDate, today))
                .stream().map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyIncome(income);

        // 本月支出
        BigDecimal expense = transactionMapper.selectList(
                        new LambdaQueryWrapper<CardTransaction>()
                                .eq(CardTransaction::getTxType, 2)
                                .ge(CardTransaction::getTxDate, monthStart)
                                .le(CardTransaction::getTxDate, today))
                .stream().map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyExpense(expense);

        // 即将到期（7天内）
        vo.setUpcomingCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getReminderType, 1)));

        // 今日到期
        vo.setTodayDueCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getReminderType, 2)));

        // 近7日收支趋势
        vo.setDailyTrend(buildDailyTrend(today));

        // 最近操作日志（5条）
        List<OperationLog> recentLogs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .select(OperationLog::getId, OperationLog::getOperator,
                                OperationLog::getModule, OperationLog::getAction,
                                OperationLog::getResult, OperationLog::getCreateTime)
                        .orderByDesc(OperationLog::getCreateTime)
                        .last("LIMIT 5"));
        vo.setRecentLogs(recentLogs.stream().map(log -> {
            DashboardVO.RecentLogVO vo1 = new DashboardVO.RecentLogVO();
            vo1.setId(log.getId());
            vo1.setOperator(log.getOperator());
            vo1.setModule(log.getModule());
            vo1.setAction(log.getAction());
            vo1.setResult(log.getResult());
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            vo1.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().format(dtf) : "");
            return vo1;
        }).toList());

        // 即将到期账单 Top 5
        List<Map<String, Object>> upcomingBills = cardBillMapper.selectUpcomingBills(today);
        vo.setUpcomingBills(upcomingBills.stream().map(m -> {
            DashboardVO.UpcomingBillVO vo2 = new DashboardVO.UpcomingBillVO();
            vo2.setId(((Number) m.get("id")).longValue());
            vo2.setOwnerName((String) m.get("owner_name"));
            vo2.setBankName((String) m.get("bank_name"));
            vo2.setCardNoLast4((String) m.get("card_no_last4"));
            vo2.setBillMonth((String) m.get("bill_month"));
            vo2.setBillAmount((BigDecimal) m.get("bill_amount"));
            Object rd = m.get("repay_date");
            if (rd instanceof LocalDate ld) {
                vo2.setRepayDate(ld);
            } else if (rd != null) {
                vo2.setRepayDate(LocalDate.parse(rd.toString()));
            }
            vo2.setStatus(((Number) m.get("status")).intValue());
            return vo2;
        }).toList());

        // 银行分布统计
        List<BankCard> allCards = bankCardMapper.selectList(
                new LambdaQueryWrapper<BankCard>()
                        .select(BankCard::getBankName));
        Map<String, Long> bankGroup = allCards.stream()
                .filter(c -> c.getBankName() != null)
                .collect(Collectors.groupingBy(BankCard::getBankName, Collectors.counting()));
        List<DashboardVO.BankDistVO> bankDist = bankGroup.entrySet().stream()
                .map(e -> {
                    DashboardVO.BankDistVO d = new DashboardVO.BankDistVO();
                    d.setBankName(e.getKey());
                    d.setCardCount(e.getValue());
                    return d;
                })
                .sorted((a, b) -> Long.compare(b.getCardCount(), a.getCardCount()))
                .toList();
        vo.setBankDistribution(bankDist);

        return vo;
    }

    private List<Map<String, Object>> buildDailyTrend(LocalDate today) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            BigDecimal dayIncome = transactionMapper.selectList(
                            new LambdaQueryWrapper<CardTransaction>()
                                    .eq(CardTransaction::getTxType, 1)
                                    .eq(CardTransaction::getTxDate, date))
                    .stream().map(CardTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayExpense = transactionMapper.selectList(
                            new LambdaQueryWrapper<CardTransaction>()
                                    .eq(CardTransaction::getTxType, 2)
                                    .eq(CardTransaction::getTxDate, date))
                    .stream().map(CardTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(fmt));
            item.put("income", dayIncome);
            item.put("expense", dayExpense);
            trend.add(item);
        }
        return trend;
    }
}
