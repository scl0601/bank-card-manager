package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpecialCardVO {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal feeRate;
    private String bankName;
    private String cardNoLast4;
    private BigDecimal totalAmount;
    private String expireDate;
    private Integer billDay;
    private Integer repaymentDay;
    private Integer status;
    private String statusDesc;
    private String remark;
    private Long billCount;
    private LocalDateTime createTime;
}
