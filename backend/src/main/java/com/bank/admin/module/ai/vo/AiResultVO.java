package com.bank.admin.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 响应 VO
 */
@Data
@Schema(description = "AI响应VO")
public class AiResultVO {

    @Schema(description = "回复内容")
    private String content;

    @Schema(description = "消耗的Token数")
    private Integer totalTokens;

    @Schema(description = "输入Token数")
    private Integer inputTokens;

    @Schema(description = "输出Token数")
    private Integer outputTokens;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;

    // ========== 配额相关 ==========

    @Schema(description = "总配额")
    private Long totalQuota;

    @Schema(description = "已使用量（估算，基于本次服务累计）")
    private Long usedQuota;

    @Schema(description = "剩余可用（估算）")
    private Long remainingQuota;

    @Schema(description = "使用百分比(0-100)")
    private Integer usagePercent;

    public static AiResultVO success(String content, Integer totalTokens, Integer inputTokens, Integer outputTokens, String model) {
        AiResultVO vo = new AiResultVO();
        vo.setContent(content);
        vo.setTotalTokens(totalTokens);
        vo.setInputTokens(inputTokens);
        vo.setOutputTokens(outputTokens);
        vo.setModel(model);
        vo.setSuccess(true);
        return vo;
    }

    public static AiResultVO fail(String message) {
        AiResultVO vo = new AiResultVO();
        vo.setSuccess(false);
        vo.setErrorMessage(message);
        return vo;
    }

    /**
     * 构建带配额信息的响应
     */
    public AiResultVO withQuota(long totalQuota, long usedQuota) {
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.remainingQuota = Math.max(0, totalQuota - usedQuota);
        this.usagePercent = totalQuota > 0 ? (int) (usedQuota * 100 / totalQuota) : 0;
        return this;
    }
}
