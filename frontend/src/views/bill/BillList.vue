<template>
  <div class="bill-page">
    <div class="page-header">
      <div class="header-copy">
        <div class="header-title-row">
          <div class="header-title">账单管理</div>
          <span v-if="detailModeMessage" class="detail-mode-chip">明细模式</span>
        </div>
        <div class="header-subtitle">{{ currentScopeLabel }} · 极致一屏压缩布局</div>
      </div>

      <div class="header-stat-row" v-loading="overviewLoading">
        <div v-for="item in summaryCards" :key="item.label" class="header-stat">
          <div class="header-stat-icon" :style="{ background: item.iconBg, color: item.iconColor }">
            <el-icon :size="15"><component :is="item.icon" /></el-icon>
          </div>
          <div class="header-stat-body">
            <span class="header-stat-value" :class="item.className">{{ item.value }}</span>
            <span class="header-stat-label">{{ item.label }}</span>
          </div>
        </div>
      </div>

      <div class="header-actions">
        <el-button class="action-btn" :icon="RefreshRight" @click="refresh">刷新</el-button>
        <ExportButton class="export-btn" :loading="exporting" @click="handleExport" />
      </div>
    </div>

    <div class="filter-panel card-shell">
      <div class="filter-main">
        <div class="filter-title">筛选</div>
        <el-select v-model="query.ownerId" class="filter-item filter-owner" placeholder="持卡人" clearable filterable :loading="ownerLoading" @focus="fetchOwners">
          <el-option v-for="u in ownerOptions" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
        <el-input v-model="query.cardName" class="filter-item filter-card" placeholder="银行/尾号" clearable />
        <el-select v-model="query.year" class="filter-item" placeholder="年份" clearable>
          <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
        </el-select>
        <el-date-picker v-model="query.billMonth" class="filter-item filter-month" type="month" value-format="YYYY-MM" placeholder="账单月份" clearable />
        <el-select v-model="query.status" class="filter-item" placeholder="状态" clearable>
          <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button class="action-btn" @click="handleResetAll">重置</el-button>
      </div>

      <div class="filter-extra">
        <span class="toolbar-hint">共 {{ total }} 条</span>
        <span v-if="detailModeMessage" class="toolbar-tip">{{ detailModeMessage }}</span>
      </div>
    </div>

    <div class="workspace-grid">
      <aside class="side-panel card-shell">
        <div class="panel-head">
          <div>
            <div class="panel-title">状态分区</div>
            <div class="panel-desc">按账单状态快速切换</div>
          </div>
        </div>

        <div class="menu-list">
          <button
            v-for="item in quickMenus"
            :key="item.key"
            type="button"
            class="menu-item"
            :class="{ active: activeQuickMenu === item.key }"
            @click="applyQuickMenu(item.value)"
          >
            <span class="menu-dot" :style="{ background: item.color }" />
            <span class="menu-label">{{ item.label }}</span>
            <span class="menu-count">{{ item.count }}</span>
          </button>
        </div>
      </aside>

      <section class="data-panel card-shell">
        <div class="panel-head data-head">
          <div>
            <div class="panel-title">账单数据区</div>
            <div class="panel-desc">{{ currentScopeLabel }} · 展开后可维护账单明细</div>
          </div>
          <div class="inline-summary">
            <span>账单 {{ billOverview.billCount }}</span>
            <span>待还 {{ billOverview.pendingCount }}</span>
            <span>利润 {{ formatMoney(billOverview.totalNetProfit) }}</span>
          </div>
        </div>

        <PageTable
          class="bill-page-table"
          :data="list"
          :loading="loading"
          :total="total"
          :page-num="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          height="calc(100% - 42px)"
          pagination-layout="total, prev, pager, next"
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

      <el-table-column label="持卡人" min-width="124" fixed="left">
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
      <el-table-column prop="bankName" label="银行" width="98">
        <template #default="{ row }">
          <div class="bank-cell">
            <el-icon :size="14" color="#67c23a"><CreditCard /></el-icon>
            <span>{{ row.bankName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="尾号" width="84" align="center">
        <template #default="{ row }">
          <span class="card-no-badge">{{ row.cardNoLast4 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="billMonth" label="月份" width="92" align="center">
        <template #default="{ row }">
          <el-tag type="primary" size="small" effect="light">{{ row.billMonth }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账期" width="86" align="center">
        <template #default="{ row }">
          <span class="period-text">{{ billPeriodLabel(row.billMonth, row.billDay) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="还款日" width="84" align="center">
        <template #default="{ row }">
          <span class="repay-date">{{ row.repayDate ? fmtRepayDate(row.repayDate) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="代还" width="112" align="right">
        <template #default="{ row }">
          <div class="amount-cell">
            <span class="amount-value">{{ formatMoney(row.billAmount) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="费率" width="82" align="center">
        <template #default="{ row }">
          <span class="fee-rate-badge">{{ formatRate(row.feeRate) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费" width="112" align="right">
        <template #default="{ row }">
          <span class="amt-pos amount-value">{{ formatMoney(row.feeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="POS" width="102" align="right">
        <template #default="{ row }">
          <span class="amt-neg amount-value">{{ formatMoney(row.posCostAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="利润" width="112" align="right">
        <template #default="{ row }">
          <span class="amount-value" :class="Number(row.netProfit || 0) >= 0 ? 'amt-pos' : 'amt-neg'">{{ formatMoney(row.netProfit) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="还款" width="92" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.repayMethod === 'cloudpay'" type="primary" size="small" effect="light">云闪付</el-tag>
          <el-tag v-else-if="row.repayMethod === 'invoice'" type="warning" size="small" effect="light">开票</el-tag>
          <span v-else class="no-data">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="BILL_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="72" fixed="right" align="center">
        <template #default="{ row }">
          <el-popconfirm title="确认删除该月账单？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small" class="delete-icon-btn" title="删除">
                <el-icon :size="14"><Delete /></el-icon>
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
        </PageTable>
      </section>

      <aside class="action-panel card-shell">
        <div v-if="detailModeMessage" class="side-card detail-mode-card">
          <div class="side-card-title">当前模式</div>
          <div class="detail-mode-text">{{ detailModeMessage }}</div>
          <el-button type="primary" link @click="clearRouteFilters">查看全部账单</el-button>
        </div>

        <div class="side-card">
          <div class="side-card-title">快捷操作</div>
          <div class="action-stack">
            <el-button type="primary" :icon="RefreshRight" @click="refresh">刷新</el-button>
            <el-button @click="handleResetAll">重置</el-button>
            <el-button v-if="detailModeMessage" @click="clearRouteFilters">全部</el-button>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">当前视图</div>
          <div class="summary-list">
            <div class="summary-item">
              <span>范围</span>
              <strong>{{ currentScopeLabel }}</strong>
            </div>
            <div class="summary-item">
              <span>账单</span>
              <strong>{{ billOverview.billCount }}</strong>
            </div>
            <div class="summary-item">
              <span>待还</span>
              <strong>{{ billOverview.pendingCount }}</strong>
            </div>
            <div class="summary-item">
              <span>利润</span>
              <strong :class="billOverview.totalNetProfit >= 0 ? 'amt-pos' : 'amt-neg'">{{ formatMoney(billOverview.totalNetProfit) }}</strong>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">快捷键</div>
          <div class="shortcut-list">
            <div class="shortcut-item"><span>保存</span><kbd>Ctrl+S</kbd></div>
            <div class="shortcut-item"><span>新增明细</span><kbd>Ctrl+N</kbd></div>
            <div class="shortcut-item"><span>关闭弹窗</span><kbd>ESC</kbd></div>
          </div>
        </div>
      </aside>
    </div>

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

  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Bills' })
import { computed, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, UserFilled, CreditCard, Money, Delete, Document, Wallet, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
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
  refresh,
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

const currentScopeLabel = computed(() => {
  if (query.billMonth) return `${query.billMonth} 账单`
  if (query.year) return `${query.year} 年账单`
  return '全部账单'
})

const summaryCards = computed(() => {
  return [
    {
      label: '账单',
      value: String(billOverview.value.billCount),
      className: '',
      icon: Document,
      iconBg: '#eaf2ff',
      iconColor: '#0958d9'
    },
    {
      label: '代还',
      value: `¥${formatMoney(billOverview.value.totalBillAmount)}`,
      className: '',
      icon: Wallet,
      iconBg: '#edf7ff',
      iconColor: '#0ea5e9'
    },
    {
      label: '手续费',
      value: `¥${formatMoney(billOverview.value.totalFeeAmount)}`,
      className: 'amt-pos',
      icon: Money,
      iconBg: '#e8f7ed',
      iconColor: '#2f9e44'
    },
    {
      label: 'POS',
      value: `¥${formatMoney(billOverview.value.totalPosCostAmount)}`,
      className: 'amt-neg',
      icon: CreditCard,
      iconBg: '#fdebec',
      iconColor: '#cf1322'
    },
    {
      label: '利润',
      value: `¥${formatMoney(billOverview.value.totalNetProfit)}`,
      className: billOverview.value.totalNetProfit >= 0 ? 'amt-pos' : 'amt-neg',
      icon: TrendCharts,
      iconBg: billOverview.value.totalNetProfit >= 0 ? '#e8f7ed' : '#fdebec',
      iconColor: billOverview.value.totalNetProfit >= 0 ? '#2f9e44' : '#cf1322'
    }
  ]
})

const quickMenus = computed(() => [
  { key: 'all', label: '全部', value: undefined, count: billOverview.value.billCount, color: '#0958d9' },
  { key: 'pending', label: '待还', value: 0, count: billOverview.value.pendingCount, color: '#d97706' },
  { key: 'partial', label: '部分', value: 2, count: billOverview.value.partialCount, color: '#7c8799' },
  { key: 'paid', label: '已还', value: 1, count: billOverview.value.repaidCount, color: '#2f9e44' },
  { key: 'overdue', label: '逾期', value: 3, count: billOverview.value.overdueCount, color: '#cf1322' }
])

const activeQuickMenu = computed(() => {
  const matched = quickMenus.value.find(item => item.value === query.status)
  return matched?.key || 'all'
})

function applyQuickMenu(status?: number) {
  query.status = status as any
}

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
.bill-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: -20px;
  width: calc(100% + 40px);
  height: calc(100% + 40px);
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
  display: grid;
  grid-template-columns: minmax(150px, 0.72fr) minmax(360px, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98) 0%, rgba(248,250,253,0.98) 100%);
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.035);
  flex-shrink: 0;
}

.header-copy,
.header-stat-body,
.owner-info {
  min-width: 0;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 16px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.detail-mode-chip {
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #0958d9;
  font-size: 11px;
  line-height: 20px;
  font-weight: 600;
}

.header-subtitle {
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.2;
  color: #8a94a6;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-stat-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
}

.header-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  min-width: 0;
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

.header-stat-value {
  font-size: 15px;
  line-height: 1;
  font-weight: 700;
  color: #1f2a37;
}

.header-stat-label {
  margin-top: 2px;
  font-size: 10px;
  color: #7c8799;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn,
.header-actions :deep(.el-button),
.filter-main :deep(.el-button) {
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
}

.filter-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  flex-shrink: 0;
}

.filter-main {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
}

.filter-title {
  padding: 0 2px;
  font-size: 11px;
  font-weight: 700;
  color: #526074;
  white-space: nowrap;
}

.filter-item {
  width: 96px;
}

.filter-owner {
  width: 118px;
}

.filter-card {
  width: 132px;
}

.filter-month {
  width: 108px;
}

.filter-main :deep(.el-input__wrapper),
.filter-main :deep(.el-select__wrapper),
.filter-main :deep(.el-date-editor.el-input__wrapper),
.filter-main :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
}

.filter-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toolbar-hint,
.toolbar-tip {
  font-size: 11px;
  color: #7c8799;
}

.toolbar-tip {
  max-width: 320px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 144px minmax(0, 1fr) 166px;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

.side-panel,
.action-panel,
.data-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 8px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #1f2a37;
  line-height: 1.1;
}

.panel-desc {
  margin-top: 2px;
  font-size: 11px;
  color: #8a94a6;
  line-height: 1.2;
}

.menu-list {
  display: grid;
  gap: 6px;
}

.menu-item {
  display: grid;
  grid-template-columns: 8px 1fr auto;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #e7edf4;
  border-radius: 10px;
  background: #f8fafc;
  color: #435266;
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-item:hover {
  border-color: #b9c7d8;
  background: #f2f6fb;
}

.menu-item.active {
  border-color: #b4ceff;
  background: #eaf2ff;
  color: #0958d9;
  box-shadow: inset 0 0 0 1px rgba(9, 88, 217, 0.04);
}

.menu-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.menu-label {
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-count {
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(9, 88, 217, 0.08);
  color: inherit;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.data-head {
  align-items: center;
}

.inline-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  height: 26px;
  border-radius: 8px;
  background: #f5f8fc;
  color: #667085;
  font-size: 11px;
  white-space: nowrap;
}

.bill-page-table {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  box-shadow: none;
}

.bill-page-table :deep(.el-table) {
  --el-table-border-color: #e5eaf1;
  font-size: 12px;
}

.bill-page-table :deep(.el-table .cell) {
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

.bill-page-table :deep(.el-table th.el-table__cell) {
  height: 34px;
  background: #f7f9fc;
  font-size: 11px;
  color: #5b6472;
  font-weight: 600;
}

.bill-page-table :deep(.el-table td.el-table__cell) {
  height: 48px;
}

.bill-page-table :deep(.pagination-wrapper) {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.bill-page-table :deep(.el-pagination) {
  transform: scale(0.94);
  transform-origin: right center;
}

.bill-page-table :deep(.el-table__body-wrapper) {
  scrollbar-width: none;
}

.bill-page-table :deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

.owner-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.owner-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e6f0ff 0%, #cfe0ff 100%);
  color: #0958d9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.owner-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.owner-info :deep(.el-tag) {
  width: fit-content;
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
}

.owner-name {
  font-weight: 600;
  font-size: 13px;
  color: #1f2a37;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bank-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
}

.card-no-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 22px;
  padding: 0 8px;
  font-weight: 700;
  font-size: 11px;
  color: #1f2a37;
  font-family: var(--font-mono);
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-radius: 999px;
  border: 1px solid #bae0ff;
}

.period-text,
.repay-date,
.amount-value,
.fee-rate-badge {
  font-size: 12px;
  font-weight: 600;
}

.amount-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.amount-value {
  font-family: var(--font-mono);
  letter-spacing: 0.02em;
}

.fee-rate-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  color: #fa8c16;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbf0 100%);
  border-radius: 999px;
  border: 1px solid #ffe7ba;
  font-family: var(--font-mono);
}

.no-data {
  color: #c0c7d6;
  font-size: 12px;
}

.delete-icon-btn {
  width: 24px;
  height: 24px;
  padding: 0;
}

.expand-bill-content {
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 10px;
}

.quick-edit-bar {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e5eaf1;
  box-shadow: none;
}

.qe-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qe-item > label {
  font-size: 11px;
  color: #8a94a6;
}

.qe-readonly {
  min-width: 86px;
}

.qe-readonly > span {
  font-size: 13px;
  font-weight: 700;
}

.detail-section {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e5eaf1;
  overflow: hidden;
  box-shadow: none;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: #fafbfc;
  border-bottom: 1px solid #e5eaf1;
}

.detail-title {
  font-weight: 700;
  font-size: 12px;
  color: #1f2a37;
}

.batch-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5eaf1;
}

.side-card {
  padding: 8px;
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

.detail-mode-text {
  margin-bottom: 4px;
  font-size: 11px;
  line-height: 1.5;
  color: #667085;
}

.action-stack {
  display: grid;
  gap: 6px;
}

.action-stack :deep(.el-button) {
  width: 100%;
  height: 30px;
  margin-left: 0;
  border-radius: 8px;
}

.summary-list,
.shortcut-list {
  display: grid;
  gap: 6px;
}

.summary-item,
.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-size: 11px;
  color: #667085;
}

.summary-item strong {
  color: #1f2a37;
  font-size: 12px;
}

.shortcut-item kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 20px;
  padding: 0 6px;
  font-family: monospace;
  font-size: 11px;
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 6px;
}

@media (max-width: 1480px) {
  .page-header {
    grid-template-columns: minmax(140px, 0.7fr) minmax(300px, 1fr) auto;
  }

  .workspace-grid {
    grid-template-columns: 132px minmax(0, 1fr) 152px;
  }
}

@media (max-width: 1320px) {
  .header-subtitle,
  .toolbar-tip,
  .panel-desc {
    display: none;
  }

  .workspace-grid {
    grid-template-columns: 124px minmax(0, 1fr) 142px;
  }

  .inline-summary {
    display: none;
  }
}
</style>
