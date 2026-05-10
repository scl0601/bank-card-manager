package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecialProfitCardVO {
    private Long cardId;
    private String bankName;
    private String cardNoLast4;
    private BigDecimal totalAmount;
    private Long billCount;
    private BigDecimal totalMonthlyTotalBillAmount;
    private BigDecimal totalBillAmount;
    private BigDecimal totalXiaohuanRepayAmount;
    private BigDecimal totalCustomerRepayAmount;
    private BigDecimal totalXiaohuanConsumeAmount;
    private BigDecimal totalCustomerNeedAmount;
    private BigDecimal totalCustomerConsumeAmount;
    private BigDecimal totalDiffAmount;
    private BigDecimal totalRepaymentFee;
    private BigDecimal totalConsumeFee;
    private BigDecimal totalInterestAmount;
    private BigDecimal totalLateFeeAmount;
    private BigDecimal totalInstallmentFeeAmount;
    private BigDecimal totalProfitAmount;
}
