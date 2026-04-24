package com.bank.admin.module.bill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账单汇总 VO
 */
@Data
@Schema(description = "账单汇总")
public class CardBillOverviewVO {

    @Schema(description = "统计年份")
    private Integer year;

    @Schema(description = "账单数")
    private Long billCount;

    @Schema(description = "待还款数")
    private Long pendingCount;

    @Schema(description = "已还清数")
    private Long repaidCount;

    @Schema(description = "部分还款数")
    private Long partialCount;

    @Schema(description = "逾期数")
    private Long overdueCount;

    @Schema(description = "代还总金额")
    private BigDecimal totalBillAmount;

    @Schema(description = "手续费总收入")
    private BigDecimal totalFeeAmount;

    @Schema(description = "POS总成本")
    private BigDecimal totalPosCostAmount;

    @Schema(description = "净利润总额")
    private BigDecimal totalNetProfit;
}
