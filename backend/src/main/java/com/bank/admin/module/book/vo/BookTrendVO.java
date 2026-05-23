package com.bank.admin.module.book.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookTrendVO {
    private String date;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;
}
