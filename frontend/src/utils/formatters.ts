/**
 * 格式化工具函数
 */

/**
 * 格式化金额（千分位，保留2位小数）
 */
export function formatAmount(val: number | string | null | undefined): string {
  if (!val && val !== 0) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 格式化时间（ISO格式转显示格式）
 */
export function formatTime(time: string | null | undefined): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

/**
 * 格式化日期（只取日期部分）
 */
export function formatDate(date: string | null | undefined): string {
  if (!date) return '-'
  return date.substring(0, 10)
}

/**
 * 卡号脱敏（显示后四位）
 */
export function maskCardNo(last4: string | null | undefined, prefix = '****'): string {
  if (!last4) return '-'
  return `${prefix} ${last4}`
}

/**
 * 手机号脱敏
 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone) return '-'
  if (phone.length !== 11) return phone
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 身份证号脱敏
 */
export function maskIdCard(idCard: string | null | undefined): string {
  if (!idCard) return '-'
  if (idCard.length < 8) return idCard
  return idCard.replace(/(\d{4})\d+(\d{4})/, '$1**********$2')
}

/**
 * 格式化百分比
 */
export function formatPercent(val: number | string | null | undefined, decimals = 2): string {
  if (!val && val !== 0) return '0%'
  return Number(val).toFixed(decimals) + '%'
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 截断文本
 */
export function truncate(text: string | null | undefined, length = 20): string {
  if (!text) return '-'
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

/**
 * 格式化金额（带正负号）
 */
export function formatAmountWithSign(val: number | string | null | undefined): string {
  if (!val && val !== 0) return '0.00'
  const num = Number(val)
  const formatted = formatAmount(Math.abs(num))
  return num >= 0 ? `+${formatted}` : `-${formatted}`
}

/**
 * 解析金额字符串为数字
 */
export function parseAmount(str: string | null | undefined): number {
  if (!str) return 0
  const num = parseFloat(str.replace(/,/g, ''))
  return isNaN(num) ? 0 : num
}
