package com.bank.admin.module.profit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 银行卡收益统计 VO
 */
@Data
@Schema(description = "银行卡收益统计")
public class CardProfitVO {

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "一级用户ID")
    private Long userId;

    @Schema(description = "一级用户名称")
    private String userName;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名")
    private String ownerName;

    @Schema(description = "归属人关系")
    private String ownerRelation;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "账单数量")
    private Long billCount;

    @Schema(description = "代还总金额")
    private BigDecimal totalBillAmount;

    @Schema(description = "手续费总收入")
    private BigDecimal totalFeeAmount;

    @Schema(description = "POS总成本")
    private BigDecimal totalPosCostAmount;

    @Schema(description = "净利润总额")
    private BigDecimal totalNetProfit;
}
