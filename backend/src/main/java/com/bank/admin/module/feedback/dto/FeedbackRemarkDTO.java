package com.bank.admin.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 追加处理备注 DTO
 */
@Data
@Schema(description = "追加处理备注参数")
public class FeedbackRemarkDTO {

    @NotBlank(message = "备注内容不能为空")
    @Size(max = 1000, message = "备注不超过1000字")
    @Schema(description = "备注内容", required = true)
    private String remark;
}
