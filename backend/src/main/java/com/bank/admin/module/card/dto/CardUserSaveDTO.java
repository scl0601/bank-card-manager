package com.bank.admin.module.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户新增/编辑DTO
 */
@Data
@Schema(description = "用户保存参数")
public class CardUserSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "父用户ID（不填则为一级用户）")
    private Long parentId;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "手续费率%（仅一级用户可设）")
    private BigDecimal feeRate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;
}
