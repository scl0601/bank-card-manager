package com.bank.admin.module.profit.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收益统计查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "收益统计查询参数")
public class ProfitQueryDTO extends PageDTO {

    @Schema(description = "统计年份，为空默认当前年")
    private Integer year;

    @Schema(description = "统计月份，1-12；为空表示全年")
    private Integer month;

    @Schema(description = "一级用户ID")
    private Long userId;

    @Schema(description = "银行卡ID")
    private Long cardId;
}
