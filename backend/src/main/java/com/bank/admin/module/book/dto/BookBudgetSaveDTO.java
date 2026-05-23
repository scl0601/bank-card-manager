package com.bank.admin.module.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "记账预算保存参数")
public class BookBudgetSaveDTO {

    private Long id;

    @NotBlank(message = "预算月份不能为空")
    private String budgetMonth;

    /** Null means total monthly budget. */
    private Long categoryId;

    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0.01", message = "预算金额必须大于0")
    private BigDecimal amount;

    private String remark;
}
