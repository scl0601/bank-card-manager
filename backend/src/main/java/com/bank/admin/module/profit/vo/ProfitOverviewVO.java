package com.bank.admin.module.profit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收益总览 VO
 */
@Data
@Schema(description = "收益总览")
public class ProfitOverviewVO {

    @Schema(description = "统计年份")
    private Integer year;

    @Schema(description = "统计月份")
    private Integer month;

    @Schema(description = "用户数")
    private Long userCount;

    @Schema(description = "银行卡数")
    private Long cardCount;

    @Schema(description = "代还总金额")
    private BigDecimal totalBillAmount;

    @Schema(description = "手续费总收入")
    private BigDecimal totalFeeAmount;

    @Schema(description = "POS总成本")
    private BigDecimal totalPosCostAmount;

    @Schema(description = "其他费用总额")
    private BigDecimal totalOtherFeeAmount;

    @Schema(description = "净利润总额")
    private BigDecimal totalNetProfit;
}
