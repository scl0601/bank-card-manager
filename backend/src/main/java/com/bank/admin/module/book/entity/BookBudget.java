package com.bank.admin.module.book.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Monthly bookkeeping budget.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book_budget")
public class BookBudget extends BaseEntity {

    /** Budget month in yyyy-MM format. */
    private String budgetMonth;

    /** Category id. Null means total monthly budget. */
    private Long categoryId;

    /** Budget amount. */
    private BigDecimal amount;

    /** Remark. */
    private String remark;
}
