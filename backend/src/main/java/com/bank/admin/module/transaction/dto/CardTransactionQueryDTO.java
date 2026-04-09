package com.bank.admin.module.transaction.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 流水分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流水查询参数")
public class CardTransactionQueryDTO extends PageDTO {

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "交易类型：1收入 2支出")
    private Integer txType;

    @Schema(description = "交易日期 开始")
    private LocalDate txDateStart;

    @Schema(description = "交易日期 结束")
    private LocalDate txDateEnd;
}
