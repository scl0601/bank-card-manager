<template>
  <div class="profit-page">
    <section class="page-header card-shell">
      <div class="header-top">
        <div class="header-copy">
          <div class="header-title">收益统计</div>
          <div class="header-sub">{{ currentScopeLabel }} · {{ activeViewDesc }}</div>
        </div>
        <div class="header-actions">
          <el-button class="action-btn" :icon="RefreshRight" @click="refresh">刷新</el-button>
          <el-button class="action-btn" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="header-stat-row">
        <div v-for="item in summaryCards" :key="item.label" class="header-stat" :class="{ 'is-net': item.emphasis }">
          <div class="header-stat-icon" :style="{ background: item.iconBg, color: item.iconColor }">
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
          </div>
          <div class="header-stat-body">
            <div class="header-stat-label">{{ item.label }}</div>
            <div class="header-stat-value" :class="item.className">{{ item.value }}</div>
            <div class="header-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="app-search-panel card-shell profit-filter-panel">
      <div class="app-search-main profit-filter-grid">
        <div class="app-search-title">筛选</div>
        <el-date-picker
          v-model="queryYearDate"
          class="app-search-item profit-filter-item"
          type="year"
          value-format="YYYY"
          placeholder="统计年份"
          :editable="false"
          clearable
        />
        <el-select-v2
          v-model="query.month"
          class="app-search-item profit-filter-item"
          :options="monthFilterOptions"
          placeholder="全部月份"
          clearable
          filterable
          :height="240"
          :item-height="30"
          popper-class="profit-filter-dropdown"
        />
        <el-select-v2
          v-model="query.userId"
          class="app-search-item app-search-item-md profit-filter-item"
          :options="userFilterOptions"
          placeholder="全部洽谈人"
          clearable
          filterable
          :height="280"
          :item-height="30"
          popper-class="profit-filter-dropdown"
          no-match-text="没有匹配洽谈人"
          no-data-text="暂无洽谈人"
        />
        <el-select-v2
          v-model="query.cardId"
          class="app-search-item app-search-item-lg profit-filter-item"
          :options="cardFilterOptions"
          placeholder="全部银行卡"
          clearable
          filterable
          :height="300"
          :item-height="30"
          popper-class="profit-filter-dropdown"
          no-match-text="没有匹配银行卡"
          no-data-text="暂无银行卡"
        />
        <el-select-v2
          v-model="activeTab"
          class="app-search-item app-search-item-lg profit-filter-item"
          :options="viewFilterOptions"
          placeholder="统计视角"
          :height="90"
          :item-height="30"
          popper-class="profit-filter-dropdown"
        />
      </div>
    </section>

    <section class="main-panel card-shell">
      <div class="panel-head">
        <div>
          <div class="panel-title">{{ activeViewTitle }}</div>
          <div class="panel-desc">{{ tableRows.length }} 行 · {{ billRows.length }} 条账单</div>
        </div>
        <div class="inline-summary">
          <span>用户 {{ summary.userCount }}</span>
          <span>卡 {{ summary.cardCount }}</span>
          <span>账单 {{ summary.billCount }}</span>
        </div>
      </div>

      <div ref="tableShellRef" class="table-shell">
        <el-table :data="pagedRows" border stripe height="100%" table-layout="fixed" size="small" row-key="id">
          <el-table-column prop="userName" width="70" align="center" header-align="center" show-overflow-tooltip>
            <template #header>
              <div class="sortable-header" @click="toggleUserSort">
                <span>洽谈人</span>
                <span class="sort-indicator">
                  <span class="sort-arrow up" :class="{ active: userSortOrder === 'asc' }">▲</span>
                  <span class="sort-arrow down" :class="{ active: userSortOrder === 'desc' }">▼</span>
                </span>
              </div>
            </template>
            <template #default="{ row }">
              <span class="person-cell negotiator-cell">{{ row.userName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="持卡人" width="78" align="center" header-align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="person-cell holder-cell">{{ holderCellText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="银行卡" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="single-line card-scope-cell">{{ row.cardInfoLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column label="月份" width="44" align="center" header-align="center">
            <template #default="{ row }">
              <span class="single-line">{{ row.monthLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column label="账单金额" min-width="76" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell">{{ formatMoney(row.totalBillAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手续费" min-width="76" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell amount-income">{{ formatMoney(row.totalFeeAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="支付状态" width="60" align="center" header-align="center">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="feeStatusTagType(row)" size="small" effect="light">{{ row.feePayStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已支付" min-width="72" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell amount-income">{{ formatMoney(row.feePaidAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="剩余支付" min-width="76" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell" :class="row.remainingFeeAmount > 0 ? 'amount-warn' : 'amount-muted'">{{ formatMoney(row.remainingFeeAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最近支付时间" min-width="104" align="center" header-align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="single-line">{{ formatDateTime(row.latestFeePayTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="支付方式" width="56" align="center" header-align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="single-line">{{ row.feePayMethodText || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="其他费用" min-width="72" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell amount-cost">{{ formatMoney(row.totalOtherFeeAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="POS成本" min-width="72" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="净利润" min-width="80" align="right" header-align="right">
            <template #default="{ row }">
              <span class="money-cell" :class="row.totalNetProfit >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="48" align="center" header-align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" :disabled="!row.bills.length" @click="openProfitEditor(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager-wrap">
          <el-pagination
            v-model:current-page="detailPage.current"
            v-model:page-size="detailPage.size"
            background
            small
            layout="total, prev, pager, next"
            :total="tableRows.length"
          />
        </div>
      </div>
    </section>

    <el-dialog
      v-model="profitEditorVisible"
      :title="profitEditorTitle"
      width="980px"
      class="profit-editor-dialog"
      destroy-on-close
    >
      <div class="profit-editor" v-loading="profitEditorLoading">
        <div v-if="profitEditorRows.length" class="profit-collect-panel">
          <div class="collect-summary">
            <div>
              <span class="collect-label">统一收款对象</span>
              <strong>{{ profitEditorScopeRow?.userName || '-' }}</strong>
              <span>{{ profitEditorScopeRow?.monthLabel || '-' }}</span>
            </div>
            <div>
              <span>账单 {{ profitEditorRows.length }} 条</span>
              <span>应收 {{ formatMoney(profitEditorTotalFeeAmount) }}</span>
              <span>其他费用 {{ formatMoney(profitCollectDraft.otherFeeAmount) }}</span>
              <span>POS成本 {{ formatMoney(profitCollectDraft.posCostAmount) }}</span>
              <span>剩余 {{ formatMoney(profitEditorRemainingFeeAmount) }}</span>
            </div>
          </div>
          <div class="collect-field-grid">
            <label class="editor-field">
              <span>本次统一收款</span>
              <el-input-number
                v-model="profitCollectDraft.feePaidAmount"
                :min="0"
                :max="profitEditorTotalFeeAmount"
                :precision="2"
                :controls="false"
                size="small"
                class="editor-number-input"
                :disabled="savingProfitCollect"
              />
            </label>
            <label class="editor-field">
              <span>收款方式</span>
              <el-select
                v-model="profitCollectDraft.feePayMethod"
                class="editor-method-select"
                placeholder="方式"
                clearable
                size="small"
                :disabled="savingProfitCollect"
              >
                <el-option v-for="item in PAYMENT_METHOD_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>
            <label class="editor-field">
              <span>统一其他费用</span>
              <el-input-number
                v-model="profitCollectDraft.otherFeeAmount"
                :min="0"
                :precision="2"
                :controls="false"
                size="small"
                class="editor-number-input"
                :disabled="savingProfitCollect"
              />
            </label>
            <label class="editor-field">
              <span>统一POS成本</span>
              <el-input-number
                v-model="profitCollectDraft.posCostAmount"
                :min="0"
                :precision="2"
                :controls="false"
                size="small"
                class="editor-number-input"
                :disabled="savingProfitCollect"
              />
            </label>
            <label class="editor-field is-time">
              <span>收款时间</span>
              <el-date-picker
                v-model="profitCollectDraft.feePayTime"
                class="editor-time-picker"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="收款时间"
                size="small"
                :editable="false"
                clearable
                :disabled="savingProfitCollect"
              />
            </label>
            <div class="collect-actions">
              <el-button size="small" :disabled="savingProfitCollect" @click="fillFullCollectAmount">全额收款</el-button>
              <el-button size="small" :disabled="savingProfitCollect" @click="clearCollectAmount">清空</el-button>
              <el-button type="primary" size="small" :loading="savingProfitCollect" @click="saveProfitCollection">统一保存</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前行暂无账单" :image-size="72" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProfitStats' })
import { computed, nextTick, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CreditCard, Money, RefreshRight, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { getUserTreeApi, getCardListApi } from '@/api/card'
import { getBillPageApi, updateBillApi } from '@/api/bill'

type ViewKey = 'userMonths' | 'cardMonths' | 'monthUsers'
type UserSortOrder = 'default' | 'asc' | 'desc'
type PaymentMethod = 'wechat' | 'alipay' | 'cash' | 'other'

interface UserNode {
  id: number
  name: string
  parentId?: number | null
  parentName?: string
  phone?: string
  status?: number
  createTime?: string | null
  children?: UserNode[]
}

interface CardOption {
  id: number
  userId?: number
  userName?: string
  ownerName?: string
  bankName?: string
  cardNoLast4?: string
  cardType?: number | null
}

interface BillRow {
  id: number
  cardId: number
  ownerId?: number
  ownerName?: string
  bankName?: string
  cardNoLast4?: string
  billMonth: string
  billDay?: number | null
  repayDate: string | null
  repayDay?: number | null
  billAmount: number | null
  minPayAmount?: number | null
  actualPayAmount?: number | null
  actualPayDate?: string | null
  feeRate?: number | null
  feeAmount?: number | null
  feePaid?: boolean | null
  feePaidAmount?: number | null
  feePayTime?: string | null
  feePayMethod?: PaymentMethod | string | null
  posCostAmount?: number | null
  otherFeeAmount?: number | null
  netProfit?: number | null
  verified?: boolean | null
  expenseVerified?: boolean | null
  status: number
  remark?: string | null
}

interface ProfitDetailRow {
  id: string
  userId: number
  userName: string
  cardId?: number
  cardLabel?: string
  holderLabel: string
  cardInfoLabel: string
  cardCount: number
  billMonth: string
  monthLabel: string
  billCount: number
  totalBillAmount: number
  totalFeeAmount: number
  feePaidAmount: number
  remainingFeeAmount: number
  feePayStatus: string
  latestFeePayTime: string
  feePayMethodText: string
  totalOtherFeeAmount: number
  totalPosCostAmount: number
  totalNetProfit: number
  bills: BillRow[]
}

interface ProfitDraft {
  feePaidAmount: number
  feePayMethod: string
  feePayTime: string
  otherFeeAmount: number
  posCostAmount: number
}

interface ProfitCollectDraft {
  feePaidAmount: number
  feePayMethod: string
  feePayTime: string
  otherFeeAmount: number
  posCostAmount: number
}

interface FilterOption {
  label: string
  value: number | string
}

interface ProfitQuery {
  year: number
  month?: number
  userId?: number
  cardId?: number
}

const route = useRoute()
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const MONTH_OPTIONS = Array.from({ length: 12 }, (_, index) => index + 1)
const monthFilterOptions: FilterOption[] = MONTH_OPTIONS.map((month) => ({ label: `${month}月`, value: month }))
const viewFilterOptions: FilterOption[] = [
  { label: '每个用户12个月', value: 'userMonths' },
  { label: '每月所有用户', value: 'monthUsers' }
]
const PAYMENT_METHOD_OPTIONS = [
  { label: '微信', value: 'wechat' },
  { label: '支付宝', value: 'alipay' },
  { label: '现金', value: 'cash' },
  { label: '其他', value: 'other' }
]

const activeTab = ref<ViewKey>('userMonths')
const appliedTab = ref<ViewKey>('userMonths')
const userSortOrder = ref<UserSortOrder>('default')
const queryYearDate = ref(String(currentYear))
const userTree = ref<UserNode[]>([])
const cardOptions = ref<CardOption[]>([])
const billRows = ref<BillRow[]>([])
const tableShellRef = ref<HTMLElement | null>(null)
const profitEditorVisible = ref(false)
const profitEditorLoading = ref(false)
const profitEditorRows = ref<BillRow[]>([])
const profitEditorScopeRow = ref<ProfitDetailRow | null>(null)
const profitEditorBaseTitle = ref('收益编辑')
const profitDraftMap = ref<Record<number, ProfitDraft>>({})
const savingProfitBillId = ref<number | null>(null)
const savingProfitCollect = ref(false)
const profitCollectDraft = reactive<ProfitCollectDraft>({
  feePaidAmount: 0,
  feePayMethod: '',
  feePayTime: '',
  otherFeeAmount: 0,
  posCostAmount: 0
})

const query = reactive({
  year: currentYear,
  month: undefined as number | undefined,
  userId: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const appliedQuery = reactive({
  year: currentYear,
  month: undefined as number | undefined,
  userId: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const detailPage = reactive({
  current: 1,
  size: 20
})

const TABLE_HEADER_HEIGHT = 29
const TABLE_PAGER_HEIGHT = 34
const TABLE_ROW_HEIGHT = 30
const SEARCH_DEBOUNCE_MS = 80

let tableResizeObserver: ResizeObserver | null = null
let fitPageFrame = 0
let searchSeq = 0
let searchTimer = 0
let initialLoadPending = true

const sortedTopUserOptions = computed(() => normalizeTopUsers(userTree.value, userSortOrder.value))
const userLookup = computed(() => buildUserLookup(userTree.value))
const userOrderMap = computed(() => {
  const map = new Map<number, number>()
  sortedTopUserOptions.value.forEach((user, index) => map.set(Number(user.id), index))
  return map
})
const cardCountByTopUser = computed(() => buildCardCountByTopUser())
const profitCardOptions = computed(() => cardOptions.value.filter(isProfitCardOption))
const filteredCardOptions = computed(() => filterCardsForUser(query.userId))
const filteredAppliedCardOptions = computed(() => filterCardsForUser(appliedQuery.userId))
const userFilterOptions = computed<FilterOption[]>(() => sortedTopUserOptions.value.map((user) => {
  const cardCount = cardCountByTopUser.value.get(Number(user.id)) || 0
  return {
    label: cardCount > 0 ? `${user.name} (${cardCount}张卡)` : user.name,
    value: Number(user.id)
  }
}))
const cardFilterOptions = computed<FilterOption[]>(() => filteredCardOptions.value.map((card) => ({
  label: cardLabel(card),
  value: Number(card.id)
})))

const selectedMonths = computed(() => appliedQuery.month ? [appliedQuery.month] : MONTH_OPTIONS)
const currentScopeLabel = computed(() => appliedQuery.month ? `${appliedQuery.year}年${appliedQuery.month}月` : `${appliedQuery.year}年全年`)
const activeViewTitle = computed(() => {
  if (appliedTab.value === 'cardMonths') return '每张卡的12个月'
  if (appliedTab.value === 'monthUsers') return '每个月的所有用户'
  return '每个用户的12个月'
})
const activeViewDesc = computed(() => {
  if (appliedTab.value === 'cardMonths') return '按银行卡逐月查看账单金额、手续费、成本和净利润'
  if (appliedTab.value === 'monthUsers') return '按月份展开每个用户当月收益'
  return '按用户逐月查看12个月收益'
})
const tableRows = computed(() => {
  if (appliedTab.value === 'cardMonths') return buildCardMonthRows()
  if (appliedTab.value === 'monthUsers') return buildMonthUserRows()
  return buildUserMonthRows()
})

const pagedRows = computed(() => {
  const start = (detailPage.current - 1) * detailPage.size
  return tableRows.value.slice(start, start + detailPage.size)
})

const summary = computed(() => {
  const userIds = new Set<number>()
  const cardIds = new Set<number>()
  let totalBillAmount = 0
  let totalFeeAmount = 0
  let feePaidAmount = 0
  let totalPosCostAmount = 0
  let totalOtherFeeAmount = 0

  for (const bill of billRows.value) {
    const topUser = topUserForBill(bill)
    if (topUser?.id) userIds.add(Number(topUser.id))
    if (bill.cardId) cardIds.add(Number(bill.cardId))
    totalBillAmount += toAmount(bill.billAmount)
    totalFeeAmount += billFeeAmount(bill)
    feePaidAmount += billPaidAmount(bill)
    totalPosCostAmount += toAmount(bill.posCostAmount)
    totalOtherFeeAmount += toAmount(bill.otherFeeAmount)
  }

  return {
    userCount: userIds.size,
    cardCount: cardIds.size,
    billCount: billRows.value.length,
    totalBillAmount,
    totalFeeAmount,
    feePaidAmount,
    remainingFeeAmount: Math.max(0, totalFeeAmount - feePaidAmount),
    totalPosCostAmount,
    totalOtherFeeAmount,
    totalNetProfit: totalFeeAmount - totalPosCostAmount - totalOtherFeeAmount
  }
})

const summaryCards = computed(() => [
  {
    label: '净利润',
    value: `¥${formatMoney(summary.value.totalNetProfit)}`,
    sub: '手续费 - POS成本 - 其他费用',
    className: summary.value.totalNetProfit >= 0 ? 'amount-income' : 'amount-cost',
    icon: TrendCharts,
    iconBg: summary.value.totalNetProfit >= 0 ? '#e8f7ed' : '#fdebec',
    iconColor: summary.value.totalNetProfit >= 0 ? '#2f9e44' : '#cf1322',
    emphasis: true
  },
  {
    label: '手续费',
    value: `¥${formatMoney(summary.value.totalFeeAmount)}`,
    sub: `已支付 ¥${formatMoney(summary.value.feePaidAmount)}`,
    className: 'amount-income',
    icon: Money,
    iconBg: '#e8f7ed',
    iconColor: '#2f9e44'
  },
  {
    label: '剩余支付',
    value: `¥${formatMoney(summary.value.remainingFeeAmount)}`,
    sub: '手续费待收金额',
    className: summary.value.remainingFeeAmount > 0 ? 'amount-warn' : 'amount-muted',
    icon: CreditCard,
    iconBg: '#fff7e6',
    iconColor: '#d48806'
  },
  {
    label: '总成本',
    value: `¥${formatMoney(summary.value.totalPosCostAmount + summary.value.totalOtherFeeAmount)}`,
    sub: `POS ${formatMoney(summary.value.totalPosCostAmount)} / 其他 ${formatMoney(summary.value.totalOtherFeeAmount)}`,
    className: 'amount-cost',
    icon: CreditCard,
    iconBg: '#fdebec',
    iconColor: '#cf1322'
  },
  {
    label: '账单金额总额',
    value: `¥${formatMoney(summary.value.totalBillAmount)}`,
    sub: `${summary.value.billCount} 条账单`,
    className: '',
    icon: Wallet,
    iconBg: '#eaf2ff',
    iconColor: '#0958d9'
  }
])

const profitEditorTitle = computed(() => `${profitEditorBaseTitle.value} · ${currentScopeLabel.value}`)
const profitEditorTotalFeeAmount = computed(() => roundMoney(profitEditorRows.value.reduce((sum, row) => sum + billFeeAmount(row), 0)))
const profitEditorPaidAmount = computed(() => roundMoney(profitEditorRows.value.reduce((sum, row) => sum + billPaidAmount(row), 0)))
const profitEditorRemainingFeeAmount = computed(() => Math.max(0, roundMoney(profitEditorTotalFeeAmount.value - toAmount(profitCollectDraft.feePaidAmount))))

function currentQuerySnapshot(): ProfitQuery {
  return {
    year: query.year,
    month: query.month,
    userId: query.userId,
    cardId: query.cardId
  }
}

function applyQuerySnapshot(snapshot: ProfitQuery) {
  appliedQuery.year = snapshot.year
  appliedQuery.month = snapshot.month
  appliedQuery.userId = snapshot.userId
  appliedQuery.cardId = snapshot.cardId
}

function filterCardsForUser(userId: number | undefined) {
  return profitCardOptions.value
    .filter((card) => {
      if (!userId) return true
      const topUser = topUserForOwner(Number(card.userId || 0))
      return Number(topUser?.id || 0) === Number(userId)
    })
    .sort((a, b) => {
      const aTop = topUserForOwner(Number(a.userId || 0))
      const bTop = topUserForOwner(Number(b.userId || 0))
      const orderDelta = userSortIndex(Number(aTop?.id || 0)) - userSortIndex(Number(bTop?.id || 0))
      if (orderDelta !== 0) return orderDelta
      return cardLabel(a).localeCompare(cardLabel(b), 'zh-CN')
    })
}

function normalizeTopUsers(list: UserNode[], order: UserSortOrder) {
  const activeUsers = list.filter((u) => Number(u.status ?? 0) === 0)
  const disabledUsers = list.filter((u) => Number(u.status ?? 0) === 1)
  const sortFn = order === 'asc'
    ? (a: UserNode, b: UserNode) => String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN')
    : order === 'desc'
      ? (a: UserNode, b: UserNode) => String(b.name || '').localeCompare(String(a.name || ''), 'zh-CN')
      : compareUsersByCreateTimeDesc
  return [...activeUsers.sort(sortFn), ...disabledUsers.sort(sortFn)]
}

function compareUsersByCreateTimeDesc(a: UserNode, b: UserNode) {
  const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
  const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
  return timeB - timeA
}

function buildUserLookup(list: UserNode[]) {
  const lookup = new Map<number, { id: number; name: string; parentId?: number | null; topId: number; topName: string }>()
  for (const top of list) {
    lookup.set(Number(top.id), {
      id: Number(top.id),
      name: top.name,
      parentId: top.parentId,
      topId: Number(top.id),
      topName: top.name
    })
    for (const child of top.children || []) {
      lookup.set(Number(child.id), {
        id: Number(child.id),
        name: child.name,
        parentId: Number(top.id),
        topId: Number(top.id),
        topName: top.name
      })
    }
  }
  return lookup
}

function buildCardCountByTopUser() {
  const map = new Map<number, Set<number>>()
  for (const card of profitCardOptions.value) {
    const top = topUserForOwner(Number(card.userId || 0))
    const topId = Number(top?.id || 0)
    if (!topId || !card.id) continue
    if (!map.has(topId)) map.set(topId, new Set<number>())
    map.get(topId)?.add(Number(card.id))
  }
  return new Map(Array.from(map.entries()).map(([key, value]) => [key, value.size]))
}

function buildUserMonthRows() {
  const targetUsers = baseUsersForRows()
  const rows: ProfitDetailRow[] = []
  for (const user of targetUsers) {
    for (const month of selectedMonths.value) {
      const billMonth = buildBillMonth(appliedQuery.year, month)
      const bills = billRows.value.filter((bill) => Number(topUserForBill(bill)?.id || 0) === Number(user.id) && bill.billMonth === billMonth)
      rows.push(buildProfitRow({
        id: `user-${user.id}-${billMonth}`,
        userId: Number(user.id),
        userName: user.name,
        cardId: appliedQuery.cardId ? Number(appliedQuery.cardId) : undefined,
        cardCount: appliedQuery.cardId ? 1 : (cardCountByTopUser.value.get(Number(user.id)) || distinctCount(bills.map((bill) => bill.cardId))),
        billMonth,
        cardLabel: '-',
        bills
      }))
    }
  }
  return sortRows(rows)
}

function buildCardMonthRows() {
  const targetCards = baseCardsForRows()
  const rows: ProfitDetailRow[] = []
  for (const card of targetCards) {
    const topUser = topUserForOwner(Number(card.userId || 0))
    for (const month of selectedMonths.value) {
      const billMonth = buildBillMonth(appliedQuery.year, month)
      const bills = billRows.value.filter((bill) => Number(bill.cardId || 0) === Number(card.id) && bill.billMonth === billMonth)
      rows.push(buildProfitRow({
        id: `card-${card.id}-${billMonth}`,
        userId: Number(topUser?.id || 0),
        userName: topUser?.name || card.userName || '-',
        cardId: Number(card.id),
        cardLabel: cardLabel(card),
        cardCount: 1,
        billMonth,
        bills
      }))
    }
  }
  return sortRows(rows)
}

function buildMonthUserRows() {
  const targetUsers = baseUsersForRows()
  const rows: ProfitDetailRow[] = []
  for (const month of selectedMonths.value) {
    const billMonth = buildBillMonth(appliedQuery.year, month)
    for (const user of targetUsers) {
      const bills = billRows.value.filter((bill) => Number(topUserForBill(bill)?.id || 0) === Number(user.id) && bill.billMonth === billMonth)
      rows.push(buildProfitRow({
        id: `month-user-${billMonth}-${user.id}`,
        userId: Number(user.id),
        userName: user.name,
        cardId: appliedQuery.cardId ? Number(appliedQuery.cardId) : undefined,
        cardCount: appliedQuery.cardId ? 1 : (cardCountByTopUser.value.get(Number(user.id)) || distinctCount(bills.map((bill) => bill.cardId))),
        billMonth,
        cardLabel: '-',
        bills
      }))
    }
  }
  return sortRows(rows)
}

function baseUsersForRows() {
  if (appliedQuery.cardId) {
    const card = cardOptions.value.find((item) => Number(item.id) === Number(appliedQuery.cardId))
    const top = topUserForOwner(Number(card?.userId || billRows.value[0]?.ownerId || 0))
    return top ? [top] : []
  }
  if (appliedQuery.userId) {
    return sortedTopUserOptions.value.filter((user) => Number(user.id) === Number(appliedQuery.userId))
  }
  return sortedTopUserOptions.value.filter((user) => {
    const cardCount = cardCountByTopUser.value.get(Number(user.id)) || 0
    const hasBill = billRows.value.some((bill) => Number(topUserForBill(bill)?.id || 0) === Number(user.id))
    return cardCount > 0 || hasBill
  })
}

function baseCardsForRows() {
  if (appliedQuery.cardId) {
    return filteredAppliedCardOptions.value.filter((card) => Number(card.id) === Number(appliedQuery.cardId))
  }
  return filteredAppliedCardOptions.value.filter((card) => {
    const hasBill = billRows.value.some((bill) => Number(bill.cardId) === Number(card.id))
    return hasBill || Number(card.id) > 0
  })
}

function isProfitCardOption(card: CardOption) {
  const cardType = Number(card.cardType ?? 2)
  return cardType === 2
}

function toggleUserSort() {
  if (userSortOrder.value === 'default') userSortOrder.value = 'asc'
  else if (userSortOrder.value === 'asc') userSortOrder.value = 'desc'
  else userSortOrder.value = 'default'
}

function buildProfitRow(input: {
  id: string
  userId: number
  userName: string
  cardId?: number
  cardLabel?: string
  cardCount: number
  billMonth: string
  bills: BillRow[]
}): ProfitDetailRow {
  let totalBillAmount = 0
  let totalFeeAmount = 0
  let feePaidAmount = 0
  let totalOtherFeeAmount = 0
  let totalPosCostAmount = 0
  const payTimes: string[] = []
  const payMethods = new Set<string>()

  for (const bill of input.bills) {
    totalBillAmount += toAmount(bill.billAmount)
    totalFeeAmount += billFeeAmount(bill)
    feePaidAmount += billPaidAmount(bill)
    totalOtherFeeAmount += toAmount(bill.otherFeeAmount)
    totalPosCostAmount += toAmount(bill.posCostAmount)
    if (bill.feePayTime) payTimes.push(String(bill.feePayTime))
    if (bill.feePayMethod && billPaidAmount(bill) > 0) payMethods.add(paymentMethodText(String(bill.feePayMethod)))
  }

  const remainingFeeAmount = Math.max(0, totalFeeAmount - feePaidAmount)
  return {
    id: input.id,
    userId: input.userId,
    userName: input.userName || '-',
    cardId: input.cardId,
    cardLabel: input.cardLabel,
    holderLabel: buildHolderLabel(input.userId, input.cardId, input.bills),
    cardInfoLabel: buildCardInfoLabel(input.userId, input.cardId, input.cardCount, input.bills),
    cardCount: input.cardCount,
    billMonth: input.billMonth,
    monthLabel: monthLabel(input.billMonth),
    billCount: input.bills.length,
    totalBillAmount,
    totalFeeAmount,
    feePaidAmount,
    remainingFeeAmount,
    feePayStatus: feePayStatus(totalFeeAmount, feePaidAmount),
    latestFeePayTime: latestTime(payTimes),
    feePayMethodText: Array.from(payMethods).join(' / '),
    totalOtherFeeAmount,
    totalPosCostAmount,
    totalNetProfit: totalFeeAmount - totalPosCostAmount - totalOtherFeeAmount,
    bills: input.bills
  }
}

function sortRows(rows: ProfitDetailRow[]) {
  return [...rows].sort((a, b) => {
    if (appliedTab.value === 'monthUsers') {
      const monthDelta = monthNumber(a.billMonth) - monthNumber(b.billMonth)
      if (monthDelta !== 0) return monthDelta
    }
    const userDelta = userSortIndex(a.userId) - userSortIndex(b.userId)
    if (userDelta !== 0) return userDelta
    if (appliedTab.value === 'cardMonths') {
      const cardDelta = String(a.cardLabel || '').localeCompare(String(b.cardLabel || ''), 'zh-CN')
      if (cardDelta !== 0) return cardDelta
    }
    return monthNumber(a.billMonth) - monthNumber(b.billMonth)
  })
}

function userSortIndex(userId: number) {
  return userOrderMap.value.get(Number(userId)) ?? 999999
}

function topUserForBill(bill: BillRow) {
  const owner = userLookup.value.get(Number(bill.ownerId || 0))
  if (owner) return { id: owner.topId, name: owner.topName }
  return topUserForOwner(Number(bill.ownerId || 0)) || { id: Number(bill.ownerId || 0), name: bill.ownerName || '-' }
}

function topUserForOwner(ownerId: number) {
  const owner = userLookup.value.get(Number(ownerId || 0))
  if (!owner) return null
  return { id: owner.topId, name: owner.topName }
}

function ownerNameForId(ownerId: number) {
  const owner = userLookup.value.get(Number(ownerId || 0))
  return owner?.name || ''
}

function ownerNameForCard(card: CardOption | undefined | null) {
  if (!card) return ''
  return card.ownerName || ownerNameForId(Number(card.userId || 0)) || card.userName || ''
}

function cardInfoForCard(card: CardOption | undefined | null) {
  if (!card) return ''
  return `${card.bankName || '-'} *${card.cardNoLast4 || '-'}`
}

function summarizeNames(names: string[], emptyText = '-') {
  const uniqueNames = Array.from(new Set(names.map((name) => String(name || '').trim()).filter(Boolean)))
  if (!uniqueNames.length) return emptyText
  if (uniqueNames.length <= 2) return uniqueNames.join(' / ')
  return `${uniqueNames[0]}等${uniqueNames.length}人`
}

function cardsForTopUser(userId: number) {
  return profitCardOptions.value.filter((card) => Number(topUserForOwner(Number(card.userId || 0))?.id || 0) === Number(userId))
}

function buildHolderLabel(userId: number, cardId: number | undefined, bills: BillRow[]) {
  if (cardId) {
    const card = cardOptions.value.find((item) => Number(item.id) === Number(cardId))
    return ownerNameForCard(card) || summarizeNames(bills.map((bill) => bill.ownerName || ownerNameForId(Number(bill.ownerId || 0))))
  }
  if (bills.length) {
    return summarizeNames(bills.map((bill) => bill.ownerName || ownerNameForId(Number(bill.ownerId || 0))))
  }
  return summarizeNames(cardsForTopUser(userId).map(ownerNameForCard), '全部持卡人')
}

function buildCardInfoLabel(userId: number, cardId: number | undefined, cardCount: number, bills: BillRow[]) {
  const billCardCount = distinctCount(bills.map((bill) => bill.cardId))
  const scopedCardCount = cardId ? 1 : cardCount
  const fallbackCardCount = cardId ? 1 : cardsForTopUser(userId).length
  const count = scopedCardCount || billCardCount || fallbackCardCount
  return count > 0 ? `${count}张卡` : '-'
}

function distinctCount(values: Array<number | undefined | null>) {
  return new Set(values.filter((value) => Number(value || 0) > 0).map(Number)).size
}

function applyRouteQuery() {
  const year = Number(route.query.year || 0)
  const month = Number(route.query.month || 0)
  const userId = Number(route.query.userId || 0)
  const cardId = Number(route.query.cardId || 0)
  if (year > 0) {
    query.year = year
    queryYearDate.value = String(year)
  }
  if (month >= 1 && month <= 12) query.month = month
  if (userId > 0) query.userId = userId
  if (cardId > 0) {
    query.cardId = cardId
  } else if (month > 0) {
    activeTab.value = 'monthUsers'
  }
}

async function fetchBaseOptions() {
  const [userRes, cardRes]: any = await Promise.all([getUserTreeApi(), getCardListApi()])
  userTree.value = userRes.data || []
  cardOptions.value = cardRes.data || []
}

async function fetchBillRows(snapshot: ProfitQuery) {
  return fetchAllBillRows(buildBillQueryParams(snapshot))
}

function buildBillQueryParams(snapshot: ProfitQuery) {
  const params: Record<string, any> = {
    year: snapshot.year,
    sortMode: 'monthAsc'
  }
  if (snapshot.month) params.billMonth = buildBillMonth(snapshot.year, snapshot.month)
  if (snapshot.userId) params.ownerId = snapshot.userId
  if (snapshot.cardId) params.cardId = snapshot.cardId
  return params
}

async function fetchAllBillRows(params: Record<string, any>) {
  const pageSize = 100
  const records: BillRow[] = []
  let current = 1
  let total = 0
  while (true) {
    const res: any = await getBillPageApi({ current, size: pageSize, ...params })
    const pageRecords = (res.data?.records || []) as BillRow[]
    total = Number(res.data?.total ?? total)
    records.push(...pageRecords)
    if (!pageRecords.length) break
    if (total > 0 && records.length >= total) break
    if (pageRecords.length < pageSize) break
    current += 1
  }
  return records
}

async function handleSearch() {
  clearQueuedSearch()
  const snapshot = currentQuerySnapshot()
  const tabSnapshot = activeTab.value
  const seq = ++searchSeq
  const rows = await fetchBillRows(snapshot)
  if (seq !== searchSeq) return
  detailPage.current = 1
  applyQuerySnapshot(snapshot)
  appliedTab.value = tabSnapshot
  billRows.value = rows
  scheduleFitPageSize()
}

function queueSearch() {
  clearQueuedSearch()
  searchTimer = window.setTimeout(() => {
    searchTimer = 0
    void handleSearch()
  }, SEARCH_DEBOUNCE_MS)
}

function clearQueuedSearch() {
  if (!searchTimer) return
  window.clearTimeout(searchTimer)
  searchTimer = 0
}

async function refresh() {
  clearQueuedSearch()
  await fetchBaseOptions()
  await handleSearch()
}

function resetQuery() {
  query.year = currentYear
  queryYearDate.value = String(currentYear)
  query.month = undefined
  query.userId = undefined
  query.cardId = undefined
  activeTab.value = 'userMonths'
  userSortOrder.value = 'default'
  queueSearch()
}

async function openProfitEditor(row: ProfitDetailRow) {
  if (!row.bills.length) return
  profitEditorScopeRow.value = row
  profitEditorBaseTitle.value = `${row.userName} · ${row.monthLabel} · 统一收款`
  profitEditorRows.value = []
  profitEditorVisible.value = true
  profitEditorLoading.value = true
  try {
    const rows = await resolveProfitEditorRows(row)
    profitEditorRows.value = sortProfitEditorRows(rows)
    syncProfitDrafts(profitEditorRows.value)
    syncProfitCollectDraft(profitEditorRows.value)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '加载洽谈人账单失败')
  } finally {
    profitEditorLoading.value = false
  }
}

async function resolveProfitEditorRows(row: ProfitDetailRow) {
  const localRows = billRows.value.filter((bill) => {
    return Number(topUserForBill(bill)?.id || 0) === Number(row.userId)
      && bill.billMonth === row.billMonth
  })
  if (!appliedQuery.cardId && localRows.length >= row.bills.length) {
    return localRows
  }
  return fetchAllBillRows({
    ownerId: row.userId,
    billMonth: row.billMonth,
    sortMode: 'monthAsc'
  })
}

function sortProfitEditorRows(rows: BillRow[]) {
  return [...rows].sort((a, b) => {
    const cardDelta = feeBillLabel(a).localeCompare(feeBillLabel(b), 'zh-CN')
    if (cardDelta !== 0) return cardDelta
    return Number(a.id) - Number(b.id)
  })
}

function syncProfitDrafts(rows: BillRow[]) {
  const next: Record<number, ProfitDraft> = {}
  rows.forEach((row) => {
    next[row.id] = {
      feePaidAmount: billPaidAmount(row),
      feePayMethod: String(row.feePayMethod || ''),
      feePayTime: normalizeDateTime(row.feePayTime),
      otherFeeAmount: toAmount(row.otherFeeAmount),
      posCostAmount: toAmount(row.posCostAmount)
    }
  })
  profitDraftMap.value = next
}

function syncProfitCollectDraft(rows: BillRow[]) {
  profitCollectDraft.feePaidAmount = roundMoney(rows.reduce((sum, row) => sum + billFeeAmount(row), 0))
  profitCollectDraft.feePayMethod = sharedPaymentMethod(rows)
  profitCollectDraft.feePayTime = normalizeDateTime(latestTime(rows.map((row) => row.feePayTime || '').filter(Boolean)))
  profitCollectDraft.otherFeeAmount = roundMoney(rows.reduce((sum, row) => sum + toAmount(row.otherFeeAmount), 0))
  profitCollectDraft.posCostAmount = roundMoney(rows.reduce((sum, row) => sum + toAmount(row.posCostAmount), 0))
}

function sharedPaymentMethod(rows: BillRow[]) {
  const methods = Array.from(new Set(
    rows
      .filter((row) => billPaidAmount(row) > 0)
      .map((row) => String(row.feePayMethod || '').trim())
      .filter(Boolean)
  ))
  return methods.length === 1 ? methods[0] : ''
}

function profitDraftValue(id: number, field: keyof ProfitDraft) {
  return profitDraftMap.value[id]?.[field] as any
}

function updateProfitDraft(id: number, field: keyof ProfitDraft, value: any) {
  const current = profitDraftMap.value[id]
  if (!current) return
  profitDraftMap.value = {
    ...profitDraftMap.value,
    [id]: {
      ...current,
      [field]: field === 'feePayMethod' || field === 'feePayTime' ? (value || '') : toAmount(value)
    }
  }
}

function isProfitDraftChanged(row: BillRow) {
  const draft = profitDraftMap.value[row.id]
  if (!draft) return false
  return Math.abs(draft.feePaidAmount - billPaidAmount(row)) >= 0.005
    || Math.abs(draft.otherFeeAmount - toAmount(row.otherFeeAmount)) >= 0.005
    || Math.abs(draft.posCostAmount - toAmount(row.posCostAmount)) >= 0.005
    || draft.feePayMethod !== String(row.feePayMethod || '')
    || draft.feePayTime !== normalizeDateTime(row.feePayTime)
}

function draftNetProfit(row: BillRow) {
  const draft = profitDraftMap.value[row.id]
  return billFeeAmount(row) - toAmount(draft?.posCostAmount ?? row.posCostAmount) - toAmount(draft?.otherFeeAmount ?? row.otherFeeAmount)
}

function fillFullCollectAmount() {
  profitCollectDraft.feePaidAmount = profitEditorTotalFeeAmount.value
  if (!profitCollectDraft.feePayTime) {
    profitCollectDraft.feePayTime = formatDateTimeForInput(new Date())
  }
}

function clearCollectAmount() {
  profitCollectDraft.feePaidAmount = 0
  profitCollectDraft.feePayMethod = ''
  profitCollectDraft.feePayTime = ''
  profitCollectDraft.otherFeeAmount = 0
  profitCollectDraft.posCostAmount = 0
}

function allocateAmountByFeeWeight(rows: BillRow[], amount: number, options: { capByFee?: boolean } = {}) {
  let remaining = roundMoney(Math.max(0, amount))
  const validRows = rows.filter((row) => Number(row.id) > 0)
  const allocation = new Map<number, number>()
  const totalWeight = validRows.reduce((sum, row) => sum + Math.max(0, billFeeAmount(row)), 0)

  validRows.forEach((row, index) => {
    const feeAmount = Math.max(0, billFeeAmount(row))
    const isLast = index === validRows.length - 1
    const rawValue = isLast
      ? remaining
      : totalWeight > 0
        ? roundMoney((amount * feeAmount) / totalWeight)
        : roundMoney(amount / validRows.length)
    const nextValue = options.capByFee ? Math.min(feeAmount, rawValue) : rawValue
    const value = roundMoney(Math.min(remaining, Math.max(0, nextValue)))
    allocation.set(Number(row.id), value)
    remaining = roundMoney(remaining - value)
  })

  if (remaining > 0.005 && validRows.length) {
    for (const row of validRows) {
      if (remaining <= 0.005) break
      const id = Number(row.id)
      const current = allocation.get(id) || 0
      const room = options.capByFee ? roundMoney(billFeeAmount(row) - current) : remaining
      if (room <= 0) continue
      const extra = roundMoney(Math.min(room, remaining))
      allocation.set(id, roundMoney(current + extra))
      remaining = roundMoney(remaining - extra)
    }
  }

  return allocation
}

async function saveProfitCollection() {
  if (!profitEditorRows.value.length || savingProfitCollect.value) return
  const feePaidAmount = toAmount(profitCollectDraft.feePaidAmount)
  if (feePaidAmount > 0 && !profitCollectDraft.feePayMethod) {
    ElMessage.warning('统一收款金额大于0时请选择收款方式')
    return
  }

  savingProfitCollect.value = true
  try {
    const feePayTime = feePaidAmount > 0 ? (profitCollectDraft.feePayTime || formatDateTimeForInput(new Date())) : ''
    const paidAllocation = allocateAmountByFeeWeight(profitEditorRows.value, Math.min(feePaidAmount, profitEditorTotalFeeAmount.value), { capByFee: true })
    const otherFeeAllocation = allocateAmountByFeeWeight(profitEditorRows.value, toAmount(profitCollectDraft.otherFeeAmount))
    const posCostAllocation = allocateAmountByFeeWeight(profitEditorRows.value, toAmount(profitCollectDraft.posCostAmount))
    for (const row of profitEditorRows.value) {
      const rowPaidAmount = paidAllocation.get(Number(row.id)) || 0
      const payload = buildBillUpdatePayload(row, {
        feePaidAmount: rowPaidAmount,
        feePaid: isFeePaidFlag(billFeeAmount(row), rowPaidAmount),
        feePayMethod: rowPaidAmount > 0 ? profitCollectDraft.feePayMethod : null,
        feePayTime: rowPaidAmount > 0 ? feePayTime : null,
        otherFeeAmount: otherFeeAllocation.get(Number(row.id)) || 0,
        posCostAmount: posCostAllocation.get(Number(row.id)) || 0
      })
      await updateBillApi(payload)
      applySavedProfitDraft(row, payload, { syncDrafts: false })
    }
    syncProfitDrafts(profitEditorRows.value)
    syncProfitCollectDraft(profitEditorRows.value)
    profitEditorVisible.value = false
    ElMessage.success('洽谈人统一收款已保存')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存统一收款失败')
  } finally {
    savingProfitCollect.value = false
  }
}

async function saveProfitBill(row: BillRow) {
  const draft = profitDraftMap.value[row.id]
  if (!draft || savingProfitBillId.value === row.id || savingProfitCollect.value) return

  savingProfitBillId.value = row.id
  try {
    const payload = buildBillUpdatePayload(row, {
      posCostAmount: toAmount(draft.posCostAmount),
      otherFeeAmount: toAmount(draft.otherFeeAmount)
    })
    await updateBillApi(payload)
    applySavedProfitDraft(row, payload)
    ElMessage.success('成本信息已保存')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存成本信息失败')
  } finally {
    savingProfitBillId.value = null
  }
}

function applySavedProfitDraft(row: BillRow, payload: Record<string, any>, options: { syncDrafts?: boolean } = {}) {
  const target = billRows.value.find((bill) => Number(bill.id) === Number(row.id))
  const patch = {
    feePaidAmount: payload.feePaidAmount,
    feePaid: payload.feePaid,
    feePayMethod: payload.feePayMethod,
    feePayTime: payload.feePayTime,
    posCostAmount: payload.posCostAmount,
    otherFeeAmount: payload.otherFeeAmount,
    netProfit: billFeeAmount(row) - toAmount(payload.posCostAmount) - toAmount(payload.otherFeeAmount)
  }
  if (target) Object.assign(target, patch)
  Object.assign(row, patch)
  if (options.syncDrafts !== false) {
    syncProfitDrafts(profitEditorRows.value)
    syncProfitCollectDraft(profitEditorRows.value)
  }
}

function buildBillUpdatePayload(row: BillRow, overrides: Record<string, any>) {
  return {
    id: row.id,
    cardId: row.cardId,
    ownerId: row.ownerId,
    billMonth: row.billMonth,
    billDay: row.billDay,
    repayDay: row.repayDay,
    repayDate: row.repayDate,
    billAmount: toAmount(row.billAmount),
    minPayAmount: toAmount(row.minPayAmount),
    actualPayAmount: toAmount(row.actualPayAmount),
    actualPayDate: row.actualPayDate,
    feeRate: row.feeRate,
    verified: row.verified,
    expenseVerified: row.expenseVerified,
    status: row.status,
    remark: row.remark || '',
    posCostAmount: toAmount(row.posCostAmount),
    otherFeeAmount: toAmount(row.otherFeeAmount),
    feePaid: Boolean(row.feePaid),
    feePaidAmount: billPaidAmount(row),
    feePayMethod: row.feePayMethod || null,
    feePayTime: row.feePayTime || null,
    ...overrides
  }
}

function billFeeAmount(row: BillRow) {
  const explicitFee = row.feeAmount
  if (explicitFee !== null && explicitFee !== undefined) return toAmount(explicitFee)
  const billAmount = toAmount(row.billAmount)
  const feeRate = toAmount(row.feeRate)
  return Number(((billAmount * feeRate) / 100).toFixed(2))
}

function billPaidAmount(row: BillRow) {
  const paidAmount = toAmount(row.feePaidAmount)
  if (paidAmount > 0) return paidAmount
  return row.feePaid ? billFeeAmount(row) : 0
}

function isFeePaidFlag(feeAmount: number, paidAmount: number) {
  if (paidAmount <= 0) return false
  if (feeAmount <= 0) return true
  return paidAmount + 0.005 >= feeAmount
}

function feePayStatus(feeAmount: number, paidAmount: number) {
  if (feeAmount <= 0) return paidAmount > 0 ? '已支付' : '-'
  if (paidAmount <= 0) return '未支付'
  if (paidAmount + 0.005 >= feeAmount) return '已支付'
  return '部分支付'
}

function feeStatusTagType(row: ProfitDetailRow) {
  if (row.feePayStatus === '已支付') return 'success'
  if (row.feePayStatus === '部分支付') return 'primary'
  if (row.feePayStatus === '未支付') return 'warning'
  return 'info'
}

function paymentMethodText(method: string) {
  return PAYMENT_METHOD_OPTIONS.find((item) => item.value === method)?.label || method
}

function latestTime(values: string[]) {
  if (!values.length) return ''
  return [...values].sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0]
}

function buildBillMonth(year: number, month: number) {
  return `${year}-${String(month).padStart(2, '0')}`
}

function monthNumber(billMonth: string) {
  return Number(String(billMonth || '').split('-')[1] || 0)
}

function monthLabel(billMonth: string) {
  const month = monthNumber(billMonth)
  return month ? `${month}月` : '-'
}

function cardLabel(item: CardOption) {
  const bank = item.bankName || '-'
  const last4 = item.cardNoLast4 || '-'
  const owner = item.ownerName || item.userName || '未归属'
  return `${owner} / ${bank} *${last4}`
}

function holderCellText(row: ProfitDetailRow) {
  if (!row.holderLabel || row.holderLabel === '-') return '-'
  return row.holderLabel === row.userName ? '本人' : row.holderLabel
}

function feeBillLabel(row: BillRow) {
  return `${row.ownerName || '未命名'} / ${row.bankName || '-'} *${row.cardNoLast4 || '-'}`
}

function repayDateText(date: string | null | undefined) {
  return date ? String(date).slice(5) : '-'
}

function normalizeDateTime(value: string | null | undefined) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 19)
}

function formatDateTime(value: string | null | undefined) {
  const normalized = normalizeDateTime(value)
  return normalized ? normalized.replace('T', ' ').slice(0, 16) : '-'
}

function formatDateTimeForInput(value: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

function toAmount(value: number | string | null | undefined) {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount : 0
}

function roundMoney(value: number | string | null | undefined) {
  return Number(toAmount(value).toFixed(2))
}

function formatMoney(value: number | string | null | undefined) {
  const amount = Number(value ?? 0)
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function scheduleFitPageSize() {
  if (fitPageFrame) return
  fitPageFrame = window.requestAnimationFrame(() => {
    fitPageFrame = 0
    updateFitPageSize()
  })
}

function updateFitPageSize() {
  const shell = tableShellRef.value
  if (!shell) return
  const availableHeight = shell.clientHeight - TABLE_HEADER_HEIGHT - TABLE_PAGER_HEIGHT - 8
  const nextSize = Math.max(8, Math.min(100, Math.floor(availableHeight / TABLE_ROW_HEIGHT)))
  if (!Number.isFinite(nextSize) || nextSize <= 0) return
  if (detailPage.size !== nextSize) {
    detailPage.size = nextSize
  }
  const maxPage = Math.max(1, Math.ceil(tableRows.value.length / detailPage.size))
  if (detailPage.current > maxPage) {
    detailPage.current = maxPage
  }
}

async function bindTableResizeObserver() {
  await nextTick()
  tableResizeObserver?.disconnect()
  const shell = tableShellRef.value
  if (!shell) return
  tableResizeObserver = new ResizeObserver(scheduleFitPageSize)
  tableResizeObserver.observe(shell)
  window.addEventListener('resize', scheduleFitPageSize)
  scheduleFitPageSize()
}

onMounted(async () => {
  try {
    applyRouteQuery()
    await fetchBaseOptions()
    await handleSearch()
    await bindTableResizeObserver()
  } finally {
    initialLoadPending = false
  }
})

onActivated(async () => {
  if (initialLoadPending) {
    scheduleFitPageSize()
    return
  }
  await fetchBaseOptions()
  await handleSearch()
  scheduleFitPageSize()
})

onUnmounted(() => {
  clearQueuedSearch()
  billRows.value = []
  tableResizeObserver?.disconnect()
  tableResizeObserver = null
  window.removeEventListener('resize', scheduleFitPageSize)
  if (fitPageFrame) {
    window.cancelAnimationFrame(fitPageFrame)
    fitPageFrame = 0
  }
})

watch(queryYearDate, (value) => {
  const year = Number(value || currentYear)
  query.year = Number.isFinite(year) && year > 0 ? year : currentYear
})

watch(
  () => [query.year, query.month, query.userId, query.cardId],
  () => {
    if (query.cardId && !filteredCardOptions.value.some((card) => Number(card.id) === Number(query.cardId))) {
      query.cardId = undefined
      return
    }
    queueSearch()
  }
)

watch(activeTab, () => {
  queueSearch()
})

watch(userSortOrder, () => {
  detailPage.current = 1
})

</script>

<style scoped>
.profit-page {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 6px;
  overflow: hidden;
  background: #f5f7fb;
  box-sizing: border-box;
}

.card-shell {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px 8px;
  flex-shrink: 0;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-copy,
.header-stat-body {
  min-width: 0;
}

.header-title {
  color: #1f2a37;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.1;
}

.header-sub {
  margin-top: 2px;
  color: #7c8799;
  font-size: 12px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn,
.header-actions :deep(.el-button) {
  height: 26px;
  padding: 0 9px;
  border-radius: 8px;
}

.header-stat-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.header-stat {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  min-height: 42px;
  padding: 5px 8px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.header-stat.is-net {
  border-color: rgba(47, 158, 68, 0.28);
  background: linear-gradient(180deg, #ffffff 0%, #f1fbf5 120%);
  box-shadow: inset 3px 0 0 rgba(47, 158, 68, 0.72);
}

.header-stat-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-stat-label {
  color: #7c8799;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.header-stat-value {
  margin-top: 2px;
  color: #1f2a37;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.1;
}

.header-stat-sub {
  display: none;
}

.profit-filter-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  padding: 6px 8px;
  flex-shrink: 0;
}

.profit-filter-grid {
  width: 100%;
  min-height: 28px;
  gap: 6px;
  flex-wrap: nowrap;
  overflow: hidden;
}

.profit-filter-panel .app-search-title {
  min-height: 28px;
  height: 28px;
  font-size: 11px;
}

.profit-filter-panel :deep(.el-input__wrapper),
.profit-filter-panel :deep(.el-select__wrapper),
.profit-filter-panel :deep(.el-date-editor.el-input__wrapper),
.profit-filter-panel :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
  height: 28px;
  padding: 0 9px;
  border-radius: 8px;
  box-sizing: border-box;
}

.profit-filter-panel :deep(.el-select__selection) {
  height: 100%;
  min-width: 0;
  flex-wrap: nowrap;
  overflow: hidden;
}

.profit-filter-panel :deep(.el-select__selected-item),
.profit-filter-panel :deep(.el-select__placeholder),
.profit-filter-panel :deep(.el-select__input-wrapper) {
  min-width: 0;
  height: 26px;
  min-height: 26px;
  overflow: hidden;
  line-height: 26px;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.profit-filter-panel :deep(.el-input__inner),
.profit-filter-panel :deep(.el-select__selected-item),
.profit-filter-panel :deep(.el-select__placeholder),
.profit-filter-panel :deep(.el-select__input),
.profit-filter-panel :deep(.el-range-input),
.profit-filter-panel :deep(.el-input-number__input) {
  font-size: 12px;
  color: #1f2a37;
}

.profit-filter-panel :deep(.el-select__input) {
  height: 26px;
  line-height: 26px;
}

.profit-filter-item {
  flex: 1 1 128px;
  width: auto;
  min-width: 112px;
  height: 28px;
}

.profit-filter-panel :deep(.el-input),
.profit-filter-panel :deep(.el-select),
.profit-filter-panel :deep(.el-date-editor) {
  height: 28px;
  line-height: 28px;
}

.main-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 4px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 5px;
  flex-shrink: 0;
}

.panel-title {
  color: #1f2a37;
  font-size: 13px;
  font-weight: 800;
}

.panel-desc {
  margin-top: 2px;
  color: #7c8799;
  font-size: 11px;
  font-weight: 700;
}

.inline-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 24px;
  padding: 0 8px;
  border-radius: 8px;
  background: #f5f8fc;
  color: #667085;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.table-shell {
  --profit-cell-x: 5px;
  --profit-row-h: 30px;
  --profit-head-h: 28px;
  --profit-pager-h: 34px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #fff;
}

.table-shell :deep(.el-table) {
  flex: 1;
  --el-table-border-color: #e5eaf1;
  font-size: 11px;
  color: #344054;
}

.table-shell :deep(.el-table .cell) {
  padding: 0 var(--profit-cell-x);
  overflow: hidden;
  line-height: calc(var(--profit-row-h) - 2px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-shell :deep(.el-table th.el-table__cell) {
  height: var(--profit-head-h);
  padding: 0;
  background: #f7f9fc;
  color: #5b6472;
  font-size: 11px;
  font-weight: 700;
  border-right-color: #eef2f6;
}

.table-shell :deep(.el-table td.el-table__cell) {
  height: var(--profit-row-h);
  padding: 0;
  border-right-color: #eef2f6;
}

.table-shell :deep(.el-table__body tr) {
  height: var(--profit-row-h);
}

.table-shell :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #fbfcfe;
}

.table-shell :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #eef6ff;
}

.table-shell :deep(.el-tag) {
  height: 18px;
  padding: 0 4px;
  font-size: 10.5px;
  line-height: 17px;
}

.table-shell :deep(.el-button--small) {
  height: 22px;
  padding: 0 2px;
  font-size: 11px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  height: var(--profit-pager-h);
  min-height: var(--profit-pager-h);
  padding: 3px 6px;
  border-top: 1px solid #eef2f6;
  background: #fff;
  box-sizing: border-box;
}

.pager-wrap :deep(.el-pagination) {
  transform: scale(0.9);
  transform-origin: right center;
}

.sortable-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
  cursor: pointer;
  user-select: none;
}

.sort-indicator {
  display: inline-flex;
  flex-direction: column;
  gap: 1px;
  color: #b6bfcc;
  font-size: 8px;
  line-height: 0.8;
}

.sort-arrow.active {
  color: #1677ff;
}

.person-cell {
  display: block;
  overflow: hidden;
  min-width: 0;
  line-height: calc(var(--profit-row-h) - 2px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.negotiator-cell {
  color: #0958d9;
  font-weight: 800;
}

.holder-cell {
  color: #526074;
  font-weight: 700;
}

.card-scope-cell {
  color: #526074;
  font-weight: 700;
}

.single-line,
.money-cell {
  display: block;
  min-width: 0;
  overflow: hidden;
  line-height: calc(var(--profit-row-h) - 2px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.money-cell {
  font-family: var(--font-mono), monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 50px;
  overflow: hidden;
  vertical-align: middle;
}

:global(.profit-filter-dropdown) {
  --el-border-radius-base: 8px;
}

:global(.profit-filter-dropdown .el-select-dropdown__list) {
  padding: 4px;
}

:global(.profit-filter-dropdown .el-select-dropdown__item) {
  height: 30px;
  padding: 0 8px;
  border-radius: 6px;
  color: #344054;
  font-size: 12px;
  line-height: 30px;
}

:global(.profit-filter-dropdown .el-select-dropdown__item.selected) {
  color: #0958d9;
  font-weight: 800;
  background: #eaf2ff;
}

:global(.profit-filter-dropdown .el-select-dropdown__item.hover),
:global(.profit-filter-dropdown .el-select-dropdown__item:hover) {
  background: #f2f6fb;
}

.stack-cell {
  min-width: 0;
}

.stack-cell strong,
.stack-cell span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stack-cell strong {
  color: #1f2a37;
  font-size: 12px;
  font-weight: 800;
}

.stack-cell span {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 700;
}

.metric-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.metric-stack.is-tight {
  gap: 2px;
}

.metric-stack > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.metric-stack span {
  flex-shrink: 0;
  color: #98a2b3;
  font-size: 10.5px;
  font-weight: 800;
}

.metric-stack b {
  min-width: 0;
  overflow: hidden;
  color: #1f2a37;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-cell {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
}

.payment-cell > * {
  align-self: center;
}

.amount-income {
  color: #2f9e44;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.amount-cost {
  color: #cf1322;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.amount-warn {
  color: #d48806;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.amount-muted {
  color: #667085;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}

.profit-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 260px;
  max-height: 72vh;
  overflow-y: auto;
  padding-right: 2px;
}

:global(.profit-editor-dialog .el-dialog__body) {
  padding: 8px 16px 16px;
}

:global(.profit-editor-dialog) {
  max-width: calc(100vw - 32px);
}

:global(.profit-editor-dialog .el-dialog__header) {
  padding: 14px 16px 8px;
  margin-right: 0;
}

.profit-collect-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(9, 88, 217, 0.18);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 120%);
}

.collect-summary {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(0, 1.8fr);
  gap: 10px;
  min-width: 0;
  color: #526074;
  font-size: 12px;
  font-weight: 700;
}

.collect-summary > div {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  min-height: 32px;
  padding: 6px 8px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  overflow: hidden;
  white-space: nowrap;
}

.collect-summary > div:last-child {
  justify-content: flex-end;
  flex-wrap: wrap;
  white-space: normal;
}

.collect-summary strong {
  color: #0958d9;
  font-size: 14px;
  font-weight: 800;
}

.collect-label {
  color: #667085;
}

.collect-field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.collect-field-grid .editor-field.is-time {
  grid-column: auto;
}

.collect-field-grid .editor-field {
  grid-template-columns: 96px minmax(0, 1fr);
}

.collect-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
  grid-column: 1 / -1;
  min-width: 0;
  min-height: 30px;
  padding-top: 2px;
}

.collect-actions :deep(.el-button) {
  min-width: 82px;
  margin-left: 0;
  padding: 0 12px;
}

.profit-editor-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.profit-editor-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  background: #fff;
}

.editor-card-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.editor-card-head > * {
  align-self: center;
}

.editor-bill-main {
  min-width: 0;
}

.editor-bill-title,
.editor-bill-sub {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-bill-title {
  color: #1f2a37;
  font-size: 12px;
  font-weight: 800;
}

.editor-bill-sub {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 700;
}

.editor-net {
  overflow: hidden;
  font-size: 12px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.editor-field {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
  min-height: 34px;
  margin: 0;
  padding: 6px 8px;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  background: #f8fafc;
}

.editor-field > * {
  align-self: center;
}

.editor-field.is-time {
  grid-column: span 2;
}

.editor-field > span {
  overflow: hidden;
  color: #667085;
  font-size: 11px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-field > strong {
  overflow: hidden;
  font-size: 12px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.readonly-field {
  background: #f2fbf6;
  border-color: rgba(47, 158, 68, 0.18);
}

.editor-row-actions {
  display: flex;
  justify-content: flex-end;
}

.editor-number-input,
.editor-method-select,
.editor-time-picker {
  width: 100%;
}

.editor-number-input :deep(.el-input__wrapper),
.editor-method-select :deep(.el-select__wrapper),
.editor-time-picker :deep(.el-input__wrapper) {
  min-height: 26px;
  padding: 0 7px;
  border-radius: 8px;
}

.editor-number-input :deep(.el-input__inner),
.editor-method-select :deep(.el-select__selected-item),
.editor-time-picker :deep(.el-input__inner) {
  color: #1f2a37;
  font-size: 12px;
  font-weight: 800;
}

.editor-number-input :deep(.el-input__inner) {
  text-align: right;
}

@media (max-width: 1180px) {
  .header-stat-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .profit-filter-grid {
    flex-wrap: wrap;
  }

  .collect-field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .collect-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 1120px) {
  .header-stat-sub,
  .header-sub,
  .panel-desc,
  .inline-summary {
    display: none;
  }
}

@media (max-width: 960px) {
  .collect-summary {
    grid-template-columns: 1fr;
  }

  .collect-summary > div:last-child {
    justify-content: flex-start;
  }

  .collect-field-grid {
    grid-template-columns: 1fr;
  }

  .collect-field-grid .editor-field.is-time {
    grid-column: auto;
  }

  .editor-field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .editor-field.is-time {
    grid-column: span 2;
  }
}
</style>
