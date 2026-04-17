package com.bank.admin.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改反馈状态 DTO
 */
@Data
@Schema(description = "修改反馈状态参数")
public class FeedbackStatusUpdateDTO {

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态：0待处理 1修复中 2已解决 3已关闭", required = true)
    private Integer status;

    @NotBlank
    @Size(max = 500, message = "备注不超过500字")
    @Schema(description = "处理备注（状态变更时必填）", required = true)
    private String remark;
}
