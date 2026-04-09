<template>
  <div>
    <div class="page-title">流水管理</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="交易类型">
        <el-select v-model="query.txType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in TXN_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="交易日期">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
          style="width:240px" @change="onDateChange" />
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAdd">记一笔</el-button>
        <ExportButton :loading="exporting" @click="handleExport" />
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
      <el-table-column label="银行卡" width="140">
        <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
      </el-table-column>
      <el-table-column label="交易类型" width="90">
        <template #default="{ row }">
          <StatusTag :value="row.txType" :label-map="TXN_TYPE_MAP" :type-map="TXN_TYPE_TAG_TYPE" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }">
          <span :class="row.txType === 1 ? 'amount-positive' : 'amount-negative'">
            {{ row.txType === 1 ? '+' : '-' }}<AmountDisplay :value="row.amount" />
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="txDate" label="交易日期" width="120" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="counterpart" label="对手方" width="150" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" link>删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </PageTable>

    <!-- 新增/编辑弹窗 -->
    <CrudDialog
      v-model="dialogVisible"
      :title="dialogTitle + '流水'"
      :form-data="formData"
      :rules="rules"
      :loading="submitting"
      :is-edit="isEdit"
      width="500px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="银行卡" prop="cardId">
          <el-select v-model="form.cardId" placeholder="请选择银行卡" filterable style="width:100%">
            <el-option v-for="card in cardList" :key="card.id" :value="card.id"
              :label="`${card.ownerName} · ${card.bankName} *${card.cardNoLast4}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易类型" prop="txType">
          <el-radio-group v-model="form.txType">
            <el-radio v-for="item in TXN_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="交易金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="交易日期" prop="txDate">
          <el-date-picker v-model="form.txDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="交易描述" />
        </el-form-item>
        <el-form-item label="对手方">
          <el-input v-model="form.counterpart" placeholder="商户/转账方" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatTime } from '@/utils/formatters'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import { getTransactionPageApi, saveTransactionApi, updateTransactionApi, deleteTransactionApi, exportTransactionApi, batchDeleteTransactionApi } from '@/api/transaction'
import { getCardListApi } from '@/api/card'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { TXN_TYPE_OPTIONS, TXN_TYPE_MAP, TXN_TYPE_TAG_TYPE } from '@/constants/dict'

const cardList = ref<any[]>([])
const dateRange = ref<string[]>([])
const selectedIds = ref<number[]>([])

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getTransactionPageApi,
  defaultQuery: { txType: undefined as any, txDateStart: '', txDateEnd: '' },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum; (params as any).size = params.pageSize
    delete (params as any).pageNum; delete (params as any).pageSize
  }
})

const { exporting, handleExport } = useExport({
  exportApi: exportTransactionApi,
  fileName: '流水记录'
})

// 表单（手动管理，因无 getById 接口）
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const formData = reactive<any>({ cardId: null, txType: 1, amount: 0, txDate: '', description: '', counterpart: '', remark: '' })
const rules = {
  cardId: [{ required: true, message: '请选择银行卡', trigger: 'change' }],
  txType: [{ required: true, message: '请选择交易类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  txDate: [{ required: true, message: '请选择交易日期', trigger: 'change' }]
}

function onDateChange(val: string[] | null) {
  if (val) { query.txDateStart = val[0]; query.txDateEnd = val[1] }
  else { query.txDateStart = ''; query.txDateEnd = '' }
}

function onSelectionChange(selection: any[]) {
  selectedIds.value = selection.map(item => item.id)
}

function openAdd() {
  isEdit.value = false; dialogTitle.value = '新增'
  Object.assign(formData, { id: undefined, cardId: null, txType: 1, amount: 0, txDate: '', description: '', counterpart: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true; dialogTitle.value = '编辑'
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) { await updateTransactionApi(formData) } else { await saveTransactionApi(formData) }
    ElMessage.success('操作成功'); dialogVisible.value = false; handleSearch()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteTransactionApi(id)
  ElMessage.success('删除成功'); handleSearch()
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条流水？`, '批量删除', { type: 'warning' })
  await batchDeleteTransactionApi(selectedIds.value)
  ElMessage.success('批量删除成功'); selectedIds.value = []; handleSearch()
}

onMounted(() => {
  getCardListApi().then(res => { cardList.value = res.data })
})
</script>
