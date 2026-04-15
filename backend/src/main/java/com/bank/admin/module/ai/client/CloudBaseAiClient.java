package com.bank.admin.module.ai.client;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.hunyuan.v20230901.HunyuanClient;
import com.tencentcloudapi.hunyuan.v20230901.models.ChatCompletionsRequest;
import com.tencentcloudapi.hunyuan.v20230901.models.ChatCompletionsResponse;
import com.tencentcloudapi.hunyuan.v20230901.models.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CloudBase AI 客户端 - 封装腾讯云混元大模型调用
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cloudbase.ai.enabled", havingValue = "true")
public class CloudBaseAiClient {

    @Value("${cloudbase.ai.secret-id}")
    private String secretId;

    @Value("${cloudbase.ai.secret-key}")
    private String secretKey;

    @Value("${cloudbase.ai.model:hunyuan-lite}")
    private String model;

    /**
     * 聊天对话（非流式）
     *
     * @param messages 消息数组（system/user/assistant）
     * @return ChatCompletionsResponse 原始响应
     */
    public ChatCompletionsResponse chat(Message[] messages) throws TencentCloudSDKException {
        ChatCompletionsRequest request = new ChatCompletionsRequest();
        request.setModel(model);
        request.setMessages(messages);
        // 温度：0.7 平衡创造性和准确性（0.8太发散容易胡说）
        request.setTemperature(0.7f);

        Credential cred = new Credential(secretId, secretKey);
        ClientProfile clientProfile = new ClientProfile();

        HunyuanClient client = new HunyuanClient(cred, "", clientProfile);

        log.info("[CloudBase AI] 发送聊天请求, 消息数={}, model={}", messages.length, model);
        ChatCompletionsResponse response = client.ChatCompletions(request);
        log.info("[CloudBase AI] 收到响应, requestId={}", response.getRequestId());
        return response;
    }
}
