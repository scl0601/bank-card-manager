package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecialProfitRowVO {
    private Long billId;
    private Long cardId;
    private Integer billYear;
    private Integer billMonthNo;
    private String bankName;
    private String cardNoLast4;
    private Integer cardStatus;
    private BigDecimal totalAmount;
    private BigDecimal xiaohuanRepayAmount;
    private BigDecimal xiaohuanConsumeAmount;
    private Integer billDay;
    private Integer repaymentDay;
    private BigDecimal repaymentFee;
    private BigDecimal consumeFee;
    private BigDecimal interestAmount;
    private BigDecimal lateFeeAmount;
    private BigDecimal installmentFeeAmount;
    private BigDecimal totalProfitAmount;
}
