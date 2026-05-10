package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpecialBillVO {
    private Long id;
    private Long cardId;
    private Long userId;
    private String userName;
    private String bankName;
    private String cardNoLast4;
    private BigDecimal totalAmount;
    private String billMonth;
    private Integer billYear;
    private Integer billMonthNo;
    private BigDecimal monthlyTotalBillAmount;
    private Integer billDay;
    private Integer repaymentDay;
    private BigDecimal billAmount;
    private Boolean billAmountVerified;
    private BigDecimal xiaohuanRepayAmount;
    private Boolean xiaohuanRepayVerified;
    private BigDecimal customerRepayAmount;
    private Boolean customerRepayVerified;
    private BigDecimal xiaohuanConsumeAmount;
    private Boolean xiaohuanConsumeVerified;
    private BigDecimal customerNeedAmount;
    private Boolean customerNeedVerified;
    private BigDecimal customerConsumeAmount;
    private Boolean customerConsumeVerified;
    private BigDecimal diffAmount;
    private BigDecimal balance;
    private BigDecimal feeRate;
    private BigDecimal repaymentFee;
    private BigDecimal consumeFee;
    private BigDecimal interestAmount;
    private BigDecimal lateFeeAmount;
    private BigDecimal installmentFeeAmount;
    private BigDecimal profitTotalAmount;
    private String remark;
    private LocalDateTime updateTime;
}
