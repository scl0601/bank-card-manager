package com.bank.admin.module.bill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单明细 新增/编辑DTO
 */
@Data
@Schema(description = "账单明细保存参数")
public class BillDetailSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "关联账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关联账单不能为空")
    private Long billId;

    @Schema(description = "明细日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "明细日期不能为空")
    private LocalDate detailDate;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "描述不能为空")
    private String description;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "明细类型：0=消费 1=还款", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "明细类型不能为空")
    private Integer detailType;

    @Schema(description = "备注")
    private String remark;
}
