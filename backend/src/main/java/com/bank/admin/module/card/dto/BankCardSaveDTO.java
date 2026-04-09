package com.bank.admin.module.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 银行卡新增/编辑DTO
 */
@Data
@Schema(description = "银行卡保存参数")
public class BankCardSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "持卡人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "持卡人ID不能为空")
    private Long ownerId;

    @Schema(description = "银行名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @Schema(description = "卡号（完整，编辑时可不传）")
    private String cardNo;

    @Schema(description = "卡片类型：1借记卡 2信用卡", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "卡片类型不能为空")
    private Integer cardType;

    @Schema(description = "信用额度")
    private BigDecimal creditLimit;

    @Schema(description = "当前余额")
    private BigDecimal balance;

    @Schema(description = "已用额度")
    private BigDecimal usedAmount;

    @Schema(description = "账单日（每月几号）")
    private Integer billDay;

    @Schema(description = "还款日（每月几号）")
    private Integer repayDay;

    @Schema(description = "有效期截止（格式：YYYY-MM）")
    private String expireDate;

    @Schema(description = "状态：0正常 1冻结 2注销")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
