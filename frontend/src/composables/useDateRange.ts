/**
 * 日期范围组合式函数
 * 封装日期范围选择器的通用逻辑
 */
import { ref, computed, watch } from 'vue'
import type { DateModelType } from 'element-plus'
import { DATE_RANGE_SHORTCUTS } from '@/constants/dict'

export interface DateRangeOptions {
  /** 开始日期字段名 */
  startField?: string
  /** 结束日期字段名 */
  endField?: string
  /** 日期格式 */
  format?: string
  /** 是否默认最近7天 */
  defaultRecentDays?: number
  /** 日期改变回调 */
  onChange?: (range: [Date | null, Date | null]) => void
}

export function useDateRange(options: DateRangeOptions = {}) {
  const {
    startField = 'startDate',
    endField = 'endDate',
    format = 'YYYY-MM-DD',
    defaultRecentDays,
    onChange
  } = options

  // 日期范围值
  const dateRange = ref<[DateModelType, DateModelType] | null>(null)

  // 快捷选项
  const shortcuts = DATE_RANGE_SHORTCUTS

  // 解构后的开始和结束日期
  const startDate = computed(() => {
    return dateRange.value?.[0] || null
  })

  const endDate = computed(() => {
    return dateRange.value?.[1] || null
  })

  // 格式化后的日期字符串
  const startDateStr = computed(() => {
    if (!dateRange.value?.[0]) return null
    return formatDateValue(dateRange.value[0])
  })

  const endDateStr = computed(() => {
    if (!dateRange.value?.[1]) return null
    return formatDateValue(dateRange.value[1])
  })

  // 获取查询参数
  const getQueryParams = () => {
    const params: Record<string, string | null> = {}
    params[startField] = startDateStr.value
    params[endField] = endDateStr.value
    return params
  }

  // 设置日期范围
  const setDateRange = (range: [Date | string | null, Date | string | null] | null) => {
    if (!range || !range[0] || !range[1]) {
      dateRange.value = null
      return
    }
    dateRange.value = [
      typeof range[0] === 'string' ? new Date(range[0]) : range[0],
      typeof range[1] === 'string' ? new Date(range[1]) : range[1]
    ] as [DateModelType, DateModelType]
  }

  // 设置最近N天
  const setRecentDays = (days: number) => {
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * days)
    dateRange.value = [start, end]
  }

  // 清空日期范围
  const clearDateRange = () => {
    dateRange.value = null
  }

  // 监听日期变化
  watch(dateRange, (newVal) => {
    if (onChange && newVal) {
      onChange(newVal as [Date | null, Date | null])
    }
  })

  // 初始化默认值
  if (defaultRecentDays) {
    setRecentDays(defaultRecentDays)
  }

  return {
    // 状态
    dateRange,
    shortcuts,
    startDate,
    endDate,
    startDateStr,
    endDateStr,

    // 方法
    getQueryParams,
    setDateRange,
    setRecentDays,
    clearDateRange
  }
}

// 格式化日期值
function formatDateValue(date: DateModelType): string {
  if (!date) return ''
  if (typeof date === 'string') return date.substring(0, 10)

  const d = date as Date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
