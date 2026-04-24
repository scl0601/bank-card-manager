<template>
  <div class="profit-page">
    <div class="page-title">收益统计</div>

    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="统计年份">
        <el-select v-model="query.year" style="width: 120px">
          <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
        </el-select>
      </el-form-item>
      <el-form-item label="统计月份">
        <el-select v-model="query.month" placeholder="全年" clearable style="width: 120px">
          <el-option v-for="month in monthOptions" :key="month" :label="`${month}月`" :value="month" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户">
        <el-select v-model="query.userId" placeholder="全部用户" clearable filterable style="width: 180px">
          <el-option v-for="item in userOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="银行卡">
        <el-select v-model="query.cardId" placeholder="全部银行卡" clearable filterable style="width: 240px">
          <el-option v-for="item in cardOptions" :key="item.id" :label="cardLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
    </SearchBar>

    <div class="summary-grid" v-loading="overviewLoading">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <div class="summary-label">{{ item.label }}</div>
        <div class="summary-value" :class="item.className">{{ item.value }}</div>
        <div class="summary-sub">{{ item.sub }}</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="profit-tabs">
      <el-tab-pane label="按用户统计" name="users">
        <div class="page-card" v-loading="userLoading">
          <div class="card-header">
            <span>单个用户全年/当月盈利明细</span>
            <span class="card-sub">手续费收入 - POS成本 = 实际净收益</span>
          </div>
          <el-table :data="userPage.records" border stripe>
            <el-table-column prop="userName" label="用户" min-width="140" />
            <el-table-column prop="cardCount" label="关联银行卡" width="110" align="center" />
            <el-table-column prop="billMonthCount" label="账单月份数" width="110" align="center" />
            <el-table-column label="代还总金额" min-width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费收入" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS总成本" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="120" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
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
              layout="total, prev, pager, next, sizes"
              :total="userPage.total"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="fetchUserPage"
              @size-change="handleUserSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="按银行卡统计" name="cards">
        <div class="page-card" v-loading="cardLoading">
          <div class="card-header">
            <span>单张银行卡月/年盈利明细</span>
            <span class="card-sub">支持查看归属用户、持卡关系与净利润</span>
          </div>
          <el-table :data="cardPage.records" border stripe>
            <el-table-column prop="userName" label="所属用户" min-width="120" />
            <el-table-column prop="ownerName" label="持卡人" min-width="120" />
            <el-table-column label="关系" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.ownerRelation || '本人' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="银行卡" min-width="180">
              <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
            </el-table-column>
            <el-table-column prop="billCount" label="账单数" width="90" align="center" />
            <el-table-column label="代还总金额" min-width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费收入" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS总成本" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="120" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
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
              layout="total, prev, pager, next, sizes"
              :total="cardPage.total"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="fetchCardPage"
              @size-change="handleCardSizeChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="月度汇总" name="months">
        <div class="page-card" v-loading="monthLoading">
          <div class="card-header">
            <span>年度 / 月度收益汇总</span>
            <span class="card-sub">默认展示当前年份 1-12 月完整账单利润走势</span>
          </div>
          <el-table :data="monthList" border stripe>
            <el-table-column prop="billMonth" label="月份" width="100" align="center" />
            <el-table-column prop="userCount" label="用户数" width="90" align="center" />
            <el-table-column prop="cardCount" label="银行卡数" width="100" align="center" />
            <el-table-column label="代还总金额" min-width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalBillAmount) }}</template>
            </el-table-column>
            <el-table-column label="手续费收入" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-income">{{ formatMoney(row.totalFeeAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="POS总成本" min-width="120" align="right">
              <template #default="{ row }"><span class="amount-cost">{{ formatMoney(row.totalPosCostAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="净收益" min-width="120" align="right">
              <template #default="{ row }">
                <span :class="Number(row.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'">{{ formatMoney(row.totalNetProfit) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProfitStats' })
import { computed, watch, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import SearchBar from '@/components/SearchBar/index.vue'
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

const router = useRouter()
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, index) => currentYear - 2 + index)
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)

const activeTab = ref('users')
const overviewLoading = ref(false)
const userLoading = ref(false)
const cardLoading = ref(false)
const monthLoading = ref(false)
const userOptions = ref<OptionItem[]>([])
const cardOptions = ref<OptionItem[]>([])

const query = reactive({
  year: currentYear,
  month: undefined as number | undefined,
  userId: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const overview = ref({
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

const summaryCards = computed(() => [
  {
    label: '代还总金额',
    value: `¥ ${formatMoney(overview.value.totalBillAmount)}`,
    sub: `${overview.value.year}年${query.month ? `${query.month}月` : ''}账单汇总`,
    className: ''
  },
  {
    label: '手续费收入',
    value: `¥ ${formatMoney(overview.value.totalFeeAmount)}`,
    sub: `覆盖 ${overview.value.userCount || 0} 个用户`,
    className: 'amount-income'
  },
  {
    label: 'POS总成本',
    value: `¥ ${formatMoney(overview.value.totalPosCostAmount)}`,
    sub: `涉及 ${overview.value.cardCount || 0} 张银行卡`,
    className: 'amount-cost'
  },
  {
    label: '实际净收益',
    value: `¥ ${formatMoney(overview.value.totalNetProfit)}`,
    sub: '自动按 手续费 - POS成本 计算',
    className: Number(overview.value.totalNetProfit || 0) >= 0 ? 'amount-income' : 'amount-cost'
  }
])

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
    overview.value = {
      year: res.data?.year ?? query.year,
      month: res.data?.month,
      userCount: Number(res.data?.userCount ?? 0),
      cardCount: Number(res.data?.cardCount ?? 0),
      totalBillAmount: Number(res.data?.totalBillAmount ?? 0),
      totalFeeAmount: Number(res.data?.totalFeeAmount ?? 0),
      totalPosCostAmount: Number(res.data?.totalPosCostAmount ?? 0),
      totalNetProfit: Number(res.data?.totalNetProfit ?? 0)
    }
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

function resetQuery() {
  query.year = currentYear
  query.month = undefined
  query.userId = undefined
  query.cardId = undefined
  handleSearch()
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

const billActionLabel = computed(() => query.month ? '查看当月账单' : '查看年度账单')

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

// 监听查询条件变化，自动搜索
watch(query, () => {
  handleSearch()
}, { deep: true })
</script>

<style scoped>
.profit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.summary-card {
  padding: 18px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: var(--shadow-card, 0 1px 4px rgba(0, 21, 41, 0.06));
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.summary-sub {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.profit-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.page-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: var(--shadow-card, 0 1px 4px rgba(0, 21, 41, 0.06));
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-weight: 600;
  color: #303133;
}

.card-sub {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.amount-income {
  color: #67c23a;
  font-weight: 600;
}

.amount-cost {
  color: #f56c6c;
  font-weight: 600;
}
</style>
