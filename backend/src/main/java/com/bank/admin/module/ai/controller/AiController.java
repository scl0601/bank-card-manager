package com.bank.admin.module.ai.controller;

import com.bank.admin.common.annotation.Log;
import com.bank.admin.common.enums.ActionTypeEnum;
import com.bank.admin.common.result.Result;
import com.bank.admin.module.ai.dto.AiAnalyzeRequest;
import com.bank.admin.module.ai.dto.AiChatRequest;
import com.bank.admin.module.ai.service.AiService;
import com.bank.admin.module.ai.vo.AiResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 助手 Controller
 */
@Tag(name = "AI智能助手")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI对话")
    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    @Log(module = "AI助手", type = ActionTypeEnum.OTHER, description = "AI对话交互")
    public Result<AiResultVO> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.success(aiService.chat(request));
    }

    @Operation(summary = "AI数据分析")
    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    @Log(module = "AI助手", type = ActionTypeEnum.OTHER, description = "AI数据分析")
    public Result<AiResultVO> analyze(@Valid @RequestBody AiAnalyzeRequest request) {
        return Result.success(aiService.analyze(request));
    }

    @Operation(summary = "查询Token配额")
    @GetMapping("/quota")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public Result<AiResultVO> quota() {
        return Result.success(aiService.getQuota());
    }
}
