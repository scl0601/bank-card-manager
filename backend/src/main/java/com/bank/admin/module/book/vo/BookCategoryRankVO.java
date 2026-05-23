package com.bank.admin.module.book.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookCategoryRankVO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount = BigDecimal.ZERO;
    private Integer count = 0;
}
