package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "个人记账工作台概览")
public class BookOverviewVO {
    private String yearMonth;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal budgetUsed = BigDecimal.ZERO;
    private BigDecimal budgetRemaining = BigDecimal.ZERO;
    private BigDecimal budgetUsagePercent = BigDecimal.ZERO;
    private List<BookAccountVO> accounts = new ArrayList<>();
    private List<BookBudgetVO> budgets = new ArrayList<>();
    private List<BookCategoryRankVO> categoryRanks = new ArrayList<>();
}
