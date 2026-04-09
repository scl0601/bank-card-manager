<template>
  <div>
    <div class="page-title">账单管理</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="账单月份">
        <el-date-picker v-model="query.billMonth" type="month" value-format="YYYY-MM"
          placeholder="选择月份" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
          <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
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
    >
      <el-table-column prop="ownerName" label="持卡人" width="100" />
      <el-table-column label="银行卡" width="150">
        <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
      </el-table-column>
      <el-table-column prop="billMonth" label="账单月份" width="110" />
      <el-table-column label="账单金额" width="130" align="right">
        <template #default="{ row }"><span class="amount-negative"><AmountDisplay :value="row.billAmount" /></span></template>
      </el-table-column>
      <el-table-column label="最低还款" width="120" align="right">
        <template #default="{ row }"><AmountDisplay :value="row.minPayAmount" /></template>
      </el-table-column>
      <el-table-column prop="repayDate" label="还款日" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="BILL_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="success" link @click="openRepay(row)" v-if="row.status === 0 || row.status === 2">还款</el-button>
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
      :title="dialogTitle + '账单'"
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
        <el-form-item label="账单月份" prop="billMonth">
          <el-date-picker v-model="form.billMonth" type="month" value-format="YYYY-MM" style="width:100%" />
        </el-form-item>
        <el-form-item label="账单金额" prop="billAmount">
          <el-input-number v-model="form.billAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="最低还款额">
          <el-input-number v-model="form.minPayAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="还款日">
          <el-date-picker v-model="form.repayDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 还款弹窗 -->
    <el-dialog v-model="repayVisible" title="登记还款" width="420px">
      <el-form :model="repayForm" label-width="100px">
        <el-form-item label="还款金额">
          <el-input-number v-model="repayForm.amount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="还款日期">
          <el-date-picker v-model="repayForm.date" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repayVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRepay">确认还款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import { getBillPageApi, saveBillApi, updateBillApi, deleteBillApi, repayBillApi, exportBillApi } from '@/api/bill'
import { getCardListApi } from '@/api/card'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { BILL_STATUS_OPTIONS, BILL_STATUS_MAP, BILL_STATUS_TAG_TYPE } from '@/constants/dict'

const cardList = ref<any[]>([])
const rules = {
  cardId: [{ required: true, message: '请选择银行卡', trigger: 'change' }],
  billMonth: [{ required: true, message: '请选择账单月份', trigger: 'change' }],
  billAmount: [{ required: true, message: '请输入账单金额', trigger: 'blur' }]
}

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getBillPageApi,
  defaultQuery: { billMonth: '', status: undefined as any },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum; (params as any).size = params.pageSize
    delete (params as any).pageNum; delete (params as any).pageSize
  }
})

const { exporting, handleExport } = useExport({
  exportApi: exportBillApi,
  fileName: '账单记录'
})

// 表单（手动管理，因无 getById 接口）
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const formData = reactive<any>({ cardId: null, billMonth: '', billAmount: 0, minPayAmount: 0, repayDate: '', status: 0, remark: '' })

// 还款弹窗
const repayVisible = ref(false)
const currentRepayId = ref(0)
const repayForm = reactive({ amount: 0, date: '' })

function openAdd() {
  isEdit.value = false; dialogTitle.value = '新增'
  Object.assign(formData, { id: undefined, cardId: null, billMonth: '', billAmount: 0, minPayAmount: 0, repayDate: '', status: 0, remark: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true; dialogTitle.value = '编辑'
  Object.assign(formData, row)
  dialogVisible.value = true
}

function openRepay(row: any) {
  currentRepayId.value = row.id
  Object.assign(repayForm, { amount: row.billAmount, date: '' })
  repayVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) { await updateBillApi(formData) } else { await saveBillApi(formData) }
    ElMessage.success('操作成功'); dialogVisible.value = false; handleSearch()
  } finally { submitting.value = false }
}

async function handleRepay() {
  if (!repayForm.amount || !repayForm.date) { ElMessage.warning('请填写完整还款信息'); return }
  await repayBillApi(currentRepayId.value, repayForm.amount, repayForm.date)
  ElMessage.success('还款登记成功'); repayVisible.value = false; handleSearch()
}

async function handleDelete(id: number) {
  await deleteBillApi(id)
  ElMessage.success('删除成功'); handleSearch()
}

onMounted(() => {
  getCardListApi().then(res => { cardList.value = res.data })
})
</script>
