package com.bank.admin.module.bill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账单明细VO
 */
@Data
@Schema(description = "账单明细VO")
public class BillDetailVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "关联账单ID")
    private Long billId;

    @Schema(description = "明细日期")
    private LocalDate detailDate;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "类型：0=支出 1=收入")
    private Integer detailType;

    @Schema(description = "类型描述")
    private String detailTypeDesc;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
