package com.bank.admin.module.profit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户收益统计 VO
 */
@Data
@Schema(description = "用户收益统计")
public class UserProfitVO {

    @Schema(description = "一级用户ID")
    private Long userId;

    @Schema(description = "一级用户名称")
    private String userName;

    @Schema(description = "银行卡数量")
    private Long cardCount;

    @Schema(description = "账单月份数量")
    private Long billMonthCount;

    @Schema(description = "代还总金额")
    private BigDecimal totalBillAmount;

    @Schema(description = "手续费总收入")
    private BigDecimal totalFeeAmount;

    @Schema(description = "POS总成本")
    private BigDecimal totalPosCostAmount;

    @Schema(description = "净利润总额")
    private BigDecimal totalNetProfit;
}
