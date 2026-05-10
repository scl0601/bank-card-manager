package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "特殊收益额外费用编辑参数")
public class SpecialProfitExtraFeeUpdateDTO {

    @Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账单ID不能为空")
    private Long billId;

    @Schema(description = "利息")
    private BigDecimal interestAmount;

    @Schema(description = "滞纳金")
    private BigDecimal lateFeeAmount;

    @Schema(description = "分期费")
    private BigDecimal installmentFeeAmount;
}
