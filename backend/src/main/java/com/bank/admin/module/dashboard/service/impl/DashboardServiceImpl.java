package com.bank.admin.module.dashboard.service.impl;

import com.bank.admin.module.bill.entity.CardBill;
import com.bank.admin.module.bill.mapper.CardBillMapper;
import com.bank.admin.module.card.entity.BankCard;
import com.bank.admin.module.card.entity.CardUser;
import com.bank.admin.module.card.mapper.BankCardMapper;
import com.bank.admin.module.card.mapper.CardUserMapper;
import com.bank.admin.module.dashboard.service.DashboardService;
import com.bank.admin.module.dashboard.vo.DashboardVO;
import com.bank.admin.module.log.entity.OperationLog;
import com.bank.admin.module.log.mapper.OperationLogMapper;
import com.bank.admin.module.reminder.mapper.ReminderTaskMapper;
import com.bank.admin.module.transaction.entity.CardTransaction;
import com.bank.admin.module.transaction.mapper.CardTransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页看板 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CardUserMapper cardUserMapper;
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

        vo.setTotalOwners(cardUserMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setTotalCards(bankCardMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setCreditCardCount(bankCardMapper.selectCount(new LambdaQueryWrapper<BankCard>().eq(BankCard::getCardType, 2)));
        vo.setDebitCardCount(bankCardMapper.selectCount(new LambdaQueryWrapper<BankCard>().eq(BankCard::getCardType, 1)));
        fillCardExpireStats(vo, today);
        vo.setPendingReminderCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)));
        vo.setOverdueBillCount(cardBillMapper.selectCount(new LambdaQueryWrapper<CardBill>().eq(CardBill::getStatus, 3)));

        BigDecimal income = transactionMapper.selectList(
                        new LambdaQueryWrapper<CardTransaction>()
                                .eq(CardTransaction::getTxType, 1)
                                .ge(CardTransaction::getTxDate, monthStart)
                                .le(CardTransaction::getTxDate, today))
                .stream().map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyIncome(income);

        BigDecimal expense = transactionMapper.selectList(
                        new LambdaQueryWrapper<CardTransaction>()
                                .eq(CardTransaction::getTxType, 2)
                                .ge(CardTransaction::getTxDate, monthStart)
                                .le(CardTransaction::getTxDate, today))
                .stream().map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthlyExpense(expense);

        vo.setUpcomingCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getReminderType, 1)));

        vo.setTodayDueCount(reminderTaskMapper.selectCount(
                new LambdaQueryWrapper<com.bank.admin.module.reminder.entity.ReminderTask>()
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getStatus, 0)
                        .eq(com.bank.admin.module.reminder.entity.ReminderTask::getReminderType, 2)));

        vo.setDailyTrend(buildDailyTrend(today));

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
        }).collect(Collectors.toList()));

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

    private void fillCardExpireStats(DashboardVO vo, LocalDate today) {
        int currentIndex = today.getYear() * 12 + today.getMonthValue();

        List<BankCard> cards = bankCardMapper.selectList(
                new LambdaQueryWrapper<BankCard>()
                        .select(BankCard::getId, BankCard::getUserId, BankCard::getBankName,
                                BankCard::getCardNoLast4, BankCard::getCardType,
                                BankCard::getExpireDate, BankCard::getStatus)
                        .orderByAsc(BankCard::getBankName)
                        .orderByAsc(BankCard::getCardNoLast4));
        Map<Long, CardUser> userMap = loadCardUsers(cards);
        List<DashboardVO.CardExpireReminderVO> reminders = new ArrayList<>();

        for (BankCard card : cards) {
            YearMonth expire = parseCardExpireYearMonth(card.getExpireDate());
            if (expire == null) {
                continue;
            }

            int expireIndex = expire.year() * 12 + expire.month();
            int monthsLeft = expireIndex - currentIndex;
            if (monthsLeft < 0) {
                reminders.add(buildCardExpireReminder(card, userMap.get(card.getUserId()), "expired"));
            } else if (monthsLeft <= 1) {
                reminders.add(buildCardExpireReminder(card, userMap.get(card.getUserId()), "soon"));
            }
        }

        long soonCount = reminders.stream().filter(item -> "soon".equals(item.getExpireStatus())).count();
        long expiredCount = reminders.stream().filter(item -> "expired".equals(item.getExpireStatus())).count();
        vo.setCardExpireSoonCount(soonCount);
        vo.setCardExpiredCount(expiredCount);
        vo.setCardExpireReminderCount(soonCount + expiredCount);
        vo.setCardExpireReminders(reminders);
    }

    private Map<Long, CardUser> loadCardUsers(List<BankCard> cards) {
        Set<Long> userIds = cards.stream()
                .map(BankCard::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return cardUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(CardUser::getId, user -> user, (a, b) -> a));
    }

    private DashboardVO.CardExpireReminderVO buildCardExpireReminder(
            BankCard card,
            CardUser user,
            String expireStatus) {
        DashboardVO.CardExpireReminderVO reminder = new DashboardVO.CardExpireReminderVO();
        reminder.setId(card.getId());
        reminder.setUserName(user != null ? user.getName() : "");
        reminder.setBankName(card.getBankName());
        reminder.setCardNoLast4(card.getCardNoLast4());
        reminder.setCardType(card.getCardType());
        reminder.setCardTypeDesc(card.getCardType() != null && card.getCardType() == 2 ? "信用卡" : "借记卡");
        reminder.setExpireDate(card.getExpireDate());
        reminder.setStatus(card.getStatus());
        reminder.setStatusDesc(cardStatusDesc(card.getStatus()));
        reminder.setExpireStatus(expireStatus);
        reminder.setExpireStatusDesc("expired".equals(expireStatus) ? "已过期" : "一个月内到期");
        return reminder;
    }

    private String cardStatusDesc(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "冻结";
            case 2 -> "注销";
            case 3 -> "停用";
            default -> "正常";
        };
    }

    private YearMonth parseCardExpireYearMonth(String expireDate) {
        if (expireDate == null || expireDate.trim().isEmpty()) {
            return null;
        }

        String raw = expireDate.trim();
        String compact = raw.replaceAll("\\D+", "");
        if (compact.length() == 4) {
            return buildYearMonth(compact.substring(2), compact.substring(0, 2));
        }

        String[] parts = raw.split("\\D+");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                values.add(part);
            }
        }
        if (values.size() < 2) {
            return null;
        }

        if (values.get(0).length() == 4) {
            return buildYearMonth(values.get(0), values.get(1));
        }
        return buildYearMonth(values.get(1), values.get(0));
    }

    private YearMonth buildYearMonth(String yearText, String monthText) {
        try {
            int rawYear = Integer.parseInt(yearText);
            int month = Integer.parseInt(monthText);
            if (month < 1 || month > 12) {
                return null;
            }

            int year = rawYear < 100 ? rawYear + 2000 : rawYear;
            return new YearMonth(year, month);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private record YearMonth(int year, int month) {
    }
}
