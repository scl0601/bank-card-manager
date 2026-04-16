<template>
  <div>
    <div class="page-title">银行卡管理</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="银行"><el-input v-model="query.bankName" placeholder="银行名称" clearable style="width:140px" /></el-form-item>
      <el-form-item label="卡号后四位"><el-input v-model="query.cardNoLast4" placeholder="后四位" clearable style="width:120px" /></el-form-item>
      <el-form-item label="卡片类型">
        <el-select v-model="query.cardType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in CARD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增银行卡</el-button>
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
      <el-table-column prop="bankName" label="银行" width="120" />
      <el-table-column label="卡号后四位" width="100">
        <template #default="{ row }"><CardNumberDisplay :last4="row.cardNoLast4" compact /></template>
      </el-table-column>
      <el-table-column prop="cardTypeDesc" label="卡片类型" width="90" />
      <el-table-column label="余额/额度" width="140" align="right">
        <template #default="{ row }">
          <AmountDisplay :value="row.cardType === 2 ? row.usedAmount : row.balance" />
        </template>
      </el-table-column>
      <el-table-column prop="expireDate" label="有效期" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="CARD_STATUS_MAP" :type-map="CARD_STATUS_TAG_TYPE" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
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
      :title="dialogTitle + '银行卡'"
      :form-data="formData"
      :rules="computedRules"
      :loading="submitting"
      :is-edit="isEdit"
      width="560px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="持卡人ID" prop="ownerId">
          <el-input v-model.number="form.ownerId" placeholder="请输入持卡人ID" />
        </el-form-item>
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="form.bankName" placeholder="如：招商银行" />
        </el-form-item>
        <el-form-item label="卡号" prop="cardNo">
          <el-input v-if="!isEdit" v-model="form.cardNo" placeholder="请输入完整卡号" />
          <el-input v-else :value="`**** **** **** ${form.cardNoLast4 || '****'}`" disabled />
        </el-form-item>
        <el-form-item label="卡片类型" prop="cardType">
          <el-radio-group :model-value="form.cardType" @change="onCardTypeChange">
            <el-radio v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.cardType === 2">
          <el-form-item label="信用额度">
            <el-input-number :key="'credit-' + form.cardType" v-model="form.creditLimit" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="已用额度">
            <el-input-number :key="'used-' + form.cardType" v-model="form.usedAmount" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="账单日">
            <el-input-number :key="'bill-' + form.cardType" v-model="form.billDay" :min="1" :max="31" />
          </el-form-item>
          <el-form-item label="还款日">
            <el-input-number :key="'repay-' + form.cardType" v-model="form.repayDay" :min="1" :max="31" />
          </el-form-item>
        </template>
        <el-form-item v-else label="余额">
          <el-input-number :key="'balance-' + form.cardType" v-model="form.balance" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-input v-model="form.expireDate" placeholder="格式：2028-06" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="item in CARD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cards' })
import { computed, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import CardNumberDisplay from '@/components/CardNumberDisplay/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import { getCardPageApi, saveCardApi, updateCardApi, deleteCardApi, getCardDetailApi } from '@/api/card'
import { usePageTable } from '@/composables/usePageTable'
import { CARD_TYPE_OPTIONS, CARD_STATUS_OPTIONS, CARD_STATUS_MAP, CARD_STATUS_TAG_TYPE } from '@/constants/dict'

const defaultForm = {
  id: undefined as number | undefined,
  ownerId: null as any,
  bankName: '',
  cardNo: '',
  cardType: 1,
  creditLimit: 0,
  usedAmount: 0,
  balance: 0,
  billDay: null as any,
  repayDay: null as any,
  expireDate: '',
  status: 0,
  remark: ''
}

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getCardPageApi,
  defaultQuery: { bankName: '', cardNoLast4: '', cardType: undefined as any, status: undefined as any },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum; (params as any).size = params.pageSize
    delete (params as any).pageNum; delete (params as any).pageSize
  }
})

// 卡片类型切换确认
async function onCardTypeChange(newType: number) {
  const oldType = formData.cardType
  if (newType === oldType) return

  if (!isEdit.value) {
    formData.cardType = newType
    clearCardTypeFields(newType)
    return
  }

  const clearFields = oldType === 2 ? '信用额度、已用额度、账单日、还款日' : '余额'
  try {
    await ElMessageBox.confirm(`切换卡片类型将清空${clearFields}，是否继续？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    formData.cardType = newType
    clearCardTypeFields(oldType)
  } catch { /* 用户取消 */ }
}

function clearCardTypeFields(cardType: number) {
  if (cardType === 2) Object.assign(formData, { creditLimit: 0, usedAmount: 0, billDay: null, repayDay: null })
  else if (cardType === 1) Object.assign(formData, { balance: 0 })
}

// 表单数据（使用 reactive 保持响应式）
const formData = reactive({ ...defaultForm })
const isEdit = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

const dialogTitle = computed(() => isEdit.value ? '编辑' : '新增')
const computedRules = computed(() => ({
  ownerId: [{ required: true, message: '请输入持卡人ID', trigger: 'blur' }],
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  cardNo: isEdit.value ? [] : [{ required: true, message: '请输入卡号', trigger: 'blur' }],
  cardType: [{ required: true, message: '请选择卡片类型', trigger: 'change' }]
}))

function openAdd() {
  isEdit.value = false
  Object.assign(formData, defaultForm)
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  const data: any = { ...formData }
  if (isEdit.value && !data.cardNo) delete data.cardNo
  if (data.billDay !== null && data.billDay < 1) data.billDay = null
  if (data.repayDay !== null && data.repayDay < 1) data.repayDay = null

  submitting.value = true
  try {
    if (isEdit.value) { await updateCardApi(data) } else { await saveCardApi(data) }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    handleSearch()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteCardApi(id)
  ElMessage.success('删除成功')
  handleSearch()
}
</script>
