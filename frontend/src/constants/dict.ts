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

// ==================== 明细类型 ====================
export const DETAIL_TYPE_VALUE = {
  EXPENSE: 0,  // 支出
  INCOME: 1    // 收入
} as const

export const DETAIL_TYPE_OPTIONS = [
  { label: '支出', value: DETAIL_TYPE_VALUE.EXPENSE },
  { label: '收入', value: DETAIL_TYPE_VALUE.INCOME }
]

export const DETAIL_TYPE_MAP: Record<number, string> = {
  [DETAIL_TYPE_VALUE.EXPENSE]: '支出',
  [DETAIL_TYPE_VALUE.INCOME]: '收入'
}

export const DETAIL_TYPE_TAG_TYPE: Record<number, string> = {
  [DETAIL_TYPE_VALUE.EXPENSE]: 'danger',
  [DETAIL_TYPE_VALUE.INCOME]: 'success'
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

// ==================== 日程事项分类 ====================
export const EVENT_CATEGORY_VALUE = {
  WORK: 0,     // 工作
  LIFE: 1,     // 生活
  STUDY: 2,    // 学习
  HEALTH: 3,   // 健康
  OTHER: 4,    // 其他
  DIARY: 5     // 日记
} as const

export const EVENT_CATEGORY_OPTIONS = [
  { label: '工作', value: EVENT_CATEGORY_VALUE.WORK },
  { label: '生活', value: EVENT_CATEGORY_VALUE.LIFE },
  { label: '学习', value: EVENT_CATEGORY_VALUE.STUDY },
  { label: '健康', value: EVENT_CATEGORY_VALUE.HEALTH },
  { label: '日记', value: EVENT_CATEGORY_VALUE.DIARY },
  { label: '其他', value: EVENT_CATEGORY_VALUE.OTHER }
]

export const EVENT_CATEGORY_MAP: Record<number, string> = {
  [EVENT_CATEGORY_VALUE.WORK]: '工作',
  [EVENT_CATEGORY_VALUE.LIFE]: '生活',
  [EVENT_CATEGORY_VALUE.STUDY]: '学习',
  [EVENT_CATEGORY_VALUE.HEALTH]: '健康',
  [EVENT_CATEGORY_VALUE.OTHER]: '其他',
  [EVENT_CATEGORY_VALUE.DIARY]: '日记'
}

export const EVENT_CATEGORY_TAG_TYPE: Record<number, string> = {
  [EVENT_CATEGORY_VALUE.WORK]: '',
  [EVENT_CATEGORY_VALUE.LIFE]: 'success',
  [EVENT_CATEGORY_VALUE.STUDY]: 'warning',
  [EVENT_CATEGORY_VALUE.HEALTH]: 'danger',
  [EVENT_CATEGORY_VALUE.OTHER]: 'info',
  [EVENT_CATEGORY_VALUE.DIARY]: ''
}

// ==================== 日程事项优先级 ====================
export const EVENT_PRIORITY_VALUE = {
  LOW: 0,      // 低
  MEDIUM: 1,   // 中
  HIGH: 2      // 高
} as const

export const EVENT_PRIORITY_OPTIONS = [
  { label: '低', value: EVENT_PRIORITY_VALUE.LOW },
  { label: '中', value: EVENT_PRIORITY_VALUE.MEDIUM },
  { label: '高', value: EVENT_PRIORITY_VALUE.HIGH }
]

export const EVENT_PRIORITY_MAP: Record<number, string> = {
  [EVENT_PRIORITY_VALUE.LOW]: '低',
  [EVENT_PRIORITY_VALUE.MEDIUM]: '中',
  [EVENT_PRIORITY_VALUE.HIGH]: '高'
}

export const EVENT_PRIORITY_TAG_TYPE: Record<number, string> = {
  [EVENT_PRIORITY_VALUE.LOW]: 'info',
  [EVENT_PRIORITY_VALUE.MEDIUM]: 'warning',
  [EVENT_PRIORITY_VALUE.HIGH]: 'danger'
}

// ==================== 日程事项状态 ====================
export const EVENT_STATUS_VALUE = {
  TODO: 0,       // 待办
  DOING: 1,      // 进行中
  DONE: 2,       // 已完成
  CANCELLED: 3   // 已取消
} as const

export const EVENT_STATUS_OPTIONS = [
  { label: '待办', value: EVENT_STATUS_VALUE.TODO },
  { label: '进行中', value: EVENT_STATUS_VALUE.DOING },
  { label: '已完成', value: EVENT_STATUS_VALUE.DONE },
  { label: '已取消', value: EVENT_STATUS_VALUE.CANCELLED }
]

export const EVENT_STATUS_MAP: Record<number, string> = {
  [EVENT_STATUS_VALUE.TODO]: '待办',
  [EVENT_STATUS_VALUE.DOING]: '进行中',
  [EVENT_STATUS_VALUE.DONE]: '已完成',
  [EVENT_STATUS_VALUE.CANCELLED]: '已取消'
}

export const EVENT_STATUS_TAG_TYPE: Record<number, string> = {
  [EVENT_STATUS_VALUE.TODO]: 'warning',
  [EVENT_STATUS_VALUE.DOING]: '',
  [EVENT_STATUS_VALUE.DONE]: 'success',
  [EVENT_STATUS_VALUE.CANCELLED]: 'info'
}

// ==================== 用户反馈状态 ====================
export const FEEDBACK_STATUS_VALUE = {
  PENDING: 0,      // 待处理
  IN_PROGRESS: 1,  // 修复中
  RESOLVED: 2,     // 已解决
  CLOSED: 3        // 已关闭
} as const

export const FEEDBACK_STATUS_OPTIONS = [
  { label: '待处理', value: FEEDBACK_STATUS_VALUE.PENDING },
  { label: '修复中', value: FEEDBACK_STATUS_VALUE.IN_PROGRESS },
  { label: '已解决', value: FEEDBACK_STATUS_VALUE.RESOLVED },
  { label: '已关闭', value: FEEDBACK_STATUS_VALUE.CLOSED }
]

export const FEEDBACK_STATUS_MAP: Record<number, string> = {
  [FEEDBACK_STATUS_VALUE.PENDING]: '待处理',
  [FEEDBACK_STATUS_VALUE.IN_PROGRESS]: '修复中',
  [FEEDBACK_STATUS_VALUE.RESOLVED]: '已解决',
  [FEEDBACK_STATUS_VALUE.CLOSED]: '已关闭'
}

export const FEEDBACK_STATUS_TAG_TYPE: Record<number, string> = {
  [FEEDBACK_STATUS_VALUE.PENDING]: 'warning',
  [FEEDBACK_STATUS_VALUE.IN_PROGRESS]: '',
  [FEEDBACK_STATUS_VALUE.RESOLVED]: 'success',
  [FEEDBACK_STATUS_VALUE.CLOSED]: 'info'
}

// ==================== 用户反馈优先级 ====================
export const FEEDBACK_PRIORITY_VALUE = {
  LOW: 0,    // 低
  MEDIUM: 1, // 中
  HIGH: 2,   // 高
  URGENT: 3  // 紧急
} as const

export const FEEDBACK_PRIORITY_OPTIONS = [
  { label: '低', value: FEEDBACK_PRIORITY_VALUE.LOW },
  { label: '中', value: FEEDBACK_PRIORITY_VALUE.MEDIUM },
  { label: '高', value: FEEDBACK_PRIORITY_VALUE.HIGH },
  { label: '紧急', value: FEEDBACK_PRIORITY_VALUE.URGENT }
]

export const FEEDBACK_PRIORITY_MAP: Record<number, string> = {
  [FEEDBACK_PRIORITY_VALUE.LOW]: '低',
  [FEEDBACK_PRIORITY_VALUE.MEDIUM]: '中',
  [FEEDBACK_PRIORITY_VALUE.HIGH]: '高',
  [FEEDBACK_PRIORITY_VALUE.URGENT]: '紧急'
}

export const FEEDBACK_PRIORITY_TAG_TYPE: Record<number, string> = {
  [FEEDBACK_PRIORITY_VALUE.LOW]: 'info',
  [FEEDBACK_PRIORITY_VALUE.MEDIUM]: '',
  [FEEDBACK_PRIORITY_VALUE.HIGH]: 'warning',
  [FEEDBACK_PRIORITY_VALUE.URGENT]: 'danger'
}

// ==================== 用户反馈类型 ====================
export const FEEDBACK_TYPE_VALUE = {
  BUG: 0,        // 功能异常
  UI: 1,         // 界面体验
  DATA: 2,       // 数据问题
  PERMISSION: 3, // 权限问题
  OTHER: 4       // 其他
} as const

export const FEEDBACK_TYPE_OPTIONS = [
  { label: '功能异常', value: FEEDBACK_TYPE_VALUE.BUG },
  { label: '界面体验', value: FEEDBACK_TYPE_VALUE.UI },
  { label: '数据问题', value: FEEDBACK_TYPE_VALUE.DATA },
  { label: '权限问题', value: FEEDBACK_TYPE_VALUE.PERMISSION },
  { label: '其他', value: FEEDBACK_TYPE_VALUE.OTHER }
]

export const FEEDBACK_TYPE_MAP: Record<number, string> = {
  [FEEDBACK_TYPE_VALUE.BUG]: '功能异常',
  [FEEDBACK_TYPE_VALUE.UI]: '界面体验',
  [FEEDBACK_TYPE_VALUE.DATA]: '数据问题',
  [FEEDBACK_TYPE_VALUE.PERMISSION]: '权限问题',
  [FEEDBACK_TYPE_VALUE.OTHER]: '其他'
}

export const FEEDBACK_TYPE_TAG_TYPE: Record<number, string> = {
  [FEEDBACK_TYPE_VALUE.BUG]: 'danger',
  [FEEDBACK_TYPE_VALUE.UI]: '',
  [FEEDBACK_TYPE_VALUE.DATA]: 'warning',
  [FEEDBACK_TYPE_VALUE.PERMISSION]: 'warning',
  [FEEDBACK_TYPE_VALUE.OTHER]: 'info'
}

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
