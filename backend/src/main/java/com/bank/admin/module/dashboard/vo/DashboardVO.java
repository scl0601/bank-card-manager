package com.bank.admin.module.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 首页看板统计VO
 */
@Data
@Schema(description = "首页看板VO")
public class DashboardVO {

    @Schema(description = "持卡人总数")
    private Long totalOwners;

    @Schema(description = "银行卡总数")
    private Long totalCards;

    @Schema(description = "信用卡数量")
    private Long creditCardCount;

    @Schema(description = "借记卡数量")
    private Long debitCardCount;

    @Schema(description = "待处理提醒数")
    private Long pendingReminderCount;

    @Schema(description = "逾期账单数")
    private Long overdueBillCount;

    @Schema(description = "本月收入合计")
    private BigDecimal monthlyIncome;

    @Schema(description = "本月支出合计")
    private BigDecimal monthlyExpense;

    @Schema(description = "即将到期提醒数（7天内）")
    private Long upcomingCount;

    @Schema(description = "今日到期提醒数")
    private Long todayDueCount;

    @Schema(description = "近7日每日收支趋势（key: date, income, expense）")
    private List<Map<String, Object>> dailyTrend;

    @Schema(description = "最近操作日志（5条）")
    private List<RecentLogVO> recentLogs;

    @Schema(description = "即将到期账单 Top 5")
    private List<UpcomingBillVO> upcomingBills;

    @Schema(description = "银行分布统计")
    private List<BankDistVO> bankDistribution;

    /**
     * 银行分布统计 VO
     */
    @Data
    public static class BankDistVO {
        private String bankName;
        private Long cardCount;
    }

    /**
     * 最近操作日志 VO
     */
    @Data
    public static class RecentLogVO {
        private Long id;
        private String operator;
        private String module;
        private String action;
        private Integer result;
        private String createTime;
    }

    /**
     * 即将到期账单 VO
     */
    @Data
    public static class UpcomingBillVO {
        private Long id;
        private String ownerName;
        private String bankName;
        private String cardNoLast4;
        private String billMonth;
        private BigDecimal billAmount;
        private LocalDate repayDate;
        private Integer status;
    }
}
