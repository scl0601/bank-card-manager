package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "特殊账单按年份之后批量删除参数")
public class SpecialBillAfterYearDeleteDTO {

    @Schema(description = "特殊银行卡ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "银行卡不能为空")
    private Long cardId;

    @Schema(description = "保留截止年份，删除该年份之后的账单", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "保留截止年份不能为空")
    private Integer afterYear;
}
