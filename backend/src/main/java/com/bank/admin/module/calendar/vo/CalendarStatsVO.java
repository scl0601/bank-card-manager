package com.bank.admin.module.calendar.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 日历统计 VO
 */
@Data
@Schema(description = "日历统计数据")
public class CalendarStatsVO {

    @Schema(description = "待办数量")
    private Long todoCount;

    @Schema(description = "进行中数量")
    private Long doingCount;

    @Schema(description = "已完成数量")
    private Long doneCount;
}
