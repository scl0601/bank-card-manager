<template>
  <div class="card-user-page">
    <!-- 顶部统计 -->
    <div class="stat-row">
      <div class="stat-card" v-for="(s, i) in statCards" :key="i">
        <div class="stat-icon" :style="{ background: s.bg }"><el-icon :size="22" :color="s.color"><component :is="s.icon" /></el-icon></div>
        <div class="stat-body">
          <span class="stat-num">{{ s.value }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @reset="resetQuery">
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="输入姓名搜索" clearable prefix-icon="Search" style="width:180px" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="query.phone" placeholder="电话号码" clearable style="width:160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAddTopUser">新增主账户</el-button>
      </template>
    </SearchBar>

    <!-- 工具栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-button link :icon="expandAll ? 'Fold' : 'Expand'" size="small" @click="toggleExpandAll">{{ expandAll ? '收起全部' : '展开全部' }}</el-button>
        <span class="toolbar-hint">共 {{ filteredCount }} 条记录</span>
      </div>
    </div>

    <!-- 树形表格 -->
    <div v-loading="loading" class="user-table-wrap">
      <el-table
        ref="tableRef"
        :data="treeData"
        border
        stripe
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        @expand-change="onExpandChange"
        :header-cell-style="{ background:'#f5f7fa', color:'#606266', fontWeight:'600' }"
        :row-class-name="rowClassName"
        :cell-style="{ padding: '6px 0' }"
        :default-expand-all="expandAll"
      >
        <!-- 用户信息列 -->
        <el-table-column prop="name" label="用户信息" min-width="260">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="avatar-wrap" :class="{ 'avatar-child': !!row.parentId }">
                <el-icon v-if="!row.parentId" :size="18"><UserFilled /></el-icon>
                <el-icon v-else :size="16"><User /></el-icon>
              </div>
              <div class="user-info">
                <div class="name-row">
                  <span class="name-text">{{ row.name }}</span>
                  <el-tag v-if="!row.parentId" type="warning" size="small" effect="light">主账户</el-tag>
                  <el-tag v-else type="info" size="small" effect="plain">子用户</el-tag>
                </div>
                <div class="meta-row" v-if="row.phone || row.createTime">
                  <span v-if="row.phone" class="meta-item">
                    <el-icon :size="12"><Phone /></el-icon>{{ row.phone }}
                  </span>
                  <span v-if="row.createTime" class="meta-time">
                    <el-icon :size="12"><Clock /></el-icon>{{ formatTime(row.createTime) }}
                  </span>
                </div>
                <div v-if="row.remark" class="remark-row">
                  <el-icon :size="12"><Document /></el-icon>
                  <span class="remark-text">{{ row.remark }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 手续费率 -->
        <el-table-column label="手续费率" width="180" align="center">
          <template #default="{ row }">
            <div class="fee-cell">
              <template v-if="editingFeeId === row.id && !row.parentId">
                <div class="fee-edit-wrap">
                  <el-input-number v-model="editingFeeRate" :min="0" :max="100" :precision="2" :controls="false"
                    size="small" style="width:90px" />
                  <span class="pct-sign">%</span>
                </div>
                <div class="fee-actions">
                  <el-button type="primary" size="small" :loading="savingFee" @click="handleSaveFeeRate(row)">保存</el-button>
                  <el-button size="small" @click="cancelEditFee">取消</el-button>
                </div>
              </template>
              <template v-else>
                <div class="fee-display">
                  <span v-if="!row.parentId" class="fee-badge" :class="{ 'has-rate': (row.effectiveFeeRate ?? row.feeRate) > 0 }">
                    {{ formatFeeRate(row.effectiveFeeRate ?? row.feeRate) }}%
                  </span>
                  <span v-else class="fee-inherit-badge">
                    <el-icon :size="12"><Connection /></el-icon>
                    继承 {{ formatFeeRate(row.effectiveFeeRate ?? row.feeRate) }}%
                  </span>
                </div>
                <el-button v-if="!row.parentId" type="primary" link size="small" @click="startEditFee(row)">
                  <el-icon :size="13"><EditPen /></el-icon>修改
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>

        <!-- 卡片数 -->
        <el-table-column prop="cardCount" label="卡片数" width="100" align="center">
          <template #default="{ row }">
            <div class="card-count-cell">
              <el-badge v-if="row.cardCount > 0" :value="row.cardCount" :max="99" type="primary" class="count-badge" />
              <span v-else class="no-data">0</span>
            </div>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
                <span class="status-dot" :class="row.status === 0 ? 'dot-green' : 'dot-red'" />
                {{ row.status === 0 ? '正常' : '停用' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button v-if="!row.parentId" type="success" link size="small" @click="openAddChild(row)">
                <el-icon :size="13"><CirclePlus /></el-icon>添加子用户
              </el-button>
              <el-button type="primary" link size="small" @click="openEditUser(row)">
                <el-icon :size="13"><EditPen /></el-icon>编辑
              </el-button>
              <el-popconfirm :title="row.status === 0 ? '确认停用该用户？停用后将禁止继续新增关联业务' : '确认启用该用户？'"
                @confirm="handleToggleStatus(row)" confirm-button-text="确定" cancel-button-text="取消"
                :icon-color="row.status === 0 ? '#e6a23c' : '#67c23a'">
                <template #reference>
                  <el-button :type="row.status === 0 ? 'warning' : 'success'" link size="small">
                    <el-icon :size="13"><component :is="row.status === 0 ? 'CircleClose' : 'CircleCheck'" /></el-icon>
                    {{ row.status === 0 ? '停用' : '启用' }}
                  </el-button>
                </template>
              </el-popconfirm>
              <el-popconfirm title="确认删除该用户？关联的子用户、银行卡及历史账单/流水需先处理。" @confirm="handleDeleteUser(row.id)" confirm-button-text="确定" cancel-button-text="取消" icon-color="#f56c6c">
                <template #reference>
                  <el-button type="danger" link size="small"><el-icon :size="13"><Delete /></el-icon>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && treeData.length === 0" description="暂无持卡人数据">
        <template #image><el-icon :size="64" color="#dcdfe6"><UserFilled /></el-icon></template>
        <el-button type="primary" :icon="Plus" @click="openAddTopUser">立即添加</el-button>
      </el-empty>
    </div>

    <!-- 新增/编辑用户弹窗 -->
    <el-dialog v-model="editVisible" :title="editTitle" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px" ref="formRef" :rules="formRules" size="large">
        <el-form-item label="用户类型" v-if="!editForm.id">
          <el-radio-group v-model="isChildMode" :disabled="!!editForm.id">
            <el-radio :label="false">主账户（可设置费率）</el-radio>
            <el-radio :label="true">子用户（继承父级费率）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入姓名" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="所属主账户" v-if="isChildMode || editForm.parentId">
          <el-select v-model="editForm.parentId" placeholder="选择主账户" clearable style="width:100%" :disabled="!!editForm.parentId">
            <el-option v-for="u in parentUserOptions" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手续费率" prop="feeRate" v-if="(!isChildMode && !editForm.parentId) || (!editForm.id && !isChildMode)">
          <el-input
            v-model="editForm.feeRate"
            placeholder="请输入手续费率，例如: 0.5"
            @blur="formatFeeRateOnBlur"
          >
            <template #append>%</template>
          </el-input>
          <div class="form-tip">修改此值将级联同步更新所有关联的子用户</div>
        </el-form-item>
        <el-form-item label="手续费率" v-else-if="isChildMode || editForm.parentId">
          <span class="inherit-hint">继承自父级（{{ formatFeeRate(inheritedRate) }}%）</span>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editForm.phone" placeholder="选填，用于联系" maxlength="11" />
        </el-form-item>
        <el-form-item label="状态" v-if="editForm.id">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" rows="3" placeholder="选填" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveUser">
          {{ editForm.id ? '保存修改' : '确认创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CardUsers' })
import { ref, reactive, computed, watch, onMounted, onActivated } from 'vue'
import { Plus, UserFilled, User, Phone, EditPen, Delete, CirclePlus, Clock, Document, Connection, CircleClose, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import {
  getUserTreeApi, saveUserApi, updateUserApi, deleteUserApi,
  updateUserFeeRateApi, getUserListActiveApi
} from '@/api/card'

// ====== 类型 ======
interface UserData {
  id: number
  name: string
  parentId: number | null
  phone?: string
  feeRate: number
  effectiveFeeRate?: number
  cardCount: number
  status: number
  remark?: string
  createTime?: string
  children?: UserData[]
  hasChildren?: boolean
  parentName?: string
}

// ====== 状态 ======
const loading = ref(false)
const treeData = ref<UserData[]>([])
const rawData = ref<UserData[]>([])
const tableRef = ref()
const expandAll = ref(true)

const query = reactive({ name: '', phone: '', status: undefined as any })

// 费率编辑
const editingFeeId = ref<number | null>(null)
const editingFeeRate = ref(0)
const savingFee = ref(false)

// 弹窗
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const editTitle = ref('新增主账户')
const inheritedRate = ref(0)
const isChildMode = ref(false)
const parentUserOptions = ref<{ id: number; name: string }[]>([])

const defaultForm: Record<string, any> = {
  id: undefined, parentId: null, name: '', phone: '', feeRate: 0, remark: '', status: 0
}
const editForm = reactive({ ...defaultForm })
const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  feeRate: [{ required: true, message: '请输入手续费率', trigger: 'blur' }]
}

// ====== 统计数据 ======
const statCards = computed(() => {
  const flat = flattenTree(rawData.value)
  const total = flat.length
  const active = flat.filter((u: UserData) => u.status === 0).length
  const mainAccounts = flat.filter((u: UserData) => !u.parentId).length
  const totalCards = flat.reduce((s: number, u: UserData) => s + (u.cardCount || 0), 0)
  return [
    { value: total, label: '总人数', icon: 'UserFilled', bg: '#ecf5ff', color: '#409eff' },
    { value: active, label: '正常', icon: 'CircleCheck', bg: '#f0f9eb', color: '#67c23a' },
    { value: mainAccounts, label: '主账户', icon: 'UserFilled', bg: '#fdf6ec', color: '#e6a23c' },
    { value: totalCards, label: '银行卡总数', icon: 'CreditCard', bg: '#fef0f0', color: '#f56c6c' }
  ]
})

const filteredCount = computed(() => flattenTree(treeData.value).length)

// ====== 数据加载 ======
async function fetchData() {
  loading.value = true
  try {
    const res: any = await getUserTreeApi()
    rawData.value = res.data || []
    applyFilter()
    // 加载主账户选项（用于选择父用户）
    const res2: any = await getUserListActiveApi()
    if (res2.data) {
      parentUserOptions.value = (res2.data as UserData[])
        .filter(u => !u.parentId)
        .map(u => ({ id: u.id, name: u.name }))
    }
  } finally { loading.value = false }
}

function applyFilter() {
  let list = [...rawData.value]
  if (query.name?.trim()) list = filterTree(list, 'name', query.name.trim())
  if (query.phone?.trim()) list = filterTree(list, 'phone', query.phone.trim())
  if (query.status !== undefined && query.status !== null && query.status !== '') list = filterTree(list, 'status', query.status)
  treeData.value = list
}

function filterTree<T extends { children?: T[] }>(list: T[], field: string, value: any): T[] {
  return list.filter(item => {
    const matched = item[field as keyof T] != null && String(item[field as keyof T]).includes(value)
    if (item.children?.length) item.children = filterTree(item.children, field, value) as T[]
    return matched || (item.children && item.children.length > 0)
  })
}

function flattenTree<T extends { children?: T[] }>(list: T[]): T[] {
  const result: T[] = []
  function walk(items: T[]) {
    for (const it of items) {
      result.push(it)
      if (it.children?.length) walk(it.children)
    }
  }
  walk(list)
  return result
}

function resetQuery() {
  query.name = ''
  query.phone = ''
  query.status = undefined
}

// ====== 展开/收起 ======
function onExpandChange(_row: UserData, expanded: boolean) {
  expandAll.value = expanded
}

function toggleExpandAll() {
  const rows = tableRef.value?.store?.states?.data?.value || []
  if (!rows.length) return
  expandAll.value = !expandAll.value
  setExpandRecursive(rows, expandAll.value)
}

function setExpandRecursive(rows: any[], expanded: boolean) {
  for (const row of rows) {
    tableRef.value?.toggleRowExpansion(row, expanded)
    if (row.children?.length) setExpandRecursive(row.children as any[], expanded)
  }
}

function rowClassName({ row }: { row: UserData }) {
  return row.status === 1 ? 'row-disabled' : ''
}

// ====== 时间格式化 ======
function formatTime(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

// ====== 费率编辑 ======
function startEditFee(row: UserData) {
  editingFeeId.value = row.id
  editingFeeRate.value = Number(formatFeeRate(row.effectiveFeeRate ?? row.feeRate ?? 0))
}

async function handleSaveFeeRate(row: UserData) {
  savingFee.value = true
  try {
    await updateUserFeeRateApi({ userId: row.id, feeRate: editingFeeRate.value })
    ElMessage.success('费率已更新并同步到所有子用户')
    cancelEditFee()
    fetchData()
  } finally { savingFee.value = false }
}

function cancelEditFee() { editingFeeId.value = null }

function formatFeeRate(rate: number | string | null | undefined) {
  return Number(rate ?? 0).toFixed(2)
}

// ====== 手续费输入处理 ======
function formatFeeRateOnBlur() {
  const num = parseFloat(String(editForm.feeRate))
  if (isNaN(num) || num < 0) {
    editForm.feeRate = 0 as any
  } else if (num > 100) {
    editForm.feeRate = 100 as any
  } else {
    editForm.feeRate = num.toFixed(2) as any
  }
}

// ====== 新增主账户 ======
function openAddTopUser() {
  isChildMode.value = false
  editTitle.value = '新增主账户'
  Object.assign(editForm, defaultForm)
  editVisible.value = true
}

// ====== 添加子用户 ======
function openAddChild(parentRow: UserData) {
  isChildMode.value = true
  editTitle.value = `添加子用户 - ${parentRow.name}`
  inheritedRate.value = Number(parentRow.effectiveFeeRate ?? parentRow.feeRate ?? 0)
  Object.assign(editForm, { id: undefined, parentId: parentRow.id, name: '', phone: '', feeRate: 0, remark: '', status: 0 })
  editVisible.value = true
}

// ====== 编辑 ======
function openEditUser(row: UserData) {
  editTitle.value = '编辑用户'
  isChildMode.value = !!row.parentId
  inheritedRate.value = Number(row.effectiveFeeRate ?? row.feeRate ?? 0)
  Object.assign(editForm, {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    phone: row.phone || '',
    feeRate: Number(formatFeeRate(row.feeRate || 0)),
    remark: row.remark || '',
    status: row.status
  })
  editVisible.value = true
}

// ====== 保存 ======
async function handleSaveUser() {
  try { await formRef.value?.validate() } catch { return }

  saving.value = true
  try {
    const payload: Record<string, any> = {
      id: editForm.id,
      parentId: editForm.parentId,
      name: editForm.name,
      phone: editForm.phone,
      remark: editForm.remark,
      status: editForm.status
    }
    if (!editForm.parentId) payload.feeRate = editForm.feeRate

    if (editForm.id) {
      await updateUserApi(payload)
      ElMessage.success('修改成功')
    } else {
      await saveUserApi(payload)
      ElMessage.success('创建成功')
    }
    editVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

// ====== 切换状态 ======
async function handleToggleStatus(row: UserData) {
  const newStatus = row.status === 0 ? 1 : 0
  await updateUserApi({
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    phone: row.phone || '',
    feeRate: Number(formatFeeRate(row.feeRate || 0)),
    remark: row.remark || '',
    status: newStatus
  })
  ElMessage.success(newStatus === 1 ? '已停用' : '已启用')
  fetchData()
}

// ====== 删除 ======
async function handleDeleteUser(id: number) {
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，请先处理关联数据')
  }
}

// ====== 初始化 & 监听 ======
onMounted(() => fetchData())
onActivated(() => fetchData())

watch(query, () => applyFilter(), { deep: true })
</script>

<style scoped>
.card-user-page { display: flex; flex-direction: column; gap: var(--spacing-lg); }

/* ========== 统计卡片区 ========== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}
.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-2xl) var(--spacing-xl);
  background: var(--color-card);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--color-border-light);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-light));
  opacity: 0;
  transition: opacity 0.3s;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
  border-color: var(--color-primary-light);
}
.stat-card:hover::before {
  opacity: 1;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}
.stat-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.stat-num {
  font-size: 32px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.2;
  font-family: var(--font-mono);
}
.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

/* ========== 工具栏 ========== */
.table-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--spacing-sm) 0;
}
.toolbar-left { display: flex; align-items: center; gap: var(--spacing-md); }
.toolbar-hint { font-size: var(--font-size-sm); color: var(--color-text-placeholder); }

/* ========== 表格区 ========== */
.user-table-wrap {
  background: var(--color-card);
  border-radius: var(--border-radius-lg);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}
:deep(.row-disabled) {
  opacity: 0.5;
  background-color: #fafafa;
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

/* 用户单元格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-wrap {
  width: 42px;
  height: 42px;
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
.avatar-wrap:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}
.avatar-child {
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  color: #8c8c8c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.user-info {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.name-text {
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.meta-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-placeholder);
}
.remark-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: #f5f5f5;
  border-radius: var(--border-radius);
  max-width: 280px;
  border-left: 3px solid var(--color-primary-light);
}
.remark-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 费率 */
.fee-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.fee-edit-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}
.fee-actions {
  display: flex;
  gap: 6px;
}
.fee-display {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}
.fee-badge {
  font-size: 16px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-secondary);
  padding: 6px 14px;
  background: #f5f5f5;
  border-radius: var(--border-radius);
  font-family: var(--font-mono);
  border: 1px solid var(--color-border-light);
}
.fee-badge.has-rate {
  color: #fff;
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  box-shadow: 0 2px 8px rgba(250, 173, 20, 0.3);
  border: none;
}
.pct-sign {
  font-size: var(--font-size-sm);
  color: var(--color-text-regular);
  margin-left: 2px;
}
.fee-inherit-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: var(--border-radius);
  border: 1px solid var(--color-border-light);
}

/* 卡片数 */
.card-count-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}
.count-badge :deep(.el-badge__content) {
  font-weight: 600;
}

/* 状态 */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-cell :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-weight: 500;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--border-radius-full);
  display: inline-block;
  flex-shrink: 0;
}
.dot-green {
  background: var(--color-success);
  box-shadow: 0 0 6px rgba(103,194,58,.5);
}
.dot-red {
  background: var(--color-danger);
  box-shadow: 0 0 6px rgba(245,108,108,.5);
}

/* 操作 */
.action-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* 空状态占位 */
.no-data { color: #d9d9d9; font-size: 14px; }

/* 表单提示 */
.form-tip { font-size: 12px; color: #e6a23c; margin-top: 4px; line-height: 1.4; }
.inherit-hint { color: #909399; font-size: 13px; background: #f5f7fa; padding: 8px 12px; border-radius: 4px; }

/* 响应式 */
@media (max-width: 1200px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stat-row { grid-template-columns: 1fr; }
}
</style>
