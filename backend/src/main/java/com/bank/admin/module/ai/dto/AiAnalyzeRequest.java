package com.bank.admin.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * AI 分析请求 DTO
 */
@Data
@Schema(description = "AI分析请求")
public class AiAnalyzeRequest {

    @NotNull(message = "分析类型不能为空")
    @Schema(description = "分析类型: reconcile-对账分析, anomaly-异常检测, summary-收支总结", example = "reconcile")
    private String type;

    @Schema(description = "开始日期", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-04-15")
    private LocalDate endDate;

    @Schema(description = "银行卡ID（可选，不传则分析全部）")
    private Long cardId;

    @Schema(description = "持卡人ID（可选，不传则分析全部）")
    private Long ownerId;
}
