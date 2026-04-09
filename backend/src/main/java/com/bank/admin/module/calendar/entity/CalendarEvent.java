package com.bank.admin.module.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bank.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 日程事项实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("calendar_event")
public class CalendarEvent extends BaseEntity {

    /** 标题 */
    private String title;

    /** 描述内容 */
    private String description;

    /** 事项日期 */
    private LocalDate eventDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /**
     * 分类
     * 0=工作 1=生活 2=学习 3=健康 4=其他
     */
    private Integer category;

    /**
     * 优先级
     * 0=低 1=中 2=高
     */
    private Integer priority;

    /**
     * 状态
     * 0=待办 1=进行中 2=已完成 3=已取消
     */
    private Integer status;

    /** 提前提醒分钟数（null=不提醒） */
    private Integer remindBeforeMin;

    /** 备注 */
    private String remark;
}
