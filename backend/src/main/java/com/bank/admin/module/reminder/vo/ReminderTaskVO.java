package com.bank.admin.module.reminder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒任务VO（列表/详情展示）
 */
@Data
@Schema(description = "提醒任务VO")
public class ReminderTaskVO {

    @Schema(description = "提醒任务ID")
    private Long id;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名")
    private String ownerName;

    @Schema(description = "关联银行卡ID")
    private Long cardId;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "关联账单ID")
    private Long billId;

    @Schema(description = "账单月份，格式 yyyy-MM")
    private String billMonth;

    @Schema(description = "提醒类型：1即将到期 2今日到期 3已逾期 4卡片即将过期")
    private Integer reminderType;

    @Schema(description = "提醒类型描述")
    private String reminderTypeDesc;

    @Schema(description = "提醒触发日期")
    private LocalDate reminderDate;

    @Schema(description = "提醒内容")
    private String content;

    @Schema(description = "状态：0待处理 1已处理 2已忽略")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    @Schema(description = "处理人")
    private String handledBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
