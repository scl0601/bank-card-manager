package com.bank.admin.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 反馈列表/详情 VO
 */
@Data
@Schema(description = "反馈信息")
public class FeedbackVO {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "反馈编号")
    private String feedbackNo;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "详细描述")
    private String content;

    @Schema(description = "反馈类型")
    private Integer feedbackType;

    @Schema(description = "反馈类型描述")
    private String feedbackTypeDesc;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "优先级描述")
    private String priorityDesc;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "提交人")
    private String submitter;

    @Schema(description = "处理人")
    private String assignee;

    @Schema(description = "最近处理备注")
    private String latestRemark;

    @Schema(description = "附件数量")
    private Integer attachmentCount;

    @Schema(description = "解决时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "关闭时间")
    private LocalDateTime closedTime;

    @Schema(description = "创建时间（提交时间）")
    private LocalDateTime createTime;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "附件列表（详情接口返回）")
    private List<FeedbackAttachmentVO> attachments;

    @Schema(description = "处理轨迹（详情接口返回）")
    private List<FeedbackProcessLogVO> processLogs;
}
