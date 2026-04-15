package com.bank.admin.module.ai.service;

import com.bank.admin.module.ai.dto.AiAnalyzeRequest;
import com.bank.admin.module.ai.dto.AiChatRequest;
import com.bank.admin.module.ai.vo.AiResultVO;

/**
 * AI 服务接口
 */
public interface AiService {

    /**
     * AI 对话
     */
    AiResultVO chat(AiChatRequest request);

    /**
     * AI 数据分析
     */
    AiResultVO analyze(AiAnalyzeRequest request);

    /**
     * 查询 Token 配额信息（总配额、已用估算、剩余）
     */
    AiResultVO getQuota();
}
