package com.bank.admin.module.book.dto;

import com.bank.admin.common.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/**
 * 记账分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "记账查询参数")
public class BookQueryDTO extends PageDTO {

    @Schema(description = "记账类型：1收入 2支出")
    private Integer bookType;

    @Schema(description = "分类ID列表（多选）")
    private List<Long> categoryIds;

    @Schema(description = "关联银行卡ID")
    private Long cardId;

    @Schema(description = "记账日期 开始")
    private LocalDate bookDateStart;

    @Schema(description = "记账日期 结束")
    private LocalDate bookDateEnd;
}
