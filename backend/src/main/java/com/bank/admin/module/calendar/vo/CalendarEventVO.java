package com.bank.admin.module.calendar.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 日程事项 VO
 */
@Data
@Schema(description = "日程事项信息")
public class CalendarEventVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述内容")
    private String description;

    @Schema(description = "事项日期")
    private LocalDate eventDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "分类：0工作 1生活 2学习 3健康 4其他")
    private Integer category;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "优先级：0低 1中 2高")
    private Integer priority;

    @Schema(description = "状态：0待办 1进行中 2已完成 3已取消")
    private Integer status;

    @Schema(description = "提前提醒分钟数")
    private Integer remindBeforeMin;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
