package com.bank.admin.module.bill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单新增/编辑DTO
 */
@Data
@Schema(description = "账单保存参数")
public class CardBillSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "银行卡ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "银行卡ID不能为空")
    private Long cardId;

    @Schema(description = "账单月份（yyyy-MM）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账单月份不能为空")
    private String billMonth;

    @Schema(description = "账单金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账单金额不能为空")
    private BigDecimal billAmount;

    @Schema(description = "最低还款额")
    private BigDecimal minPayAmount;

    @Schema(description = "还款日")
    private LocalDate repayDate;

    @Schema(description = "实际还款金额")
    private BigDecimal actualPayAmount;

    @Schema(description = "实际还款日期")
    private LocalDate actualPayDate;

    @Schema(description = "状态：0待还款 1已还清 2部分还款 3逾期")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
