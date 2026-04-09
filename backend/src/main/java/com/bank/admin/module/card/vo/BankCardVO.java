package com.bank.admin.module.card.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行卡VO
 */
@Data
@Schema(description = "银行卡VO")
public class BankCardVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "持卡人ID")
    private Long ownerId;

    @Schema(description = "持卡人姓名")
    private String ownerName;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "卡片类型：1借记卡 2信用卡")
    private Integer cardType;

    @Schema(description = "卡片类型描述")
    private String cardTypeDesc;

    @Schema(description = "信用额度")
    private BigDecimal creditLimit;

    @Schema(description = "当前余额")
    private BigDecimal balance;

    @Schema(description = "已用额度")
    private BigDecimal usedAmount;

    @Schema(description = "可用额度")
    private BigDecimal availableAmount;

    @Schema(description = "账单日")
    private Integer billDay;

    @Schema(description = "还款日")
    private Integer repayDay;

    @Schema(description = "有效期截止")
    private String expireDate;

    @Schema(description = "状态：0正常 1冻结 2注销")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
