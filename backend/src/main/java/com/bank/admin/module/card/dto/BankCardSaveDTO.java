package com.bank.admin.module.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 银行卡新增/编辑DTO
 */
@Data
@Schema(description = "银行卡保存参数")
public class BankCardSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户不能为空")
    private Long userId;

    @Schema(description = "银行名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @Schema(description = "卡号后四位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "卡号后四位不能为空")
    private String cardNoLast4;

    @Schema(description = "卡片归属人关系：本人/配偶/子女等")
    private String ownerRelation;

    @Schema(description = "卡片归属人姓名（可选）")
    private String ownerName;

    @Schema(description = "卡片类型：1借记卡 2信用卡", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "卡片类型不能为空")
    private Integer cardType;

    @Schema(description = "信用额度")
    private String creditLimit;

    @Schema(description = "当前余额")
    private String balance;

    @Schema(description = "总额度（借记卡用）")
    private String totalLimit;

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

    @Schema(description = "还款方式：cloudpay云闪付 invoice开票")
    private String repayMethod;

    @Schema(description = "是否核实（云闪付时有效）")
    private Boolean verified;
}
