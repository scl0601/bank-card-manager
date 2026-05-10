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
    private BigDecimal totalAmount;
    private Integer billDay;
    private Integer repaymentDay;
    private BigDecimal repaymentFee;
    private BigDecimal consumeFee;
    private BigDecimal interestAmount;
    private BigDecimal lateFeeAmount;
    private BigDecimal installmentFeeAmount;
    private BigDecimal totalProfitAmount;
}
