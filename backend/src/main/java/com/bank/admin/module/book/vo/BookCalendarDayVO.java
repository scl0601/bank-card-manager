package com.bank.admin.module.book.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookCalendarDayVO {
    private String date;
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;
    private Integer count = 0;
}
