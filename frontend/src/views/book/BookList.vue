<template>
  <div>
    <div class="page-title">个人记账</div>

    <!-- 统计卡片区 -->
    <el-row :gutter="16" class="summary-cards">
      <el-col :span="8">
        <el-card shadow="never" class="summary-card income-card">
          <div class="card-label">本月收入</div>
          <div class="card-value">+<AmountDisplay :value="summary.totalIncome" /></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="summary-card expense-card">
          <div class="card-label">本月支出</div>
          <div class="card-value">-<AmountDisplay :value="summary.totalExpense" /></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="summary-card balance-card">
          <div class="card-label">本月结余</div>
          <div class="card-value" :class="(summary.totalIncome - summary.totalExpense) >= 0 ? 'text-success' : 'text-danger'">
            <AmountDisplay :value="Math.abs(summary.totalIncome - summary.totalExpense)" :show-sign="true" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="记账类型">
        <el-select v-model="query.bookType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in BOOK_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-cascader
          v-model="query.categoryIds"
          :options="categoryOptions"
          :props="{ multiple: true, checkStrictly: true, value: 'id', label: 'name', children: 'children', emitPath: false }"
          placeholder="全部分类"
          clearable
          filterable
          collapse-tags
          collapse-tags-tooltip
          style="width:280px"
        />
      </el-form-item>
      <el-form-item label="记账日期">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
          style="width:240px" @change="onDateChange" />
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAdd">记一笔</el-button>
        <ExportButton :loading="exporting" @click="handleExport" />
        <el-button :icon="Setting" @click="drawerVisible = true">分类管理</el-button>
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
      @update:page-num="(val) => { query.pageNum = val }"
      @update:page-size="(val) => { query.pageSize = val }"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="bookDate" label="日期" width="120" sortable />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <StatusTag :value="row.bookType" :label-map="BOOK_TYPE_MAP" :type-map="BOOK_TYPE_TAG_TYPE" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="金额" width="140" align="right">
        <template #default="{ row }">
          <span :class="row.bookType === 1 ? 'amount-positive' : 'amount-negative'">
            {{ row.bookType === 1 ? '+' : '-' }}<AmountDisplay :value="row.amount" />
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="关联卡" width="150">
        <template #default="{ row }">
          <span v-if="row.bankName">{{ row.bankName }} *{{ row.cardNoLast4 }}</span>
          <span v-else class="text-muted">--</span>
        </template>
      </el-table-column>
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
      :title="dialogTitle + '记账'"
      :form-data="formData"
      :rules="rules"
      :loading="submitting"
      :is-edit="isEdit"
      width="500px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="记账类型" prop="bookType">
          <el-radio-group v-model="form.bookType" @change="onTypeChange">
            <el-radio v-for="item in BOOK_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择分类" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="categoryOptions"
            :props="{ checkStrictly: true, value: 'id', label: 'name', children: 'children', emitPath: false }"
            placeholder="请选择分类"
            filterable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width:100%" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="记账日期" prop="bookDate">
          <el-date-picker v-model="form.bookDate" type="date" value-format="YYYY-MM-DD" style="width:100%" placeholder="请选择日期" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="备注说明（可选）" />
        </el-form-item>
        <el-form-item label="关联银行卡">
          <el-select v-model="form.cardId" placeholder="不关联（可选）" clearable filterable style="width:100%">
            <el-option v-for="card in cardList" :key="card.id" :value="card.id"
              :label="`${card.bankName} *${card.cardNoLast4}`" />
          </el-select>
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 分类管理抽屉 -->
    <el-drawer v-model="drawerVisible" title="分类管理" :size="drawerWidth + 'px'" destroy-on-close ref="drawerRef">
      <CategoryDrawer @refresh="loadCategories" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Books' })
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import CategoryDrawer from './CategoryDrawer.vue'
import {
  getBookPageApi, saveBookApi, updateBookApi, deleteBookApi,
  exportBookApi, getBookSummaryApi, getCategoryListApi
} from '@/api/book'
import { getCardListApi } from '@/api/card'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { BOOK_TYPE_OPTIONS, BOOK_TYPE_MAP, BOOK_TYPE_TAG_TYPE } from '@/constants/dict'

// ========== 统计 ==========
const summary = reactive<{ totalIncome: number; totalExpense: number }>({ totalIncome: 0, totalExpense: 0 })

async function loadSummary() {
  try {
    const res: any = await getBookSummaryApi()
    const data = res.data as any
    summary.totalIncome = Number(data.totalIncome) || 0
    summary.totalExpense = Number(data.totalExpense) || 0
  } catch {
    /* ignore */
  }
}

// ========== 分页表格 ==========
const cardList = ref<any[]>([])
const dateRange = ref<string[]>([])
const selectedIds = ref<number[]>([])

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getBookPageApi,
  defaultQuery: { bookType: undefined as any, categoryIds: [] as number[], cardId: undefined as any, bookDateStart: '', bookDateEnd: '' },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  }
})

const { exporting, handleExport } = useExport({
  exportApi: exportBookApi,
  fileName: '个人记账记录'
})

function onDateChange(val: string[] | null) {
  if (val) {
    query.bookDateStart = val[0]
    query.bookDateEnd = val[1]
  } else {
    query.bookDateStart = ''
    query.bookDateEnd = ''
  }
}

function onSelectionChange(selection: any[]) {
  selectedIds.value = selection.map((item: any) => item.id)
}

// ========== 弹窗表单 ==========
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const today = new Date().toISOString().slice(0, 10)
const formData = reactive({
  id: undefined as number | undefined,
  bookType: 2,
  amount: 0,
  bookDate: today,
  categoryId: undefined as number | undefined,
  description: '',
  cardId: undefined as number | undefined
})
const rules = {
  bookType: [{ required: true, message: '请选择记账类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  bookDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

// ========== 分类数据（筛选 + 表单用） ==========
const allCategoryTree = ref<any[]>([])

/** 筛选区：将树形数据转为 cascader 需要的 options */
const categoryOptions = computed(() => {
  return buildCascaderOptions(allCategoryTree.value)
})

function onTypeChange() {
  formData.categoryId = undefined as any
}

/** 将后端返回的 tree 数据转为 cascader options 格式 */
function buildCascaderOptions(tree: any[]): any[] {
  const result: any[] = []
  for (const group of tree) {
    const node: any = { id: group.id, name: group.name }
    if (group.children && group.children.length > 0) {
      node.children = group.children.map((child: any) => ({ id: child.id, name: child.name }))
    }
    result.push(node)
  }
  return result
}

async function loadCategories() {
  try {
    const res: any = await getCategoryListApi()
    allCategoryTree.value = res.data || []
  } catch {
    /* ignore */
  }
}

function openAdd() {
  isEdit.value = false
  dialogTitle.value = '新增'
  Object.assign(formData, {
    id: undefined,
    bookType: 2,
    amount: 0,
    bookDate: new Date().toISOString().slice(0, 10),
    categoryId: undefined,
    description: '',
    cardId: undefined
  })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑'
  Object.assign(formData, {
    id: row.id,
    bookType: row.bookType,
    amount: row.amount,
    bookDate: row.bookDate,
    categoryId: row.categoryId,
    description: row.description || '',
    cardId: row.cardId || undefined
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBookApi({ ...formData })
    } else {
      await saveBookApi({ ...formData })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    handleSearch()
    void loadSummary()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deleteBookApi(id)
  ElMessage.success('删除成功')
  handleSearch()
  void loadSummary()
}

// ========== 分类抽屉 ==========
const drawerVisible = ref(false)
const drawerWidth = ref(580)
const drawerRef = ref<any>(null)

// ========== 初始化 ==========
onMounted(() => {
  loadSummary()
  loadCategories()
  getCardListApi().then((res: any) => {
    cardList.value = res?.data ?? []
  })
})
</script>

<style scoped lang="scss">
.summary-cards {
  margin-bottom: 16px;
}
.summary-card {
  border-radius: 8px;
  text-align: center;
  padding: 8px;
  .card-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }
  .card-value {
    font-size: 24px;
    font-weight: 700;
    font-family: DIN, "Helvetica Neue", sans-serif;
  }
}
.income-card {
  border-top: 3px solid #67c23a;
  .card-value { color: #67c23a; }
}
.expense-card {
  border-top: 3px solid #f56c6c;
  .card-value { color: #f56c6c; }
}
.balance-card {
  border-top: 3px solid #409eff;
  .card-value { color: #303133; }
}
.text-success { color: #67c23a !important; }
.text-danger { color: #f56c6c !important; }
.text-muted { color: #c0c4cc; }

.amount-positive { color: #67c23a; font-weight: 600; }
.amount-negative { color: #f56c6c; font-weight: 600; }
</style>
