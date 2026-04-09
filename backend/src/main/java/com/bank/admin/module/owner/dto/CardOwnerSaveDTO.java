package com.bank.admin.module.owner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 持卡人新增/编辑DTO
 */
@Data
@Schema(description = "持卡人保存参数")
public class CardOwnerSaveDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：0正常 1禁用")
    private Integer status;
}
