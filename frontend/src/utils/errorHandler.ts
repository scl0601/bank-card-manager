import { ElMessage, ElNotification } from 'element-plus'

export enum ErrorType {
  VALIDATION = 'validation',
  PERMISSION = 'permission',
  NETWORK = 'network',
  BUSINESS = 'business',
  UNKNOWN = 'unknown'
}

interface ErrorInfo {
  type: ErrorType
  message: string
  suggestion: string
  code?: string
}

export function detectErrorType(error: any): ErrorType {
  if (error.response) {
    const status = error.response.status
    if (status === 400) return ErrorType.VALIDATION
    if (status === 403) return ErrorType.PERMISSION
    if (status >= 500) return ErrorType.BUSINESS
  }
  if (error.message?.includes('Network') || error.message?.includes('timeout')) {
    return ErrorType.NETWORK
  }
  return ErrorType.UNKNOWN
}

export function getErrorInfo(error: any): ErrorInfo {
  const type = detectErrorType(error)
  const message = error.response?.data?.message || error.message || '操作失败'

  const suggestions: Record<ErrorType, string> = {
    [ErrorType.VALIDATION]: '请检查输入的数据是否符合要求',
    [ErrorType.PERMISSION]: '您没有权限执行此操作，请联系管理员',
    [ErrorType.NETWORK]: '网络连接失败，请检查网络后重试',
    [ErrorType.BUSINESS]: '服务器处理失败，请稍后重试或联系技术支持',
    [ErrorType.UNKNOWN]: '发生未知错误，请刷新页面后重试'
  }

  return {
    type,
    message,
    suggestion: suggestions[type],
    code: error.response?.data?.code
  }
}

export function handleError(error: any, context?: string) {
  const info = getErrorInfo(error)

  const typeIcons: Record<ErrorType, string> = {
    [ErrorType.VALIDATION]: '⚠️',
    [ErrorType.PERMISSION]: '🔒',
    [ErrorType.NETWORK]: '📡',
    [ErrorType.BUSINESS]: '❌',
    [ErrorType.UNKNOWN]: '❓'
  }

  ElNotification.error({
    title: `${typeIcons[info.type]} ${context || '操作失败'}`,
    message: `${info.message}\n\n💡 ${info.suggestion}`,
    duration: 5000,
    showClose: true
  })

  console.error('[Error Handler]', { context, error, info })
}
