/**
 * 数据字典常量
 * 统一管理所有枚举值和下拉选项
 * 注意：所有 value 使用整数值，与后端实体字段类型保持一致
 */

// ==================== 卡类型 ====================
export const CARD_TYPE_VALUE = {
  DEBIT: 1,   // 借记卡
  CREDIT: 2   // 信用卡
} as const

export const CARD_TYPE_OPTIONS = [
  { label: '借记卡', value: CARD_TYPE_VALUE.DEBIT },
  { label: '信用卡', value: CARD_TYPE_VALUE.CREDIT }
]

export const CARD_TYPE_MAP: Record<number, string> = {
  [CARD_TYPE_VALUE.DEBIT]: '借记卡',
  [CARD_TYPE_VALUE.CREDIT]: '信用卡'
}

// ==================== 卡状态 ====================
export const CARD_STATUS_VALUE = {
  NORMAL: 0,      // 正常
  FROZEN: 1,      // 冻结
  CANCELLED: 2    // 注销
} as const

export const CARD_STATUS_OPTIONS = [
  { label: '正常', value: CARD_STATUS_VALUE.NORMAL },
  { label: '冻结', value: CARD_STATUS_VALUE.FROZEN },
  { label: '注销', value: CARD_STATUS_VALUE.CANCELLED }
]

export const CARD_STATUS_MAP: Record<number, string> = {
  [CARD_STATUS_VALUE.NORMAL]: '正常',
  [CARD_STATUS_VALUE.FROZEN]: '冻结',
  [CARD_STATUS_VALUE.CANCELLED]: '注销'
}

export const CARD_STATUS_TAG_TYPE: Record<number, string> = {
  [CARD_STATUS_VALUE.NORMAL]: 'success',
  [CARD_STATUS_VALUE.FROZEN]: 'warning',
  [CARD_STATUS_VALUE.CANCELLED]: 'info'
}

// ==================== 交易类型 ====================
export const TXN_TYPE_VALUE = {
  INCOME: 1,   // 收入
  EXPENSE: 2   // 支出
} as const

export const TXN_TYPE_OPTIONS = [
  { label: '收入', value: TXN_TYPE_VALUE.INCOME },
  { label: '支出', value: TXN_TYPE_VALUE.EXPENSE }
]

export const TXN_TYPE_MAP: Record<number, string> = {
  [TXN_TYPE_VALUE.INCOME]: '收入',
  [TXN_TYPE_VALUE.EXPENSE]: '支出'
}

export const TXN_TYPE_TAG_TYPE: Record<number, string> = {
  [TXN_TYPE_VALUE.INCOME]: 'success',
  [TXN_TYPE_VALUE.EXPENSE]: 'danger'
}

// ==================== 账单状态 ====================
export const BILL_STATUS_VALUE = {
  PENDING: 0,      // 待还款
  PAID: 1,         // 已还清
  PARTIAL: 2,      // 部分还款
  OVERDUE: 3       // 逾期
} as const

export const BILL_STATUS_OPTIONS = [
  { label: '待还款', value: BILL_STATUS_VALUE.PENDING },
  { label: '已还清', value: BILL_STATUS_VALUE.PAID },
  { label: '部分还款', value: BILL_STATUS_VALUE.PARTIAL },
  { label: '逾期', value: BILL_STATUS_VALUE.OVERDUE }
]

export const BILL_STATUS_MAP: Record<number, string> = {
  [BILL_STATUS_VALUE.PENDING]: '待还款',
  [BILL_STATUS_VALUE.PAID]: '已还清',
  [BILL_STATUS_VALUE.PARTIAL]: '部分还款',
  [BILL_STATUS_VALUE.OVERDUE]: '逾期'
}

export const BILL_STATUS_TAG_TYPE: Record<number, string> = {
  [BILL_STATUS_VALUE.PENDING]: 'warning',
  [BILL_STATUS_VALUE.PAID]: 'success',
  [BILL_STATUS_VALUE.PARTIAL]: 'info',
  [BILL_STATUS_VALUE.OVERDUE]: 'danger'
}

// ==================== 提醒类型 ====================
export const REMINDER_TYPE_VALUE = {
  BILL_DUE_SOON: 1,    // 即将到期
  BILL_DUE_TODAY: 2,   // 今日到期
  BILL_OVERDUE: 3,     // 已逾期
  CARD_EXPIRING: 4     // 卡片即将过期
} as const

export const REMINDER_TYPE_OPTIONS = [
  { label: '即将到期', value: REMINDER_TYPE_VALUE.BILL_DUE_SOON },
  { label: '今日到期', value: REMINDER_TYPE_VALUE.BILL_DUE_TODAY },
  { label: '已逾期', value: REMINDER_TYPE_VALUE.BILL_OVERDUE },
  { label: '卡片即将过期', value: REMINDER_TYPE_VALUE.CARD_EXPIRING }
]

export const REMINDER_TYPE_MAP: Record<number, string> = {
  [REMINDER_TYPE_VALUE.BILL_DUE_SOON]: '即将到期',
  [REMINDER_TYPE_VALUE.BILL_DUE_TODAY]: '今日到期',
  [REMINDER_TYPE_VALUE.BILL_OVERDUE]: '已逾期',
  [REMINDER_TYPE_VALUE.CARD_EXPIRING]: '卡片即将过期'
}

export const REMINDER_TYPE_TAG_TYPE: Record<number, string> = {
  [REMINDER_TYPE_VALUE.BILL_DUE_SOON]: 'warning',
  [REMINDER_TYPE_VALUE.BILL_DUE_TODAY]: 'danger',
  [REMINDER_TYPE_VALUE.BILL_OVERDUE]: 'danger',
  [REMINDER_TYPE_VALUE.CARD_EXPIRING]: 'info'
}

// ==================== 提醒状态 ====================
export const REMINDER_STATUS_VALUE = {
  PENDING: 0,    // 待处理
  HANDLED: 1,    // 已处理
  IGNORED: 2     // 已忽略
} as const

export const REMINDER_STATUS_OPTIONS = [
  { label: '待处理', value: REMINDER_STATUS_VALUE.PENDING },
  { label: '已处理', value: REMINDER_STATUS_VALUE.HANDLED },
  { label: '已忽略', value: REMINDER_STATUS_VALUE.IGNORED }
]

export const REMINDER_STATUS_MAP: Record<number, string> = {
  [REMINDER_STATUS_VALUE.PENDING]: '待处理',
  [REMINDER_STATUS_VALUE.HANDLED]: '已处理',
  [REMINDER_STATUS_VALUE.IGNORED]: '已忽略'
}

export const REMINDER_STATUS_TAG_TYPE: Record<number, string> = {
  [REMINDER_STATUS_VALUE.PENDING]: 'warning',
  [REMINDER_STATUS_VALUE.HANDLED]: 'success',
  [REMINDER_STATUS_VALUE.IGNORED]: 'info'
}

// ==================== 持卡人状态 ====================
export const OWNER_STATUS_VALUE = {
  ACTIVE: 0,     // 正常
  INACTIVE: 1    // 禁用
} as const

export const OWNER_STATUS_OPTIONS = [
  { label: '正常', value: OWNER_STATUS_VALUE.ACTIVE },
  { label: '禁用', value: OWNER_STATUS_VALUE.INACTIVE }
]

export const OWNER_STATUS_MAP: Record<number, string> = {
  [OWNER_STATUS_VALUE.ACTIVE]: '正常',
  [OWNER_STATUS_VALUE.INACTIVE]: '禁用'
}

export const OWNER_STATUS_TAG_TYPE: Record<number, string> = {
  [OWNER_STATUS_VALUE.ACTIVE]: 'success',
  [OWNER_STATUS_VALUE.INACTIVE]: 'info'
}

// ==================== 记账类型 ====================
export const BOOK_TYPE_VALUE = {
  INCOME: 1,   // 收入
  EXPENSE: 2   // 支出
} as const

export const BOOK_TYPE_OPTIONS = [
  { label: '收入', value: BOOK_TYPE_VALUE.INCOME },
  { label: '支出', value: BOOK_TYPE_VALUE.EXPENSE }
]

export const BOOK_TYPE_MAP: Record<number, string> = {
  [BOOK_TYPE_VALUE.INCOME]: '收入',
  [BOOK_TYPE_VALUE.EXPENSE]: '支出'
}

export const BOOK_TYPE_TAG_TYPE: Record<number, string> = {
  [BOOK_TYPE_VALUE.INCOME]: 'success',
  [BOOK_TYPE_VALUE.EXPENSE]: 'danger'
}

// ==================== 分类状态 ====================
export const CATEGORY_STATUS_VALUE = {
  ACTIVE: 0,     // 启用
  DISABLED: 1    // 停用
} as const

export const CATEGORY_STATUS_OPTIONS = [
  { label: '启用', value: CATEGORY_STATUS_VALUE.ACTIVE },
  { label: '停用', value: CATEGORY_STATUS_VALUE.DISABLED }
]

export const CATEGORY_STATUS_MAP: Record<number, string> = {
  [CATEGORY_STATUS_VALUE.ACTIVE]: '启用',
  [CATEGORY_STATUS_VALUE.DISABLED]: '停用'
}

// ==================== 操作结果 ====================
export const LOG_RESULT_VALUE = {
  SUCCESS: 0,   // 成功
  FAIL: 1       // 失败
} as const

export const LOG_RESULT_OPTIONS = [
  { label: '成功', value: LOG_RESULT_VALUE.SUCCESS },
  { label: '失败', value: LOG_RESULT_VALUE.FAIL }
]

export const LOG_RESULT_MAP: Record<number, string> = {
  [LOG_RESULT_VALUE.SUCCESS]: '成功',
  [LOG_RESULT_VALUE.FAIL]: '失败'
}

export const LOG_RESULT_TAG_TYPE: Record<number, string> = {
  [LOG_RESULT_VALUE.SUCCESS]: 'success',
  [LOG_RESULT_VALUE.FAIL]: 'danger'
}

// ==================== 分页默认值 ====================
export const PAGE_DEFAULTS = {
  PAGE_NUM: 1,
  PAGE_SIZE: 10,
  PAGE_SIZES: [10, 20, 50, 100]
} as const

// ==================== 日期范围快捷选项 ====================
export const DATE_RANGE_SHORTCUTS = [
  {
    text: '今天',
    value: () => {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return [today, today]
    }
  },
  {
    text: '昨天',
    value: () => {
      const yesterday = new Date()
      yesterday.setTime(yesterday.getTime() - 3600 * 1000 * 24)
      yesterday.setHours(0, 0, 0, 0)
      return [yesterday, yesterday]
    }
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '本月',
    value: () => {
      const start = new Date()
      start.setDate(1)
      start.setHours(0, 0, 0, 0)
      return [start, new Date()]
    }
  }
]
