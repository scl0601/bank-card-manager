package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记账VO
 */
@Data
@Schema(description = "记账VO")
public class BookVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "记账日期")
    private LocalDate bookDate;

    @Schema(description = "记账类型：1收入 2支出")
    private Integer bookType;

    @Schema(description = "记账类型描述")
    private String bookTypeDesc;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "描述/备注")
    private String description;

    @Schema(description = "关联银行卡ID")
    private Long cardId;

    @Schema(description = "卡号后四位")
    private String cardNoLast4;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
