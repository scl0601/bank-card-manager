package com.bank.admin.module.reminder.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reminder_task")
public class ReminderTask extends BaseEntity {

    /** 持卡人ID */
    private Long ownerId;

    /** 关联银行卡ID（可空） */
    private Long cardId;

    /** 关联账单ID（可空） */
    private Long billId;

    /**
     * 提醒类型
     * 1=即将到期 2=今日到期 3=已逾期 4=卡片即将过期
     */
    private Integer reminderType;

    /** 提醒触发日期 */
    private LocalDate reminderDate;

    /** 提醒内容 */
    private String content;

    /**
     * 状态
     * 0=待处理 1=已处理 2=已忽略
     */
    private Integer status;

    /** 处理/忽略时间 */
    private LocalDateTime handledTime;

    /** 处理人 */
    private String handledBy;
}
