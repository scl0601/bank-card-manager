<template>
  <div class="profit-page">
    <section class="page-header card-shell">
      <div class="header-top">
        <div class="header-copy">
          <div class="header-title">收益统计</div>
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
            <el-table-column label="费率" width="76" align="center">
              <template #default="{ row }">{{ formatRate(row.avgFeeRate) }}%</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="110" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="支付状态" width="92" align="center">
              <template #default="{ row }">
                <el-tag :type="feePaidTagType(row)" size="small" effect="light">{{ feePaidText(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="POS" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="其他费用" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalOtherFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="110" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="128" fixed="right" align="center">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button type="primary" link size="small" @click="openOtherFeeEditor('user', row)">费用</el-button>
                  <el-button type="primary" link size="small" @click="openUserBills(row)">{{ billActionLabel }}</el-button>
                </div>
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
            <el-table-column label="银行卡" min-width="136">
              <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
            </el-table-column>
            <el-table-column prop="billCount" label="账单" width="68" align="center" />
            <el-table-column label="代还" min-width="110" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="费率" width="76" align="center">
              <template #default="{ row }">{{ formatRate(row.avgFeeRate) }}%</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="110" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="支付状态" width="92" align="center">
              <template #default="{ row }">
                <el-tag :type="feePaidTagType(row)" size="small" effect="light">{{ feePaidText(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="POS" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="其他费用" min-width="100" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalOtherFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="110" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="128" fixed="right" align="center">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button type="primary" link size="small" @click="openOtherFeeEditor('card', row)">费用</el-button>
                  <el-button type="primary" link size="small" @click="openCardBills(row)">{{ billActionLabel }}</el-button>
                </div>
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
            <el-table-column label="费率" width="76" align="center">
              <template #default="{ row }">{{ formatRate(row.avgFeeRate) }}%</template>
            </el-table-column>
            <el-table-column label="手续费" min-width="112" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="支付状态" width="92" align="center">
              <template #default="{ row }">
                <el-tag :type="feePaidTagType(row)" size="small" effect="light">{{ feePaidText(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="POS" min-width="102" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="其他费用" min-width="102" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalOtherFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="112" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="76" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openOtherFeeEditor('month', row)">费用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

    </section>

    <el-drawer v-model="otherFeeDrawerVisible" :title="otherFeeDrawerTitle" size="560px" destroy-on-close>
      <div class="other-fee-editor" v-loading="otherFeeLoading">
        <div class="other-fee-editor-head">
          <span>账单</span>
          <span>费率 / 手续费 / 支付状态</span>
          <span>其他费用</span>
        </div>
        <div v-if="otherFeeRows.length" class="other-fee-editor-list">
          <div v-for="row in otherFeeRows" :key="row.id" class="other-fee-editor-row">
            <div class="fee-bill-main">
              <span class="fee-bill-title">{{ feeBillLabel(row) }}</span>
              <span class="fee-bill-sub">{{ row.billMonth }} · 还款日 {{ repayDateText(row.repayDate) }}</span>
            </div>
            <div class="fee-bill-stats">
              <span>{{ formatRate(row.feeRate) }}%</span>
              <span class="amount-income">¥{{ formatMoney(row.feeAmount) }}</span>
              <el-tag v-if="row.feePaid" type="success" size="small" effect="light">已支付</el-tag>
              <el-tag v-else type="warning" size="small" effect="plain">未支付</el-tag>
            </div>
            <div class="fee-bill-edit">
              <span class="currency-symbol">¥</span>
              <el-input-number
                :model-value="otherFeeDraftValue(row)"
                :min="0"
                :precision="2"
                :controls="false"
                size="small"
                class="other-fee-input"
                :disabled="savingOtherFeeBillId === row.id"
                @update:model-value="(val: any) => updateOtherFeeDraft(row.id, val)"
                @keyup.enter="saveOtherFee(row)"
              />
              <el-button
                type="primary"
                link
                size="small"
                :loading="savingOtherFeeBillId === row.id"
                :disabled="!isOtherFeeChanged(row)"
                @click="saveOtherFee(row)"
              >
                保存
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无账单" :image-size="64" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProfitStats' })
import { computed, watch, onActivated, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Wallet, Money, CreditCard, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
import { getUserTreeApi, getCardListApi } from '@/api/card'
import { getProfitOverviewApi, getProfitUserPageApi, getProfitCardPageApi, getProfitMonthListApi } from '@/api/profit'
import { getBillPageApi, updateBillApi } from '@/api/bill'

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
  billCount: number
  billMonthCount: number
  totalBillAmount: number
  avgFeeRate: number
  totalFeeAmount: number
  feePaidCount: number
  feeUnpaidCount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  totalNetProfit: number
}

interface CardProfitRow {
  cardId: number
  userName: string
  ownerName: string
  bankName: string
  cardNoLast4: string
  billCount: number
  totalBillAmount: number
  avgFeeRate: number
  totalFeeAmount: number
  feePaidCount: number
  feeUnpaidCount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  totalNetProfit: number
}

interface MonthProfitRow {
  billMonth: string
  userCount: number
  cardCount: number
  billCount: number
  totalBillAmount: number
  avgFeeRate: number
  totalFeeAmount: number
  feePaidCount: number
  feeUnpaidCount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  totalNetProfit: number
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
  posCostAmount?: number | null
  otherFeeAmount?: number | null
  verified?: boolean | null
  expenseVerified?: boolean | null
  status: number
  remark?: string | null
}

type ViewKey = 'users' | 'cards' | 'months'
type OtherFeeScope = 'user' | 'card' | 'month' | 'current'

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
const otherFeeDrawerVisible = ref(false)
const otherFeeLoading = ref(false)
const otherFeeRows = ref<BillRow[]>([])
const otherFeeDraftMap = ref<Record<number, number>>({})
const savingOtherFeeBillId = ref<number | null>(null)
const otherFeeEditorTitle = ref('其他费用')
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
  totalOtherFeeAmount: 0,
  totalNetProfit: 0
})

const userPage = reactive({ current: 1, size: 10, total: 0, records: [] as UserProfitRow[] })
const cardPage = reactive({ current: 1, size: 10, total: 0, records: [] as CardProfitRow[] })
const monthList = ref<MonthProfitRow[]>([])

const otherFeeDrawerTitle = computed(() => `${otherFeeEditorTitle.value} · ${currentScopeLabel.value}`)

const currentScopeLabel = computed(() => query.month ? `${query.year}年${query.month}月` : `${query.year}年全年`)

const summaryCards = computed(() => [
  {
    label: '账单总金额',
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
    label: '其他费用',
    value: `¥${formatMoney(overview.totalOtherFeeAmount)}`,
    sub: '编辑账单录入的其他扣费',
    className: 'amount-cost',
    icon: Money,
    iconBg: '#fff1e6',
    iconColor: '#d46b08'
  },
  {
    label: '实际净收益',
    value: `¥${formatMoney(overview.totalNetProfit)}`,
    sub: '手续费 - POS成本 - 其他费用',
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

function buildParams(extra: Record<string, any> = {}) {
  return {
    year: query.year,
    month: query.month,
    userId: query.userId,
    cardId: query.cardId,
    ...extra
  }
}

function buildBillParams(extra: Record<string, any> = {}) {
  const params: Record<string, any> = {
    year: query.year,
    ...extra
  }
  if (query.month) {
    params.billMonth = `${query.year}-${String(query.month).padStart(2, '0')}`
  }
  return params
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
    overview.totalOtherFeeAmount = Number(res.data?.totalOtherFeeAmount ?? 0)
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

async function refreshWithoutResetPosition() {
  await Promise.all([fetchOverview(), fetchUserPage(), fetchCardPage(), fetchMonthList()])
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

async function openOtherFeeEditor(scope: OtherFeeScope, row?: UserProfitRow | CardProfitRow | MonthProfitRow) {
  const params = buildOtherFeeParams(scope, row)
  otherFeeEditorTitle.value = otherFeeTitle(scope, row)
  otherFeeDrawerVisible.value = true
  otherFeeLoading.value = true
  otherFeeRows.value = []
  otherFeeDraftMap.value = {}

  try {
    const rows = await fetchAllBillRows(params)
    otherFeeRows.value = rows
    syncOtherFeeDrafts(rows)
  } finally {
    otherFeeLoading.value = false
  }
}

function buildOtherFeeParams(scope: OtherFeeScope, row?: UserProfitRow | CardProfitRow | MonthProfitRow) {
  if (scope === 'user' && row) {
    return buildBillParams({ ownerId: (row as UserProfitRow).userId })
  }
  if (scope === 'card' && row) {
    return buildBillParams({ cardId: (row as CardProfitRow).cardId })
  }
  if (scope === 'month' && row) {
    const monthRow = row as MonthProfitRow
    return {
      ...buildBillParams({}),
      billMonth: monthRow.billMonth
    }
  }
  return buildBillParams({ ownerId: query.userId, cardId: query.cardId })
}

function otherFeeTitle(scope: OtherFeeScope, row?: UserProfitRow | CardProfitRow | MonthProfitRow) {
  if (scope === 'user' && row) return `${(row as UserProfitRow).userName} 其他费用`
  if (scope === 'card' && row) {
    const card = row as CardProfitRow
    return `${card.bankName} *${card.cardNoLast4} 其他费用`
  }
  if (scope === 'month' && row) return `${(row as MonthProfitRow).billMonth} 其他费用`
  return '其他费用'
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

function syncOtherFeeDrafts(rows: BillRow[]) {
  const next: Record<number, number> = {}
  rows.forEach(row => {
    next[row.id] = otherFeeDraftMap.value[row.id] ?? toAmount(row.otherFeeAmount)
  })
  otherFeeDraftMap.value = next
}

function otherFeeDraftValue(row: BillRow) {
  return otherFeeDraftMap.value[row.id] ?? toAmount(row.otherFeeAmount)
}

function updateOtherFeeDraft(billId: number, value: any) {
  otherFeeDraftMap.value[billId] = toAmount(value)
}

function isOtherFeeChanged(row: BillRow) {
  return Math.abs(otherFeeDraftValue(row) - toAmount(row.otherFeeAmount)) >= 0.005
}

async function saveOtherFee(row: BillRow) {
  if (!isOtherFeeChanged(row) || savingOtherFeeBillId.value === row.id) return
  savingOtherFeeBillId.value = row.id
  try {
    const otherFeeAmount = otherFeeDraftValue(row)
    await updateBillApi(buildBillUpdatePayload(row, { otherFeeAmount }))
    row.otherFeeAmount = otherFeeAmount
    syncOtherFeeDrafts(otherFeeRows.value)
    ElMessage.success('其他费用已保存')
    await refreshWithoutResetPosition()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存其他费用失败')
  } finally {
    savingOtherFeeBillId.value = null
  }
}

function buildBillUpdatePayload(row: BillRow, overrides: Record<string, any> = {}) {
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
    feePaid: row.feePaid,
    verified: row.verified,
    expenseVerified: row.expenseVerified,
    posCostAmount: toAmount(row.posCostAmount),
    otherFeeAmount: toAmount(row.otherFeeAmount),
    status: row.status,
    remark: row.remark || '',
    ...overrides
  }
}

function feeBillLabel(row: BillRow) {
  return `${row.ownerName || '未命名'} · ${row.bankName || '-'} · 尾号${row.cardNoLast4 || '-'}`
}

function repayDateText(date: string | null | undefined) {
  return date ? String(date).slice(5) : '-'
}

function feePaidTotal(row: { billCount?: number; feePaidCount?: number; feeUnpaidCount?: number }) {
  const directTotal = Number(row.billCount ?? 0)
  if (directTotal > 0) return directTotal
  return Number(row.feePaidCount ?? 0) + Number(row.feeUnpaidCount ?? 0)
}

function feePaidText(row: { billCount?: number; feePaidCount?: number; feeUnpaidCount?: number }) {
  const total = feePaidTotal(row)
  const paid = Number(row.feePaidCount ?? 0)
  if (total <= 0) return '-'
  if (paid >= total) return '已支付'
  if (paid <= 0) return '未支付'
  return `部分 ${paid}/${total}`
}

function feePaidTagType(row: { billCount?: number; feePaidCount?: number; feeUnpaidCount?: number }) {
  const total = feePaidTotal(row)
  const paid = Number(row.feePaidCount ?? 0)
  if (total <= 0) return 'info'
  if (paid >= total) return 'success'
  if (paid <= 0) return 'warning'
  return 'primary'
}

function formatRate(value: number | string | null | undefined) {
  const rate = Number(value ?? 0)
  return Number.isFinite(rate) ? rate.toFixed(2) : '0.00'
}

function toAmount(value: number | string | null | undefined) {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount : 0
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.header-stat {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 76px;
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
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  flex: 1;
  min-height: 0;
}

.main-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.main-panel {
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

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  padding: 0 2px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
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

.amount-income {
  color: #2f9e44;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

.amount-cost {
  color: #cf1322;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

.other-fee-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 240px;
}

.other-fee-editor-head,
.other-fee-editor-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 176px 154px;
  align-items: center;
  gap: 12px;
}

.other-fee-editor-head {
  padding: 8px 10px;
  border-radius: 10px;
  background: #f7f9fc;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.other-fee-editor-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.other-fee-editor-row {
  min-height: 54px;
  padding: 9px 10px;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  background: #fff;
}

.fee-bill-main,
.fee-bill-stats {
  min-width: 0;
}

.fee-bill-title,
.fee-bill-sub {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-bill-title {
  color: #1f2a37;
  font-size: 12px;
  font-weight: 700;
}

.fee-bill-sub {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 600;
}

.fee-bill-stats {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #667085;
}

.fee-bill-stats > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-bill-edit {
  display: grid;
  grid-template-columns: auto 92px 38px;
  align-items: center;
  gap: 4px;
}

.currency-symbol {
  color: #98a2b3;
  font-size: 12px;
  font-weight: 700;
}

.other-fee-input {
  width: 92px;
}

.other-fee-input :deep(.el-input__wrapper) {
  min-height: 26px;
  padding: 0 7px;
  border-radius: 8px;
}

.other-fee-input :deep(.el-input__inner) {
  color: #1f2a37;
  font-size: 12px;
  font-weight: 700;
  text-align: right;
}

@media (max-width: 1480px) {
  .filter-card {
    width: 180px;
  }

  .header-stat-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

}

@media (max-width: 1320px) {
  .header-stat-sub,
  .panel-desc,
  .inline-summary {
    display: none;
  }
}
</style>
