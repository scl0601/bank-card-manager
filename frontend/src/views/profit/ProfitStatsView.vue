<template>
  <div class="profit-page">
    <section class="page-header card-shell">
      <div class="header-top">
        <div class="header-copy">
          <div class="header-title">收益统计</div>
          <div class="header-subtitle">{{ currentScopeLabel }} · 以净收益分析为核心的一屏工作台</div>
        </div>
        <div class="header-actions">
          <el-button class="action-btn" :icon="RefreshRight" @click="refresh">刷新</el-button>
          <el-button class="action-btn" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="header-stat-row" v-loading="overviewLoading">
        <div v-for="item in summaryCards" :key="item.label" class="header-stat">
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

    <section class="app-search-panel card-shell profit-search-panel">
      <div class="app-search-main">
        <div class="app-search-title">筛选</div>
        <el-date-picker
          v-model="profitMonthRange"
          class="app-search-item app-search-item-range"
          type="monthrange"
          value-format="YYYY-MM"
          range-separator="至"
          start-placeholder="请选择收益统计开始时间"
          end-placeholder="请选择收益统计结束时间"
          :editable="false"
          clearable
        />
        <el-select v-model="query.userId" class="app-search-item app-search-item-md" placeholder="请选择持卡人" clearable filterable>
          <el-option v-for="item in userOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.cardId" class="app-search-item app-search-item-lg" placeholder="请选择银行卡" clearable filterable>
          <el-option v-for="item in cardOptions" :key="item.id" :label="cardLabel(item)" :value="item.id" />
        </el-select>
        <el-select v-model="activeTab" class="app-search-item app-search-item-sm" placeholder="请选择收益类型">
          <el-option label="按用户收益" value="users" />
          <el-option label="按银行卡收益" value="cards" />
          <el-option label="按月度汇总" value="months" />
        </el-select>
      </div>

      <div class="app-search-extra">
        <span class="app-search-meta">统计范围：{{ currentScopeLabel }}</span>
        <span class="app-search-meta">{{ searchMetaText }}</span>
        <span class="app-search-meta">筛选条件变化后自动刷新统计结果</span>
        <div class="app-search-actions">
          <el-button class="app-search-btn" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <div class="main-panel card-shell">
        <div class="panel-head">
          <div>
            <div class="panel-title">{{ activeViewTitle }}</div>
            <div class="panel-desc">{{ activeViewDesc }}</div>
          </div>
          <div class="inline-summary">
            <span>{{ activeViewCountLabel }} {{ activeViewCount }}</span>
            <span>用户 {{ overview.userCount }}</span>
            <span>卡 {{ overview.cardCount }}</span>
          </div>
        </div>

        <div v-show="activeTab === 'users'" class="table-shell" v-loading="userLoading">
          <el-table :data="userPage.records" border stripe height="100%" table-layout="fixed">
            <el-table-column prop="userName" label="用户" min-width="120" />
            <el-table-column prop="cardCount" label="卡数" width="70" align="center" />
            <el-table-column prop="billMonthCount" label="月数" width="70" align="center" />
            <el-table-column label="代还" min-width="110" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="110" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="110" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="92" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openUserBills(row)">{{ billActionLabel }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-wrap">
            <el-pagination
              v-model:current-page="userPage.current"
              v-model:page-size="userPage.size"
              background
              layout="total, prev, pager, next"
              :total="userPage.total"
              @current-change="fetchUserPage"
              @size-change="handleUserSizeChange"
            />
          </div>
        </div>

        <div v-show="activeTab === 'cards'" class="table-shell" v-loading="cardLoading">
          <el-table :data="cardPage.records" border stripe height="100%" table-layout="fixed">
            <el-table-column prop="userName" label="用户" min-width="100" />
            <el-table-column prop="ownerName" label="持卡人" min-width="96" />
            <el-table-column label="关系" width="78" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.ownerRelation || '本人' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="银行卡" min-width="136">
              <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
            </el-table-column>
            <el-table-column prop="billCount" label="账单" width="68" align="center" />
            <el-table-column label="代还" min-width="110" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="110" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="110" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="92" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openCardBills(row)">{{ billActionLabel }}</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-wrap">
            <el-pagination
              v-model:current-page="cardPage.current"
              v-model:page-size="cardPage.size"
              background
              layout="total, prev, pager, next"
              :total="cardPage.total"
              @current-change="fetchCardPage"
              @size-change="handleCardSizeChange"
            />
          </div>
        </div>

        <div v-show="activeTab === 'months'" class="table-shell" v-loading="monthLoading">
          <el-table :data="monthList" border stripe height="100%" table-layout="fixed">
            <el-table-column prop="billMonth" label="月份" width="86" align="center" />
            <el-table-column prop="userCount" label="用户" width="68" align="center" />
            <el-table-column prop="cardCount" label="卡数" width="72" align="center" />
            <el-table-column label="代还" min-width="112" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="112" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS" min-width="102" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="112" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <aside class="side-panel card-shell">
        <div class="side-card">
          <div class="side-card-title">当前范围</div>
          <div class="summary-list">
            <div class="summary-item">
              <span>统计时间</span>
              <strong>{{ currentScopeLabel }}</strong>
            </div>
            <div class="summary-item">
              <span>用户筛选</span>
              <strong>{{ query.userId ? '指定用户' : '全部用户' }}</strong>
            </div>
            <div class="summary-item">
              <span>银行卡筛选</span>
              <strong>{{ query.cardId ? '指定银行卡' : '全部银行卡' }}</strong>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">收益拆解</div>
          <div class="metric-stack">
            <div class="metric-line income">
              <span>手续费收入</span>
              <strong>{{ formatMoney(overview.totalFeeAmount) }}</strong>
            </div>
            <div class="metric-line cost">
              <span>POS成本</span>
              <strong>{{ formatMoney(overview.totalPosCostAmount) }}</strong>
            </div>
            <div class="metric-line profit">
              <span>净收益</span>
              <strong>{{ formatMoney(overview.totalNetProfit) }}</strong>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">说明</div>
          <div class="formula-box">
            <div class="formula-main">手续费收入 - POS成本 = 实际净收益</div>
            <div class="formula-sub">按时间、用户、银行卡联动筛选；表格区域优先保证可读性和横向空间。</div>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProfitStats' })
import { computed, watch, onActivated, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Wallet, Money, CreditCard, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
import { getUserTreeApi, getCardListApi } from '@/api/card'
import { getProfitOverviewApi, getProfitUserPageApi, getProfitCardPageApi, getProfitMonthListApi } from '@/api/profit'

interface OptionItem {
  id: number
  name: string
  parentId?: number | null
  userName?: string
  bankName?: string
  cardNoLast4?: string
  children?: OptionItem[]
}

interface UserProfitRow {
  userId: number
  userName: string
  cardCount: number
  billMonthCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalNetProfit: number
}

interface CardProfitRow {
  cardId: number
  userName: string
  ownerName: string
  ownerRelation: string
  bankName: string
  cardNoLast4: string
  billCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalNetProfit: number
}

interface MonthProfitRow {
  billMonth: string
  userCount: number
  cardCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalNetProfit: number
}

type ViewKey = 'users' | 'cards' | 'months'

const router = useRouter()
const currentYear = new Date().getFullYear()

function createDebouncedTask(fn: () => void, delay = 300) {
  let timer = 0
  const run = () => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      timer = 0
      fn()
    }, delay)
  }
  run.cancel = () => {
    if (timer) {
      window.clearTimeout(timer)
      timer = 0
    }
  }
  return run
}

const activeTab = ref<ViewKey>('users')
const profitMonthRange = ref<[string, string] | null>(null)
const overviewLoading = ref(false)
const userLoading = ref(false)
const cardLoading = ref(false)
const monthLoading = ref(false)
const userOptions = ref<OptionItem[]>([])
const cardOptions = ref<OptionItem[]>([])
const triggerProfitSearch = createDebouncedTask(() => {
  void handleSearch()
}, 300)
let syncingProfitFilters = false

const query = reactive({
  year: currentYear,
  month: undefined as number | undefined,
  userId: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const overview = reactive({
  year: currentYear,
  month: undefined as number | undefined,
  userCount: 0,
  cardCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalNetProfit: 0
})

const userPage = reactive({ current: 1, size: 10, total: 0, records: [] as UserProfitRow[] })
const cardPage = reactive({ current: 1, size: 10, total: 0, records: [] as CardProfitRow[] })
const monthList = ref<MonthProfitRow[]>([])

const currentScopeLabel = computed(() => query.month ? `${query.year}年${query.month}月` : `${query.year}年全年`)

const summaryCards = computed(() => [
  {
    label: '代还总额',
    value: `¥${formatMoney(overview.totalBillAmount)}`,
    sub: `${currentScopeLabel.value}账单汇总`,
    className: '',
    icon: Wallet,
    iconBg: '#eaf2ff',
    iconColor: '#0958d9'
  },
  {
    label: '手续费收入',
    value: `¥${formatMoney(overview.totalFeeAmount)}`,
    sub: `覆盖 ${overview.userCount} 个用户`,
    className: 'amount-income',
    icon: Money,
    iconBg: '#e8f7ed',
    iconColor: '#2f9e44'
  },
  {
    label: 'POS总成本',
    value: `¥${formatMoney(overview.totalPosCostAmount)}`,
    sub: `涉及 ${overview.cardCount} 张银行卡`,
    className: 'amount-cost',
    icon: CreditCard,
    iconBg: '#fdebec',
    iconColor: '#cf1322'
  },
  {
    label: '实际净收益',
    value: `¥${formatMoney(overview.totalNetProfit)}`,
    sub: '手续费 - POS成本',
    className: Number(overview.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost',
    icon: TrendCharts,
    iconBg: Number(overview.totalNetProfit || 0) >= 0 ? '#e8f7ed' : '#fdebec',
    iconColor: Number(overview.totalNetProfit || 0) >= 0 ? '#2f9e44' : '#cf1322'
  }
])

const activeViewTitle = computed(() => {
  if (activeTab.value === 'cards') return '银行卡收益明细'
  if (activeTab.value === 'months') return '月度收益汇总'
  return '用户收益明细'
})

const activeViewDesc = computed(() => {
  if (activeTab.value === 'cards') return '按银行卡查看利润贡献、持卡关系和收益结构'
  if (activeTab.value === 'months') return '查看年度内各月份利润走势与汇总变化'
  return '按用户查看全年 / 当月代还、手续费、POS成本与净收益'
})

const activeViewCountLabel = computed(() => {
  if (activeTab.value === 'cards') return '银行卡'
  if (activeTab.value === 'months') return '月份'
  return '用户'
})

const activeViewCount = computed(() => {
  if (activeTab.value === 'cards') return cardPage.total
  if (activeTab.value === 'months') return monthList.value.length
  return userPage.total
})

const searchMetaText = computed(() => `${activeViewTitle.value}共 ${activeViewCount.value} 条数据`)

function buildParams(extra: Record<string, any> = {}) {
  return {
    year: query.year,
    month: query.month,
    userId: query.userId,
    cardId: query.cardId,
    ...extra
  }
}

async function fetchBaseOptions() {
  const [userRes, cardRes]: any = await Promise.all([getUserTreeApi(), getCardListApi()])
  userOptions.value = flattenTopUsers(userRes.data || [])
  cardOptions.value = cardRes.data || []
}

function flattenTopUsers(list: OptionItem[]) {
  return list.filter(item => !item.parentId).map(item => ({ id: item.id, name: item.name }))
}

async function fetchOverview() {
  overviewLoading.value = true
  try {
    const res: any = await getProfitOverviewApi(buildParams())
    overview.year = res.data?.year ?? query.year
    overview.month = res.data?.month
    overview.userCount = Number(res.data?.userCount ?? 0)
    overview.cardCount = Number(res.data?.cardCount ?? 0)
    overview.totalBillAmount = Number(res.data?.totalBillAmount ?? 0)
    overview.totalFeeAmount = Number(res.data?.totalFeeAmount ?? 0)
    overview.totalPosCostAmount = Number(res.data?.totalPosCostAmount ?? 0)
    overview.totalNetProfit = Number(res.data?.totalNetProfit ?? 0)
  } finally {
    overviewLoading.value = false
  }
}

async function fetchUserPage() {
  userLoading.value = true
  try {
    const res: any = await getProfitUserPageApi(buildParams({ current: userPage.current, size: userPage.size }))
    userPage.records = res.data?.records || []
    userPage.total = Number(res.data?.total || 0)
    userPage.current = Number(res.data?.current || userPage.current)
    userPage.size = Number(res.data?.size || userPage.size)
  } finally {
    userLoading.value = false
  }
}

async function fetchCardPage() {
  cardLoading.value = true
  try {
    const res: any = await getProfitCardPageApi(buildParams({ current: cardPage.current, size: cardPage.size }))
    cardPage.records = res.data?.records || []
    cardPage.total = Number(res.data?.total || 0)
    cardPage.current = Number(res.data?.current || cardPage.current)
    cardPage.size = Number(res.data?.size || cardPage.size)
  } finally {
    cardLoading.value = false
  }
}

async function fetchMonthList() {
  monthLoading.value = true
  try {
    const res: any = await getProfitMonthListApi(buildParams({ current: 1, size: 100 }))
    monthList.value = res.data || []
  } finally {
    monthLoading.value = false
  }
}

async function handleSearch() {
  userPage.current = 1
  cardPage.current = 1
  await Promise.all([fetchOverview(), fetchUserPage(), fetchCardPage(), fetchMonthList()])
}

async function refresh() {
  await handleSearch()
}

function resetQuery() {
  triggerProfitSearch.cancel()
  syncingProfitFilters = true
  profitMonthRange.value = null
  query.year = currentYear
  query.month = undefined
  query.userId = undefined
  query.cardId = undefined
  syncingProfitFilters = false
  void handleSearch()
}

function handleUserSizeChange(size: number) {
  userPage.size = size
  userPage.current = 1
  fetchUserPage()
}

function handleCardSizeChange(size: number) {
  cardPage.size = size
  cardPage.current = 1
  fetchCardPage()
}

function cardLabel(item: OptionItem) {
  return `${item.userName || '未归属'} / ${item.bankName || ''} *${item.cardNoLast4 || ''}`
}

const billActionLabel = computed(() => query.month ? '当月账单' : '年度账单')

function buildBillRouteQuery(extra: Record<string, any>) {
  const routeQuery: Record<string, any> = {
    year: String(query.year),
    ...extra
  }
  if (query.month) {
    routeQuery.billMonth = `${query.year}-${String(query.month).padStart(2, '0')}`
  }
  return routeQuery
}

function openUserBills(row: UserProfitRow) {
  router.push({ path: '/bills', query: buildBillRouteQuery({ ownerId: String(row.userId) }) })
}

function openCardBills(row: CardProfitRow) {
  router.push({ path: '/bills', query: buildBillRouteQuery({ cardId: String(row.cardId) }) })
}

function formatMoney(value: number | string | null | undefined) {
  const amount = Number(value ?? 0)
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

onMounted(async () => {
  await fetchBaseOptions()
  await handleSearch()
})

onActivated(async () => {
  await fetchBaseOptions()
  await handleSearch()
})

onUnmounted(() => {
  triggerProfitSearch.cancel()
})

watch(
  profitMonthRange,
  (range) => {
    if (syncingProfitFilters) return
    triggerProfitSearch.cancel()
    syncingProfitFilters = true
    if (range && range[0]) {
      const [yearStr, monthStr] = range[0].split('-')
      query.year = Number(yearStr)
      query.month = Number(monthStr) || undefined
    } else {
      query.year = currentYear
      query.month = undefined
    }
    syncingProfitFilters = false
    triggerProfitSearch()
  },
  { flush: 'sync' }
)

watch(
  () => [query.userId, query.cardId],
  () => {
    if (syncingProfitFilters) return
    triggerProfitSearch()
  },
  { flush: 'sync' }
)
</script>

<style scoped>
.profit-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
  background: #f5f7fb;
  overflow: hidden;
  box-sizing: border-box;
}

.card-shell {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.045);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 12px;
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
  font-size: 18px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.header-subtitle {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #7c8799;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.action-btn,
.header-actions :deep(.el-button) {
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
}

.header-stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.header-stat {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.header-stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-stat-label {
  font-size: 12px;
  color: #7c8799;
}

.header-stat-value {
  margin-top: 2px;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.header-stat-sub {
  margin-top: 4px;
  font-size: 11px;
  color: #98a2b3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profit-search-panel {
  flex-shrink: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

.main-panel,
.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-panel {
  padding: 8px;
}

.side-panel {
  padding: 8px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2a37;
}

.panel-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #7c8799;
}

.inline-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 10px;
  height: 30px;
  border-radius: 8px;
  background: #f5f8fc;
  color: #667085;
  font-size: 12px;
  white-space: nowrap;
}

.table-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #fff;
}

.table-shell :deep(.el-table) {
  flex: 1;
  --el-table-border-color: #e5eaf1;
  font-size: 12px;
}

.table-shell :deep(.el-table .cell) {
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

.table-shell :deep(.el-table th.el-table__cell) {
  height: 36px;
  background: #f7f9fc;
  color: #5b6472;
  font-size: 11px;
  font-weight: 600;
}

.table-shell :deep(.el-table td.el-table__cell) {
  height: 48px;
}

.table-shell :deep(.el-table__body-wrapper) {
  scrollbar-width: none;
}

.table-shell :deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

.table-shell :deep(.el-tag) {
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 8px 10px;
  border-top: 1px solid #eef2f6;
  background: #fff;
}

.pager-wrap :deep(.el-pagination) {
  transform: scale(0.94);
  transform-origin: right center;
}

.side-card {
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e7edf4;
  border-radius: 12px;
}

.side-card + .side-card {
  margin-top: 8px;
}

.side-card-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #1f2a37;
}

.summary-list,
.metric-stack {
  display: grid;
  gap: 8px;
}

.summary-item,
.metric-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #667085;
}

.summary-item strong,
.metric-line strong {
  color: #1f2a37;
  font-size: 12px;
}

.metric-line {
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #e9eef4;
}

.metric-line.income strong {
  color: #2f9e44;
}

.metric-line.cost strong {
  color: #cf1322;
}

.metric-line.profit strong {
  color: #0958d9;
}

.formula-box {
  display: grid;
  gap: 6px;
}

.formula-main {
  font-size: 12px;
  font-weight: 700;
  color: #1f2a37;
  line-height: 1.5;
}

.formula-sub {
  font-size: 11px;
  line-height: 1.5;
  color: #667085;
}

.amount-income {
  color: #2f9e44;
  font-weight: 700;
}

.amount-cost {
  color: #cf1322;
  font-weight: 700;
}

@media (max-width: 1480px) {
  .filter-card {
    width: 180px;
  }

  .content-grid {
    grid-template-columns: minmax(0, 1fr) 200px;
  }
}

@media (max-width: 1320px) {
  .header-stat-sub,
  .panel-desc,
  .inline-summary {
    display: none;
  }

  .content-grid {
    grid-template-columns: minmax(0, 1fr) 180px;
  }
}
</style>
