import request from '@/utils/request'

export interface AiChatRequest {
  message: string
  conversationId?: string
  /** 历史消息（多轮对话记忆） */
  history?: { role: 'user' | 'assistant'; content: string }[]
}

export interface AiAnalyzeRequest {
  type: 'reconcile' | 'anomaly' | 'summary'
  startDate?: string
  endDate?: string
  cardId?: number
  ownerId?: number
}

export interface AiResultVO {
  content: string
  totalTokens: number
  inputTokens?: number
  outputTokens?: number
  model: string
  success: boolean
  errorMessage?: string
  // 配额信息
  totalQuota?: number
  usedQuota?: number
  remainingQuota?: number
  usagePercent?: number
}

/** AI 对话 */
export function chatApi(data: AiChatRequest) {
  return request.post<any, { data: AiResultVO }>('/ai/chat', data)
}

/** AI 数据分析 */
export function analyzeApi(data: AiAnalyzeRequest) {
  return request.post<any, { data: AiResultVO }>('/ai/analyze', data)
}

/** 查询 Token 配额 */
export function quotaApi() {
  return request.get<any, { data: AiResultVO }>('/ai/quota')
}
