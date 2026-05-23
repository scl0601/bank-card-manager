package com.bank.admin.module.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "记账账户保存参数")
public class BookAccountSaveDTO {

    private Long id;

    @NotBlank(message = "账户名称不能为空")
    private String name;

    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    private BigDecimal initialBalance;

    private BigDecimal currentBalance;

    private Integer status;

    private Integer sortOrder;

    private String remark;
}
