<template>
  <div class="special-page">
    <section class="special-header">
      <div class="header-main">
        <div>
          <div class="page-title">特殊通道</div>
          <div class="page-meta">
            <span v-if="config.userId">{{ config.userName || '-' }}</span>
            <span v-if="config.userId">费率 {{ formatRate(config.feeRate) }}%</span>
            <span v-if="config.userId">{{ cards.length }} 张卡</span>
            <span v-else>请先绑定用户信息里的特殊用户</span>
          </div>
        </div>
      </div>

      <div class="config-bar">
        <el-select-v2
          v-model="selectedUserId"
          class="user-select"
          :options="userOptions"
          placeholder="选择特殊用户"
          filterable
          :height="300"
          :item-height="32"
          :disabled="!canEdit"
        />
        <el-button type="primary" :disabled="!canEdit || !selectedUserId" :loading="savingConfig" @click="saveConfig">
          保存配置
        </el-button>
        <el-button :icon="RefreshRight" @click="refreshAll">刷新</el-button>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="special-tabs">
      <el-tab-pane label="特殊卡务" name="cards">
        <section class="card-toolbar">
          <div class="stat-strip">
            <div class="stat-item">
              <span>银行卡</span>
              <strong>{{ cards.length }}</strong>
            </div>
            <div class="stat-item">
              <span>总额度</span>
              <strong>{{ formatMoney(totalCardAmount) }}</strong>
            </div>
            <div class="stat-item">
              <span>账单数量</span>
              <strong>{{ totalBillCount }}</strong>
            </div>
          </div>
          <el-button type="primary" :icon="Plus" :disabled="!canEdit || !config.userId" @click="openCreateCard">
            新增银行卡
          </el-button>
        </section>

        <section v-loading="cardLoading" class="special-card-grid">
          <article v-for="card in cards" :key="card.id" class="bank-card-tile" :class="{ disabled: card.status === 1 }">
            <div class="tile-head">
              <div class="bank-mark">
                <el-icon><CreditCard /></el-icon>
              </div>
              <div class="tile-title">
                <strong>{{ card.bankName }}</strong>
                <span>尾号 {{ card.cardNoLast4 }}</span>
              </div>
              <el-tag size="small" :type="card.status === 1 ? 'info' : 'success'" effect="light">
                {{ card.statusDesc || statusText(card.status) }}
              </el-tag>
            </div>
            <div class="tile-amount">
              <span>总金额</span>
              <strong>{{ formatMoney(card.totalAmount) }}</strong>
            </div>
            <div class="tile-meta">
              <span>账单 {{ card.billCount || 0 }} 条</span>
              <span>费率 {{ formatRate(card.feeRate) }}%</span>
            </div>
            <div v-if="card.remark" class="tile-remark" :title="card.remark">{{ card.remark }}</div>
            <div class="tile-actions">
              <el-button size="small" :disabled="!canEdit" @click="openEditCard(card)">编辑</el-button>
              <el-button size="small" type="danger" plain :disabled="!isAdmin" @click="deleteCard(card)">删除</el-button>
            </div>
          </article>
          <el-empty v-if="!cardLoading && cards.length === 0" description="暂无特殊银行卡" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="特殊账单" name="bills">
        <section class="filter-line">
          <el-select v-model="billQuery.year" class="filter-item" placeholder="年份">
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
          <el-select v-model="billQuery.cardId" class="filter-card" placeholder="银行卡" filterable>
            <el-option v-for="card in cards" :key="card.id" :label="cardLabel(card)" :value="card.id" />
          </el-select>
          <el-select v-model="deleteBeforeYear" class="filter-item" placeholder="保留起始年">
            <el-option v-for="year in deleteBeforeYearOptions" :key="year" :label="`保留${year}年起`" :value="year" />
          </el-select>
          <el-button
            type="danger"
            plain
            :disabled="!isAdmin || !billQuery.cardId || deletingHistoryBills"
            :loading="deletingHistoryBills"
            @click="deleteHistoryBills"
          >
            删除之前账单
          </el-button>
          <el-button @click="resetBillQuery">重置</el-button>
          <span class="filter-hint">{{ annualBillHint }}</span>
        </section>

        <section class="bill-table-shell">
          <el-table
            v-loading="billLoading"
            :data="billRows"
            border
            stripe
            size="small"
            row-key="id"
            show-summary
            :summary-method="billSummaryMethod"
            table-layout="fixed"
          >
            <el-table-column prop="billYear" label="年" width="42" align="center" />
            <el-table-column prop="billMonthNo" label="月" width="36" align="center" />
            <el-table-column label="银行" width="82" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="strong-cell">{{ row.bankName }} / {{ row.cardNoLast4 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="卡片额度" align="right">
              <template #default="{ row }">
                <span class="money-text">{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="账单日" width="46" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.billDay" class="day-input" size="small" :min="1" :max="31" :precision="0" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column label="还款日" width="46" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.repaymentDay" class="day-input" size="small" :min="1" :max="31" :precision="0" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column prop="billAmount" label="每月账单金额" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.billAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.billAmountVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="xiaohuanRepayAmount" label="小焕还款" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.xiaohuanRepayAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.xiaohuanRepayVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="customerRepayAmount" label="客户还款" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.customerRepayAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.customerRepayVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="xiaohuanConsumeAmount" label="小焕消费" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.xiaohuanConsumeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.xiaohuanConsumeVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="customerNeedAmount" label="客户需要" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.customerNeedAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.customerNeedVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="customerConsumeAmount" label="客户消费" align="right">
              <template #default="{ row }">
                <div class="amount-verify-cell">
                  <el-input-number v-model="row.customerConsumeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                  <el-switch v-model="row.customerConsumeVerified" size="small" :disabled="!canEdit" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="diffAmount" label="差额" width="70" align="right">
              <template #default="{ row }">
                <span class="money-text" :class="calcDiff(row) >= 0 ? 'amount-income' : 'amount-cost'">
                  {{ formatMoney(calcDiff(row)) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balance" label="余额" width="74" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.balance" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column prop="interestAmount" label="利息" width="62" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.interestAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column prop="lateFeeAmount" label="滞纳金" width="62" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.lateFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column prop="installmentFeeAmount" label="分期费" width="62" align="right">
              <template #default="{ row }">
                <el-input-number v-model="row.installmentFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column label="备注" width="76">
              <template #default="{ row }">
                <el-input v-model="row.remark" size="small" maxlength="500" clearable :disabled="!canEdit" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="50" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" :disabled="!canEdit" :loading="savingBillId === row.id" @click="saveBill(row)">
                  保存
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="特殊收益" name="profit">
        <section class="filter-line">
          <el-select v-model="profitQuery.year" class="filter-item" placeholder="全部年份" clearable>
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
          <el-select v-model="profitQuery.month" class="filter-item" placeholder="月份" clearable>
            <el-option v-for="month in monthOptions" :key="month" :label="`${month}月`" :value="month" />
          </el-select>
          <el-select v-model="profitQuery.cardId" class="filter-card" placeholder="银行卡" clearable filterable>
            <el-option v-for="card in cards" :key="card.id" :label="cardLabel(card)" :value="card.id" />
          </el-select>
          <el-button @click="resetProfitQuery">重置</el-button>
          <span class="filter-hint">筛选变化后自动刷新</span>
        </section>

        <section class="profit-summary-grid" v-loading="profitLoading">
          <div v-for="item in profitSummaryCards" :key="item.label" class="profit-summary-item">
            <span>{{ item.label }}</span>
            <strong :class="item.className">{{ item.value }}</strong>
          </div>
        </section>

        <section class="profit-tables">
          <div class="profit-table-block">
            <div class="block-title">
              <span>收益统计</span>
              <span>{{ profitPaginationText }}</span>
            </div>
            <el-table :data="pagedProfitRows" border stripe size="small" height="100%" table-layout="fixed">
              <el-table-column prop="billYear" label="年" width="58" align="center" />
              <el-table-column prop="billMonthNo" label="月" width="48" align="center" />
              <el-table-column label="银行" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ row.bankName }} / {{ row.cardNoLast4 }}</template>
              </el-table-column>
              <el-table-column label="每月账单金额" min-width="100" align="right">
                <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
              <el-table-column prop="billDay" label="账单日" width="64" align="center" />
              <el-table-column prop="repaymentDay" label="还款日" width="64" align="center" />
              <el-table-column label="还款手续费" min-width="100" align="right">
                <template #default="{ row }">{{ formatMoney(row.repaymentFee) }}</template>
              </el-table-column>
              <el-table-column label="消费手续费" min-width="100" align="right">
                <template #default="{ row }">{{ formatMoney(row.consumeFee) }}</template>
              </el-table-column>
              <el-table-column label="利息" min-width="86" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.interestAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                </template>
              </el-table-column>
              <el-table-column label="滞纳金" min-width="86" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.lateFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                </template>
              </el-table-column>
              <el-table-column label="分期费" min-width="86" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.installmentFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEdit" />
                </template>
              </el-table-column>
              <el-table-column label="总计" min-width="100" align="right">
                <template #default="{ row }">
                  <span class="amount-income">{{ formatMoney(calcProfitTotal(row)) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="56" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" :disabled="!canEdit" :loading="savingProfitBillId === row.billId" @click="saveProfitExtras(row)">
                    保存
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="profit-pagination">
              <span class="pagination-meta">共 {{ profitTotal }} 条</span>
              <el-pagination
                v-model:current-page="profitPage.current"
                v-model:page-size="profitPage.size"
                :total="profitTotal"
                :page-sizes="profitPageSizeOptions"
                small
                background
                layout="sizes, prev, pager, next"
              />
            </div>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="cardDialogVisible" :title="cardDialogTitle" width="520px" destroy-on-close>
      <el-form ref="cardFormRef" :model="cardForm" :rules="cardRules" label-width="96px">
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="cardForm.bankName" maxlength="64" placeholder="请输入银行名称" />
        </el-form-item>
        <el-form-item label="卡号后四位" prop="cardNoLast4">
          <el-input v-model="cardForm.cardNoLast4" maxlength="4" placeholder="请输入4位数字" />
        </el-form-item>
        <el-form-item label="总金额" prop="totalAmount">
          <el-input-number v-model="cardForm.totalAmount" class="full-input" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="cardForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cardForm.remark" type="textarea" maxlength="500" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCard" @click="submitCard">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SpecialChannel' })

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CreditCard, Plus, RefreshRight } from '@element-plus/icons-vue'
import { getUserTreeApi } from '@/api/card'
import {
  deleteSpecialBillsBeforeYearApi,
  deleteSpecialCardApi,
  getSpecialBillPageApi,
  getSpecialCardsApi,
  getSpecialConfigApi,
  getSpecialProfitStatsApi,
  saveSpecialCardApi,
  saveSpecialConfigApi,
  updateSpecialBillApi,
  updateSpecialCardApi,
  updateSpecialProfitExtraFeesApi
} from '@/api/special'
import { useAuthStore } from '@/store/modules/auth'

type TabName = 'cards' | 'bills' | 'profit'

interface UserNode {
  id: number
  name: string
  parentId?: number | null
  parentName?: string
  phone?: string
  feeRate?: number | null
  effectiveFeeRate?: number | null
  status?: number
  children?: UserNode[]
}

interface SelectOption {
  label: string
  value: number
}

interface SpecialConfig {
  id?: number
  userId?: number
  userName?: string
  feeRate?: number | null
  phone?: string
  remark?: string
}

interface SpecialCard {
  id: number
  userId?: number
  userName?: string
  feeRate?: number | null
  bankName: string
  cardNoLast4: string
  totalAmount?: number | string | null
  status?: number
  statusDesc?: string
  remark?: string
  billCount?: number
}

interface SpecialBill {
  id: number
  cardId: number
  bankName?: string
  cardNoLast4?: string
  totalAmount?: number | string | null
  billMonth: string
  billYear?: number
  billMonthNo?: number
  monthlyTotalBillAmount?: number | null
  billDay?: number | null
  repaymentDay?: number | null
  billAmount?: number | null
  billAmountVerified?: boolean
  xiaohuanRepayAmount?: number | null
  xiaohuanRepayVerified?: boolean
  customerRepayAmount?: number | null
  customerRepayVerified?: boolean
  xiaohuanConsumeAmount?: number | null
  xiaohuanConsumeVerified?: boolean
  customerNeedAmount?: number | null
  customerNeedVerified?: boolean
  customerConsumeAmount?: number | null
  customerConsumeVerified?: boolean
  diffAmount?: number | null
  balance?: number | null
  feeRate?: number | null
  repaymentFee?: number | null
  consumeFee?: number | null
  interestAmount?: number | null
  lateFeeAmount?: number | null
  installmentFeeAmount?: number | null
  profitTotalAmount?: number | null
  remark?: string | null
}

interface ProfitOverview {
  cardCount?: number
  billCount?: number
  totalMonthlyTotalBillAmount?: number | string | null
  totalBillAmount?: number | string | null
  totalXiaohuanRepayAmount?: number | string | null
  totalCustomerRepayAmount?: number | string | null
  totalXiaohuanConsumeAmount?: number | string | null
  totalCustomerNeedAmount?: number | string | null
  totalCustomerConsumeAmount?: number | string | null
  totalDiffAmount?: number | string | null
  totalRepaymentFee?: number | string | null
  totalConsumeFee?: number | string | null
  totalInterestAmount?: number | string | null
  totalLateFeeAmount?: number | string | null
  totalInstallmentFeeAmount?: number | string | null
  totalProfitAmount?: number | string | null
}

interface ProfitStats {
  overview: ProfitOverview
  rows: any[]
  cardStats: any[]
  monthStats: any[]
}

const authStore = useAuthStore()
const activeTab = ref<TabName>('cards')
const selectedUserId = ref<number>()
const users = ref<UserNode[]>([])
const config = reactive<SpecialConfig>({})
const cards = ref<SpecialCard[]>([])
const billRows = ref<SpecialBill[]>([])
const profitStats = reactive<ProfitStats>({
  overview: {},
  rows: [],
  cardStats: [],
  monthStats: []
})

const savingConfig = ref(false)
const cardLoading = ref(false)
const savingCard = ref(false)
const billLoading = ref(false)
const profitLoading = ref(false)
const savingBillId = ref<number>()
const savingProfitBillId = ref<number>()
const deletingHistoryBills = ref(false)
const cardDialogVisible = ref(false)
const cardFormRef = ref<FormInstance>()

const yearOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
const deleteBeforeYearOptions = [2021, 2022, 2023, 2024, 2025, 2026, 2027]
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)

const billQuery = reactive({
  current: 1,
  size: 12,
  year: 2026 as number | undefined,
  cardId: undefined as number | undefined
})
const billTotal = ref(0)
const deleteBeforeYear = ref(2023)

const profitQuery = reactive({
  year: undefined as number | undefined,
  month: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const profitPage = reactive({
  current: 1,
  size: 12
})
const profitPageSizeOptions = [12, 24, 50, 100]

const cardForm = reactive({
  id: undefined as number | undefined,
  bankName: '',
  cardNoLast4: '',
  totalAmount: 0,
  status: 0,
  remark: ''
})

const cardRules: FormRules = {
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  cardNoLast4: [
    { required: true, message: '请输入卡号后四位', trigger: 'blur' },
    { pattern: /^\d{4}$/, message: '卡号后四位必须是4位数字', trigger: 'blur' }
  ]
}

const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(authStore.role))
const isAdmin = computed(() => authStore.role === 'ADMIN')
const userOptions = computed<SelectOption[]>(() => flattenUsers(users.value))
const cardDialogTitle = computed(() => (cardForm.id ? '编辑特殊银行卡' : '新增特殊银行卡'))
const totalCardAmount = computed(() => cards.value.reduce((sum, card) => sum + toNumber(card.totalAmount), 0))
const totalBillCount = computed(() => cards.value.reduce((sum, card) => sum + Number(card.billCount || 0), 0))
const selectedBillCard = computed(() => cards.value.find(card => Number(card.id) === Number(billQuery.cardId)))
const annualBillHint = computed(() => {
  if (!cards.value.length) return '新增银行卡后自动生成2020-2026年度账单'
  const card = selectedBillCard.value
  const year = billQuery.year || '全年'
  return card ? `${cardLabel(card)} · ${year} 年 12 个月` : '请选择银行卡'
})

const profitSummaryCards = computed(() => {
  const overview = profitStats.overview || {}
  return [
    { label: '银行卡', value: String(overview.cardCount || 0) },
    { label: '账单数', value: String(overview.billCount || 0) },
    { label: '每月账单金额', value: formatMoney(overview.totalBillAmount) },
    { label: '还款手续费', value: formatMoney(overview.totalRepaymentFee), className: 'amount-income' },
    { label: '消费手续费', value: formatMoney(overview.totalConsumeFee), className: 'amount-income' },
    { label: '利息', value: formatMoney(overview.totalInterestAmount), className: 'amount-income' },
    { label: '滞纳金', value: formatMoney(overview.totalLateFeeAmount), className: 'amount-income' },
    { label: '分期费', value: formatMoney(overview.totalInstallmentFeeAmount), className: 'amount-income' },
    { label: '总计', value: formatMoney(overview.totalProfitAmount), className: 'amount-income' }
  ]
})

const profitTotal = computed(() => profitStats.rows.length)
const pagedProfitRows = computed(() => {
  const start = (profitPage.current - 1) * profitPage.size
  return profitStats.rows.slice(start, start + profitPage.size)
})
const profitPaginationText = computed(() => {
  if (!profitTotal.value) return '暂无数据'
  return `每页 ${profitPage.size} 条，共 ${profitTotal.value} 条`
})

onMounted(async () => {
  await refreshAll()
})

watch(activeTab, async (tab) => {
  if (tab === 'bills') {
    await fetchBills()
  }
  if (tab === 'profit') {
    await fetchProfitStats()
  }
})

watch(
  () => [billQuery.year, billQuery.cardId],
  () => {
    billQuery.current = 1
    queueBillFetch()
  }
)

watch(
  () => [profitQuery.year, profitQuery.month, profitQuery.cardId],
  () => {
    profitPage.current = 1
    queueProfitFetch()
  }
)

watch(
  () => [profitTotal.value, profitPage.size],
  () => {
    const maxPage = Math.max(1, Math.ceil(profitTotal.value / profitPage.size))
    if (profitPage.current > maxPage) {
      profitPage.current = maxPage
    }
  }
)

let billFetchTimer = 0
let profitFetchTimer = 0

function queueBillFetch() {
  if (activeTab.value !== 'bills') return
  window.clearTimeout(billFetchTimer)
  billFetchTimer = window.setTimeout(() => {
    fetchBills()
  }, 120)
}

function queueProfitFetch() {
  if (activeTab.value !== 'profit') return
  window.clearTimeout(profitFetchTimer)
  profitFetchTimer = window.setTimeout(() => {
    fetchProfitStats()
  }, 120)
}

async function refreshAll() {
  await fetchUsers()
  await fetchConfig()
  await fetchCards()
  await fetchBills()
  await fetchProfitStats()
}

async function fetchUsers() {
  const res = await getUserTreeApi()
  users.value = res.data || []
}

async function fetchConfig() {
  const res = await getSpecialConfigApi()
  Object.assign(config, res.data || {})
  selectedUserId.value = config.userId
}

async function saveConfig() {
  if (!selectedUserId.value) return
  savingConfig.value = true
  try {
    const res = await saveSpecialConfigApi({ userId: selectedUserId.value })
    Object.assign(config, res.data || {})
    ElMessage.success('特殊用户配置已保存')
    await fetchCards()
    await fetchBills()
    await fetchProfitStats()
  } finally {
    savingConfig.value = false
  }
}

async function fetchCards() {
  if (!config.userId) {
    cards.value = []
    billQuery.cardId = undefined
    profitQuery.cardId = undefined
    return
  }
  cardLoading.value = true
  try {
    const res = await getSpecialCardsApi()
    cards.value = res.data || []
    normalizeSelectedCardFilters()
  } finally {
    cardLoading.value = false
  }
}

function normalizeSelectedCardFilters() {
  const cardIds = new Set(cards.value.map(card => Number(card.id)))
  if (!cards.value.length) {
    billQuery.cardId = undefined
    profitQuery.cardId = undefined
    return
  }
  if (!billQuery.cardId || !cardIds.has(Number(billQuery.cardId))) {
    billQuery.cardId = cards.value[0].id
  }
  if (profitQuery.cardId && !cardIds.has(Number(profitQuery.cardId))) {
    profitQuery.cardId = undefined
  }
}

function openCreateCard() {
  resetCardForm()
  cardDialogVisible.value = true
}

function openEditCard(card: SpecialCard) {
  cardForm.id = card.id
  cardForm.bankName = card.bankName || ''
  cardForm.cardNoLast4 = card.cardNoLast4 || ''
  cardForm.totalAmount = toNumber(card.totalAmount)
  cardForm.status = card.status ?? 0
  cardForm.remark = card.remark || ''
  cardDialogVisible.value = true
}

async function submitCard() {
  await cardFormRef.value?.validate()
  savingCard.value = true
  try {
    const payload = {
      id: cardForm.id,
      userId: config.userId,
      bankName: cardForm.bankName,
      cardNoLast4: cardForm.cardNoLast4,
      totalAmount: cardForm.totalAmount,
      status: cardForm.status,
      remark: cardForm.remark
    }
    if (cardForm.id) {
      await updateSpecialCardApi(payload)
      ElMessage.success('银行卡已更新')
    } else {
      await saveSpecialCardApi(payload)
      ElMessage.success('银行卡已新增，2020-2026账单已生成')
    }
    cardDialogVisible.value = false
    await fetchCards()
    await fetchBills()
    await fetchProfitStats()
  } finally {
    savingCard.value = false
  }
}

async function deleteCard(card: SpecialCard) {
  await ElMessageBox.confirm(
    `确认删除 ${card.bankName} 尾号 ${card.cardNoLast4}？确认后这张卡关联的所有账单信息也会一起删除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await deleteSpecialCardApi(card.id)
  ElMessage.success('银行卡已删除')
  await fetchCards()
  await fetchBills()
  await fetchProfitStats()
}

function resetCardForm() {
  cardForm.id = undefined
  cardForm.bankName = ''
  cardForm.cardNoLast4 = ''
  cardForm.totalAmount = 0
  cardForm.status = 0
  cardForm.remark = ''
}

async function fetchBills() {
  if (!config.userId || !billQuery.cardId) {
    billRows.value = []
    billTotal.value = 0
    return
  }
  billLoading.value = true
  try {
    const res = await getSpecialBillPageApi({
      current: billQuery.current,
      size: 12,
      year: billQuery.year,
      cardId: billQuery.cardId
    })
    billRows.value = res.data?.records || []
    billTotal.value = res.data?.total || 0
  } finally {
    billLoading.value = false
  }
}

async function resetBillQuery() {
  billQuery.current = 1
  billQuery.size = 12
  billQuery.year = 2026
  billQuery.cardId = cards.value[0]?.id
  await fetchBills()
}

async function deleteHistoryBills() {
  if (!billQuery.cardId) {
    ElMessage.warning('请先选择银行卡')
    return
  }
  const card = selectedBillCard.value
  const cardName = card ? cardLabel(card) : '当前银行卡'
  await ElMessageBox.confirm(
    `确认删除 ${cardName} ${deleteBeforeYear.value} 年之前的所有账单？删除后不会影响 ${deleteBeforeYear.value} 年及之后账单。`,
    '批量删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  deletingHistoryBills.value = true
  try {
    const res = await deleteSpecialBillsBeforeYearApi({
      cardId: billQuery.cardId,
      beforeYear: deleteBeforeYear.value
    })
    ElMessage.success(`已删除 ${res.data || 0} 条账单`)
    await fetchCards()
    await fetchBills()
    await fetchProfitStats()
  } finally {
    deletingHistoryBills.value = false
  }
}

async function saveBill(row: SpecialBill) {
  savingBillId.value = row.id
  try {
    await updateSpecialBillApi({
      id: row.id,
      billDay: row.billDay || undefined,
      repaymentDay: row.repaymentDay || undefined,
      billAmount: toNumber(row.billAmount),
      billAmountVerified: Boolean(row.billAmountVerified),
      xiaohuanRepayAmount: toNumber(row.xiaohuanRepayAmount),
      xiaohuanRepayVerified: Boolean(row.xiaohuanRepayVerified),
      customerRepayAmount: toNumber(row.customerRepayAmount),
      customerRepayVerified: Boolean(row.customerRepayVerified),
      xiaohuanConsumeAmount: toNumber(row.xiaohuanConsumeAmount),
      xiaohuanConsumeVerified: Boolean(row.xiaohuanConsumeVerified),
      customerNeedAmount: toNumber(row.customerNeedAmount),
      customerNeedVerified: Boolean(row.customerNeedVerified),
      customerConsumeAmount: toNumber(row.customerConsumeAmount),
      customerConsumeVerified: Boolean(row.customerConsumeVerified),
      balance: toNumber(row.balance),
      interestAmount: toNumber(row.interestAmount),
      lateFeeAmount: toNumber(row.lateFeeAmount),
      installmentFeeAmount: toNumber(row.installmentFeeAmount),
      remark: row.remark
    })
    ElMessage.success('账单已保存')
    await fetchBills()
    await fetchProfitStats()
  } finally {
    savingBillId.value = undefined
  }
}

async function fetchProfitStats() {
  if (!config.userId) {
    Object.assign(profitStats, { overview: {}, rows: [], cardStats: [], monthStats: [] })
    return
  }
  profitLoading.value = true
  try {
    const res = await getSpecialProfitStatsApi({
      year: profitQuery.year,
      month: profitQuery.month,
      cardId: profitQuery.cardId
    })
    Object.assign(profitStats, res.data || { overview: {}, rows: [], cardStats: [], monthStats: [] })
  } finally {
    profitLoading.value = false
  }
}

async function saveProfitExtras(row: any) {
  savingProfitBillId.value = row.billId
  try {
    await updateSpecialProfitExtraFeesApi({
      billId: row.billId,
      interestAmount: toNumber(row.interestAmount),
      lateFeeAmount: toNumber(row.lateFeeAmount),
      installmentFeeAmount: toNumber(row.installmentFeeAmount)
    })
    ElMessage.success('收益费用已保存')
    await fetchProfitStats()
    await fetchBills()
  } finally {
    savingProfitBillId.value = undefined
  }
}

async function resetProfitQuery() {
  profitQuery.year = undefined
  profitQuery.month = undefined
  profitQuery.cardId = undefined
  profitPage.current = 1
  await fetchProfitStats()
}

function flattenUsers(tree: UserNode[], parentName = ''): SelectOption[] {
  const result: SelectOption[] = []
  for (const user of tree || []) {
    if (user.status === 1) continue
    const label = parentName ? `${parentName} / ${user.name}` : user.name
    result.push({ label, value: Number(user.id) })
    if (user.children?.length) {
      result.push(...flattenUsers(user.children, user.name))
    }
  }
  return result
}

function cardLabel(card: SpecialCard) {
  return `${card.bankName || '-'} / 尾号 ${card.cardNoLast4 || '-'}`
}

function statusText(status?: number) {
  return status === 1 ? '停用' : '正常'
}

function calcDiff(row: SpecialBill) {
  return roundMoney(toNumber(row.xiaohuanRepayAmount) - toNumber(row.xiaohuanConsumeAmount))
}

function calcRepaymentFee(row: SpecialBill) {
  return roundMoney(toNumber(row.billAmount) * toNumber(row.feeRate) / 100)
}

function calcConsumeFee(row: SpecialBill) {
  return roundMoney((toNumber(row.xiaohuanConsumeAmount) + toNumber(row.customerConsumeAmount)) * toNumber(row.feeRate) / 100)
}

function calcProfitTotal(row: any) {
  return roundMoney(
    toNumber(row.totalAmount) +
    toNumber(row.repaymentFee) +
    toNumber(row.consumeFee) +
    toNumber(row.interestAmount) +
    toNumber(row.lateFeeAmount) +
    toNumber(row.installmentFeeAmount)
  )
}

function toNumber(value: number | string | null | undefined) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num : 0
}

function roundMoney(value: number | string | null | undefined) {
  return Number(toNumber(value).toFixed(2))
}

function formatMoney(value: number | string | null | undefined) {
  return toNumber(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function formatRate(value: number | string | null | undefined) {
  return toNumber(value).toFixed(2)
}

function billSummaryMethod({ columns, data }: { columns: any[]; data: SpecialBill[] }) {
  const sumFields = new Set([
    'billAmount',
    'xiaohuanRepayAmount',
    'customerRepayAmount',
    'xiaohuanConsumeAmount',
    'customerNeedAmount',
    'customerConsumeAmount',
    'diffAmount',
    'balance',
    'interestAmount',
    'lateFeeAmount',
    'installmentFeeAmount'
  ])
  return columns.map((column, index) => {
    if (index === 0) return '合计'
    const property = column.property
    if (!sumFields.has(property)) return ''
    const total = data.reduce((sum, row) => {
      if (property === 'diffAmount') return sum + calcDiff(row)
      return sum + toNumber((row as any)[property])
    }, 0)
    return formatMoney(total)
  })
}
</script>

<style scoped>
.special-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f5f7fb;
  box-sizing: border-box;
}

.special-header,
.card-toolbar,
.filter-line,
.bill-table-shell,
.profit-summary-grid,
.profit-table-block {
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
}

.special-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 8px 10px;
  flex-shrink: 0;
}

.header-main {
  min-width: 0;
}

.page-title {
  color: #1f2a37;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.page-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.config-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-select {
  width: 260px;
}

.special-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.special-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 8px;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  flex-shrink: 0;
}

.special-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  padding-top: 6px;
  overflow: hidden;
}

.special-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: hidden;
}

.card-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 8px 10px;
  flex-shrink: 0;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 8px;
  min-width: 0;
}

.stat-item,
.profit-summary-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  min-height: 34px;
  padding: 4px 6px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #f8fafc;
}

.stat-item span,
.profit-summary-item span {
  color: #7c8799;
  font-size: 10px;
  font-weight: 700;
}

.stat-item strong,
.profit-summary-item strong {
  margin-top: 3px;
  overflow: hidden;
  color: #1f2a37;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.special-card-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-auto-rows: minmax(150px, auto);
  align-content: start;
  gap: 10px;
  overflow-y: auto;
  padding: 2px;
}

.bank-card-tile {
  min-width: 0;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}

.bank-card-tile.disabled {
  background: #f8fafc;
}

.tile-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.bank-mark {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #0958d9;
  background: #eaf2ff;
}

.tile-title {
  min-width: 0;
}

.tile-title strong,
.tile-title span,
.tile-remark {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tile-title strong {
  color: #1f2a37;
  font-size: 14px;
  font-weight: 800;
}

.tile-title span,
.tile-meta,
.tile-remark {
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.tile-amount {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  background: #f5f8fc;
}

.tile-amount span {
  color: #7c8799;
  font-size: 12px;
  font-weight: 700;
}

.tile-amount strong {
  min-width: 0;
  overflow: hidden;
  color: #1f2a37;
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tile-meta,
.tile-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tile-actions {
  margin-top: auto;
  justify-content: flex-end;
}

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
  padding: 5px 8px;
  flex-shrink: 0;
}

.filter-item {
  width: 112px;
}

.filter-card {
  width: 190px;
}

.filter-hint {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.bill-table-shell {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
  padding: 4px;
}

.bill-table-shell :deep(.el-table) {
  width: 100% !important;
  font-size: 11px;
}

.bill-table-shell :deep(.el-table th.el-table__cell) {
  height: 26px;
  padding: 1px 0;
}

.bill-table-shell :deep(.el-table td.el-table__cell) {
  height: 28px;
  padding: 1px 0;
}

.bill-table-shell :deep(.el-table__row) {
  height: 28px;
}

.bill-table-shell :deep(.el-table .cell),
.profit-table-block :deep(.el-table .cell) {
  min-width: 0;
  padding: 0 3px;
  line-height: 1.15;
  white-space: normal;
  word-break: break-word;
}

.money-input {
  width: 100%;
  min-width: 0;
}

.day-input {
  width: 100%;
  min-width: 0;
}

.amount-verify-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px;
  align-items: center;
  gap: 2px;
}

.bill-table-shell .money-input :deep(.el-input__wrapper),
.bill-table-shell .day-input :deep(.el-input__wrapper),
.bill-table-shell :deep(.el-input__wrapper) {
  min-height: 22px;
  padding: 0 3px;
}

.bill-table-shell :deep(.el-switch) {
  --el-switch-on-color: #2f9e44;
  height: 18px;
  min-width: 28px;
  width: 28px;
}

.bill-table-shell :deep(.el-switch__core) {
  min-width: 28px;
  width: 28px;
  height: 16px;
}

.bill-table-shell :deep(.el-switch__core .el-switch__action) {
  width: 12px;
  height: 12px;
}

.money-input :deep(.el-input__inner) {
  text-align: right;
  font-family: var(--font-mono);
  font-size: 11px;
}

.money-text,
.strong-cell {
  font-family: var(--font-mono);
  font-weight: 800;
}

.strong-cell {
  color: #1f2a37;
}

.profit-summary-grid {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 4px;
  padding: 4px;
  flex-shrink: 0;
}

.profit-tables {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  overflow: hidden;
}

.profit-table-block {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 4px;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  color: #1f2a37;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

.profit-table-block :deep(.el-table) {
  flex: 1;
  width: 100% !important;
  font-size: 11px;
}

.profit-table-block :deep(.el-table th.el-table__cell) {
  height: 24px;
  padding: 1px 0;
}

.profit-table-block :deep(.el-table td.el-table__cell) {
  height: 28px;
  padding: 1px 0;
}

.profit-pagination {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
  flex-shrink: 0;
}

.profit-pagination :deep(.el-pagination) {
  --el-pagination-button-height: 20px;
  --el-pagination-button-width: 20px;
  --el-pagination-font-size: 11px;
}

.amount-income {
  color: #2f9e44 !important;
}

.amount-cost {
  color: #cf1322 !important;
}

.full-input {
  width: 100%;
}

@media (max-width: 1360px) {
  .special-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profit-tables {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .special-header {
    align-items: stretch;
    flex-direction: column;
  }

  .config-bar,
  .card-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .user-select,
  .filter-card {
    width: 100%;
  }

  .stat-strip,
  .profit-summary-grid,
  .special-card-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
