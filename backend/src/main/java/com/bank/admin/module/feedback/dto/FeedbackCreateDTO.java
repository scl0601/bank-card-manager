package com.bank.admin.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交反馈 DTO（JSON部分，附件通过 multipart 上传）
 */
@Data
@Schema(description = "提交反馈参数")
public class FeedbackCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不超过200字")
    @Schema(description = "问题标题", required = true)
    private String title;

    @NotBlank(message = "详细描述不能为空")
    @Size(max = 5000, message = "描述不超过5000字")
    @Schema(description = "详细描述", required = true)
    private String content;

    @NotNull(message = "反馈类型不能为空")
    @Schema(description = "反馈类型：0功能异常 1界面体验 2数据问题 3权限问题 4其他", required = true)
    private Integer feedbackType;

    @NotNull(message = "优先级不能为空")
    @Schema(description = "优先级：0低 1中 2高 3紧急", required = true)
    private Integer priority;
}
