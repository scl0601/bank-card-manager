package com.bank.admin.module.reminder.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 提醒任务分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "提醒任务查询参数")
public class ReminderTaskQueryDTO extends PageDTO {

    @Schema(description = "持卡人姓名（模糊）")
    private String ownerName;

    @Schema(description = "提醒类型：1即将到期 2今日到期 3已逾期 4卡片即将过期")
    private Integer reminderType;

    @Schema(description = "状态：0待处理 1已处理 2已忽略")
    private Integer status;

    @Schema(description = "提醒日期 开始")
    private LocalDate reminderDateStart;

    @Schema(description = "提醒日期 结束")
    private LocalDate reminderDateEnd;
}
