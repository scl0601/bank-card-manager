package com.bank.admin.module.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 流水新增DTO
 */
@Data
@Schema(description = "流水保存参数")
public class CardTransactionSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "银行卡ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "银行卡ID不能为空")
    private Long cardId;

    @Schema(description = "交易类型：1收入 2支出", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易类型不能为空")
    private Integer txType;

    @Schema(description = "交易金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "交易日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易日期不能为空")
    private LocalDate txDate;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "交易对手方")
    private String counterpart;

    @Schema(description = "备注")
    private String remark;
}
