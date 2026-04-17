package com.bank.admin.module.feedback.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户反馈主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_feedback")
@Schema(description = "用户反馈")
public class UserFeedback extends BaseEntity {

    @Schema(description = "反馈编号")
    private String feedbackNo;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "详细描述")
    private String content;

    /**
     * 类型：0功能异常 1界面体验 2数据问题 3权限问题 4其他
     */
    @Schema(description = "反馈类型：0功能异常 1界面体验 2数据问题 3权限问题 4其他")
    private Integer feedbackType;

    /**
     * 优先级：0低 1中 2高 3紧急
     */
    @Schema(description = "优先级：0低 1中 2高 3紧急")
    private Integer priority;

    /**
     * 状态：0待处理 1修复中 2已解决 3已关闭
     */
    @Schema(description = "状态：0待处理 1修复中 2已解决 3已关闭")
    private Integer status;

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
}
