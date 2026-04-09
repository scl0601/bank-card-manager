package com.bank.admin.module.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 日程事项查询 DTO
 */
@Data
@Schema(description = "日程事项查询条件")
public class CalendarEventQueryDTO {

    @Schema(description = "月份（yyyy-MM）", example = "2026-04")
    private String month;

    @Schema(description = "日期", example = "2026-04-09")
    private LocalDate eventDate;

    @Schema(description = "分类：0工作 1生活 2学习 3健康 4其他")
    private Integer category;

    @Schema(description = "状态：0待办 1进行中 2已完成 3已取消")
    private Integer status;
}
