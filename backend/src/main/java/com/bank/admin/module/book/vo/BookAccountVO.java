package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "记账账户")
public class BookAccountVO {
    private Long id;
    private String name;
    private Integer accountType;
    private String accountTypeDesc;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private Integer status;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createTime;
}
