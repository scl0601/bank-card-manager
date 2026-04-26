<template>
  <div class="mobile-page mobile-calendar-page">
    <section class="hero-card mobile-card mobile-card--flat">
      <div class="hero-top">
        <div class="month-switcher">
          <button class="icon-circle btn-bounce" :disabled="monthChanging" @click="prevMonth">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button class="month-trigger btn-bounce" @click="toggleMonthPicker">
            <span>{{ monthTitle }}</span>
            <small>{{ viewMode === 'month' ? '月视图' : '日视图' }}</small>
          </button>
          <button class="icon-circle btn-bounce" :disabled="monthChanging" @click="nextMonth">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="hero-actions">
          <button class="ghost-pill btn-bounce" :disabled="isTodayContext" @click="goToday">今天</button>
          <button class="primary-pill btn-bounce" @click="openQuickCreate()">
            <el-icon><Plus /></el-icon>
            <span>新建</span>
          </button>
        </div>
      </div>

      <div class="hero-toolbar">
        <div class="view-switch">
          <button
            :class="['view-btn', { active: viewMode === 'month' }]"
            @click="switchView('month')"
          >
            月视图
          </button>
          <button
            :class="['view-btn', { active: viewMode === 'day' }]"
            @click="switchView('day')"
          >
            日视图
          </button>
        </div>
        <div class="toolbar-actions">
          <button class="toolbar-btn btn-bounce" @click="filterSheetVisible = true">
            <el-icon><Filter /></el-icon>
            <span>筛选</span>
          </button>
          <button class="toolbar-btn btn-bounce" @click="actionSheetVisible = true">
            <el-icon><MoreFilled /></el-icon>
            <span>更多</span>
          </button>
        </div>
      </div>

      <div class="search-row">
        <div class="search-box" :class="{ focused: searchFocused }">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="keyword"
            placeholder="搜索标题或备注"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <button v-if="keyword" class="search-clear" @click="keyword = ''">×</button>
        </div>
      </div>

      <div class="stats-scroll">
        <button
          v-for="item in statsCards"
          :key="item.key"
          :class="['stats-card', `stats-${item.key}`, { active: quickFilter === item.key }]"
          @click="toggleQuickFilter(item.key)"
        >
          <span class="stats-label">{{ item.label }}</span>
          <strong class="stats-value">{{ item.value }}</strong>
          <small class="stats-hint">{{ item.hint }}</small>
        </button>
      </div>

      <div v-if="hasActiveFilters" class="active-filters">
        <span class="active-summary">筛选中</span>
        <span v-for="tag in activeFilterTags" :key="tag" class="filter-chip">{{ tag }}</span>
        <button class="link-btn" @click="resetFilters">清空</button>
      </div>

      <div class="hero-foot">
        <span>{{ monthStats.total }} 项日程</span>
        <span v-if="hasActiveFilters">当前命中 {{ filteredMonthData.length }} 项</span>
        <span>{{ monthCompletionRate }}% 完成率</span>
      </div>

      <div class="calendar-legend">
        <span class="legend-item"><i class="legend-dot legend-dot--today"></i>今天</span>
        <span class="legend-item"><i class="legend-dot legend-dot--selected"></i>已选日期</span>
        <span class="legend-item"><i class="legend-dot legend-dot--marked"></i>有日程</span>
      </div>
    </section>

    <section
      v-if="viewMode === 'month'"
      class="month-view"
      @touchstart.passive="onMonthTouchStart"
      @touchend.passive="onMonthTouchEnd"
    >
      <div class="panel-card calendar-panel mobile-card mobile-card--flat" :class="{ busy: loadingMonth || monthChanging }">
        <div class="week-header">
          <span v-for="(day, index) in weekDays" :key="day" :class="{ weekend: index >= 5 }">{{ day }}</span>
        </div>
        <div class="month-grid">
          <button
            v-for="cell in calendarCells"
            :key="cell.key"
            type="button"
            :class="[
              'day-cell',
              {
                other: cell.isOther,
                today: cell.isToday,
                weekend: cell.isWeekend,
                selected: selectedDate === cell.date,
                pressed: pressedCellDate === cell.date,
                dense: viewportWidth <= 360,
                hidden: hasActiveFilters && getSourceCellEvents(cell.date).length > 0 && getFilteredCellEvents(cell.date).length === 0,
                marked: getFilteredCellEvents(cell.date).length > 0,
              },
            ]"
            @click="handleCellTap(cell)"
            @touchstart="startCellPress(cell)"
            @touchend="clearCellPress"
            @touchcancel="clearCellPress"
            @touchmove="clearCellPress"
          >
            <div class="day-cell-head">
              <span class="day-number">{{ cell.day }}</span>
              <span v-if="getFilteredCellEvents(cell.date).length" class="day-count">
                {{ getFilteredCellEvents(cell.date).length }}
              </span>
            </div>
            <div class="day-cell-body">
              <template v-if="getFilteredCellEvents(cell.date).length > 0">
                <span
                  v-for="event in getFilteredCellEvents(cell.date).slice(0, compactCellLimit)"
                  :key="event.id"
                  class="mini-event"
                  :style="{ '--cell-accent': CATEGORY_COLOR[event.category] }"
                >
                  <i></i>
                  <span>{{ event.title }}</span>
                </span>
                <span v-if="getFilteredCellEvents(cell.date).length > compactCellLimit" class="mini-more">
                  +{{ getFilteredCellEvents(cell.date).length - compactCellLimit }}
                </span>
              </template>
              <span v-else-if="hasActiveFilters && getSourceCellEvents(cell.date).length" class="mini-muted">
                已筛掉
              </span>
              <div v-else class="day-dots">
                <i
                  v-for="(event, index) in getSourceCellEvents(cell.date).slice(0, 3)"
                  :key="`${cell.date}-${index}`"
                  :style="{ background: CATEGORY_COLOR[event.category] }"
                ></i>
              </div>
            </div>
          </button>
        </div>
      </div>

      <div class="gesture-hint">左右滑动切换月份，长按日期快速新建</div>

      <section class="panel-card selected-panel mobile-card mobile-card--flat">
        <div class="panel-head">
          <div>
            <h3>{{ selectedDateText }}</h3>
            <p>{{ relativeDateText }} · {{ selectedDaySourceEvents.length }} 项日程</p>
          </div>
          <button class="panel-link btn-bounce" @click="switchView('day')">查看日视图</button>
        </div>

        <div class="day-stat-row">
          <span class="day-stat total">共 {{ selectedDaySourceEvents.length }} 项</span>
          <span v-if="selectedDayStats.todoCount" class="day-stat todo">待办 {{ selectedDayStats.todoCount }}</span>
          <span v-if="selectedDayStats.doingCount" class="day-stat doing">进行中 {{ selectedDayStats.doingCount }}</span>
          <span v-if="selectedDayStats.doneCount" class="day-stat done">已完成 {{ selectedDayStats.doneCount }}</span>
          <span v-if="selectedDayStats.cancelledCount" class="day-stat cancelled">已取消 {{ selectedDayStats.cancelledCount }}</span>
        </div>

        <div class="selection-overview">
          <div class="selection-pill selection-pill--primary">
            <strong>{{ relativeDateText }}</strong>
            <span>{{ selectedDaySourceEvents.length ? '当前日期概览' : '可安排新日程' }}</span>
          </div>
          <div class="selection-pill">
            <strong>{{ selectedDayTimeSummary }}</strong>
            <span>时间分布</span>
          </div>
          <div class="selection-pill" :class="{ accent: selectedDayStats.doneCount === selectedDaySourceEvents.length && selectedDaySourceEvents.length > 0 }">
            <strong>{{ selectedDayFocusText }}</strong>
            <span>关注重点</span>
          </div>
        </div>

        <div v-if="loadingDay" class="skeleton-list">
          <div v-for="item in 3" :key="item" class="skeleton-item shimmer-bg"></div>
        </div>
        <template v-else>
          <transition-group v-if="agendaSections.length" name="list-stagger" tag="div" class="agenda-wrap">
            <section v-for="section in agendaSections" :key="section.key" class="agenda-section">
              <div class="agenda-head">
                <div>
                  <strong>{{ section.label }}</strong>
                  <small>{{ section.hint }}</small>
                </div>
                <span>{{ section.events.length }}</span>
              </div>
              <transition-group name="list-stagger" tag="div" class="agenda-list">
                <EventCard
                  v-for="event in section.events"
                  :key="event.id"
                  :event="event"
                  :pending-id="pendingStatusId"
                  @click="viewEvent(event)"
                  @status-change="changeStatus"
                />
              </transition-group>
            </section>
          </transition-group>
          <div v-else class="mobile-empty compact-empty">
            <div class="empty-icon">📅</div>
            <div class="empty-text">当前日期暂无匹配日程</div>
            <button class="empty-add-btn btn-bounce" @click="openQuickCreate(selectedDate)">新建日程</button>
          </div>
        </template>
      </section>
    </section>

    <section
      v-else
      class="day-view"
      @touchstart.passive="onDayTouchStart"
      @touchend.passive="onDayTouchEnd"
    >
      <section class="panel-card day-focus-card mobile-card mobile-card--flat">
        <div class="focus-top">
          <button class="icon-circle btn-bounce" @click="moveDay(-1)">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <div class="focus-main">
            <strong>{{ selectedDateHeadline }}</strong>
            <span>{{ selectedDateText }}</span>
          </div>
          <button class="icon-circle btn-bounce" @click="moveDay(1)">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="focus-foot">
          <div class="focus-badges">
            <span class="focus-badge primary">{{ relativeDateText }}</span>
            <span class="focus-badge">完成率 {{ selectedDayCompletionRate }}%</span>
            <span class="focus-badge" v-if="hasActiveFilters">筛选后 {{ filteredDayEvents.length }} 项</span>
          </div>
          <div class="focus-actions">
            <button class="ghost-chip btn-bounce" :disabled="isTodaySelected" @click="goToday">回到今天</button>
            <button class="ghost-chip btn-bounce" @click="switchView('month')">查看月历</button>
          </div>
        </div>
      </section>

      <section class="panel-card week-strip-card mobile-card mobile-card--flat">
        <div class="panel-head panel-head-compact">
          <div>
            <h3>本周日期</h3>
            <p>轻触切换日期，单手查看更顺畅</p>
          </div>
          <button class="panel-link btn-bounce" :disabled="isTodaySelected" @click="goToday">今天</button>
        </div>
        <div class="week-strip">
          <button
            v-for="item in selectedWeekDays"
            :key="item.date"
            type="button"
            :class="['week-day', { selected: item.isSelected, today: item.isToday }]"
            @click="applySelectedDate(item.date)"
          >
            <span class="week-day-label">{{ item.weekLabel }}</span>
            <strong>{{ item.dayLabel }}</strong>
            <small>{{ item.count }}项</small>
          </button>
        </div>
      </section>

      <transition name="fade-slide">
        <section v-if="batchMode" class="panel-card batch-panel mobile-card mobile-card--flat">
          <div class="batch-top">
            <strong>批量模式</strong>
            <button class="link-btn" @click="exitBatchMode">退出</button>
          </div>
          <div class="batch-foot">
            <label class="batch-check">
              <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
              <span>全选可操作日程</span>
            </label>
            <span>{{ selectedIds.length }} 项已选</span>
          </div>
        </section>
      </transition>

      <section class="panel-card agenda-panel mobile-card mobile-card--flat">
        <div class="panel-head panel-head-compact">
          <div>
            <h3>当天安排</h3>
            <p>{{ filteredDayEvents.length }} 项日程</p>
          </div>
          <button class="panel-link btn-bounce" @click="openQuickCreate(selectedDate)">新增</button>
        </div>

        <div v-if="loadingDay" class="skeleton-list">
          <div v-for="item in 4" :key="item" class="skeleton-item shimmer-bg"></div>
        </div>
        <template v-else>
          <transition-group v-if="agendaSections.length" name="list-stagger" tag="div" class="agenda-wrap large-gap">
            <section v-for="section in agendaSections" :key="section.key" class="agenda-section">
              <div class="agenda-head">
                <div>
                  <strong>{{ section.label }}</strong>
                  <small>{{ section.hint }}</small>
                </div>
                <span>{{ section.events.length }}</span>
              </div>
              <transition-group name="list-stagger" tag="div" class="agenda-list">
                <EventCard
                  v-for="event in section.events"
                  :key="event.id"
                  :event="event"
                  :batch-mode="batchMode"
                  :selected="selectedIds.includes(event.id)"
                  :pending-id="pendingStatusId"
                  @click="viewEvent(event)"
                  @toggle-select="toggleSelect"
                  @status-change="changeStatus"
                />
              </transition-group>
            </section>
          </transition-group>
          <div v-else class="mobile-empty compact-empty">
            <div class="empty-icon">🗂️</div>
            <div class="empty-text">当天暂无匹配日程</div>
            <button class="empty-add-btn btn-bounce" @click="openQuickCreate(selectedDate)">新建日程</button>
          </div>
        </template>
      </section>

      <div class="gesture-hint">左右滑动切换日期</div>
    </section>

    <transition name="fade-slide">
      <div v-if="showMonthPicker" class="overlay-mask" @click.self="showMonthPicker = false">
        <div class="month-picker-panel">
          <div class="picker-head">
            <button class="picker-year-btn btn-bounce" @click="pickerYear--">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <strong>{{ pickerYear }} 年</strong>
            <button class="picker-year-btn btn-bounce" @click="pickerYear++">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <div class="picker-grid">
            <button
              v-for="month in 12"
              :key="month"
              :class="['picker-month', { active: pickerYear === currentYear && month === currentMonth }]"
              @click="jumpToMonth(pickerYear, month)"
            >
              {{ month }}月
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade-slide">
      <div v-if="filterSheetVisible" class="overlay-mask" @click.self="filterSheetVisible = false">
        <div class="sheet-panel">
          <div class="sheet-handle"></div>
          <div class="sheet-head">
            <strong>筛选条件</strong>
            <button class="link-btn" @click="resetFilters">清空</button>
          </div>
          <div class="sheet-body">
            <section class="sheet-section">
              <h4>状态</h4>
              <div class="sheet-options wrap">
                <button
                  v-for="item in quickFilterOptions"
                  :key="item.key"
                  :class="['sheet-option', { active: quickFilter === item.key }]"
                  @click="quickFilter = item.key"
                >
                  {{ item.label }}
                </button>
              </div>
            </section>
            <section class="sheet-section">
              <h4>分类</h4>
              <div class="sheet-options wrap">
                <button
                  :class="['sheet-option', { active: filterCategory === undefined }]"
                  @click="filterCategory = undefined"
                >
                  全部
                </button>
                <button
                  v-for="item in EVENT_CATEGORY_OPTIONS"
                  :key="item.value"
                  :class="['sheet-option', { active: filterCategory === item.value }]"
                  @click="filterCategory = item.value"
                >
                  <i class="sheet-dot" :style="{ background: CATEGORY_COLOR[item.value] }"></i>
                  {{ item.label }}
                </button>
              </div>
            </section>
          </div>
          <div class="sheet-actions">
            <button class="ghost-pill btn-bounce" @click="filterSheetVisible = false">完成</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade-slide">
      <div v-if="actionSheetVisible" class="overlay-mask" @click.self="actionSheetVisible = false">
        <div class="sheet-panel action-sheet">
          <div class="sheet-handle"></div>
          <div class="sheet-head">
            <strong>更多操作</strong>
            <button class="link-btn" @click="actionSheetVisible = false">关闭</button>
          </div>
          <div class="action-list">
            <button class="action-item action-item--primary" @click="handleUtilityAction('create')">
              <el-icon><Plus /></el-icon>
              <span>快速新建</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('refresh')">
              <el-icon><RefreshRight /></el-icon>
              <span>刷新数据</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('batch')">
              <el-icon><Operation /></el-icon>
              <span>{{ batchMode ? '退出批量模式' : '进入批量模式' }}</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('export-day')">
              <el-icon><Download /></el-icon>
              <span>导出当天 CSV</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('export-month')">
              <el-icon><Download /></el-icon>
              <span>导出本月 CSV</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('export-month-filtered')">
              <el-icon><Download /></el-icon>
              <span>导出筛选结果 CSV</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('print-day')">
              <el-icon><Download /></el-icon>
              <span>打印当天日程</span>
            </button>
            <button class="action-item" @click="handleUtilityAction('print-month')">
              <el-icon><Download /></el-icon>
              <span>打印本月日程</span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade-slide">
      <div v-if="quickCreateVisible" class="overlay-mask" @click.self="closeQuickCreate">
        <div class="sheet-panel quick-create-sheet">
          <div class="sheet-handle"></div>
          <div class="sheet-head">
            <strong>快速新建</strong>
            <button class="link-btn" @click="closeQuickCreate">关闭</button>
          </div>
          <div class="quick-create-date">{{ quickCreateDateText }}</div>
          <p class="quick-create-tip">保留与 PC 端一致的字段逻辑，先选择常用模板，再进入完整编辑页。</p>
          <div class="quick-create-grid">
            <button
              v-for="preset in quickCreatePresets"
              :key="preset.key"
              type="button"
              class="quick-create-card btn-bounce"
              @click="createFromPreset(preset)"
            >
              <strong>{{ preset.label }}</strong>
              <small>{{ preset.hint }}</small>
            </button>
          </div>
          <div class="sheet-actions dual-actions">
            <button class="ghost-pill btn-bounce" @click="closeQuickCreate">取消</button>
            <button class="primary-pill btn-bounce" @click="openAdvancedCreate">高级创建</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="mobile-popup-up">
      <div v-if="batchMode && selectedIds.length > 0 && viewMode === 'day'" class="batch-actions-bar">
        <button class="batch-bar-btn done btn-bounce" @click="batchMarkDone">批量完成</button>
        <button class="batch-bar-btn delete btn-bounce" @click="batchDelete">批量删除</button>
      </div>
    </transition>

    <div class="mobile-safe-bottom spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Download,
  Filter,
  MoreFilled,
  Operation,
  Plus,
  RefreshRight,
  Search,
} from '@element-plus/icons-vue'
import {
  EVENT_CATEGORY_MAP,
  EVENT_CATEGORY_OPTIONS,
  EVENT_PRIORITY_MAP,
  EVENT_STATUS_MAP,
  EVENT_STATUS_VALUE,
} from '@/constants/dict'
import {
  deleteEventApi,
  getDayEventsApi,
  getMonthEventsApi,
  updateEventStatusApi,
} from '@/api/calendar'
import EventCard from '../../components/EventCard.vue'
import {
  CATEGORY_COLOR,
  formatDate,
  formatDisplayDate,
  formatTime,
  isWeekend,
} from '../../utils/constants'

type ViewMode = 'month' | 'day'
type QuickFilter = 'all' | 'todo' | 'doing' | 'done' | 'cancelled'

interface CalendarCell {
  key: string
  date: string
  day: number
  isOther: boolean
  isToday: boolean
  isWeekend: boolean
}

interface WeekDayItem {
  date: string
  dayLabel: string
  weekLabel: string
  isToday: boolean
  isSelected: boolean
  count: number
}

interface EventCreatePrefill {
  startTime?: string
  endTime?: string
  priority?: number
}

interface QuickCreatePreset extends EventCreatePrefill {
  key: string
  label: string
  hint: string
  dateOffset?: number
}

const router = useRouter()
const route = useRoute()
const weekDays = ['一', '二', '三', '四', '五', '六', '日']
const quickFilterOptions: Array<{ key: QuickFilter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'todo', label: '待办' },
  { key: 'doing', label: '进行中' },
  { key: 'done', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]
const quickCreatePresets: QuickCreatePreset[] = [
  { key: 'all-day', label: '全天事项', hint: '直接进入完整编辑', priority: 1 },
  { key: 'morning', label: '上午跟进', hint: '09:00 - 10:00', startTime: '09:00', endTime: '10:00', priority: 1 },
  { key: 'afternoon', label: '下午处理', hint: '14:00 - 15:00', startTime: '14:00', endTime: '15:00', priority: 1 },
  { key: 'evening', label: '晚间提醒', hint: '19:00 - 20:00', startTime: '19:00', endTime: '20:00' },
  { key: 'tomorrow', label: '明日待办', hint: '自动带入明天 09:00', dateOffset: 1, startTime: '09:00', endTime: '10:00' },
]

const now = new Date()
const todayStr = formatDate(now)
const viewMode = ref<ViewMode>('month')
const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth() + 1)
const selectedDate = ref(todayStr)
const monthData = ref<any[]>([])
const dayEvents = ref<any[]>([])
const loadingMonth = ref(false)
const loadingDay = ref(false)
const monthChanging = ref(false)
const pendingStatusId = ref<number | null>(null)
const searchFocused = ref(false)
const keyword = ref('')
const filterCategory = ref<number | undefined>(undefined)
const quickFilter = ref<QuickFilter>('all')
const batchMode = ref(false)
const selectedIds = ref<number[]>([])
const showMonthPicker = ref(false)
const filterSheetVisible = ref(false)
const actionSheetVisible = ref(false)
const quickCreateVisible = ref(false)
const quickCreateDate = ref(todayStr)
const pickerYear = ref(now.getFullYear())
const viewportWidth = ref(window.innerWidth)
const pressedCellDate = ref('')
const compactCellLimit = computed(() => viewportWidth.value <= 375 ? 1 : 2)

let monthTouchStartX = 0
let monthTouchStartY = 0
let dayTouchStartX = 0
let dayTouchStartY = 0
let monthRequestToken = 0
let dayRequestToken = 0
let cellPressTimer = 0
let suppressNextCellClick = false

const monthTitle = computed(() => `${currentYear.value}年${String(currentMonth.value).padStart(2, '0')}月`)
const isTodaySelected = computed(() => selectedDate.value === todayStr)
const isTodayContext = computed(() => isTodaySelected.value && currentYear.value === now.getFullYear() && currentMonth.value === now.getMonth() + 1)

const activeStatusValue = computed<number | undefined>(() => {
  if (quickFilter.value === 'todo') return EVENT_STATUS_VALUE.TODO
  if (quickFilter.value === 'doing') return EVENT_STATUS_VALUE.DOING
  if (quickFilter.value === 'done') return EVENT_STATUS_VALUE.DONE
  if (quickFilter.value === 'cancelled') return EVENT_STATUS_VALUE.CANCELLED
  return undefined
})

const hasActiveFilters = computed(() => {
  return !!keyword.value.trim() || filterCategory.value !== undefined || quickFilter.value !== 'all'
})

const activeFilterTags = computed(() => {
  const tags: string[] = []
  if (quickFilter.value !== 'all') {
    tags.push(`状态：${quickFilterOptions.find((item) => item.key === quickFilter.value)?.label || '全部'}`)
  }
  if (filterCategory.value !== undefined) {
    tags.push(`分类：${EVENT_CATEGORY_MAP[filterCategory.value]}`)
  }
  if (keyword.value.trim()) {
    tags.push(`关键词：${keyword.value.trim()}`)
  }
  return tags
})

const filteredMonthData = computed(() => filterEvents(monthData.value))
const filteredDayEvents = computed(() => filterEvents(dayEvents.value))
const selectedDaySourceEvents = computed(() => sortEvents(dayEvents.value))

const monthEventMap = computed(() => buildEventMap(monthData.value))
const filteredMonthEventMap = computed(() => buildEventMap(filteredMonthData.value))
const selectedDayStats = computed(() => createStats(dayEvents.value))
const monthStats = computed(() => createStats(monthData.value))

const monthCompletionRate = computed(() => {
  const total = monthStats.value.total - monthStats.value.cancelledCount
  if (!total) return 0
  return Math.round((monthStats.value.doneCount / total) * 100)
})

const selectedDayCompletionRate = computed(() => {
  const total = selectedDayStats.value.total - selectedDayStats.value.cancelledCount
  if (!total) return 0
  return Math.round((selectedDayStats.value.doneCount / total) * 100)
})

const statsCards = computed(() => [
  {
    key: 'all' as QuickFilter,
    label: '全部',
    value: monthStats.value.total,
    hint: hasActiveFilters.value ? `命中 ${filteredMonthData.value.length}` : '查看全部日程',
  },
  {
    key: 'todo' as QuickFilter,
    label: '待办',
    value: monthStats.value.todoCount,
    hint: '优先跟进',
  },
  {
    key: 'doing' as QuickFilter,
    label: '进行中',
    value: monthStats.value.doingCount,
    hint: '处理中任务',
  },
  {
    key: 'done' as QuickFilter,
    label: '已完成',
    value: monthStats.value.doneCount,
    hint: '已归档内容',
  },
  {
    key: 'cancelled' as QuickFilter,
    label: '已取消',
    value: monthStats.value.cancelledCount,
    hint: '已关闭任务',
  },
])

const selectedDateText = computed(() => formatDisplayDate(selectedDate.value))
const selectedDateHeadline = computed(() => {
  const date = new Date(`${selectedDate.value}T00:00:00`)
  return `${date.getMonth() + 1}月${date.getDate()}日`
})
const relativeDateText = computed(() => getRelativeDateText(selectedDate.value))
const quickCreateDateText = computed(() => formatDisplayDate(quickCreateDate.value))
const selectedDayTimeSummary = computed(() => {
  if (!selectedDaySourceEvents.value.length) return '暂无安排'
  const timedEvents = selectedDaySourceEvents.value.filter((event: any) => !!event.startTime)
  if (!timedEvents.length) return '全天安排为主'
  const first = timedEvents[0]
  const last = timedEvents[timedEvents.length - 1]
  const start = formatTime(first.startTime)
  const end = formatTime(last.endTime || last.startTime)
  return end && end !== start ? `${start} - ${end}` : `${start} 开始`
})
const selectedDayFocusText = computed(() => {
  if (!selectedDaySourceEvents.value.length) return '保持轻量安排'
  if (selectedDayStats.value.doingCount) return `进行中 ${selectedDayStats.value.doingCount} 项`
  if (selectedDayStats.value.todoCount) return `待办 ${selectedDayStats.value.todoCount} 项`
  if (selectedDayStats.value.doneCount === selectedDaySourceEvents.value.length) return '已全部完成'
  return `${agendaSections.value[0]?.label || '全天'}优先`
})
const selectedWeekDays = computed<WeekDayItem[]>(() => buildWeekDays(selectedDate.value))

const agendaSections = computed(() => {
  const sections = [
    { key: 'all-day', label: '全天事项', hint: '未设置开始时间', events: [] as any[] },
    { key: 'morning', label: '上午', hint: '06:00 - 11:59', events: [] as any[] },
    { key: 'afternoon', label: '下午', hint: '12:00 - 17:59', events: [] as any[] },
    { key: 'evening', label: '晚间', hint: '18:00 - 23:59', events: [] as any[] },
  ]

  filteredDayEvents.value.forEach((event: any) => {
    sections[getAgendaSectionIndex(event.startTime)].events.push(event)
  })

  return sections.filter((section) => section.events.length > 0)
})

const filteredSelectableDayEvents = computed(() => {
  return filteredDayEvents.value.filter((event: any) => event.status !== EVENT_STATUS_VALUE.CANCELLED)
})

const isAllSelected = computed(() => {
  return filteredSelectableDayEvents.value.length > 0
    && filteredSelectableDayEvents.value.every((event: any) => selectedIds.value.includes(event.id))
})

const calendarCells = computed<CalendarCell[]>(() => {
  const firstDate = new Date(currentYear.value, currentMonth.value - 1, 1)
  const offset = firstDate.getDay() === 0 ? 6 : firstDate.getDay() - 1
  const startDate = new Date(currentYear.value, currentMonth.value - 1, 1 - offset)

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + index)
    const dateKey = formatDate(date)
    return {
      key: dateKey,
      date: dateKey,
      day: date.getDate(),
      isOther: date.getMonth() !== currentMonth.value - 1,
      isToday: dateKey === todayStr,
      isWeekend: isWeekend(date.getFullYear(), date.getMonth() + 1, date.getDate()),
    }
  })
})

watch(
  () => filteredSelectableDayEvents.value.map((event: any) => event.id).join(','),
  () => {
    selectedIds.value = selectedIds.value.filter((id) => filteredSelectableDayEvents.value.some((event: any) => event.id === id))
  },
)

watch(viewMode, (mode) => {
  if (mode === 'month') {
    exitBatchMode()
  }
})

onMounted(async () => {
  window.addEventListener('resize', syncViewportWidth, { passive: true })
  initFromRoute()
  await Promise.all([loadMonthData(), loadDayEvents(selectedDate.value)])
})

onBeforeUnmount(() => {
  clearCellPress()
  window.removeEventListener('resize', syncViewportWidth)
})

function syncViewportWidth() {
  viewportWidth.value = window.innerWidth
}

function initFromRoute() {
  const queryDate = typeof route.query.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(route.query.date)
    ? route.query.date
    : todayStr
  const date = new Date(`${queryDate}T00:00:00`)
  selectedDate.value = queryDate
  currentYear.value = date.getFullYear()
  currentMonth.value = date.getMonth() + 1
  pickerYear.value = currentYear.value

  if (route.query.view === 'day') {
    viewMode.value = 'day'
  }

  if (typeof route.query.category === 'string' && route.query.category !== '') {
    const category = Number(route.query.category)
    if (!Number.isNaN(category)) {
      filterCategory.value = category
    }
  }

  if (typeof route.query.quick === 'string') {
    const matched = quickFilterOptions.find((item) => item.key === route.query.quick)
    if (matched) quickFilter.value = matched.key
  }

  if (typeof route.query.keyword === 'string') {
    keyword.value = route.query.keyword
  }
}

function buildCalendarQuery() {
  const query: Record<string, string> = {
    date: selectedDate.value,
    view: viewMode.value,
  }
  if (filterCategory.value !== undefined) {
    query.category = String(filterCategory.value)
  }
  if (quickFilter.value !== 'all') {
    query.quick = quickFilter.value
  }
  if (keyword.value.trim()) {
    query.keyword = keyword.value.trim()
  }
  return query
}

function toggleMonthPicker() {
  pickerYear.value = currentYear.value
  showMonthPicker.value = !showMonthPicker.value
}

function switchView(mode: ViewMode) {
  viewMode.value = mode
}

function toggleQuickFilter(key: QuickFilter) {
  quickFilter.value = quickFilter.value === key ? 'all' : key
}

function resetFilters() {
  keyword.value = ''
  filterCategory.value = undefined
  quickFilter.value = 'all'
}

function getSourceCellEvents(date: string) {
  return monthEventMap.value[date] || []
}

function getFilteredCellEvents(date: string) {
  return filteredMonthEventMap.value[date] || []
}

function buildEventMap(list: any[]) {
  return list.reduce<Record<string, any[]>>((acc, event) => {
    if (!event.eventDate) return acc
    if (!acc[event.eventDate]) acc[event.eventDate] = []
    acc[event.eventDate].push(event)
    acc[event.eventDate] = sortEvents(acc[event.eventDate])
    return acc
  }, {})
}

function createStats(list: any[]) {
  return {
    total: list.length,
    todoCount: list.filter((event: any) => event.status === EVENT_STATUS_VALUE.TODO).length,
    doingCount: list.filter((event: any) => event.status === EVENT_STATUS_VALUE.DOING).length,
    doneCount: list.filter((event: any) => event.status === EVENT_STATUS_VALUE.DONE).length,
    cancelledCount: list.filter((event: any) => event.status === EVENT_STATUS_VALUE.CANCELLED).length,
  }
}

function filterEvents(list: any[]) {
  let filtered = sortEvents(list)
  if (filterCategory.value !== undefined) {
    filtered = filtered.filter((event: any) => event.category === filterCategory.value)
  }
  if (activeStatusValue.value !== undefined) {
    filtered = filtered.filter((event: any) => event.status === activeStatusValue.value)
  }
  if (keyword.value.trim()) {
    const text = keyword.value.trim().toLowerCase()
    filtered = filtered.filter((event: any) => {
      return `${event.title || ''} ${event.remark || ''} ${event.description || ''}`.toLowerCase().includes(text)
    })
  }
  return filtered
}

function sortEvents(list: any[]) {
  return [...list].sort((a: any, b: any) => {
    const timeDiff = parseTimeMinutes(a.startTime) - parseTimeMinutes(b.startTime)
    if (timeDiff !== 0) return timeDiff
    const priorityDiff = (b.priority ?? 0) - (a.priority ?? 0)
    if (priorityDiff !== 0) return priorityDiff
    return String(a.title || '').localeCompare(String(b.title || ''))
  })
}

function parseTimeMinutes(time?: string) {
  if (!time) return -1
  const [hourText, minuteText = '0'] = String(time).split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText)
  if (Number.isNaN(hour) || Number.isNaN(minute)) return -1
  return hour * 60 + minute
}

function getAgendaSectionIndex(time?: string) {
  const minutes = parseTimeMinutes(time)
  if (minutes < 0) return 0
  if (minutes < 12 * 60) return 1
  if (minutes < 18 * 60) return 2
  return 3
}

function getRelativeDateText(dateKey: string) {
  const today = new Date(`${todayStr}T00:00:00`)
  const target = new Date(`${dateKey}T00:00:00`)
  const diff = Math.round((target.getTime() - today.getTime()) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === -1) return '昨天'
  if (diff === 2) return '后天'
  if (diff === -2) return '前天'
  return diff > 0 ? `${diff}天后` : `${Math.abs(diff)}天前`
}

function shiftDate(dateKey: string, offset: number) {
  const target = new Date(`${dateKey}T00:00:00`)
  target.setDate(target.getDate() + offset)
  return formatDate(target)
}

function buildWeekDays(dateKey: string) {
  const target = new Date(`${dateKey}T00:00:00`)
  const dayIndex = target.getDay() === 0 ? 6 : target.getDay() - 1
  const monday = new Date(target)
  monday.setDate(target.getDate() - dayIndex)

  return Array.from({ length: 7 }, (_, index) => {
    const current = new Date(monday)
    current.setDate(monday.getDate() + index)
    const currentDate = formatDate(current)
    return {
      date: currentDate,
      dayLabel: String(current.getDate()).padStart(2, '0'),
      weekLabel: weekDays[index],
      isToday: currentDate === todayStr,
      isSelected: currentDate === selectedDate.value,
      count: getSourceCellEvents(currentDate).length,
    }
  })
}

async function loadMonthData() {
  const token = ++monthRequestToken
  loadingMonth.value = true
  try {
    const month = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`
    const res: any = await getMonthEventsApi(month)
    if (token !== monthRequestToken) return
    monthData.value = res.data || []
  } catch (error) {
    if (token === monthRequestToken) {
      ElMessage.error('加载月份数据失败')
    }
  } finally {
    if (token === monthRequestToken) {
      loadingMonth.value = false
    }
  }
}

async function loadDayEvents(dateKey = selectedDate.value) {
  const token = ++dayRequestToken
  loadingDay.value = true
  try {
    const res: any = await getDayEventsApi({ eventDate: dateKey })
    if (token !== dayRequestToken) return
    dayEvents.value = res.data || []
  } catch (error) {
    if (token === dayRequestToken) {
      ElMessage.error('加载当天日程失败')
    }
  } finally {
    if (token === dayRequestToken) {
      loadingDay.value = false
    }
  }
}

async function applySelectedDate(dateKey: string) {
  const date = new Date(`${dateKey}T00:00:00`)
  selectedDate.value = dateKey

  const needMonthReload = date.getFullYear() !== currentYear.value || date.getMonth() + 1 !== currentMonth.value
  if (needMonthReload) {
    currentYear.value = date.getFullYear()
    currentMonth.value = date.getMonth() + 1
    pickerYear.value = currentYear.value
    await Promise.all([loadMonthData(), loadDayEvents(dateKey)])
    return
  }

  await loadDayEvents(dateKey)
}

async function changeMonth(offset: number) {
  if (monthChanging.value) return
  monthChanging.value = true
  try {
    const target = new Date(currentYear.value, currentMonth.value - 1 + offset, 1)
    const selectedDay = new Date(`${selectedDate.value}T00:00:00`).getDate()
    const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
    currentYear.value = target.getFullYear()
    currentMonth.value = target.getMonth() + 1
    pickerYear.value = currentYear.value
    selectedDate.value = `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}-${String(Math.min(selectedDay, lastDay)).padStart(2, '0')}`
    await Promise.all([loadMonthData(), loadDayEvents(selectedDate.value)])
  } finally {
    window.setTimeout(() => {
      monthChanging.value = false
    }, 160)
  }
}

async function jumpToMonth(year: number, month: number) {
  showMonthPicker.value = false
  if (year === currentYear.value && month === currentMonth.value) return
  const selectedDay = new Date(`${selectedDate.value}T00:00:00`).getDate()
  const lastDay = new Date(year, month, 0).getDate()
  currentYear.value = year
  currentMonth.value = month
  selectedDate.value = `${year}-${String(month).padStart(2, '0')}-${String(Math.min(selectedDay, lastDay)).padStart(2, '0')}`
  await Promise.all([loadMonthData(), loadDayEvents(selectedDate.value)])
}

async function prevMonth() {
  await changeMonth(-1)
}

async function nextMonth() {
  await changeMonth(1)
}

async function goToday() {
  const date = new Date()
  selectedDate.value = formatDate(date)
  currentYear.value = date.getFullYear()
  currentMonth.value = date.getMonth() + 1
  pickerYear.value = currentYear.value
  await Promise.all([loadMonthData(), loadDayEvents(selectedDate.value)])
}

async function moveDay(offset: number) {
  const target = new Date(`${selectedDate.value}T00:00:00`)
  target.setDate(target.getDate() + offset)
  await applySelectedDate(formatDate(target))
}

async function handleCellTap(cell: CalendarCell) {
  clearCellPress()
  if (suppressNextCellClick) {
    suppressNextCellClick = false
    return
  }
  await applySelectedDate(cell.date)
}

function startCellPress(cell: CalendarCell) {
  clearCellPress()
  pressedCellDate.value = cell.date
  suppressNextCellClick = false
  cellPressTimer = window.setTimeout(() => {
    suppressNextCellClick = true
    selectedDate.value = cell.date
    currentYear.value = new Date(`${cell.date}T00:00:00`).getFullYear()
    currentMonth.value = new Date(`${cell.date}T00:00:00`).getMonth() + 1
    openQuickCreate(cell.date)
  }, 420)
}

function clearCellPress() {
  pressedCellDate.value = ''
  if (cellPressTimer) {
    window.clearTimeout(cellPressTimer)
    cellPressTimer = 0
  }
}

function openQuickCreate(date = selectedDate.value) {
  quickCreateDate.value = date
  quickCreateVisible.value = true
}

function closeQuickCreate() {
  quickCreateVisible.value = false
}

function openNewEvent(date = selectedDate.value, prefill: EventCreatePrefill = {}) {
  const query: Record<string, string> = {
    ...buildCalendarQuery(),
    date,
  }

  if (prefill.startTime) {
    query.startTime = prefill.startTime
  }
  if (prefill.endTime) {
    query.endTime = prefill.endTime
  }
  if (prefill.priority !== undefined) {
    query.priority = String(prefill.priority)
  }

  router.push({
    path: '/m/event',
    query,
  })
}

function createFromPreset(preset: QuickCreatePreset) {
  const date = shiftDate(quickCreateDate.value, preset.dateOffset || 0)
  closeQuickCreate()
  openNewEvent(date, preset)
}

function openAdvancedCreate() {
  const date = quickCreateDate.value
  closeQuickCreate()
  openNewEvent(date)
}

function viewEvent(event: any) {
  if (batchMode.value) {
    toggleSelect(event.id)
    return
  }
  router.push({
    path: `/m/event/${event.id}`,
    query: buildCalendarQuery(),
  })
}

async function changeStatus(id: number, status: number) {
  if (pendingStatusId.value === id) return
  pendingStatusId.value = id
  try {
    await updateEventStatusApi(id, status)
    monthData.value = monthData.value.map((event: any) => event.id === id ? { ...event, status } : event)
    dayEvents.value = dayEvents.value.map((event: any) => event.id === id ? { ...event, status } : event)
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('状态更新失败')
  } finally {
    pendingStatusId.value = null
  }
}

function toggleSelect(id: number) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
    return
  }
  selectedIds.value = [...selectedIds.value, id]
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
    return
  }
  selectedIds.value = filteredSelectableDayEvents.value.map((event: any) => event.id)
}

function exitBatchMode() {
  batchMode.value = false
  selectedIds.value = []
}

async function batchMarkDone() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定将 ${selectedIds.value.length} 项日程标记为完成吗？`, '批量完成', {
      type: 'info',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await Promise.all(selectedIds.value.map((id) => updateEventStatusApi(id, EVENT_STATUS_VALUE.DONE)))
    monthData.value = monthData.value.map((event: any) => selectedIds.value.includes(event.id) ? { ...event, status: EVENT_STATUS_VALUE.DONE } : event)
    dayEvents.value = dayEvents.value.map((event: any) => selectedIds.value.includes(event.id) ? { ...event, status: EVENT_STATUS_VALUE.DONE } : event)
    ElMessage.success('批量完成成功')
    exitBatchMode()
  } catch (error) {
  }
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除已选中的 ${selectedIds.value.length} 项日程吗？`, '批量删除', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    await Promise.all(selectedIds.value.map((id) => deleteEventApi(id)))
    monthData.value = monthData.value.filter((event: any) => !selectedIds.value.includes(event.id))
    dayEvents.value = dayEvents.value.filter((event: any) => !selectedIds.value.includes(event.id))
    ElMessage.success('批量删除成功')
    exitBatchMode()
  } catch (error) {
  }
}

function onMonthTouchStart(event: TouchEvent) {
  const touch = event.changedTouches[0]
  monthTouchStartX = touch.clientX
  monthTouchStartY = touch.clientY
}

async function onMonthTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = touch.clientX - monthTouchStartX
  const deltaY = touch.clientY - monthTouchStartY
  if (Math.abs(deltaX) < 60 || Math.abs(deltaX) < Math.abs(deltaY) * 1.2) return
  if (deltaX < 0) await nextMonth()
  else await prevMonth()
}

function onDayTouchStart(event: TouchEvent) {
  const touch = event.changedTouches[0]
  dayTouchStartX = touch.clientX
  dayTouchStartY = touch.clientY
}

async function onDayTouchEnd(event: TouchEvent) {
  const touch = event.changedTouches[0]
  const deltaX = touch.clientX - dayTouchStartX
  const deltaY = touch.clientY - dayTouchStartY
  if (Math.abs(deltaX) < 60 || Math.abs(deltaX) < Math.abs(deltaY) * 1.2) return
  await moveDay(deltaX < 0 ? 1 : -1)
}

function escapeCsvText(text: unknown) {
  return `"${String(text ?? '').replace(/"/g, '""')}"`
}

function exportCsv(list: any[], fileName: string) {
  if (!list.length) {
    ElMessage.warning('暂无可导出数据')
    return
  }
  const header = ['标题', '日期', '开始时间', '结束时间', '分类', '优先级', '状态', '备注']
  const rows = list.map((event: any) => [
    escapeCsvText(event.title),
    event.eventDate || '',
    formatTime(event.startTime || ''),
    formatTime(event.endTime || ''),
    EVENT_CATEGORY_MAP[event.category] || '',
    EVENT_PRIORITY_MAP[event.priority] || '',
    EVENT_STATUS_MAP[event.status] || '',
    escapeCsvText(event.remark || event.description || ''),
  ])
  const csvContent = `\uFEFF${[header, ...rows].map((row) => row.join(',')).join('\n')}`
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${fileName}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function escapeHtml(text: unknown) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function buildPrintWindow(title: string, body: string) {
  const printWindow = window.open('', '_blank', 'width=960,height=720')
  if (!printWindow) {
    ElMessage.warning('请允许浏览器弹出打印窗口')
    return
  }
  printWindow.document.write(`
    <!DOCTYPE html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>${escapeHtml(title)}</title>
        <style>
          * { box-sizing: border-box; }
          body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; margin: 0; padding: 24px; color: #1f2937; }
          .print-wrap { display: flex; flex-direction: column; gap: 16px; }
          .print-head { border-bottom: 2px solid #0958d9; padding-bottom: 12px; }
          .print-title { font-size: 20px; font-weight: 700; color: #0958d9; margin-bottom: 4px; }
          .print-sub { color: #6b7280; font-size: 12px; }
          .print-list { display: flex; flex-direction: column; gap: 10px; }
          .print-item { border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px 14px; }
          .print-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
          .print-name { font-size: 15px; font-weight: 700; }
          .print-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; color: #6b7280; font-size: 12px; }
          .print-chip { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; background: #f3f4f6; }
          .print-remark { margin-top: 8px; font-size: 12px; line-height: 1.65; color: #4b5563; white-space: pre-wrap; }
          .print-day-block { border: 1px solid #e5e7eb; border-radius: 14px; overflow: hidden; }
          .print-day-title { padding: 10px 14px; background: #f8fafc; font-weight: 700; }
          .print-day-content { padding: 12px 14px; display: flex; flex-direction: column; gap: 10px; }
          @media print { body { padding: 0; } }
        </style>
      </head>
      <body>${body}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  window.setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 300)
}

function printDayEvents() {
  const list = filteredDayEvents.value
  if (!list.length) {
    ElMessage.warning('当天暂无可打印数据')
    return
  }
  const body = `
    <div class="print-wrap">
      <div class="print-head">
        <div class="print-title">当天日程</div>
        <div class="print-sub">${escapeHtml(selectedDateText.value)}</div>
      </div>
      <div class="print-list">
        ${list.map((event: any) => `
          <div class="print-item">
            <div class="print-top">
              <div class="print-name">${escapeHtml(event.title)}</div>
              <div>${escapeHtml(EVENT_STATUS_MAP[event.status] || '')}</div>
            </div>
            <div class="print-meta">
              <span class="print-chip">${escapeHtml(formatTime(event.startTime) || '全天')}${event.endTime ? ` - ${escapeHtml(formatTime(event.endTime))}` : ''}</span>
              <span class="print-chip">${escapeHtml(EVENT_CATEGORY_MAP[event.category] || '')}</span>
              <span class="print-chip">${escapeHtml(EVENT_PRIORITY_MAP[event.priority] || '')}</span>
            </div>
            ${(event.remark || event.description) ? `<div class="print-remark">${escapeHtml(event.remark || event.description)}</div>` : ''}
          </div>
        `).join('')}
      </div>
    </div>
  `
  buildPrintWindow(`当天日程_${selectedDate.value}`, body)
}

function printMonthEvents() {
  const groups = filteredMonthData.value.reduce<Record<string, any[]>>((acc, event) => {
    if (!acc[event.eventDate]) acc[event.eventDate] = []
    acc[event.eventDate].push(event)
    return acc
  }, {})

  const entries = Object.entries(groups).sort(([a], [b]) => a.localeCompare(b))
  if (!entries.length) {
    ElMessage.warning('本月暂无可打印数据')
    return
  }

  const body = `
    <div class="print-wrap">
      <div class="print-head">
        <div class="print-title">本月日程</div>
        <div class="print-sub">${escapeHtml(monthTitle.value)} · 共 ${filteredMonthData.value.length} 项</div>
      </div>
      ${entries.map(([date, events]) => `
        <section class="print-day-block">
          <div class="print-day-title">${escapeHtml(formatDisplayDate(date))}</div>
          <div class="print-day-content">
            ${sortEvents(events).map((event: any) => `
              <div class="print-item">
                <div class="print-top">
                  <div class="print-name">${escapeHtml(event.title)}</div>
                  <div>${escapeHtml(EVENT_STATUS_MAP[event.status] || '')}</div>
                </div>
                <div class="print-meta">
                  <span class="print-chip">${escapeHtml(formatTime(event.startTime) || '全天')}${event.endTime ? ` - ${escapeHtml(formatTime(event.endTime))}` : ''}</span>
                  <span class="print-chip">${escapeHtml(EVENT_CATEGORY_MAP[event.category] || '')}</span>
                  <span class="print-chip">${escapeHtml(EVENT_PRIORITY_MAP[event.priority] || '')}</span>
                </div>
                ${(event.remark || event.description) ? `<div class="print-remark">${escapeHtml(event.remark || event.description)}</div>` : ''}
              </div>
            `).join('')}
          </div>
        </section>
      `).join('')}
    </div>
  `
  buildPrintWindow(`本月日程_${currentYear.value}_${currentMonth.value}`, body)
}

async function handleUtilityAction(action: string) {
  actionSheetVisible.value = false
  if (action === 'create') {
    openQuickCreate()
    return
  }
  if (action === 'refresh') {
    await Promise.all([loadMonthData(), loadDayEvents(selectedDate.value)])
    ElMessage.success('已刷新')
    return
  }
  if (action === 'batch') {
    switchView('day')
    batchMode.value = !batchMode.value
    if (!batchMode.value) selectedIds.value = []
    return
  }
  if (action === 'export-day') {
    exportCsv(filteredDayEvents.value, `日程_${selectedDate.value}`)
    return
  }
  if (action === 'export-month') {
    exportCsv(monthData.value, `日程_${currentYear.value}${String(currentMonth.value).padStart(2, '0')}`)
    return
  }
  if (action === 'export-month-filtered') {
    exportCsv(filteredMonthData.value, `日程_${currentYear.value}${String(currentMonth.value).padStart(2, '0')}_筛选结果`)
    return
  }
  if (action === 'print-day') {
    printDayEvents()
    return
  }
  if (action === 'print-month') {
    printMonthEvents()
  }
}
</script>

<style scoped lang="scss">
@import '../../../styles/index.scss';
@import '../../styles/mobile.scss';

.mobile-calendar-page {
  padding: 12px 12px calc(24px + var(--mobile-safe-bottom));
}

.hero-card,
.panel-card {
  margin: 0 0 12px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 254, 0.98) 100%);
  border: 1px solid rgba(221, 228, 238, 0.95);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.hero-card {
  position: sticky;
  top: 8px;
  z-index: 20;
  backdrop-filter: blur(14px);
}

.hero-top,
.hero-toolbar,
.search-row,
.hero-foot,
.focus-top,
.focus-foot,
.batch-foot,
.panel-head,
.agenda-head,
.sheet-head,
.batch-top,
.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.month-switcher,
.hero-actions,
.toolbar-actions,
.view-switch,
.focus-actions,
.day-stat-row,
.focus-badges,
.sheet-options,
.action-list {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-switcher {
  flex: 1;
  min-width: 0;
}

.icon-circle {
  width: var(--mobile-touch-min);
  height: var(--mobile-touch-min);
  border: none;
  border-radius: 50%;
  background: #f5f7fb;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-circle:disabled,
.ghost-pill:disabled,
.primary-pill:disabled {
  opacity: 0.45;
}

.month-trigger {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 0 2px;

  span {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.2;
  }

  small {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.hero-actions {
  flex-shrink: 0;
}

.ghost-pill,
.primary-pill,
.toolbar-btn,
.panel-link,
.empty-add-btn,
.batch-bar-btn,
.picker-year-btn,
.sheet-option,
.action-item,
.ghost-chip {
  border: none;
  border-radius: 999px;
  min-height: 40px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.ghost-pill,
.ghost-chip {
  background: #f5f7fb;
  color: var(--color-text-secondary);
}

.primary-pill {
  background: var(--color-primary);
  color: #fff;
}

.hero-toolbar {
  margin-top: 12px;
}

.view-switch {
  flex: 1;
  background: #f3f6fa;
  padding: 4px;
  border-radius: 999px;
}

.view-btn {
  flex: 1;
  min-height: 36px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.view-btn.active {
  background: #fff;
  color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(9, 88, 217, 0.12);
}

.toolbar-actions {
  flex-shrink: 0;
}

.toolbar-btn {
  min-height: 36px;
  padding: 0 12px;
  background: #f5f7fb;
  color: var(--color-text-secondary);
}

.search-row {
  margin-top: 12px;
}

.search-box {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  min-height: 44px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: #f5f7fb;
}

.search-box.focused {
  border-color: rgba(9, 88, 217, 0.3);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(9, 88, 217, 0.08);
}

.search-icon {
  color: var(--color-text-secondary);
}

.search-box input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text-primary);
  outline: none;
}

.search-clear,
.link-btn {
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  padding: 0;
}

.stats-scroll {
  display: grid;
  grid-template-columns: repeat(5, minmax(92px, 1fr));
  gap: 8px;
  overflow-x: auto;
  margin-top: 12px;
  padding-bottom: 2px;
  scroll-snap-type: x proximity;
}

.stats-scroll::-webkit-scrollbar,
.filter-tabs::-webkit-scrollbar,
.day-stat-row::-webkit-scrollbar,
.focus-badges::-webkit-scrollbar,
.sheet-options::-webkit-scrollbar,
.action-list::-webkit-scrollbar {
  display: none;
}

.stats-card {
  position: relative;
  overflow: hidden;
  border: none;
  min-width: 92px;
  padding: 12px 10px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f6fb 100%);
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 5px;
  scroll-snap-align: start;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.stats-card::after {
  content: '';
  position: absolute;
  inset: auto 10px 0;
  height: 3px;
  border-radius: 999px;
  background: rgba(9, 88, 217, 0.08);
}

.stats-card.active {
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.14);
}

.stats-card.active::after {
  background: rgba(255, 255, 255, 0.26);
}

.stats-all.active { background: linear-gradient(135deg, #2b6ef2, #0958d9); }
.stats-todo.active { background: linear-gradient(135deg, #f0a53b, #d97706); }
.stats-doing.active { background: linear-gradient(135deg, #4d8eff, #2563eb); }
.stats-done.active { background: linear-gradient(135deg, #4ab56a, #2f9e44); }
.stats-cancelled.active { background: linear-gradient(135deg, #7b8798, #64748b); }

.stats-label {
  font-size: 12px;
  color: inherit;
  opacity: 0.88;
}

.stats-value {
  font-size: 22px;
  line-height: 1;
}

.stats-hint {
  font-size: 11px;
  color: inherit;
  opacity: 0.72;
}

.active-filters,
.hero-foot,
.day-stat-row,
.focus-badges {
  flex-wrap: wrap;
}

.active-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.active-summary,
.filter-chip,
.day-stat,
.focus-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: #f3f6fa;
  color: var(--color-text-secondary);
}

.day-stat.total,
.focus-badge.primary {
  background: #eaf2ff;
  color: #0958d9;
}

.day-stat.todo { background: #fff4db; color: #d97706; }
.day-stat.doing { background: #eaf2ff; color: #2563eb; }
.day-stat.done { background: #e8f7ed; color: #2f9e44; }
.day-stat.cancelled { background: #eef2f6; color: #64748b; }

.selection-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 12px 0 2px;
}

.selection-pill {
  min-height: 76px;
  padding: 12px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f2f6fb 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
}

.selection-pill strong {
  font-size: 15px;
  line-height: 1.3;
  color: var(--color-text-primary);
}

.selection-pill span {
  font-size: 11px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.selection-pill--primary {
  background: linear-gradient(180deg, #edf5ff 0%, #f8fbff 100%);
}

.selection-pill--primary strong {
  color: #0958d9;
}

.selection-pill.accent {
  background: linear-gradient(180deg, #eefbf1 0%, #fbfefc 100%);
}

.selection-pill.accent strong {
  color: #2f9e44;
}

.hero-foot {
  margin-top: 10px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.calendar-legend {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot--today {
  background: #0958d9;
}

.legend-dot--selected {
  background: #7cb5ff;
}

.legend-dot--marked {
  background: #2f9e44;
}

.calendar-panel.busy {
  opacity: 0.72;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 8px;

  span {
    text-align: center;
    font-size: 11px;
    font-weight: 700;
    color: var(--color-text-secondary);
    letter-spacing: 0.8px;
  }

  .weekend {
    color: #cf1322;
  }
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.day-cell {
  position: relative;
  overflow: hidden;
  min-height: clamp(74px, 16vw, 96px);
  border: 1px solid rgba(219, 226, 234, 0.88);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.day-cell::after {
  content: '';
  position: absolute;
  inset: auto 8px 8px;
  height: 2px;
  border-radius: 999px;
  background: transparent;
  transition: background 0.18s ease;
}

.day-cell.today {
  border-color: rgba(9, 88, 217, 0.32);
  background: linear-gradient(180deg, #f2f7ff 0%, #ffffff 100%);
}

.day-cell.selected {
  border-color: rgba(9, 88, 217, 0.4);
  background: linear-gradient(180deg, #f3f8ff 0%, #ffffff 100%);
  box-shadow: 0 12px 22px rgba(9, 88, 217, 0.14);
}

.day-cell.selected::after,
.day-cell.marked::after {
  background: rgba(9, 88, 217, 0.16);
}

.day-cell.marked:not(.selected) {
  border-color: rgba(148, 181, 231, 0.7);
}

.day-cell.pressed {
  transform: scale(0.97);
}

.day-cell.weekend:not(.other) {
  background: linear-gradient(180deg, #fff8f5 0%, #ffffff 100%);
}

.day-cell.other {
  opacity: 0.52;
}

.day-cell.hidden {
  background: #f7f8fa;
  opacity: 0.65;
}

.day-cell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.day-number {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.day-cell.today .day-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
}

.day-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #0958d9;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
}

.day-cell-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
}

.mini-event,
.mini-more,
.mini-muted {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.mini-event {
  background: rgba(9, 88, 217, 0.08);
  color: var(--color-text-primary);

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--cell-accent);
    flex-shrink: 0;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.mini-more,
.mini-muted {
  justify-content: center;
  background: #f3f6fa;
  color: var(--color-text-secondary);
}

.day-cell.dense .mini-event {
  justify-content: center;
  padding: 0 4px;
}

.day-cell.dense .mini-event span {
  display: none;
}

.day-cell.dense .mini-event i {
  width: 7px;
  height: 7px;
}

.day-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }
}

.gesture-hint {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 6px 0 12px;
}

.panel-head {
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  p {
    margin: 4px 0 0;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.panel-head-compact {
  margin-bottom: 8px;
}

.panel-link {
  min-height: 34px;
  padding: 0 12px;
  background: #eaf2ff;
  color: #0958d9;
}

.focus-main {
  flex: 1;
  min-width: 0;
  text-align: center;

  strong {
    display: block;
    font-size: 24px;
    line-height: 1.1;
    color: var(--color-text-primary);
  }

  span {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.focus-foot {
  margin-top: 12px;
  flex-wrap: wrap;
}

.focus-actions {
  flex-wrap: wrap;
}

.week-strip-card {
  padding-bottom: 12px;
}

.week-strip {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(62px, 1fr);
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scroll-snap-type: x proximity;
}

.week-strip::-webkit-scrollbar {
  display: none;
}

.week-day {
  min-height: 74px;
  border: 1px solid rgba(221, 228, 238, 0.92);
  border-radius: 16px;
  background: #f7faff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px 6px;
  color: var(--color-text-secondary);
  scroll-snap-align: start;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.week-day-label {
  font-size: 11px;
  font-weight: 700;
}

.week-day strong {
  font-size: 18px;
  line-height: 1;
  color: var(--color-text-primary);
}

.week-day small {
  font-size: 10px;
}

.week-day.today {
  border-color: rgba(9, 88, 217, 0.18);
  background: #eef5ff;
}

.week-day.selected {
  background: linear-gradient(135deg, #2b6ef2, #0958d9);
  border-color: transparent;
  box-shadow: 0 14px 24px rgba(9, 88, 217, 0.18);
  color: rgba(255, 255, 255, 0.82);
}

.week-day.selected strong {
  color: #fff;
}

.batch-panel {
  background: linear-gradient(180deg, #fffef7 0%, #ffffff 100%);
  border-color: rgba(217, 119, 6, 0.18);
}

.batch-foot {
  margin-top: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.batch-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.agenda-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agenda-wrap.large-gap {
  gap: 14px;
}

.agenda-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.9) 0%, rgba(255, 255, 255, 0.92) 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.agenda-head {
  padding: 0 2px;

  strong {
    display: block;
    font-size: 14px;
    color: var(--color-text-primary);
  }

  small {
    font-size: 11px;
    color: var(--color-text-secondary);
  }

  span {
    min-width: 22px;
    height: 22px;
    border-radius: 999px;
    background: #eaf2ff;
    color: #0958d9;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: 700;
  }
}

.agenda-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-item {
  height: 90px;
  border-radius: 16px;
}

.compact-empty {
  padding: 32px 18px;
}

.empty-add-btn {
  margin-top: 12px;
  background: var(--color-primary);
  color: #fff;
}

.overlay-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(15, 23, 42, 0.38);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 16px;
  backdrop-filter: blur(6px);
}

.month-picker-panel,
.sheet-panel {
  width: min(100%, 456px);
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.month-picker-panel {
  margin-bottom: auto;
  margin-top: 88px;
  padding: 18px;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.picker-month {
  min-height: 42px;
  border: none;
  border-radius: 14px;
  background: #f5f7fb;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.picker-month.active {
  background: var(--color-primary);
  color: #fff;
}

.picker-year-btn {
  width: 40px;
  padding: 0;
  background: #f5f7fb;
  color: var(--color-text-primary);
}

.sheet-panel {
  padding: 14px 16px calc(18px + var(--mobile-safe-bottom));
}

.sheet-handle {
  width: 44px;
  height: 5px;
  border-radius: 999px;
  background: #d6dde8;
  margin: 0 auto 14px;
}

.sheet-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sheet-section h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--color-text-primary);
}

.sheet-options.wrap {
  flex-wrap: wrap;
}

.sheet-option {
  min-height: 36px;
  padding: 0 14px;
  background: #f5f7fb;
  color: var(--color-text-secondary);
}

.sheet-option.active {
  background: #eaf2ff;
  color: #0958d9;
}

.sheet-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.sheet-actions {
  margin-top: 18px;
}

.dual-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-create-sheet {
  padding-bottom: calc(18px + var(--mobile-safe-bottom));
}

.quick-create-date {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.quick-create-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.quick-create-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.quick-create-card {
  min-height: 84px;
  border: none;
  border-radius: 18px;
  background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  padding: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.quick-create-card strong {
  font-size: 15px;
  color: var(--color-text-primary);
}

.quick-create-card small {
  font-size: 11px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.action-sheet {
  padding-bottom: calc(16px + var(--mobile-safe-bottom));
}

.action-list {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-top: 10px;
}

.action-item {
  justify-content: flex-start;
  min-height: 48px;
  border-radius: 16px;
  background: #f5f7fb;
  color: var(--color-text-primary);
}

.action-item--primary {
  background: linear-gradient(135deg, #2b6ef2, #0958d9);
  color: #fff;
}

.batch-actions-bar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(12px + var(--mobile-safe-bottom));
  z-index: 140;
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(221, 228, 238, 0.92);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(14px);
}

.batch-bar-btn {
  flex: 1;
  min-height: 46px;
  color: #fff;
}

.batch-bar-btn.done {
  background: #2f9e44;
}

.batch-bar-btn.delete {
  background: #cf1322;
}

.spacer {
  height: 8px;
}

@media (max-width: 420px) {
  .mobile-calendar-page {
    padding-left: 10px;
    padding-right: 10px;
  }

  .hero-card,
  .panel-card {
    padding: 12px;
    border-radius: 16px;
  }

  .hero-top,
  .hero-toolbar {
    flex-wrap: wrap;
  }

  .hero-actions,
  .toolbar-actions,
  .focus-actions {
    width: 100%;
  }

  .ghost-pill,
  .primary-pill,
  .toolbar-btn,
  .ghost-chip {
    flex: 1;
  }

  .selection-overview,
  .quick-create-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .month-grid {
    gap: 4px;
  }

  .day-cell {
    min-height: 66px;
    padding: 7px 5px;
    border-radius: 14px;
  }

  .stats-scroll {
    grid-template-columns: repeat(5, minmax(86px, 1fr));
  }
}

@media (max-width: 360px) {
  .stats-scroll {
    gap: 6px;
  }

  .stats-card {
    min-width: 84px;
    padding: 10px 8px;
  }

  .stats-value {
    font-size: 20px;
  }

  .day-number {
    font-size: 13px;
  }

  .mini-event,
  .mini-more,
  .mini-muted {
    font-size: 9px;
    padding: 0 5px;
  }

  .focus-main strong {
    font-size: 22px;
  }

  .selection-overview,
  .quick-create-grid,
  .dual-actions {
    grid-template-columns: 1fr;
  }
}
</style>
