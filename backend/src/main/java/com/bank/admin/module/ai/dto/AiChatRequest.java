package com.bank.admin.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI 聊天请求 DTO
 */
@Data
@Schema(description = "AI聊天请求")
public class AiChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户输入的消息", example = "帮我分析一下本月的支出情况")
    private String message;

    @Schema(description = "会话ID", hidden = true)
    private String conversationId;

    /**
     * 历史消息列表，用于多轮对话上下文
     * 格式: [{role:"user/assistant", content:"..."}, ...]
     * 最多保留最近20条
     */
    @Schema(description = "历史消息列表")
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role;   // user 或 assistant
        private String content;
    }
}
