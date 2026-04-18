package com.bank.admin.module.calendar.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 日历年度统计 VO
 */
@Data
@Schema(description = "日历年度统计数据")
public class CalendarYearStatsVO {

    @Schema(description = "年份")
    private String year;

    @Schema(description = "待办数量")
    private Long todoCount;

    @Schema(description = "进行中数量")
    private Long doingCount;

    @Schema(description = "已完成数量")
    private Long doneCount;

    @Schema(description = "已取消数量")
    private Long cancelledCount;

    @Schema(description = "总数量")
    private Long totalCount;

    @Schema(description = "完成率")
    private Double doneRate;

    @Schema(description = "月度分布数据")
    private List<MonthStatItem> monthStats;

    @Data
    public static class MonthStatItem {
        @Schema(description = "月份 1-12")
        private Integer month;

        @Schema(description = "该月总事项数")
        private Long count;
    }
}
