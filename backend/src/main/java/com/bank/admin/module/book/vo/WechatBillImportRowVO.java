package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "WeChat bill import preview row")
public class WechatBillImportRowVO {

    private Integer rowNo;
    private String tradeTime;
    private LocalDate bookDate;
    private LocalTime bookTime;
    private String tradeType;
    private String counterparty;
    private String product;
    private String incomeExpense;
    private Integer bookType;
    private String bookTypeDesc;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String tradeNo;
    private String merchantTradeNo;
    private String remark;
    private Long categoryId;
    private String categoryName;
    private Long accountId;
    private String accountName;
    private Long targetAccountId;
    private String targetAccountName;
    private String sourceHash;
    private String importStatus;
    private String importStatusDesc;
    private String message;
}
