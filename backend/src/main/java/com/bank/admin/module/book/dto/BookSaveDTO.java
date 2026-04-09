package com.bank.admin.module.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 记账保存DTO
 */
@Data
@Schema(description = "记账保存参数")
public class BookSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "记账类型：1收入 2支出", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记账类型不能为空")
    private Integer bookType;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "记账日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记账日期不能为空")
    private LocalDate bookDate;

    @Schema(description = "分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Schema(description = "描述/备注")
    private String description;

    @Schema(description = "关联银行卡ID（可选）")
    private Long cardId;
}
