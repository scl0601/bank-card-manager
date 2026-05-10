package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecialConfigVO {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal feeRate;
    private String phone;
    private String remark;
}
