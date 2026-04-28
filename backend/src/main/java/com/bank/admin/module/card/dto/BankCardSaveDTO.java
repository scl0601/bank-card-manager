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

    @Schema(description = "卡片类型：1借记卡 2信用卡", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "卡片类型不能为空")
    private Integer cardType;

    @Schema(description = "信用额度")
    private String creditLimit;

    @Schema(description = "账单日（每月几号）")
    private Integer billDay;

    @Schema(description = "还款日（每月几号）")
    private Integer repayDay;

    @Schema(description = "有效期截止（原样保存用户输入，如 06/28、06-28、06月28年）")
    private String expireDate;

    @Schema(description = "状态：0正常 1冻结 2注销")
    private Integer status;

    @Schema(description = "APP：cloudpay云闪付 wechat微信 alipay支付宝 other其他")
    private String repayMethod;

    @Schema(description = "是否已核实")
    private Boolean verified;

    @Schema(description = "备注")
    private String remark;
}
