/**
 * 移动端共享常量与工具函数
 * 统一管理颜色映射、状态标签、格式化函数，消除跨组件重复定义
 */

// ==================== 分类颜色 ====================
export const CATEGORY_COLOR: Record<number | string, string> = {
  0: '#0958d9', // 工作-蓝
  1: '#2f9e44', // 生活-绿
  2: '#d97706', // 学习-橙
  3: '#cf1322', // 健康-红
  4: '#64748b', // 其他-灰
}

export const CATEGORY_COLOR_LIGHT: Record<number | string, string> = {
  0: '#eaf2ff',
  1: '#e8f7ed',
  2: '#fff4db',
  3: '#fff1f0',
  4: '#eef2f6',
}

// ==================== 状态标签 ====================
export const STATUS_LABEL: Record<number, string> = {
  0: '待办',
  1: '进行中',
  2: '已完成',
  3: '已取消',
}

// ==================== 优先级标签 ====================
export const PRIORITY_LABEL: Record<number, string> = {
  0: '低',
  1: '中',
  2: '高',
}

// ==================== 格式化工具 ====================

/** 格式化日期为 YYYY-MM-DD */
export function formatDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 格式化时间字符串 HH:MM:SS → HH:MM */
export function formatTime(t: string): string {
  if (!t) return ''
  return t.length >= 5 ? t.slice(0, 5) : t
}

/** 获取当前时间 HH:MM */
export function getCurrentTime(): string {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

/** 格式化显示日期：2026年4月15日 周三 */
export function formatDisplayDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr + 'T00:00:00')
  const w = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 周${w[d.getDay()]}`
}

/** 判断是否周末 */
export function isWeekend(y: number, m: number, d: number): boolean {
  const dow = new Date(y, m - 1, d).getDay()
  return dow === 0 || dow === 6
}
