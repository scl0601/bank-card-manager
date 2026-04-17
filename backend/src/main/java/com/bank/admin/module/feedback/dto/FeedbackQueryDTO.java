package com.bank.admin.module.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 反馈分页查询参数
 */
@Data
@Schema(description = "反馈分页查询参数")
public class FeedbackQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "标题关键词（模糊）")
    private String titleKeyword;

    @Schema(description = "状态：0待处理 1修复中 2已解决 3已关闭")
    private Integer status;

    @Schema(description = "优先级：0低 1中 2高 3紧急")
    private Integer priority;

    @Schema(description = "反馈类型：0功能异常 1界面体验 2数据问题 3权限问题 4其他")
    private Integer feedbackType;

    @Schema(description = "提交人")
    private String submitter;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;
}
