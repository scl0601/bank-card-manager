package com.bank.admin.module.special.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SpecialBillImportResultVO {
    private Integer year;
    private Integer updatedRows;
    private Integer importedCardCount;
    private List<String> importedCardNames = new ArrayList<>();
    private List<String> skippedCardNames = new ArrayList<>();
    private BigDecimal totalBillAmount;
    private BigDecimal totalXiaohuanRepayAmount;
    private BigDecimal totalXiaohuanConsumeAmount;
    private BigDecimal totalRepaymentFee;
    private BigDecimal totalConsumeFee;
    private BigDecimal totalProfitAmount;
    private List<String> warnings = new ArrayList<>();
}
