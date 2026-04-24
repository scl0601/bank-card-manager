<template>
  <div>
    <div class="page-title">账单管理</div>

    <el-alert
      v-if="detailModeMessage"
      class="card-filter-alert"
      :title="detailModeMessage"
      type="info"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button type="primary" link @click="clearRouteFilters">查看全部账单</el-button>
      </template>
    </el-alert>

    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="handleResetAll">
      <el-form-item label="持卡人">
        <el-select v-model="query.ownerId" placeholder="全部" clearable filterable style="width: 160px" :loading="ownerLoading" @focus="fetchOwners">
          <el-option v-for="u in ownerOptions" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="银行卡">
        <el-input v-model="query.cardName" placeholder="银行名称/卡号后四位" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="年份">
        <el-select v-model="query.year" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
        </el-select>
      </el-form-item>
      <el-form-item label="账单月份">
        <el-date-picker v-model="query.billMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <ExportButton :loading="exporting" @click="handleExport" />
      </template>
    </SearchBar>

    <div class="summary-grid" v-loading="overviewLoading">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <div class="summary-icon" :style="{ background: item.iconBg }">
          <el-icon :size="20" :color="item.iconColor"><component :is="item.icon" /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-label">{{ item.label }}</div>
          <div class="summary-value" :class="item.className">{{ item.value }}</div>
          <div class="summary-sub">{{ item.sub }}</div>
        </div>
      </div>
    </div>

    <PageTable
      :data="list"
      :loading="loading"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      border
      row-key="id"
      @update:page-num="val => query.pageNum = val"
      @update:page-size="val => query.pageSize = val"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      @expand-change="onExpandChange"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-bill-content">
            <div class="quick-edit-bar">
              <div class="qe-item">
                <label>本月代还金额</label>
                <el-input-number
                  :model-value="editFormMap[row.id]?.billAmount"
                  :min="0"
                  :precision="2"
                  size="small"
                  controls-position="right"
                  style="width: 150px"
                  @update:model-value="(val:any) => updateEditField(row.id, 'billAmount', val)"
                />
              </div>
              <div class="qe-item">
                <label>账单日</label>
                <el-input-number
                  :model-value="editFormMap[row.id]?.billDay"
                  :min="1"
                  :max="31"
                  size="small"
                  controls-position="right"
                  style="width: 120px"
                  @update:model-value="(val:any) => updateEditField(row.id, 'billDay', val)"
                />
              </div>
              <div class="qe-item">
                <label>POS成本</label>
                <el-input-number
                  :model-value="editFormMap[row.id]?.posCostAmount"
                  :min="0"
                  :precision="2"
                  size="small"
                  controls-position="right"
                  style="width: 150px"
                  @update:model-value="(val:any) => updateEditField(row.id, 'posCostAmount', val)"
                />
              </div>
              <div class="qe-item qe-readonly">
                <label>手续费率</label>
                <span>{{ formatRate(row.feeRate) }}%</span>
              </div>
              <div class="qe-item qe-readonly">
                <label>手续费收入</label>
                <span class="amt-pos">¥{{ formatMoney(editFormMap[row.id]?.feeAmount) }}</span>
              </div>
              <div class="qe-item qe-readonly">
                <label>净利润</label>
                <span :class="Number(editFormMap[row.id]?.netProfit ?? 0) >= 0 ? 'amt-pos' : 'amt-neg'">
                  ¥{{ formatMoney(editFormMap[row.id]?.netProfit) }}
                </span>
              </div>
              <div class="qe-item">
                <label>还款日</label>
                <el-input-number
                  :model-value="editFormMap[row.id]?.repayDay"
                  :min="1"
                  :max="31"
                  size="small"
                  controls-position="right"
                  style="width: 120px"
                  @update:model-value="(val:any) => updateEditField(row.id, 'repayDay', val)"
                />
              </div>
              <el-button type="primary" size="small" :loading="savingId === row.id" @click="handleInlineSave(row)">保存</el-button>
            </div>

            <div class="detail-section">
              <div class="detail-header">
                <span class="detail-title">本月明细流水</span>
                <el-button type="success" size="small" @click="openAddDetail(row)">+ 新增</el-button>
              </div>

              <BillDetailSkeleton v-if="detailLoadingMap[row.id]" />

              <template v-else-if="detailListMap[row.id]?.length">
                <div v-if="selectedDetailsMap[row.id]?.length > 0" class="batch-toolbar">
                  <el-button type="danger" size="small" @click="handleBatchDelete(row.id)">
                    批量删除 ({{ selectedDetailsMap[row.id].length }})
                  </el-button>
                  <el-dropdown @command="(type: number) => handleBatchUpdateType(row.id, type)">
                    <el-button size="small">
                      批量修改类型 <el-icon><arrow-down /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="0">刷出</el-dropdown-item>
                        <el-dropdown-item :command="1">刷入</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

                <el-table
                  :data="detailListMap[row.id]"
                  size="small"
                  border
                  stripe
                  @selection-change="(val: any) => handleDetailSelection(row.id, val)"
                >
                  <el-table-column type="selection" width="55" />
                  <el-table-column prop="detailDate" label="日期" width="115" />
                  <el-table-column prop="description" label="描述/备注" min-width="220" show-overflow-tooltip />
                  <el-table-column label="类型" width="110" align="center">
                    <template #default="{ row: detail }">
                      <el-tag :type="DETAIL_TYPE_TAG_TYPE[detail.detailType] ?? ''" size="small">{{ DETAIL_TYPE_MAP[detail.detailType] ?? '未知' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="金额" width="120" align="right">
                    <template #default="{ row: detail }">
                      <span :class="detail.detailType === 1 ? 'amt-pos' : 'amt-neg'" class="font-mono">
                        {{ detail.detailType === 1 ? '+' : '-' }}{{ formatMoney(detail.amount) }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="90" fixed="right" align="center">
                    <template #default="{ row: detail }">
                      <el-button type="primary" link size="small" @click="openEditDetail(row, detail)">编辑</el-button>
                      <el-popconfirm title="确认删除？" @confirm="handleDeleteDetail(detail.id)">
                        <template #reference><el-button type="danger" link size="small">删</el-button></template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                </el-table>
              </template>

              <el-empty v-else description="暂无明细" :image-size="50" />
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="持卡人" min-width="140" fixed="left">
        <template #default="{ row }">
          <div class="owner-cell">
            <div class="owner-avatar">
              <el-icon :size="16"><UserFilled /></el-icon>
            </div>
            <div class="owner-info">
              <span class="owner-name">{{ row.ownerName }}</span>
              <el-tag size="small" effect="plain" type="info">{{ row.ownerRelation || '本人' }}</el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="bankName" label="银行" width="120">
        <template #default="{ row }">
          <div class="bank-cell">
            <el-icon :size="14" color="#67c23a"><CreditCard /></el-icon>
            <span>{{ row.bankName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="卡号后四位" width="110" align="center">
        <template #default="{ row }">
          <span class="card-no-badge">{{ row.cardNoLast4 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="billMonth" label="账单月份" width="110" align="center">
        <template #default="{ row }">
          <el-tag type="primary" size="small" effect="light">{{ row.billMonth }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账期" width="110" align="center">
        <template #default="{ row }">
          <span class="period-text">{{ billPeriodLabel(row.billMonth, row.billDay) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="还款日" width="105" align="center">
        <template #default="{ row }">
          <span class="repay-date">{{ row.repayDate ? fmtRepayDate(row.repayDate) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="代还金额" width="130" align="right">
        <template #default="{ row }">
          <div class="amount-cell">
            <span class="amount-value">{{ formatMoney(row.billAmount) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="手续费率" width="100" align="center">
        <template #default="{ row }">
          <span class="fee-rate-badge">{{ formatRate(row.feeRate) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费收入" width="130" align="right">
        <template #default="{ row }">
          <span class="amt-pos amount-value">{{ formatMoney(row.feeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="POS成本" width="120" align="right">
        <template #default="{ row }">
          <span class="amt-neg amount-value">{{ formatMoney(row.posCostAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="净利润" width="130" align="right">
        <template #default="{ row }">
          <span class="amount-value" :class="Number(row.netProfit || 0) >= 0 ? 'amt-pos' : 'amt-neg'">{{ formatMoney(row.netProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="还款方式" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.repayMethod === 'cloudpay'" type="primary" size="small" effect="light">云闪付</el-tag>
          <el-tag v-else-if="row.repayMethod === 'invoice'" type="warning" size="small" effect="light">开票</el-tag>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="BILL_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该月账单？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">
                <el-icon :size="13"><Delete /></el-icon>删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </PageTable>

    <el-dialog v-model="detailDialogVisible" :title="detailDialogTitle" width="500px" destroy-on-close>
      <el-form :model="detailForm" label-width="90px" :rules="detailRules" ref="detailFormRef">
        <el-form-item label="日期" prop="detailDate">
          <el-date-picker v-model="detailForm.detailDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="detailForm.description" placeholder="如：帮客户代还信用卡" />
        </el-form-item>
        <el-form-item label="类型" prop="detailType">
          <el-radio-group v-model="detailForm.detailType">
            <el-radio v-for="t in DETAIL_TYPE_OPTIONS" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="detailForm.amount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="detailForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="detailSaving" @click="handleSaveDetail">保存</el-button>
      </template>
    </el-dialog>

    <!-- 快捷键提示 -->
    <div class="keyboard-shortcuts-hint">
      <el-popover placement="top" :width="300" trigger="hover">
        <template #reference>
          <el-button circle size="small">
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </template>
        <div>
          <h4 style="margin: 0 0 10px 0;">快捷键</h4>
          <ul style="padding-left: 20px; margin: 10px 0;">
            <li><kbd>Ctrl+S</kbd> 保存当前账单</li>
            <li><kbd>Ctrl+N</kbd> 新增明细</li>
            <li><kbd>ESC</kbd> 关闭对话框</li>
          </ul>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Bills' })
import { computed, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuestionFilled, ArrowDown, UserFilled, CreditCard, Money, Delete, Document, Wallet, TrendCharts } from '@element-plus/icons-vue'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import BillDetailSkeleton from '@/components/BillDetailSkeleton.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { handleError } from '@/utils/errorHandler'
import {
  getBillPageApi,
  getBillOverviewApi,
  updateBillApi,
  deleteBillApi,
  exportBillApi,
  getDetailListApi as fetchDetailListApi,
  saveDetailApi,
  updateDetailApi,
  deleteDetailApi,
  batchDeleteDetailsApi,
  batchUpdateTypeApi
} from '@/api/bill'
import { getUserTreeApi } from '@/api/card'
import { formatTime, formatMoney, formatRate, toNumber } from '@/utils/formatters'
import {
  BILL_STATUS_OPTIONS,
  BILL_STATUS_MAP,
  BILL_STATUS_TAG_TYPE,
  DETAIL_TYPE_OPTIONS,
  DETAIL_TYPE_MAP,
  DETAIL_TYPE_TAG_TYPE
} from '@/constants/dict'

interface BillRow {
  id: number
  cardId: number
  ownerName: string
  ownerRelation: string
  bankName: string
  cardNoLast4: string
  billMonth: string
  billDay: number | null
  repayDate: string | null
  billAmount: number | null
  feeRate: number | null
  feeAmount: number | null
  posCostAmount: number | null
  netProfit: number | null
  status: number
  remark?: string
  repayMethod?: string
}

interface EditFormItem {
  id: number
  billAmount: number
  billDay: number | null
  posCostAmount: number
  repayDay: number | null
  feeAmount: string
  netProfit: string
}

interface BillOverview {
  year?: number
  billCount: number
  pendingCount: number
  repaidCount: number
  partialCount: number
  overdueCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalNetProfit: number
}

const route = useRoute()
const router = useRouter()
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, index) => currentYear - 2 + index)

const {
  loading,
  list,
  total,
  query,
  handleSearch,
  resetQuery,
  handleCurrentChange,
  handleSizeChange,
  refreshFirstPage
} = usePageTable({
  fetchApi: getBillPageApi,
  defaultQuery: { cardId: undefined as any, ownerId: undefined as any, cardName: '', year: undefined as any, billMonth: '', status: undefined as any },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  },
  afterFetch: () => {
    fetchBillOverview()
  }
})

const ownerLoading = ref(false)
const ownerOptions = ref<any[]>([])
const overviewLoading = ref(false)
const billOverview = ref<BillOverview>({
  year: undefined,
  billCount: 0,
  pendingCount: 0,
  repaidCount: 0,
  partialCount: 0,
  overdueCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalNetProfit: 0
})

const detailModeMessage = computed(() => {
  if (route.query.cardId) {
    return query.billMonth
      ? `当前为单张银行卡账单明细模式，已定位到 ${query.billMonth}。`
      : `当前为单张银行卡年度账单模式${query.year ? `，已定位 ${query.year} 年。` : '。'}`
  }
  if (route.query.ownerId) {
    return query.billMonth
      ? `当前为单个持卡人账单明细模式，已定位到 ${query.billMonth}。`
      : `当前为单个持卡人年度账单模式${query.year ? `，已定位 ${query.year} 年。` : '。'}`
  }
  return ''
})

const summaryCards = computed(() => {
  const scopeText = query.billMonth ? `${query.billMonth} 账单汇总` : (query.year ? `${query.year} 年账单汇总` : '当前筛选结果汇总')
  return [
    {
      label: '账单笔数',
      value: String(billOverview.value.billCount),
      sub: `待还 ${billOverview.value.pendingCount} / 部分 ${billOverview.value.partialCount} / 已还 ${billOverview.value.repaidCount} / 逾期 ${billOverview.value.overdueCount}`,
      className: '',
      icon: 'Document',
      iconBg: '#ecf5ff',
      iconColor: '#409eff'
    },
    {
      label: '代还总金额',
      value: `¥ ${formatMoney(billOverview.value.totalBillAmount)}`,
      sub: scopeText,
      className: '',
      icon: 'Wallet',
      iconBg: '#f0f9ff',
      iconColor: '#0ea5e9'
    },
    {
      label: '手续费收入',
      value: `¥ ${formatMoney(billOverview.value.totalFeeAmount)}`,
      sub: '按账单费率自动汇总',
      className: 'amt-pos',
      icon: 'Money',
      iconBg: '#f0f9eb',
      iconColor: '#67c23a'
    },
    {
      label: 'POS总成本',
      value: `¥ ${formatMoney(billOverview.value.totalPosCostAmount)}`,
      sub: '已纳入利润计算',
      className: 'amt-neg',
      icon: 'CreditCard',
      iconBg: '#fef0f0',
      iconColor: '#f56c6c'
    },
    {
      label: '净利润',
      value: `¥ ${formatMoney(billOverview.value.totalNetProfit)}`,
      sub: '手续费收入 - POS成本',
      className: billOverview.value.totalNetProfit >= 0 ? 'amt-pos' : 'amt-neg',
      icon: 'TrendCharts',
      iconBg: billOverview.value.totalNetProfit >= 0 ? '#f0f9eb' : '#fef0f0',
      iconColor: billOverview.value.totalNetProfit >= 0 ? '#67c23a' : '#f56c6c'
    }
  ]
})

async function fetchOwners() {
  if (ownerOptions.value.length) return
  ownerLoading.value = true
  try {
    const res: any = await getUserTreeApi()
    ownerOptions.value = flattenUsers(res.data || [])
  } finally {
    ownerLoading.value = false
  }
}

function flattenUsers(list: any[]): any[] {
  return list.reduce((acc: any[], item: any) => {
    acc.push(item)
    if (item.children?.length) {
      acc.push(...flattenUsers(item.children))
    }
    return acc
  }, [])
}

const { exporting, handleExport: doExport } = useExport({
  exportApi: exportBillApi,
  fileName: '账单记录'
})

function handleExport() {
  return doExport(buildExportParams())
}

function buildExportParams() {
  return {
    cardId: query.cardId,
    ownerId: query.ownerId,
    cardName: query.cardName,
    year: query.year,
    billMonth: query.billMonth,
    status: query.status
  }
}

async function fetchBillOverview() {
  overviewLoading.value = true
  try {
    const res: any = await getBillOverviewApi(buildExportParams())
    billOverview.value = {
      year: res.data?.year != null ? Number(res.data.year) : undefined,
      billCount: Number(res.data?.billCount ?? 0),
      pendingCount: Number(res.data?.pendingCount ?? 0),
      repaidCount: Number(res.data?.repaidCount ?? 0),
      partialCount: Number(res.data?.partialCount ?? 0),
      overdueCount: Number(res.data?.overdueCount ?? 0),
      totalBillAmount: Number(res.data?.totalBillAmount ?? 0),
      totalFeeAmount: Number(res.data?.totalFeeAmount ?? 0),
      totalPosCostAmount: Number(res.data?.totalPosCostAmount ?? 0),
      totalNetProfit: Number(res.data?.totalNetProfit ?? 0)
    }
  } finally {
    overviewLoading.value = false
  }
}

function toRouteNumber(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  const num = Number(target)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

function toRouteBillStatus(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  const num = Number(target)
  return [0, 1, 2, 3].includes(num) ? num : undefined
}

function toRouteYear(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  const num = Number(target)
  return Number.isFinite(num) && num >= 2000 ? num : undefined
}

function toRouteBillMonth(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  return typeof target === 'string' && /^\d{4}-\d{2}$/.test(target) ? target : ''
}

function clearRouteFilters() {
  const nextQuery = { ...route.query }
  delete nextQuery.cardId
  delete nextQuery.ownerId
  delete nextQuery.status
  delete nextQuery.year
  delete nextQuery.billMonth
  router.replace({ path: route.path, query: nextQuery })
}

async function handleResetAll() {
  if (route.query.cardId || route.query.ownerId || route.query.status || route.query.year || route.query.billMonth) {
    clearRouteFilters()
  }
  await resetQuery()
}

const editFormMap = ref<Record<number, EditFormItem>>({})
const savingId = ref<number | null>(null)

function ensureEditForm(row: BillRow) {
  if (!editFormMap.value[row.id]) {
    const form: EditFormItem = {
      id: row.id,
      billAmount: toNumber(row.billAmount),
      billDay: row.billDay ?? null,
      posCostAmount: toNumber(row.posCostAmount),
      repayDay: parseRepayDay(row.repayDate),
      feeAmount: '0.00',
      netProfit: '0.00'
    }
    editFormMap.value[row.id] = form
    syncInlineAmounts(row.id)
  }
  return editFormMap.value[row.id]
}

function syncInlineAmounts(billId: number) {
  const form = editFormMap.value[billId]
  const row = (list.value as BillRow[]).find(item => item.id === billId)
  if (!form || !row) return
  const feeAmount = buildFeeAmount(form.billAmount, row.feeRate)
  form.feeAmount = feeAmount.toFixed(2)
  form.netProfit = buildNetProfit(form.billAmount, row.feeRate, form.posCostAmount).toFixed(2)
}

const detailListMap = ref<Record<number, any[]>>({})
const detailLoadingMap = ref<Record<number, boolean>>({})
const selectedDetailsMap = ref<Record<number, any[]>>({})

async function loadDetails(billId: number) {
  detailLoadingMap.value[billId] = true
  try {
    const res: any = await fetchDetailListApi(billId)
    detailListMap.value[billId] = res.data || []
  } catch (error) {
    detailListMap.value[billId] = []
    handleError(error, '加载明细列表')
  } finally {
    detailLoadingMap.value[billId] = false
  }
}

const currentExpandedRow = ref<BillRow | null>(null)

function onExpandChange(row: BillRow, expanded: boolean) {
  if (expanded) {
    currentExpandedRow.value = row
    ensureEditForm(row)
    loadDetails(row.id)
  } else {
    if (currentExpandedRow.value?.id === row.id) {
      currentExpandedRow.value = null
    }
  }
}

const detailDialogVisible = ref(false)
const detailDialogTitle = ref('新增明细')
const detailSaving = ref(false)
const detailFormRef = ref<any>()
const currentBillRow = ref<BillRow | null>(null)
const detailForm = reactive({
  id: undefined as number | undefined,
  billId: 0,
  detailDate: '',
  description: '',
  amount: 0,
  detailType: 0,
  remark: ''
})
const detailRules = {
  detailDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  detailType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function openAddDetail(row: BillRow) {
  currentBillRow.value = row
  Object.assign(detailForm, { id: undefined, billId: row.id, detailDate: '', description: '', amount: 0, detailType: 0, remark: '' })
  detailDialogTitle.value = '新增明细'
  detailDialogVisible.value = true
}

function openEditDetail(billRow: BillRow, detail: any) {
  currentBillRow.value = billRow
  Object.assign(detailForm, detail)
  detailDialogTitle.value = '编辑明细'
  detailDialogVisible.value = true
}

async function handleSaveDetail() {
  try {
    await detailFormRef.value?.validate()
  } catch {
    return
  }
  detailSaving.value = true
  try {
    if (detailForm.id) {
      await updateDetailApi(detailForm)
    } else {
      await saveDetailApi(detailForm)
    }
    ElMessage.success('操作成功')
    detailDialogVisible.value = false
    if (currentBillRow.value) {
      await loadDetails(currentBillRow.value.id)
      handleSearch()
    }
  } catch (error) {
    handleError(error, '保存明细')
  } finally {
    detailSaving.value = false
  }
}

async function handleDeleteDetail(id: number) {
  try {
    await deleteDetailApi(id)
    ElMessage.success('删除成功')
    if (currentBillRow.value) {
      await loadDetails(currentBillRow.value.id)
    }
    handleSearch()
  } catch (error) {
    handleError(error, '删除明细')
  }
}

function handleDetailSelection(billId: number, selection: any[]) {
  selectedDetailsMap.value[billId] = selection
}

async function handleBatchDelete(billId: number) {
  const ids = selectedDetailsMap.value[billId]?.map(d => d.id) || []
  if (ids.length === 0) return

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条明细？`, '批量删除', {
      type: 'warning'
    })
    await batchDeleteDetailsApi(ids)
    ElMessage.success('批量删除成功')
    selectedDetailsMap.value[billId] = []
    await loadDetails(billId)
    handleSearch()
  } catch (error: any) {
    if (error !== 'cancel') {
      handleError(error, '批量删除明细')
    }
  }
}

async function handleBatchUpdateType(billId: number, detailType: number) {
  const ids = selectedDetailsMap.value[billId]?.map(d => d.id) || []
  if (ids.length === 0) return

  try {
    await batchUpdateTypeApi({ ids, detailType })
    ElMessage.success('批量修改成功')
    selectedDetailsMap.value[billId] = []
    await loadDetails(billId)
    handleSearch()
  } catch (error) {
    handleError(error, '批量修改类型')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteBillApi(id)
    ElMessage.success('删除成功')
    handleSearch()
  } catch (error) {
    handleError(error, '删除账单')
  }
}

function updateEditField(billId: number, field: string, value: any) {
  if (!editFormMap.value[billId]) return
  const form = editFormMap.value[billId] as any
  form[field] = value

  if (field === 'billAmount' || field === 'posCostAmount') {
    syncInlineAmounts(billId)
  }
}

async function handleInlineSave(row: BillRow) {
  const form = editFormMap.value[row.id]
  if (!form) return
  savingId.value = row.id
  try {
    const rowData = row as any
    await updateBillApi({
      id: row.id,
      cardId: row.cardId,
      ownerId: rowData.ownerId,
      billMonth: row.billMonth,
      billDay: form.billDay,
      repayDay: form.repayDay,
      billAmount: toNumber(form.billAmount),
      minPayAmount: toNumber(rowData.minPayAmount),
      actualPayAmount: toNumber(rowData.actualPayAmount),
      actualPayDate: rowData.actualPayDate,
      feeRate: row.feeRate,
      posCostAmount: toNumber(form.posCostAmount),
      status: row.status,
      remark: row.remark || ''
    })

    ElMessage.success(`保存成功，净利润 ¥${form.netProfit}`)
    handleSearch()
  } catch (error) {
    handleError(error, '保存账单')
  } finally {
    savingId.value = null
  }
}

// 快捷键支持
function handleKeydown(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault()
    if (currentExpandedRow.value) {
      handleInlineSave(currentExpandedRow.value)
    }
  }

  if (e.key === 'Escape') {
    detailDialogVisible.value = false
  }

  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    if (currentExpandedRow.value) {
      openAddDetail(currentExpandedRow.value)
    }
  }
}

onMounted(() => {
  fetchOwners()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

function buildFeeAmount(amount: number, feeRate: number | null | undefined) {
  return Number(((amount * toNumber(feeRate)) / 100).toFixed(2))
}

function buildNetProfit(amount: number, feeRate: number | null | undefined, posCostAmount: number) {
  return Number((buildFeeAmount(amount, feeRate) - posCostAmount).toFixed(2))
}

function parseRepayDay(repayDate: string | null | undefined) {
  if (!repayDate) return null
  const parts = String(repayDate).split('-')
  return parts[2] ? Number(parts[2]) : null
}

function fmtRepayDate(date: string) {
  const match = String(date).match(/(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${Number(match[2])}月${Number(match[3])}日` : date
}

function billPeriodLabel(billMonth: string | null | undefined, billDay: number | null | undefined) {
  if (!billMonth || !billDay) return '-'
  const month = Number(String(billMonth).split('-')[1] || 0)
  return `${month}月${billDay}日`
}

watch(
  () => [route.query.cardId, route.query.ownerId, route.query.status, route.query.year, route.query.billMonth],
  ([cardId, ownerId, status, year, billMonth]) => {
    query.cardId = toRouteNumber(cardId) as any
    query.ownerId = toRouteNumber(ownerId) as any
    query.status = toRouteBillStatus(status) as any
    query.year = toRouteYear(year) as any
    query.billMonth = toRouteBillMonth(billMonth)
    refreshFirstPage()
  },
  { immediate: true }
)
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px var(--spacing-lg);
  background: var(--color-card);
  border-radius: var(--border-radius-lg);
  border: 1px solid transparent;
  box-shadow: var(--shadow-card);
  transition: all 0.3s;
}

.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--el-color-primary-light-7);
}

.summary-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-content {
  flex: 1;
  min-width: 0;
}

.summary-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-placeholder);
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}

.summary-sub {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
}

.card-filter-alert {
  margin-bottom: var(--spacing-lg);
}

:deep(.el-table) {
  --el-table-border-color: var(--color-border-light);
  font-size: var(--font-size-base);
}

:deep(.el-table__row) {
  transition: background-color 0.2s ease;
}

:deep(.el-table .cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}

:deep(.el-table th.el-table__cell) {
  background-color: #fafafa;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-regular);
  font-size: var(--font-size-sm);
}

/* 持卡人单元格 */
.owner-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.owner-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--border-radius-lg);
  background: linear-gradient(135deg, #e6f7ff 0%, #bae0ff 100%);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
  transition: all 0.3s;
}

.owner-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}

.owner-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.owner-name {
  font-weight: var(--font-weight-semibold);
  font-size: 14px;
  color: var(--color-text-primary);
}

/* 银行单元格 */
.bank-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: var(--font-weight-medium);
}

/* 卡号徽章 */
.card-no-badge {
  font-weight: var(--font-weight-bold);
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  padding: 5px 12px;
  border-radius: var(--border-radius);
  border: 1px solid #bae0ff;
  letter-spacing: 0.5px;
}

/* 账期和还款日 */
.period-text,
.repay-date {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

/* 金额单元格 */
.amount-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.amount-value {
  font-weight: var(--font-weight-semibold);
  font-size: 14px;
  font-family: var(--font-mono);
  letter-spacing: 0.02em;
}

/* 费率徽章 */
.fee-rate-badge {
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  color: #fa8c16;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbf0 100%);
  padding: 5px 12px;
  border-radius: var(--border-radius);
  border: 1px solid #ffe7ba;
  font-family: var(--font-mono);
}

.no-data {
  color: #d9d9d9;
  font-size: 14px;
}

/* 操作单元格 */
.action-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.expand-bill-content {
  padding: var(--spacing-xl) var(--spacing-2xl);
  background: #fafafa;
  border-radius: var(--border-radius);
}

.quick-edit-bar {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-lg) var(--spacing-xl);
  background: var(--color-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--color-border-light);
  box-shadow: var(--shadow-sm);
}

.qe-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.qe-item > label {
  font-size: var(--font-size-xs);
  color: var(--color-text-placeholder);
}

.qe-readonly {
  min-width: 100px;
}

.qe-readonly > span {
  font-size: var(--font-size-lg);
  font-weight: 700;
}

.detail-section {
  background: var(--color-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: #fafafa;
  border-bottom: 1px solid var(--color-border-light);
}

.detail-title {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.batch-toolbar {
  display: flex;
  gap: 10px;
  padding: 12px var(--spacing-lg);
  background: #f5f5f5;
  border-bottom: 1px solid var(--color-border-light);
}

.keyboard-shortcuts-hint {
  position: fixed;
  bottom: var(--spacing-xl);
  right: var(--spacing-xl);
  z-index: 1000;
}

.keyboard-shortcuts-hint kbd {
  display: inline-block;
  padding: 2px 6px;
  font-family: monospace;
  font-size: var(--font-size-xs);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius-sm);
  box-shadow: var(--shadow-sm);
}
</style>
