package com.bank.admin.module.book.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账分类VO
 */
@Data
@Schema(description = "记账分类VO")
public class BookCategoryVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标标识")
    private String icon;

    @Schema(description = "分类类型：1收入 2支出")
    private Integer type;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "状态：0启用 1停用")
    private Integer status;

    @Schema(description = "子分类列表")
    private List<BookCategoryVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
