package com.bank.admin.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑反馈 DTO（JSON部分，附件通过 multipart 上传）
 */
@Data
@Schema(description = "编辑反馈参数")
public class FeedbackUpdateDTO {

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

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0待处理 1修复中 2已解决 3已关闭", required = true)
    private Integer status;

    @NotBlank(message = "提交人不能为空")
    @Size(max = 50, message = "提交人不超过50字")
    @Schema(description = "提交人", required = true)
    private String submitter;

    @Size(max = 50, message = "处理人不超过50字")
    @Schema(description = "处理人")
    private String assignee;

    @Size(max = 500, message = "最近备注不超过500字")
    @Schema(description = "最近处理备注")
    private String latestRemark;

    @Schema(description = "解决时间，格式：yyyy-MM-dd HH:mm:ss")
    private String resolvedTime;

    @Schema(description = "关闭时间，格式：yyyy-MM-dd HH:mm:ss")
    private String closedTime;

    @Schema(description = "要删除的附件ID列表")
    private List<Long> deleteAttachmentIds;
}
