package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "记账预算")
public class BookBudgetVO {
    private Long id;
    private String budgetMonth;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercent;
    private String remark;
}
