<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="dashboard-header">
      <div class="header-left">
        <span class="page-title-icon">
          <el-icon :size="18"><House /></el-icon>
        </span>
        <div class="header-title-group">
          <h1 class="page-title">首页看板</h1>
          <span class="page-subtitle">
            {{ currentMonthText }} · 用户信息、卡务、账单、收益与日历计划概览
          </span>
        </div>
      </div>

      <div class="header-actions">
        <div class="period-controls">
          <el-date-picker
            v-model="selectedMonthValue"
            class="period-date-picker"
            type="month"
            size="small"
            format="YYYY年MM月"
            value-format="YYYY-MM"
            :clearable="false"
            :editable="false"
            placeholder="选择月份"
            @change="handlePeriodChange"
          />
        </div>
        <button
          v-for="item in headerModules"
          :key="item.label"
          class="module-nav-btn"
          type="button"
          @click="router.push(item.route)"
        >
          <el-icon :size="13"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </button>
        <el-button class="refresh-btn" :icon="RefreshRight" :loading="loading" @click="refreshDashboard">
          刷新
        </el-button>
      </div>
    </section>

    <section class="module-grid">
      <article
        v-for="item in moduleCards"
        :key="item.key"
        class="module-card"
        :class="`is-${item.key}`"
      >
        <div class="module-card-head">
          <span class="module-icon"><el-icon :size="18"><component :is="item.icon" /></el-icon></span>
          <div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.desc }}</p>
          </div>
          <button class="card-enter-btn" type="button" @click="router.push(item.route)">进入</button>
        </div>
        <div class="module-metrics">
          <div v-for="metric in item.metrics" :key="metric.label" class="metric-item">
            <span>{{ metric.label }}</span>
            <el-tooltip :content="tooltipText(metric.value)" placement="top" popper-class="dashboard-value-tooltip">
              <strong :class="['dashboard-value-ellipsis', metric.className]">{{ metric.value }}</strong>
            </el-tooltip>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-content">
      <article class="dashboard-panel calendar-panel">
        <div class="panel-head">
          <div>
            <h2>日历计划</h2>
            <p>{{ currentMonthText }} · 完成率 {{ calendarDonePercent }}%</p>
          </div>
          <div class="panel-actions">
            <button class="plain-pill" type="button" @click="goTodayInCalendar">今天</button>
            <button class="plain-pill is-primary" type="button" @click="openCalendarDrawer(null, selectedDayDate || todayKey)">新建日程</button>
            <button class="plain-pill" type="button" @click="router.push('/calendar')">查看日历</button>
          </div>
        </div>

        <div class="calendar-body">
          <div class="calendar-left">
            <div class="calendar-summary-row">
              <div
                v-for="item in calendarSummary"
                :key="item.key"
                class="calendar-stat"
                :class="`is-${item.key}`"
              >
                <span>{{ item.label }}</span>
                <el-tooltip :content="tooltipText(item.value)" placement="top" popper-class="dashboard-value-tooltip">
                  <strong class="dashboard-value-ellipsis">{{ item.value }}</strong>
                </el-tooltip>
                <i class="stat-track"><b :style="{ width: item.percent + '%' }"></b></i>
              </div>
            </div>
            <div class="mini-calendar">
              <div class="mini-week">
                <span v-for="day in weekDays" :key="day">{{ day }}</span>
              </div>
              <div class="mini-grid">
                <div
                  v-for="cell in miniCalendarCells"
                  :key="cell.key"
                  class="mini-cell"
                  :class="{
                    other: cell.other,
                    today: cell.isToday,
                    selected: cell.key === selectedDayDate,
                    'has-events': cell.eventCount > 0
                  }"
                  @click="openDayPanel(cell.key)"
                >
                  <span class="mini-day">{{ cell.day }}</span>
                  <span v-if="cell.eventCount" class="mini-count">{{ cell.eventCount }}</span>
                  <button class="mini-add" type="button" title="新建日程" @click.stop="openCalendarDrawer(null, cell.key)">+</button>
                </div>
              </div>
            </div>
          </div>

          <div class="calendar-right">
            <div class="section-title selected-day-title">
              <span>
                {{ selectedDayTitle }}
                <em v-if="selectedDayDate === todayKey">今天</em>
              </span>
              <small>{{ selectedDayStats.total }} 项日程，点击可快速编辑</small>
            </div>
            <div class="day-stat-row inline-day-stats">
              <span>待办 <strong>{{ selectedDayStats.todo }}</strong></span>
              <span>进行中 <strong>{{ selectedDayStats.doing }}</strong></span>
              <span>已完成 <strong>{{ selectedDayStats.done }}</strong></span>
              <span>已取消 <strong>{{ selectedDayStats.cancelled }}</strong></span>
            </div>
            <div class="event-list">
              <div v-for="event in selectedDayEvents" :key="event.id" class="event-item" @click="openCalendarDrawer(event)">
                <span class="event-date">{{ eventTimeRange(event) }}</span>
                <span class="event-content">
                  <span class="event-title" :title="event.title">{{ event.title || '未命名日程' }}</span>
                  <span class="event-meta">
                    <i :style="{ background: eventCategoryColor(event.category) }"></i>
                    {{ eventCategoryLabel(event.category) }}
                  </span>
                </span>
                <span class="event-status" :class="eventStatusClass(event.status)">
                  {{ eventStatusLabel(event.status) }}
                </span>
              </div>
              <el-empty v-if="!selectedDayEvents.length" description="当天暂无日程" :image-size="56" />
            </div>
          </div>
        </div>
      </article>

      <aside class="summary-stack">
        <article class="dashboard-panel compact-panel bill-summary-panel">
          <div class="panel-head">
            <div>
              <h2>账单信息</h2>
              <p>待还、部分还款与逾期风险</p>
            </div>
            <button class="plain-pill" type="button" @click="router.push('/bills')">查看账单</button>
          </div>
          <div class="bill-status-grid">
            <div v-for="item in billStatusItems" :key="item.label" class="status-chip" :class="item.className">
              <span>{{ item.label }}</span>
              <el-tooltip :content="tooltipText(item.value)" placement="top" popper-class="dashboard-value-tooltip">
                <strong class="dashboard-value-ellipsis">{{ item.value }}</strong>
              </el-tooltip>
            </div>
          </div>
          <div class="amount-line">
            <span>待还总额</span>
            <el-tooltip :content="tooltipText(money(billOverview.totalBillAmount))" placement="top" popper-class="dashboard-value-tooltip">
              <strong class="dashboard-value-ellipsis">{{ money(billOverview.totalBillAmount) }}</strong>
            </el-tooltip>
          </div>
        </article>

        <article class="dashboard-panel compact-panel profit-summary-panel">
          <div class="panel-head">
            <div>
              <h2>收益统计</h2>
              <p>{{ selectedYear }} 年利润与手续费</p>
            </div>
            <button class="plain-pill" type="button" @click="router.push('/profits')">查看收益</button>
          </div>
          <div class="profit-highlight">
            <span>年度净利润</span>
            <el-tooltip :content="tooltipText(signedMoney(profitOverview.totalNetProfit))" placement="top" popper-class="dashboard-value-tooltip">
              <strong :class="['dashboard-value-ellipsis', numberValue(profitOverview.totalNetProfit) >= 0 ? 'is-income' : 'is-cost']">
                {{ signedMoney(profitOverview.totalNetProfit) }}
              </strong>
            </el-tooltip>
          </div>
          <div class="amount-line">
            <span>手续费</span>
            <el-tooltip :content="tooltipText(money(profitOverview.totalFeeAmount))" placement="top" popper-class="dashboard-value-tooltip">
              <strong class="dashboard-value-ellipsis">{{ money(profitOverview.totalFeeAmount) }}</strong>
            </el-tooltip>
          </div>
          <div class="amount-line">
            <span>待收手续费</span>
            <el-tooltip :content="tooltipText(money(profitOverview.unpaidFeeAmount))" placement="top" popper-class="dashboard-value-tooltip">
              <strong class="dashboard-value-ellipsis">{{ money(profitOverview.unpaidFeeAmount) }}</strong>
            </el-tooltip>
          </div>
        </article>

        <article class="dashboard-panel compact-panel card-summary-panel">
          <div class="panel-head">
            <div>
              <h2>卡务概况</h2>
              <p>信用卡、借记卡与银行分布</p>
            </div>
            <button class="plain-pill" type="button" @click="router.push('/cards')">查看卡务</button>
          </div>
          <div class="card-kpi-grid">
            <div class="card-kpi-item">
              <span>信用卡</span>
              <el-tooltip :content="tooltipText(stats.creditCardCount || 0)" placement="top" popper-class="dashboard-value-tooltip">
                <strong class="dashboard-value-ellipsis">{{ stats.creditCardCount || 0 }}</strong>
              </el-tooltip>
            </div>
            <div class="card-kpi-item">
              <span>借记卡</span>
              <el-tooltip :content="tooltipText(stats.debitCardCount || 0)" placement="top" popper-class="dashboard-value-tooltip">
                <strong class="dashboard-value-ellipsis">{{ stats.debitCardCount || 0 }}</strong>
              </el-tooltip>
            </div>
            <div class="card-kpi-item is-clickable" role="button" tabindex="0" @click="openCardExpireDialog('all')" @keydown.enter="openCardExpireDialog('all')">
              <span>到期提醒</span>
              <el-tooltip :content="tooltipText(stats.cardExpireReminderCount || 0)" placement="top" popper-class="dashboard-value-tooltip">
                <strong :class="['dashboard-value-ellipsis', stats.cardExpireReminderCount ? 'is-warning' : '']">
                  {{ stats.cardExpireReminderCount || 0 }}
                </strong>
              </el-tooltip>
            </div>
            <div class="card-kpi-item is-clickable" role="button" tabindex="0" @click="openCardExpireDialog('soon')" @keydown.enter="openCardExpireDialog('soon')">
              <span>一个月内</span>
              <el-tooltip :content="tooltipText(stats.cardExpireSoonCount || 0)" placement="top" popper-class="dashboard-value-tooltip">
                <strong :class="['dashboard-value-ellipsis', stats.cardExpireSoonCount ? 'is-warning' : '']">
                  {{ stats.cardExpireSoonCount || 0 }}
                </strong>
              </el-tooltip>
            </div>
            <div class="card-kpi-item is-clickable" role="button" tabindex="0" @click="openCardExpireDialog('expired')" @keydown.enter="openCardExpireDialog('expired')">
              <span>已过期</span>
              <el-tooltip :content="tooltipText(stats.cardExpiredCount || 0)" placement="top" popper-class="dashboard-value-tooltip">
                <strong :class="['dashboard-value-ellipsis', stats.cardExpiredCount ? 'is-danger' : '']">
                  {{ stats.cardExpiredCount || 0 }}
                </strong>
              </el-tooltip>
            </div>
          </div>
          <div class="bank-chart-section">
            <div v-if="bankRankItems.length" class="bank-chart-wrap">
              <VChart class="bank-donut-chart" :option="bankDistributionChartOption" autoresize />
              <div class="bank-chart-legend">
                <div v-for="bank in bankRankItems" :key="bank.bankName" class="bank-legend-row">
                  <span class="legend-name" :title="bank.bankName">{{ bank.bankName || '未知银行' }}</span>
                  <span class="legend-meta">
                    <strong>{{ bank.cardCount }}</strong>
                    <em>{{ bank.share }}%</em>
                  </span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无银行分布" :image-size="50" />
          </div>
        </article>
      </aside>
    </section>

    <el-dialog
      v-model="cardExpireDialogVisible"
      class="card-expire-dialog"
      :title="cardExpireDialogTitle"
      width="860px"
      append-to-body
    >
      <el-table
        :data="filteredCardExpireReminders"
        height="420"
        empty-text="暂无到期银行卡"
      >
        <el-table-column prop="userName" label="用户" min-width="110" show-overflow-tooltip />
        <el-table-column label="银行卡" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.bankName || '-' }}</span>
            <span class="card-last4">尾号 {{ row.cardNoLast4 || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cardTypeDesc" label="卡类型" width="92" />
        <el-table-column prop="expireDate" label="有效期" width="100" />
        <el-table-column label="卡状态" width="92">
          <template #default="{ row }">
            <el-tag :type="cardStatusTagType(row.status)" effect="light" size="small">
              {{ row.statusDesc || '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="到期状态" width="118">
          <template #default="{ row }">
            <el-tag :type="row.expireStatus === 'expired' ? 'danger' : 'warning'" effect="light" size="small">
              {{ row.expireStatusDesc }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <EventDrawer
      v-model:visible="drawerVisible"
      :event-id="editingId"
      :default-date="defaultNewDate"
      @saved="handleCalendarChanged"
      @deleted="handleCalendarChanged"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Dashboard' })

import { RefreshRight } from '@element-plus/icons-vue'
import { defineAsyncComponent, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardStatsApi } from '@/api/dashboard'
import { getBillOverviewApi } from '@/api/bill'
import { getProfitOverviewApi } from '@/api/profit'
import { getCalendarStatsApi, getMonthEventsApi } from '@/api/calendar'
import { ElMessage } from '@/plugins/element-feedback'
import { formatAmount } from '@/utils/formatters'
import {
  BILL_STATUS_MAP,
  BILL_STATUS_TAG_TYPE,
  EVENT_CATEGORY_MAP,
  EVENT_STATUS_MAP,
  EVENT_STATUS_VALUE
} from '@/constants/dict'

use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

const EventDrawer = defineAsyncComponent(() => import('../calendar/EventDrawer.vue'))
const router = useRouter()

interface DailyTrendItem { date: string; income: number; expense: number }
interface BankDistItem { bankName: string; cardCount: number }
type CardExpireFilter = 'all' | 'soon' | 'expired'
interface CardExpireReminderItem {
  id: number
  userName: string
  bankName: string
  cardNoLast4: string
  cardType: number
  cardTypeDesc: string
  expireDate: string
  status: number
  statusDesc: string
  expireStatus: 'soon' | 'expired'
  expireStatusDesc: string
}
interface DashboardStats {
  totalOwners: number
  totalCards: number
  creditCardCount: number
  debitCardCount: number
  cardExpireSoonCount: number
  cardExpiredCount: number
  cardExpireReminderCount: number
  pendingReminderCount: number
  overdueBillCount: number
  monthlyIncome: number
  monthlyExpense: number
  upcomingCount: number
  todayDueCount: number
  dailyTrend: DailyTrendItem[]
  recentLogs: any[]
  upcomingBills: any[]
  bankDistribution: BankDistItem[]
  cardExpireReminders: CardExpireReminderItem[]
}
interface BillOverview {
  billCount: number
  pendingCount: number
  repaidCount: number
  partialCount: number
  overdueCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  totalNetProfit: number
}
interface ProfitOverview {
  userCount: number
  cardCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  expectedNetProfit: number
  totalNetProfit: number
  paidFeeAmount: number
  unpaidFeeAmount: number
  paidFeeCount: number
  unpaidFeeCount: number
}
interface CalendarStats {
  todoCount: number
  doingCount: number
  doneCount: number
  cancelledCount: number
}
interface CalendarEvent {
  id: number
  title: string
  eventDate: string
  startTime?: string
  endTime?: string
  category?: number
  categoryName?: string
  status?: number
}

const currentDate = new Date()
const todayKey = formatDateKey(currentDate)
const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const selectedMonthValue = ref(`${currentDate.getFullYear()}-${String(currentDate.getMonth() + 1).padStart(2, '0')}`)
const selectedYear = computed(() => Number(selectedMonthValue.value.slice(0, 4)) || currentDate.getFullYear())
const selectedMonth = computed(() => Number(selectedMonthValue.value.slice(5, 7)) || currentDate.getMonth() + 1)
const currentMonthValue = computed(() => selectedMonthValue.value)
const currentMonthText = computed(() => `${selectedYear.value}年${String(selectedMonth.value).padStart(2, '0')}月`)

const stats = ref<DashboardStats>({
  totalOwners: 0,
  totalCards: 0,
  creditCardCount: 0,
  debitCardCount: 0,
  cardExpireSoonCount: 0,
  cardExpiredCount: 0,
  cardExpireReminderCount: 0,
  pendingReminderCount: 0,
  overdueBillCount: 0,
  monthlyIncome: 0,
  monthlyExpense: 0,
  upcomingCount: 0,
  todayDueCount: 0,
  dailyTrend: [],
  recentLogs: [],
  upcomingBills: [],
  bankDistribution: [],
  cardExpireReminders: []
})
const billOverview = ref<BillOverview>({
  billCount: 0,
  pendingCount: 0,
  repaidCount: 0,
  partialCount: 0,
  overdueCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalOtherFeeAmount: 0,
  totalNetProfit: 0
})
const profitOverview = ref<ProfitOverview>({
  userCount: 0,
  cardCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalOtherFeeAmount: 0,
  expectedNetProfit: 0,
  totalNetProfit: 0,
  paidFeeAmount: 0,
  unpaidFeeAmount: 0,
  paidFeeCount: 0,
  unpaidFeeCount: 0
})
const calendarStats = ref<CalendarStats>({
  todoCount: 0,
  doingCount: 0,
  doneCount: 0,
  cancelledCount: 0
})
const calendarEvents = ref<CalendarEvent[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const editingId = ref<number | null>(null)
const defaultNewDate = ref('')
const selectedDayDate = ref(todayKey)
const cardExpireDialogVisible = ref(false)
const cardExpireFilter = ref<CardExpireFilter>('all')

const headerModules = [
  { label: '用户', icon: 'UserFilled', route: '/users' },
  { label: '卡务', icon: 'CreditCard', route: '/cards' },
  { label: '账单', icon: 'Document', route: '/bills' },
  { label: '收益', icon: 'TrendCharts', route: '/profits' },
  { label: '日历', icon: 'Calendar', route: '/calendar' }
]

const calendarTotal = computed(() =>
  calendarStats.value.todoCount
  + calendarStats.value.doingCount
  + calendarStats.value.doneCount
  + calendarStats.value.cancelledCount
)
const calendarActiveTotal = computed(() => Math.max(calendarTotal.value - calendarStats.value.cancelledCount, 0))
const calendarDonePercent = computed(() => {
  if (!calendarActiveTotal.value) return 0
  return Math.round((calendarStats.value.doneCount / calendarActiveTotal.value) * 100)
})
const billAttentionCount = computed(() =>
  numberValue(billOverview.value.pendingCount)
  + numberValue(billOverview.value.partialCount)
  + numberValue(billOverview.value.overdueCount)
)
const monthlyNet = computed(() => numberValue(stats.value.monthlyIncome) - numberValue(stats.value.monthlyExpense))
const todayEventsCount = computed(() =>
  calendarEvents.value.filter(event => event.eventDate === todayKey && Number(event.status) !== EVENT_STATUS_VALUE.CANCELLED).length
)
const cardExpireDialogTitle = computed(() => {
  if (cardExpireFilter.value === 'soon') return '一个月内到期银行卡'
  if (cardExpireFilter.value === 'expired') return '已过期银行卡'
  return '银行卡到期提醒'
})
const filteredCardExpireReminders = computed(() => {
  const rows = stats.value.cardExpireReminders || []
  if (cardExpireFilter.value === 'all') return rows
  return rows.filter(item => item.expireStatus === cardExpireFilter.value)
})
const calendarSummary = computed(() => [
  { key: 'todo', label: '待办', value: calendarStats.value.todoCount || 0, percent: safePercent(calendarStats.value.todoCount, calendarTotal.value) },
  { key: 'doing', label: '进行中', value: calendarStats.value.doingCount || 0, percent: safePercent(calendarStats.value.doingCount, calendarTotal.value) },
  { key: 'done', label: '已完成', value: calendarStats.value.doneCount || 0, percent: safePercent(calendarStats.value.doneCount, calendarTotal.value) },
  { key: 'cancelled', label: '已取消', value: calendarStats.value.cancelledCount || 0, percent: safePercent(calendarStats.value.cancelledCount, calendarTotal.value) }
])

const billStatusItems = computed(() => [
  { label: '待还款', value: billOverview.value.pendingCount || 0, className: 'is-warning' },
  { label: '部分还款', value: billOverview.value.partialCount || 0, className: 'is-muted' },
  { label: '已还清', value: billOverview.value.repaidCount || 0, className: 'is-success' },
  { label: '逾期', value: billOverview.value.overdueCount || 0, className: 'is-danger' }
])

const moduleCards = computed(() => [
  {
    key: 'users',
    title: '用户信息',
    desc: '持卡人与关联业务',
    icon: 'UserFilled',
    route: '/users',
    metrics: [
      { label: '用户总数', value: `${stats.value.totalOwners || 0}` },
      { label: '关联银行卡', value: `${stats.value.totalCards || 0}` },
      { label: '待处理提醒', value: `${stats.value.pendingReminderCount || 0}`, className: stats.value.pendingReminderCount ? 'is-warning' : '' }
    ]
  },
  {
    key: 'cards',
    title: '卡务管理',
    desc: '卡片结构与银行分布',
    icon: 'CreditCard',
    route: '/cards',
    metrics: [
      { label: '银行卡总数', value: `${stats.value.totalCards || 0}` },
      { label: '信用卡', value: `${stats.value.creditCardCount || 0}` },
      { label: '借记卡', value: `${stats.value.debitCardCount || 0}` }
    ]
  },
  {
    key: 'bills',
    title: '账单信息',
    desc: '待还、逾期与到期风险',
    icon: 'Document',
    route: '/bills',
    metrics: [
      { label: '待处理账单', value: `${billAttentionCount.value}`, className: billAttentionCount.value ? 'is-warning' : '' },
      { label: '逾期账单', value: `${billOverview.value.overdueCount || stats.value.overdueBillCount || 0}`, className: (billOverview.value.overdueCount || stats.value.overdueBillCount) ? 'is-danger' : '' },
      { label: '今日到期', value: `${stats.value.todayDueCount || 0}` }
    ]
  },
  {
    key: 'profits',
    title: '收益统计',
    desc: `${selectedYear.value} 年利润概览`,
    icon: 'TrendCharts',
    route: '/profits',
    metrics: [
      { label: '年度净利润', value: signedMoney(profitOverview.value.totalNetProfit), className: numberValue(profitOverview.value.totalNetProfit) >= 0 ? 'is-income' : 'is-danger' },
      { label: '本月净额', value: signedMoney(monthlyNet.value), className: monthlyNet.value >= 0 ? 'is-income' : 'is-danger' },
      { label: '待收手续费', value: money(profitOverview.value.unpaidFeeAmount) }
    ]
  },
  {
    key: 'calendar',
    title: '日历计划',
    desc: `${currentMonthText.value} 待办安排`,
    icon: 'Calendar',
    route: '/calendar',
    metrics: [
      { label: '今日日程', value: `${todayEventsCount.value}` },
      { label: '本月待办', value: `${calendarStats.value.todoCount || 0}` },
      { label: '完成率', value: `${calendarDonePercent.value}%`, className: 'is-income' }
    ]
  }
])

const bankRankItems = computed(() => {
  const max = Math.max(...bankChartItems.value.map(item => numberValue(item.cardCount)), 1)
  return bankChartItems.value.map(item => ({
    ...item,
    percent: Math.round((numberValue(item.cardCount) / max) * 100),
    share: safePercent(item.cardCount, bankDistributionTotal.value)
  }))
})

const bankChartItems = computed(() => {
  const rows = [...(stats.value.bankDistribution || [])]
    .sort((a, b) => numberValue(b.cardCount) - numberValue(a.cardCount))
  const topRows = rows.slice(0, 5)
  const otherCount = rows.slice(5).reduce((sum, item) => sum + numberValue(item.cardCount), 0)
  return otherCount > 0
    ? [...topRows, { bankName: '其他', cardCount: otherCount }]
    : topRows
})

const bankDistributionTotal = computed(() =>
  bankChartItems.value.reduce((sum, item) => sum + numberValue(item.cardCount), 0)
)

const bankDistributionCenterText = computed(() => `${bankDistributionTotal.value || 0}张`)

const bankDistributionChartOption = computed(() => ({
  color: ['#2563eb', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br/>{c} 张 · {d}%'
  },
  legend: { show: false },
  series: [
    {
      name: '银行分布',
      type: 'pie',
      radius: ['54%', '78%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      labelLine: { show: false },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 3,
        borderRadius: 6
      },
      emphasis: {
        scaleSize: 4,
        label: { show: false }
      },
      data: bankRankItems.value.map(item => ({
        name: item.bankName || '未知银行',
        value: numberValue(item.cardCount)
      })),
      z: 1
    },
    {
      name: '银行总数',
      type: 'pie',
      radius: ['0%', '42%'],
      center: ['50%', '50%'],
      silent: true,
      label: {
        show: true,
        position: 'center',
        formatter: bankDistributionCenterText.value,
        color: '#1f2a37',
        fontSize: 18,
        fontWeight: 900,
        lineHeight: 22
      },
      labelLine: { show: false },
      itemStyle: { color: 'transparent' },
      data: [{ value: 1 }],
      z: 0
    }
  ]
}))

const miniCalendarCells = computed(() => {
  const year = selectedYear.value
  const month = selectedMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const startOffset = (firstDay.getDay() + 6) % 7
  const startDate = new Date(year, month - 1, 1 - startOffset)
  const eventMap = new Map<string, number>()
  for (const event of calendarEvents.value) {
    if (!event.eventDate) continue
    eventMap.set(event.eventDate, (eventMap.get(event.eventDate) || 0) + 1)
  }

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)
    const key = formatDateKey(date)
    return {
      key,
      day: date.getDate(),
      other: date.getMonth() + 1 !== month,
      isToday: key === todayKey,
      eventCount: eventMap.get(key) || 0
    }
  })
})

const selectedDayEvents = computed(() =>
  [...calendarEvents.value]
    .filter(event => event.eventDate === selectedDayDate.value)
    .sort((a, b) => `${a.startTime || '99:99'} ${a.endTime || ''} ${a.id}`.localeCompare(`${b.startTime || '99:99'} ${b.endTime || ''} ${b.id}`))
)
const selectedDayStats = computed(() => ({
  total: selectedDayEvents.value.length,
  todo: selectedDayEvents.value.filter(event => Number(event.status ?? EVENT_STATUS_VALUE.TODO) === EVENT_STATUS_VALUE.TODO).length,
  doing: selectedDayEvents.value.filter(event => Number(event.status) === EVENT_STATUS_VALUE.DOING).length,
  done: selectedDayEvents.value.filter(event => Number(event.status) === EVENT_STATUS_VALUE.DONE).length,
  cancelled: selectedDayEvents.value.filter(event => Number(event.status) === EVENT_STATUS_VALUE.CANCELLED).length
}))
const selectedDayTitle = computed(() => formatDisplayDate(selectedDayDate.value))

function numberValue(value: unknown) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num : 0
}

function money(value: unknown) {
  return `¥${formatAmount(numberValue(value))}`
}

function signedMoney(value: unknown) {
  const num = numberValue(value)
  if (num > 0) return `+${money(num)}`
  if (num < 0) return `-${money(Math.abs(num))}`
  return money(num)
}

function tooltipText(value: unknown) {
  if (value == null || value === '') return '-'
  return String(value)
}

function safePercent(value: unknown, total: unknown) {
  const totalNum = numberValue(total)
  if (!totalNum) return 0
  return Math.max(0, Math.min(100, Math.round((numberValue(value) / totalNum) * 100)))
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDisplayDate(date?: string) {
  if (!date) return '-'
  const [year, month, day] = date.split('-')
  if (!year || !month || !day) return date
  return `${year}年${Number(month)}月${Number(day)}日`
}

function eventTimeRange(event: CalendarEvent) {
  const start = event.startTime ? String(event.startTime).slice(0, 5) : '全天'
  const end = event.endTime ? String(event.endTime).slice(0, 5) : ''
  return end && start !== '全天' ? `${start}-${end}` : start
}

function eventCategoryLabel(category?: number) {
  if (category == null) return '其他'
  return EVENT_CATEGORY_MAP[category] || '其他'
}

function eventCategoryColor(category?: number) {
  const colors: Record<number, string> = {
    0: '#0958d9',
    1: '#2f9e44',
    2: '#d97706',
    3: '#cf1322',
    4: '#64748b',
    5: '#6d28d9'
  }
  return category == null ? '#64748b' : (colors[category] || '#64748b')
}

function eventStatusLabel(status?: number) {
  if (status == null) return '待办'
  return EVENT_STATUS_MAP[status] || '待办'
}

function eventStatusClass(status?: number) {
  if (status === EVENT_STATUS_VALUE.DOING) return 'is-doing'
  if (status === EVENT_STATUS_VALUE.DONE) return 'is-done'
  if (status === EVENT_STATUS_VALUE.CANCELLED) return 'is-cancelled'
  return 'is-todo'
}

function openCalendarDrawer(event: CalendarEvent | null, date?: string) {
  if (event?.id) {
    editingId.value = event.id
    defaultNewDate.value = ''
  } else {
    editingId.value = null
    defaultNewDate.value = date || todayKey
  }
  drawerVisible.value = true
}

function openDayPanel(date: string) {
  selectedDayDate.value = date
}

function openCardExpireDialog(filter: CardExpireFilter) {
  cardExpireFilter.value = filter
  cardExpireDialogVisible.value = true
}

function cardStatusTagType(status: number | null | undefined) {
  const value = Number(status ?? 0)
  if (value === 0) return 'success'
  if (value === 1) return 'warning'
  return 'danger'
}

async function goTodayInCalendar() {
  selectedDayDate.value = todayKey
  const todayMonth = todayKey.slice(0, 7)
  if (selectedMonthValue.value !== todayMonth) {
    selectedMonthValue.value = todayMonth
    await refreshDashboard()
  }
}

async function refreshCalendarPanel() {
  const [statsRes, eventsRes] = await Promise.allSettled([
    getCalendarStatsApi(currentMonthValue.value),
    getMonthEventsApi(currentMonthValue.value)
  ])
  if (statsRes.status === 'fulfilled') {
    calendarStats.value = { ...calendarStats.value, ...(statsRes.value.data || {}) }
  }
  if (eventsRes.status === 'fulfilled') {
    calendarEvents.value = eventsRes.value.data || []
  }
}

async function handlePeriodChange() {
  await refreshDashboard()
}

async function handleCalendarChanged() {
  await refreshCalendarPanel()
}

function billStatusDesc(status: number) {
  return BILL_STATUS_MAP[status] || '未知'
}

function billStatusTag(status: number) {
  return BILL_STATUS_TAG_TYPE[status] || 'info'
}

async function refreshDashboard() {
  loading.value = true
  const results = await Promise.allSettled([
    getDashboardStatsApi(),
    getBillOverviewApi({ year: selectedYear.value }),
    getProfitOverviewApi({ year: selectedYear.value }),
    getCalendarStatsApi(currentMonthValue.value),
    getMonthEventsApi(currentMonthValue.value)
  ])

  if (results[0].status === 'fulfilled') {
    stats.value = { ...stats.value, ...(results[0].value.data || {}) }
  }
  if (results[1].status === 'fulfilled') {
    billOverview.value = { ...billOverview.value, ...(results[1].value.data || {}) }
  }
  if (results[2].status === 'fulfilled') {
    profitOverview.value = { ...profitOverview.value, ...(results[2].value.data || {}) }
  }
  if (results[3].status === 'fulfilled') {
    calendarStats.value = { ...calendarStats.value, ...(results[3].value.data || {}) }
  }
  if (results[4].status === 'fulfilled') {
    calendarEvents.value = results[4].value.data || []
  }

  if (results.every(item => item.status === 'rejected')) {
    ElMessage.error('首页看板数据加载失败')
  }
  loading.value = false
}

onMounted(refreshDashboard)
</script>

<style scoped lang="scss">
$primary: #0958d9;
$primary-light: #eaf2ff;
$bg: #f5f8fc;
$panel: #ffffff;
$line: #dbe2ea;
$line-soft: #edf2f7;
$ink: #1f2a37;
$ink2: #667085;
$muted: #98a2b3;
$success: #2f9e44;
$warning: #d97706;
$danger: #cf1322;
$purple: #6d28d9;

.dashboard-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  margin: -20px;
  padding: 10px 12px;
  height: calc(100vh - var(--header-height));
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(234, 242, 255, 0.72) 0%, rgba(245, 248, 252, 0.96) 330px),
    $bg;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba($line, 0.9);
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-title-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: $primary;
  background: linear-gradient(180deg, rgba($primary, 0.12) 0%, rgba($primary, 0.06) 100%);
  border: 1px solid rgba($primary, 0.1);
}

.header-title-group {
  min-width: 0;
}

.page-title {
  margin: 0;
  color: $ink;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0;
}

.page-subtitle {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 3px;
  color: $ink2;
  font-size: 12px;
  line-height: 1.4;

  strong {
    color: $primary;
    font-weight: 800;
  }
}

.sub-divider {
  width: 1px;
  height: 10px;
  background: #d7dee8;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.module-nav-btn,
.plain-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba($line, 0.95);
  border-radius: 999px;
  background: #fff;
  color: #526074;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease, transform 0.2s ease;

  &:hover {
    color: $primary;
    border-color: rgba($primary, 0.26);
    background: $primary-light;
    transform: translateY(-1px);
  }
}

.plain-pill.is-primary {
  color: #fff;
  border-color: $primary;
  background: $primary;

  &:hover {
    color: #fff;
    background: #1677ff;
  }
}

.refresh-btn.el-button {
  height: 28px;
  border-radius: 999px;
  border-color: rgba($primary, 0.2);
  color: $primary;
  background: rgba($primary, 0.06);
  font-weight: 700;
}

.dashboard-shell {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  grid-template-rows: minmax(0, 0.96fr) minmax(0, 1.04fr);
  gap: 10px;
  align-items: stretch;
  min-width: 0;
  min-height: 0;
}

.calendar-summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 0;
}

.calendar-stat {
  min-width: 0;
  padding: 5px 7px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 10px;
  background: #fff;

  span {
    display: block;
    color: $ink2;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    display: block;
    margin-top: 4px;
    color: $ink;
    font-size: 16px;
    line-height: 1;
    font-weight: 900;
  }

  .stat-track {
    display: block;
    height: 3px;
    margin-top: 5px;
    border-radius: 999px;
    background: #eef3f8;
    overflow: hidden;
  }

  .stat-track b {
    display: block;
    height: 100%;
    min-width: 4px;
    border-radius: inherit;
    background: $primary;
  }

  &.is-todo .stat-track b { background: $warning; }
  &.is-doing .stat-track b { background: $primary; }
  &.is-done .stat-track b { background: $success; }
  &.is-cancelled .stat-track b { background: #64748b; }
}

.mini-calendar {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 7px;
  border: 1px solid rgba($line, 0.9);
  border-radius: 12px;
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
  box-sizing: border-box;
}

.mini-week,
.mini-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  width: 100%;
  box-sizing: border-box;
}

.mini-grid {
  flex: 0 0 auto;
  grid-template-rows: repeat(6, minmax(0, 1fr));
  aspect-ratio: 7 / 6;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba($line, 0.75);
  border-radius: 10px;
}

.mini-week span {
  padding: 0 0 4px;
  min-width: 0;
  box-sizing: border-box;
  color: $muted;
  font-size: 11px;
  font-weight: 800;
  line-height: 16px;
  text-align: center;
}

.mini-cell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1 / 1;
  min-height: 0;
  min-width: 0;
  padding: 3px;
  box-sizing: border-box;
  border-right: 1px solid rgba($line, 0.75);
  border-bottom: 1px solid rgba($line, 0.75);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(249, 251, 254, 0.98) 100%);
  cursor: pointer;
  overflow: hidden;
  transition: background 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;

  &:nth-child(7n) {
    border-right: none;
  }

  &:nth-last-child(-n + 7) {
    border-bottom: none;
  }

  &:hover {
    z-index: 1;
    background: $primary-light;
    box-shadow: inset 0 0 0 1px rgba($primary, 0.22), 0 8px 16px rgba(9, 88, 217, 0.08);

    .mini-add {
      opacity: 1;
      transform: scale(1);
    }
  }

  &.other {
    color: #c1cad6;
    background: #f8fafc;
  }

  &.today {
    box-shadow: inset 0 0 0 1px rgba($primary, 0.32);
    background: $primary-light;
  }

  &.selected {
    z-index: 1;
    background: #fff;
    box-shadow: inset 0 0 0 2px rgba($primary, 0.5), 0 8px 16px rgba(9, 88, 217, 0.1);

    .mini-day {
      color: $primary;
    }
  }

  &.has-events::after {
    content: '';
    position: absolute;
    left: 50%;
    bottom: 5px;
    width: 18px;
    height: 3px;
    border-radius: 999px;
    background: linear-gradient(90deg, $primary, $success);
    transform: translateX(-50%);
    pointer-events: none;
  }
}

.mini-day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  color: $ink2;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  pointer-events: none;
}

.mini-count {
  position: absolute;
  top: 3px;
  right: 3px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  box-sizing: border-box;
  border-radius: 999px;
  color: $primary;
  background: rgba($primary, 0.08);
  border: 1px solid rgba($primary, 0.16);
  font-size: 10px;
  font-weight: 800;
  line-height: 14px;
  text-align: center;
  pointer-events: none;
}

.mini-add {
  position: absolute;
  right: 4px;
  bottom: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  box-sizing: border-box;
  border: 1px solid rgba($primary, 0.2);
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.92);
  color: $primary;
  font-size: 12px;
  line-height: 1;
  font-weight: 900;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.88);
  transition: opacity 0.18s ease, transform 0.18s ease, background 0.18s ease;

  &:hover {
    background: #fff;
  }
}

.event-list {
  display: flex;
  order: 3;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  min-height: 0;
  overflow: auto;
  padding: 0 2px 10px 0;
}

.selected-day-title {
  order: 2;
  align-items: flex-start;
  margin: 8px 0 7px;

  span {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  em {
    display: inline-flex;
    align-items: center;
    height: 20px;
    padding: 0 7px;
    border-radius: 999px;
    color: #fff;
    background: $primary;
    font-size: 10px;
    font-style: normal;
    font-weight: 900;
  }
}

.inline-day-stats {
  order: 1;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  height: var(--calendar-top-row-height);
  flex: 0 0 var(--calendar-top-row-height);
  margin-bottom: 0;

  span {
    display: flex;
    flex-direction: column;
    justify-content: center;
    height: 100%;
    padding: 5px 4px;
    box-sizing: border-box;
    border-radius: 10px;
    font-size: 10px;
  }

  strong {
    margin-top: 2px;
    font-size: 13px;
  }
}

.event-item {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 11px;
  background: #fbfdff;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    border-color: rgba($primary, 0.24);
    background: $primary-light;
    box-shadow: 0 8px 16px rgba(9, 88, 217, 0.06);

    .event-edit-btn {
      color: $primary;
      border-color: rgba($primary, 0.22);
      background: #fff;
    }
  }
}

.event-date {
  color: $primary;
  font-size: 11px;
  font-weight: 800;
}

.event-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.event-title {
  color: $ink;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 2px;
  color: $muted;
  font-size: 11px;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  em {
    font-style: normal;
    color: #7c8799;
  }
}

.event-status {
  justify-self: end;
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;

  &.is-todo { color: $warning; background: #fff4db; }
  &.is-doing { color: $primary; background: $primary-light; }
  &.is-done { color: $success; background: #e8f7ed; }
  &.is-cancelled { color: #64748b; background: #eef2f6; }
}

.event-edit-btn {
  display: none;
  height: 24px;
  padding: 0 8px;
  border: 1px solid rgba($line, 0.95);
  border-radius: 999px;
  background: #fff;
  color: $muted;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.day-stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;

  span {
    min-width: 0;
    padding: 8px;
    border: 1px solid rgba($line-soft, 0.95);
    border-radius: 11px;
    background: #f8fbff;
    color: $muted;
    font-size: 11px;
    font-weight: 800;
    text-align: center;
  }

  strong {
    display: block;
    margin-top: 4px;
    color: $ink;
    font-size: 16px;
    line-height: 1;
    font-weight: 900;
  }
}

.bill-status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-bottom: 0;
}

.status-chip {
  min-width: 0;
  padding: 6px 7px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 12px;
  background: #fff;

  span {
    color: $muted;
    font-size: 11px;
    font-weight: 700;
  }

  strong {
    display: block;
    margin-top: 4px;
    color: $ink;
    font-size: 16px;
    line-height: 1;
    font-weight: 900;
  }

  &.is-warning strong { color: $warning; }
  &.is-success strong { color: $success; }
  &.is-danger strong { color: $danger; }
  &.is-muted strong { color: #64748b; }
}

.amount-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  padding: 5px 0;
  border-top: 1px dashed rgba($line, 0.95);

  span {
    color: $ink2;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    color: $ink;
    font-family: var(--font-mono);
    font-size: 14px;
    font-weight: 900;
  }

  .is-income { color: $success; }
  .is-cost { color: $danger; }
}

.upcoming-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.upcoming-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 86px 76px 64px;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 8px 9px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 12px;
  background: #fff;
}

.upcoming-main {
  display: flex;
  flex-direction: column;
  min-width: 0;

  strong {
    color: $ink;
    font-size: 13px;
    font-weight: 800;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  small {
    margin-top: 3px;
    color: $muted;
    font-size: 11px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.upcoming-money {
  color: $ink;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 900;
  text-align: right;
}

.upcoming-date {
  color: $ink2;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.dashboard-shell {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
  overflow: hidden;
  border: 1px solid rgba($line, 0.95);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.module-lane {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  padding: 14px;
  border-right: 1px solid rgba($line, 0.82);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 251, 255, 0.92) 100%);
  overflow: hidden;

  &:last-child {
    border-right: none;
  }

  &::before {
    content: '';
    position: absolute;
    left: 14px;
    right: 14px;
    top: 0;
    height: 3px;
    border-radius: 0 0 999px 999px;
    background: var(--lane-color, #{$primary});
  }
}

.module-lane.is-users { --lane-color: #0958d9; }
.module-lane.is-cards { --lane-color: #2f9e44; }
.module-lane.is-bills { --lane-color: #d97706; }
.module-lane.is-profits { --lane-color: #cf1322; }
.module-lane.is-calendar { --lane-color: #6d28d9; }

.lane-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 9px;
  align-items: center;
  min-width: 0;
}

.lane-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 11px;
  color: var(--lane-color);
  background: color-mix(in srgb, var(--lane-color) 10%, white);
  border: 1px solid color-mix(in srgb, var(--lane-color) 18%, white);
  font-size: 12px;
  font-weight: 900;
}

.lane-title {
  color: $ink;
  font-size: 15px;
  font-weight: 900;
  line-height: 1.25;
}

.lane-desc {
  margin-top: 2px;
  color: $muted;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lane-main {
  min-width: 0;
  padding: 12px 0 10px;
  border-bottom: 1px dashed rgba($line, 0.95);

  span,
  small {
    display: block;
    color: $muted;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    display: block;
    margin-top: 8px;
    color: var(--lane-color);
    font-size: 34px;
    line-height: 1;
    font-weight: 900;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &.is-income { color: $success; }
    &.is-cost { color: $danger; }
  }

  small {
    margin-top: 8px;
    color: $ink2;
  }
}

.lane-pairs {
  display: grid;
  gap: 8px;
  min-width: 0;

  div {
    min-width: 0;
    padding: 9px 10px;
    border-radius: 12px;
    background: rgba(245, 248, 252, 0.92);
  }

  span {
    display: block;
    color: $muted;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    display: block;
    margin-top: 5px;
    color: $ink;
    font-size: 17px;
    font-weight: 900;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.lane-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.lane-action,
.lane-actions .lane-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 30px;
  margin-top: auto;
  border: 1px solid color-mix(in srgb, var(--lane-color) 18%, white);
  border-radius: 10px;
  background: color-mix(in srgb, var(--lane-color) 7%, white);
  color: var(--lane-color);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.lane-action.is-primary {
  color: #fff;
  background: var(--lane-color);
  border-color: var(--lane-color);
}

.lane-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: auto;
}

.bank-rank-list {
  display: grid;
  align-content: start;
  gap: 8px;
}

.rank-row {
  display: grid;
  grid-template-columns: minmax(56px, 0.72fr) minmax(0, 1fr) 28px;
  align-items: center;
  gap: 8px;
  color: $ink2;
  font-size: 12px;
  font-weight: 800;

  span {
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  i {
    display: block;
    height: 6px;
    border-radius: 999px;
    background: #eef3f8;
    overflow: hidden;
  }

  b {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: var(--lane-color);
  }

  strong {
    color: $ink;
    font-size: 12px;
    text-align: right;
  }
}

.module-lane .bill-status-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.module-lane .status-chip {
  background: rgba(245, 248, 252, 0.92);
}

.module-lane .upcoming-list,
.module-lane .event-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.module-lane .upcoming-item {
  grid-template-columns: minmax(0, 1fr) 78px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.82);

  .upcoming-date,
  :deep(.el-tag) {
    display: none;
  }
}

.module-lane .calendar-summary-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.module-lane .mini-calendar {
  flex: 0 0 210px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.82);
}

.module-lane .mini-cell {
  padding: 3px 4px;
}

.module-lane .event-item {
  grid-template-columns: 46px minmax(0, 1fr) auto;
  padding: 8px;
  background: rgba(255, 255, 255, 0.82);
}

@media (max-width: 1440px) {
  .dashboard-page {
    height: auto;
    min-height: calc(100vh - var(--header-height));
    overflow: auto;
  }

  .dashboard-shell {
    grid-template-columns: 1fr;
    grid-template-rows: none;
  }

  .module-lane {
    border-right: none;
    border-bottom: 1px solid rgba($line, 0.82);
  }
}

/* Second-version dashboard: module preview cards + natural summary area */
.dashboard-page {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  margin: -20px;
  padding: 10px 12px;
  height: calc(100vh - var(--header-height));
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(234, 242, 255, 0.76) 0%, rgba(245, 248, 252, 0.96) 320px),
    $bg;
}

.dashboard-header {
  min-height: 54px;
  padding: 9px 12px;
  border-radius: 14px;
}

.period-controls {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px;
  border: 1px solid rgba($line, 0.9);
  border-radius: 999px;
  background: #f8fbff;
}

.period-date-picker {
  width: 136px;

  :deep(.el-input__wrapper) {
    min-height: 24px;
    border-radius: 999px;
    box-shadow: none;
    background: #fff;
  }

  :deep(.el-input__inner) {
    color: $ink;
    font-size: 12px;
    font-weight: 800;
  }
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
  align-items: stretch;
}

.module-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 112px;
  padding: 10px;
  border: 1px solid rgba($line, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.045);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 14px;
    right: 14px;
    top: 0;
    height: 3px;
    border-radius: 0 0 999px 999px;
    background: var(--module-color, #{$primary});
  }
}

.module-card.is-users { --module-color: #0958d9; }
.module-card.is-cards { --module-color: #2f9e44; }
.module-card.is-bills { --module-color: #d97706; }
.module-card.is-profits { --module-color: #cf1322; }
.module-card.is-calendar { --module-color: #6d28d9; }

.dashboard-value-ellipsis {
  display: block;
  min-width: 0;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.module-card-head {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
  min-height: 32px;

  h2 {
    margin: 0;
    color: $ink;
    font-size: 15px;
    line-height: 1.25;
    font-weight: 900;
  }

  p {
    margin: 3px 0 0;
    color: $muted;
    font-size: 11px;
    line-height: 1.35;
    font-weight: 700;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.module-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  color: var(--module-color);
  background: color-mix(in srgb, var(--module-color) 10%, white);
  border: 1px solid color-mix(in srgb, var(--module-color) 18%, white);
}

.module-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 9px;
  min-height: 44px;
}

.metric-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 44px;
  padding: 6px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 12px;
  background: #f8fbff;

  span {
    display: block;
    color: $muted;
    font-size: 11px;
    line-height: 1.25;
    font-weight: 800;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  strong {
    display: block;
    margin-top: 5px;
    color: $ink;
    font-size: 14px;
    line-height: 1;
    font-weight: 900;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  strong.is-income { color: $success; }
  strong.is-warning { color: $warning; }
  strong.is-danger { color: $danger; }
}

.card-enter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: auto;
  height: 24px;
  margin-top: 0;
  padding: 0 9px;
  border: 1px solid color-mix(in srgb, var(--module-color) 18%, white);
  border-radius: 10px;
  background: color-mix(in srgb, var(--module-color) 7%, white);
  color: var(--module-color);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
    background: color-mix(in srgb, var(--module-color) 11%, white);
  }
}

.dashboard-content {
  display: grid;
  grid-template-columns: minmax(720px, 1.08fr) minmax(430px, 0.92fr);
  gap: 10px;
  align-items: stretch;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.dashboard-panel {
  min-width: 0;
  min-height: 0;
  border: 1px solid rgba($line, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.97);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.045);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: 0 0 52px;
  min-width: 0;
  min-height: 52px;
  padding: 9px 12px;
  border-bottom: 1px solid rgba($line-soft, 0.98);

  h2 {
    margin: 0;
    color: $ink;
    font-size: 15px;
    line-height: 1.25;
    font-weight: 900;
  }

  p {
    margin: 3px 0 0;
    color: $muted;
    font-size: 12px;
    line-height: 1.35;
    font-weight: 700;
  }
}

.panel-actions {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
}

.calendar-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.calendar-body {
  --calendar-top-row-height: 58px;
  display: grid;
  grid-template-columns: minmax(280px, 0.42fr) minmax(0, 0.58fr);
  gap: 12px;
  padding: 12px;
  min-width: 0;
  flex: 1;
  height: 100%;
  min-height: 0;
  max-height: none;
  overflow: hidden;
}

.calendar-left,
.calendar-right {
  min-width: 0;
}

.calendar-left,
.calendar-right {
  display: flex;
  flex-direction: column;
  min-height: 0;
  align-self: start;
}

.calendar-left {
  height: 100%;
}

.calendar-right {
  height: 100%;
  overflow: hidden;
}

.calendar-summary-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  height: var(--calendar-top-row-height);
  flex: 0 0 var(--calendar-top-row-height);
  margin-bottom: 0;
}

.calendar-summary-row .calendar-stat {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  box-sizing: border-box;
}

.mini-calendar {
  flex: 0 0 auto;
  height: auto;
  min-height: 0;
  margin-top: 8px;
}

.calendar-left .mini-calendar {
  width: 100%;
}

.calendar-left .mini-grid {
  flex: 0 0 auto;
  aspect-ratio: 7 / 6;
  grid-template-rows: repeat(6, minmax(0, 1fr));
  min-height: 0;
}

.calendar-left .mini-cell {
  min-height: 0;
}

.event-list {
  order: 3;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;

  span {
    color: $ink;
    font-size: 13px;
    font-weight: 900;
  }

  small {
    color: $muted;
    font-size: 12px;
    font-weight: 700;
  }
}

.summary-stack {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: minmax(142px, 0.82fr) minmax(0, 1.18fr);
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.bill-summary-panel,
.profit-summary-panel {
  min-height: 0;

  .panel-head {
    flex: 0 0 48px;
    min-height: 48px;
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .plain-pill {
    padding: 0 8px;
  }
}

.card-summary-panel {
  grid-column: 1 / -1;
  min-height: 0;
}

.compact-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding-bottom: 0;

  .bill-status-grid,
  .bank-rank-list,
  .bank-chart-section,
  .card-kpi-grid,
  .card-type-row,
  .card-expire-row,
  .profit-highlight,
  .amount-line {
    margin-left: 12px;
    margin-right: 12px;
  }

  .bill-status-grid,
  .card-kpi-grid,
  .card-type-row,
  .card-expire-row,
  .profit-highlight {
    margin-top: 9px;
  }
}

.bill-status-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 48px;
  gap: 5px;
}

.bill-summary-panel .status-chip {
  min-height: 44px;
}

.bill-summary-panel .amount-line,
.profit-summary-panel .amount-line {
  min-height: 30px;
  padding-top: 4px;
  padding-bottom: 4px;
}

.profit-highlight {
  min-height: 58px;
  padding: 9px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 13px;
  background: linear-gradient(180deg, #fff 0%, #f8fbff 100%);

  span {
    display: block;
    color: $muted;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin-top: 6px;
    font-family: var(--font-mono);
    font-size: 19px;
    line-height: 1;
    font-weight: 900;
  }

  .is-income { color: $success; }
  .is-cost { color: $danger; }
}

.card-kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
  min-height: 54px;
}

.card-kpi-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 12px;
  background: #f8fbff;

  span {
    display: block;
    min-width: 0;
    color: $muted;
    font-size: 12px;
    line-height: 1.2;
    font-weight: 800;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  strong {
    display: block;
    min-width: 0;
    margin-top: 5px;
    color: $ink;
    font-size: 18px;
    line-height: 1;
    font-weight: 900;
  }

  .is-warning { color: $warning; }
  .is-danger { color: $danger; }
}

.card-kpi-item.is-clickable {
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

  &:hover,
  &:focus-visible {
    border-color: rgba($primary, 0.28);
    background: $primary-light;
    box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
    outline: none;
    transform: translateY(-1px);
  }
}

.card-type-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  min-height: 48px;

  div {
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 8px;
    border: 1px solid rgba($line-soft, 0.95);
    border-radius: 12px;
    background: #f8fbff;
  }

  span {
    display: block;
    color: $muted;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin-top: 4px;
    color: $ink;
    font-size: 17px;
    line-height: 1;
    font-weight: 900;
  }
}

.card-expire-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  min-height: 48px;

  .expire-stat-card {
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 8px;
    border: 1px solid rgba($line-soft, 0.95);
    border-radius: 12px;
    background: #fff;
  }

  .is-clickable {
    cursor: pointer;
    transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

    &:hover,
    &:focus-visible {
      border-color: rgba($primary, 0.28);
      background: $primary-light;
      box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
      outline: none;
      transform: translateY(-1px);
    }
  }

  span {
    display: block;
    color: $muted;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    display: block;
    margin-top: 4px;
    color: $ink;
    font-size: 16px;
    line-height: 1;
    font-weight: 900;
  }

  .is-warning { color: $warning; }
  .is-danger { color: $danger; }
}

.card-last4 {
  margin-left: 6px;
  color: $muted;
  font-size: 12px;
  font-weight: 700;
}

:deep(.card-expire-dialog .el-dialog__body) {
  padding-top: 8px;
}

.bank-rank-list {
  margin-top: 8px;
  display: grid;
  gap: 6px;
  align-content: start;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 2px;
}

.rank-row {
  grid-template-columns: minmax(74px, 0.8fr) minmax(0, 1fr) 34px;
}

.bank-chart-section {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 10px;
  overflow: hidden;
}

.bank-chart-wrap {
  display: grid;
  grid-template-columns: minmax(170px, 0.95fr) minmax(0, 1.05fr);
  align-items: center;
  gap: 12px;
  height: 100%;
  min-height: 176px;
  padding: 10px 12px;
  border: 1px solid rgba($line-soft, 0.95);
  border-radius: 12px;
  background: linear-gradient(180deg, #fff 0%, #f8fbff 100%);
  overflow: hidden;
}

.bank-donut-chart {
  width: 100%;
  height: 176px;
  min-height: 176px;
}

.bank-chart-legend {
  display: grid;
  align-content: center;
  gap: 7px;
  min-width: 0;
  min-height: 0;
  max-height: 176px;
  overflow: hidden;
}

.bank-legend-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: $ink2;
  font-size: 12px;
  line-height: 1.15;
  font-weight: 800;
}

.legend-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-meta {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  color: $muted;
  font-family: var(--font-mono);
  white-space: nowrap;

  strong {
    color: $ink;
    font-size: 13px;
    font-weight: 900;
  }

  em {
    font-style: normal;
    font-size: 11px;
    font-weight: 800;
  }
}

@media (max-width: 1280px) {
  .module-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-content {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .summary-stack {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto;
  }

  .card-summary-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1440px) and (min-width: 1281px) {
  .calendar-body {
    grid-template-columns: minmax(260px, 0.40fr) minmax(0, 0.60fr);
  }
}

@media (max-width: 1100px) {
  .dashboard-header,
  .header-actions {
    align-items: flex-start;
  }

  .dashboard-header {
    flex-direction: column;
  }

  .module-grid,
  .summary-stack,
  .calendar-body {
    grid-template-columns: 1fr;
  }

  .summary-stack {
    grid-template-rows: none;
  }

  .card-summary-panel {
    grid-column: auto;
  }

  .calendar-body {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .calendar-right {
    min-height: 260px;
  }

  .mini-calendar {
    width: 100%;
    height: auto;
  }

  .calendar-left .mini-grid {
    aspect-ratio: 7 / 6;
    flex: 0 0 auto;
    grid-template-rows: repeat(6, minmax(0, 1fr));
    min-height: 0;
  }

  .card-kpi-grid,
  .bank-chart-wrap {
    grid-template-columns: 1fr;
  }

  .bank-donut-chart {
    height: 160px;
    min-height: 160px;
  }
}
</style>
