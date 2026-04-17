package com.bank.admin.module.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 反馈统计 VO（首页统计卡片）
 */
@Data
@Schema(description = "反馈统计数据")
public class FeedbackStatsVO {

    @Schema(description = "待处理数量")
    private long pendingCount;

    @Schema(description = "修复中数量")
    private long inProgressCount;

    @Schema(description = "已解决数量")
    private long resolvedCount;

    @Schema(description = "本周新增数量")
    private long weekNewCount;

    @Schema(description = "总数量")
    private long totalCount;
}
