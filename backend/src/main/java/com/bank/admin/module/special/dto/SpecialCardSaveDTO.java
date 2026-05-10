package com.bank.admin.module.special.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "特殊银行卡保存参数")
public class SpecialCardSaveDTO {

    @Schema(description = "ID，编辑时传入")
    private Long id;

    @Schema(description = "用户ID，不传时使用特殊通道配置用户")
    private Long userId;

    @Schema(description = "银行名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @Schema(description = "卡号后四位 / 卡片标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "卡号后四位不能为空")
    private String cardNoLast4;

    @Schema(description = "总金额：卡片总额度")
    private BigDecimal totalAmount;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
