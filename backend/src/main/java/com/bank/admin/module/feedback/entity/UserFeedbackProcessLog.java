package com.bank.admin.module.feedback.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 反馈处理轨迹表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_feedback_process_log")
@Schema(description = "反馈处理轨迹")
public class UserFeedbackProcessLog extends BaseEntity {

    @Schema(description = "关联反馈ID")
    private Long feedbackId;

    /**
     * 动作类型：SUBMIT / STATUS_CHANGE / REMARK / ATTACHMENT / ASSIGN / CLOSE
     */
    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "变更前状态")
    private Integer fromStatus;

    @Schema(description = "变更后状态")
    private Integer toStatus;

    @Schema(description = "备注内容")
    private String remark;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
