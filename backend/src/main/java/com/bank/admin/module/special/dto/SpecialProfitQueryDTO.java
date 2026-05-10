package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "特殊收益统计查询参数")
public class SpecialProfitQueryDTO {

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "统计年份")
    private Integer year;

    @Schema(description = "统计月份")
    private Integer month;
}
