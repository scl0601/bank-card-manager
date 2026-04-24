<template>
  <div>
    <div class="page-title">提醒中心</div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card stat-pending">
          <div class="stat-icon"><el-icon><Bell /></el-icon></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.pending }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-overdue">
          <div class="stat-icon"><el-icon><Warning /></el-icon></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.overdue }}</div>
            <div class="stat-label">账单逾期</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-due-today">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.dueToday }}</div>
            <div class="stat-label">今日到期</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-expiring">
          <div class="stat-icon"><el-icon><CreditCard /></el-icon></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.cardExpiring }}</div>
            <div class="stat-label">卡片即将过期</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="handleReset">
      <el-form-item label="持卡人">
        <el-input v-model="query.ownerName" placeholder="姓名" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="提醒类型">
        <el-select v-model="query.reminderType" placeholder="全部" clearable style="width:140px">
          <el-option v-for="item in REMINDER_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in REMINDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
          style="width:240px" @change="onDateChange" />
      </el-form-item>
      <template #extra-buttons>
        <el-dropdown trigger="click" @command="handleScan">
          <el-button :loading="scanLoading">
            立即扫描<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="bill">扫描账单到期提醒</el-dropdown-item>
              <el-dropdown-item command="card">扫描卡片过期提醒</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </SearchBar>

    <!-- 表格区 -->
    <PageTable
      :data="list"
      :loading="loading"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      border
      @update:page-num="val => query.pageNum = val"
      @update:page-size="val => query.pageSize = val"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="ownerName" label="持卡人" width="100" />
      <el-table-column prop="bankName" label="银行" width="100">
        <template #default="{ row }">
          <span v-if="row.bankName">{{ row.bankName }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="卡号后四位" width="100">
        <template #default="{ row }">
          <CardNumberDisplay v-if="row.cardNoLast4" :last4="row.cardNoLast4" compact />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="提醒类型" width="130">
        <template #default="{ row }">
          <StatusTag :value="row.reminderType" :label-map="REMINDER_TYPE_MAP" :type-map="REMINDER_TYPE_TAG_TYPE" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="reminderDate" label="提醒日期" width="120" />
      <el-table-column prop="billMonth" label="账单月份" width="100">
        <template #default="{ row }">
          <span v-if="row.billMonth">{{ row.billMonth }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="提醒内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="REMINDER_STATUS_MAP" :type-map="REMINDER_STATUS_TAG_TYPE" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="handledBy" label="处理人" width="100">
        <template #default="{ row }">
          <span v-if="row.handledBy">{{ row.handledBy }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="handledTime" label="处理时间" width="160">
        <template #default="{ row }">
          <span v-if="row.handledTime">{{ row.handledTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-popconfirm title="确认标记为已处理？" @confirm="handleMark(row.id, 'handle')">
              <template #reference><el-button type="success" link size="small">已处理</el-button></template>
            </el-popconfirm>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认忽略该提醒？" @confirm="handleMark(row.id, 'ignore')">
              <template #reference><el-button type="info" link size="small">忽略</el-button></template>
            </el-popconfirm>
          </template>
          <span v-else class="text-muted">已完结</span>
        </template>
      </el-table-column>
    </PageTable>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Reminders' })
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { Bell, Warning, Clock, CreditCard, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import CardNumberDisplay from '@/components/CardNumberDisplay/index.vue'
import {
  getReminderPageApi, markHandledApi, markIgnoredApi,
  scanBillRemindersApi, scanCardRemindersApi,
  batchMarkHandledApi, batchMarkIgnoredApi
} from '@/api/reminder'
import { usePageTable } from '@/composables/usePageTable'
import {
  REMINDER_TYPE_OPTIONS, REMINDER_TYPE_MAP, REMINDER_TYPE_TAG_TYPE,
  REMINDER_STATUS_OPTIONS, REMINDER_STATUS_MAP, REMINDER_STATUS_TAG_TYPE
} from '@/constants/dict'

const scanLoading = ref(false)
const dateRange = ref<string[]>([])
const selectedIds = ref<number[]>([])

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: (params) => {
    // 清理空参数
    const clean: Record<string, any> = {}
    for (const key in params) {
      const val = (params as any)[key]
      if (val !== undefined && val !== null && val !== '') clean[key] = val
    }
    return getReminderPageApi(clean)
  },
  defaultQuery: { ownerName: '', reminderType: undefined as number | undefined, status: undefined as number | undefined, reminderDateStart: '', reminderDateEnd: '' },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum; (params as any).size = params.pageSize
    delete (params as any).pageNum; delete (params as any).pageSize
  }
})

// 重置时额外清空日期范围
const handleReset = () => {
  resetQuery()
  dateRange.value = []
}

// 统计数据
const allPendingList = ref<any[]>([])
const stats = computed(() => ({
  pending: allPendingList.value.filter(r => r.status === 0).length,
  overdue: allPendingList.value.filter(r => r.status === 0 && r.reminderType === 3).length,
  dueToday: allPendingList.value.filter(r => r.status === 0 && r.reminderType === 2).length,
  cardExpiring: allPendingList.value.filter(r => r.status === 0 && r.reminderType === 4).length
}))

async function loadStats() {
  try {
    const res = await getReminderPageApi({ current: 1, size: 100 })
    allPendingList.value = res.data.records
  } catch {}
}

function onDateChange(val: string[] | null) {
  if (val) { query.reminderDateStart = val[0]; query.reminderDateEnd = val[1] }
  else { query.reminderDateStart = ''; query.reminderDateEnd = '' }
}

function onSelectionChange(selection: any[]) {
  selectedIds.value = selection.map(item => item.id)
}

async function handleMark(id: number, type: 'handle' | 'ignore') {
  if (type === 'handle') { await markHandledApi(id) } else { await markIgnoredApi(id) }
  ElMessage.success('操作成功'); handleSearch(); loadStats()
}

async function handleBatchMark(type: 'handle' | 'ignore') {
  if (selectedIds.value.length === 0) return
  const action = type === 'handle' ? '标记为已处理' : '忽略'
  await ElMessageBox.confirm(`确认将选中的 ${selectedIds.value.length} 条提醒${action}？`, '批量操作', { type: 'warning' })
  if (type === 'handle') { await batchMarkHandledApi(selectedIds.value) } else { await batchMarkIgnoredApi(selectedIds.value) }
  ElMessage.success('批量操作成功'); selectedIds.value = []; handleSearch(); loadStats()
}

async function handleScan(command: string) {
  scanLoading.value = true
  try {
    if (command === 'bill') {
      await scanBillRemindersApi()
      ElMessage.success('账单到期扫描完成，已生成新提醒')
    } else {
      await scanCardRemindersApi()
      ElMessage.success('卡片过期扫描完成，已生成新提醒')
    }
    handleSearch(); loadStats()
  } catch {
    ElMessage.error('扫描失败，请检查权限或稍后重试')
  } finally { scanLoading.value = false }
}

onMounted(loadStats)
</script>

<style scoped lang="scss">
.stat-cards { margin-bottom: 20px; }

.stat-card {
  display: flex; align-items: center; gap: 16px;
  padding: 20px 24px; background: #fff; border-radius: 8px;
  border-left: 4px solid transparent; box-shadow: 0 1px 4px rgba(0,0,0,.06);

  .stat-icon { font-size: 28px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
  .stat-num { font-size: 28px; font-weight: 700; line-height: 1; margin-bottom: 4px; }
  .stat-label { font-size: 13px; color: #909399; }

  &.stat-pending { border-left-color: #e6a23c; .stat-icon { background: #fdf6ec; color: #e6a23c; } .stat-num { color: #e6a23c; } }
  &.stat-overdue { border-left-color: #f56c6c; .stat-icon { background: #fef0f0; color: #f56c6c; } .stat-num { color: #f56c6c; } }
  &.stat-due-today { border-left-color: #409eff; .stat-icon { background: #ecf5ff; color: #409eff; } .stat-num { color: #409eff; } }
  &.stat-card-expiring { border-left-color: #909399; .stat-icon { background: #f4f4f5; color: #909399; } .stat-num { color: #606266; } }
}

.text-muted { color: #c0c4cc; }
</style>
