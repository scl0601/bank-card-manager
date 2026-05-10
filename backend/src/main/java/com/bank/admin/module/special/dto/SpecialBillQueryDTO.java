package com.bank.admin.module.special.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "特殊账单查询参数")
public class SpecialBillQueryDTO extends PageDTO {

    @Schema(description = "银行卡ID")
    private Long cardId;

    @Schema(description = "统计年份")
    private Integer year;

    @Schema(description = "统计月份")
    private Integer month;

    @Schema(description = "银行名称或卡号关键字")
    private String cardKeyword;
}
