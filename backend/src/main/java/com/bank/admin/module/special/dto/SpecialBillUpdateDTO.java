package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "特殊账单编辑参数")
public class SpecialBillUpdateDTO {

    @Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账单ID不能为空")
    private Long id;

    @Schema(description = "历史兼容字段，当前页面使用银行卡卡片额度展示")
    private BigDecimal monthlyTotalBillAmount;

    @Schema(description = "账单日")
    private Integer billDay;

    @Schema(description = "还款日")
    private Integer repaymentDay;

    @Schema(description = "本月账单金额")
    private BigDecimal billAmount;

    @Schema(description = "本月账单金额是否核实")
    private Boolean billAmountVerified;

    @Schema(description = "小焕还款")
    private BigDecimal xiaohuanRepayAmount;

    @Schema(description = "小焕还款是否核实")
    private Boolean xiaohuanRepayVerified;

    @Schema(description = "客户还款")
    private BigDecimal customerRepayAmount;

    @Schema(description = "客户还款是否核实")
    private Boolean customerRepayVerified;

    @Schema(description = "小焕消费")
    private BigDecimal xiaohuanConsumeAmount;

    @Schema(description = "小焕消费是否核实")
    private Boolean xiaohuanConsumeVerified;

    @Schema(description = "客户需要")
    private BigDecimal customerNeedAmount;

    @Schema(description = "客户需要是否核实")
    private Boolean customerNeedVerified;

    @Schema(description = "客户消费")
    private BigDecimal customerConsumeAmount;

    @Schema(description = "客户消费是否核实")
    private Boolean customerConsumeVerified;

    @Schema(description = "余额")
    private BigDecimal balance;

    @Schema(description = "利息")
    private BigDecimal interestAmount;

    @Schema(description = "滞纳金")
    private BigDecimal lateFeeAmount;

    @Schema(description = "分期费")
    private BigDecimal installmentFeeAmount;

    @Schema(description = "备注")
    private String remark;
}
