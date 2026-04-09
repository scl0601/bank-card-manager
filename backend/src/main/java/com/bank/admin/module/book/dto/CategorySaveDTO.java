package com.bank.admin.module.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 记账分类保存DTO
 */
@Data
@Schema(description = "记账分类保存参数")
public class CategorySaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "图标标识")
    private String icon;

    @Schema(description = "分类类型：1收入 2支出", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类类型不能为空")
    private Integer type;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "父级ID（0=一级分类）")
    private Long parentId;

    @Schema(description = "状态：0启用 1停用")
    private Integer status;
}
