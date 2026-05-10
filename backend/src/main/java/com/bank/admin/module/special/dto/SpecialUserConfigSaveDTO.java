package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "特殊通道用户配置保存参数")
public class SpecialUserConfigSaveDTO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "特殊用户不能为空")
    private Long userId;

    @Schema(description = "备注")
    private String remark;
}
