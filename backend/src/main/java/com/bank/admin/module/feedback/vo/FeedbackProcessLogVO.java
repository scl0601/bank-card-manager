package com.bank.admin.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处理轨迹 VO
 */
@Data
@Schema(description = "反馈处理轨迹")
public class FeedbackProcessLogVO {

    @Schema(description = "轨迹ID")
    private Long id;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "动作描述（可读）")
    private String actionDesc;

    @Schema(description = "变更前状态")
    private Integer fromStatus;

    @Schema(description = "变更前状态描述")
    private String fromStatusDesc;

    @Schema(description = "变更后状态")
    private Integer toStatus;

    @Schema(description = "变更后状态描述")
    private String toStatusDesc;

    @Schema(description = "备注内容")
    private String remark;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
