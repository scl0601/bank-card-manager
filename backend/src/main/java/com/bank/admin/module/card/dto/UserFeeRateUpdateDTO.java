package com.bank.admin.module.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 手续费率修改DTO
 */
@Data
@Schema(description = "手续费率修改参数")
public class UserFeeRateUpdateDTO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "新手续费率%", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "手续费率不能为空")
    private BigDecimal feeRate;
}
