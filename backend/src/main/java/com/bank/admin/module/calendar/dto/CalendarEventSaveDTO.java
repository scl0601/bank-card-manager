package com.bank.admin.module.calendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 日程事项保存 DTO
 */
@Data
@Schema(description = "日程事项保存请求体")
public class CalendarEventSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "标题", example = "团队周会")
    private String title;

    @Schema(description = "描述内容")
    private String description;

    @NotNull(message = "日期不能为空")
    @Schema(description = "事项日期", example = "2026-04-09")
    private LocalDate eventDate;

    @Schema(description = "开始时间", example = "10:00")
    private LocalTime startTime;

    @Schema(description = "结束时间", example = "11:00")
    private LocalTime endTime;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类：0工作 1生活 2学习 3健康 4其他")
    private Integer category;

    @Schema(description = "优先级：0低 1中 2高", example = "1")
    private Integer priority = 1;

    @Schema(description = "状态：0待办 1进行中 2已完成 3已取消", example = "0")
    private Integer status = 0;

    @Schema(description = "提前提醒分钟数（null不提醒）")
    private Integer remindBeforeMin;

    @Schema(description = "备注")
    private String remark;
}
