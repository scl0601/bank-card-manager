package com.bank.admin.module.profit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度收益统计 VO
 */
@Data
@Schema(description = "月度收益统计")
public class MonthlyProfitVO {

    @Schema(description = "账单月份")
    private String billMonth;

    @Schema(description = "用户数")
    private Long userCount;

    @Schema(description = "银行卡数")
    private Long cardCount;

    @Schema(description = "账单数量")
    private Long billCount;

    @Schema(description = "代还总金额")
    private BigDecimal totalBillAmount;

    @Schema(description = "平均手续费率")
    private BigDecimal avgFeeRate;

    @Schema(description = "手续费总收入")
    private BigDecimal totalFeeAmount;

    @Schema(description = "手续费已支付账单数")
    private Long feePaidCount;

    @Schema(description = "手续费未支付账单数")
    private Long feeUnpaidCount;

    @Schema(description = "POS总成本")
    private BigDecimal totalPosCostAmount;

    @Schema(description = "其他费用总额")
    private BigDecimal totalOtherFeeAmount;

    @Schema(description = "净利润总额")
    private BigDecimal totalNetProfit;
}
