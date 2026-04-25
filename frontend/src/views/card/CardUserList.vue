<template>
  <div class="card-user-page">
    <div class="page-header">
      <div class="header-copy">
        <div class="header-title">持卡人管理</div>
        <div class="header-subtitle">极致一屏压缩布局，保留持卡人管理全部操作</div>
      </div>

      <div class="header-stat-row">
        <div class="header-stat" v-for="item in statCards" :key="item.label">
          <div class="header-stat-icon" :style="{ background: item.bg, color: item.color }">
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
          </div>
          <div class="header-stat-body">
            <span class="header-stat-value">{{ item.value }}</span>
            <span class="header-stat-label">{{ item.label }}</span>
          </div>
        </div>
      </div>

      <div class="header-actions">
        <el-input
          v-model="query.name"
          class="header-search"
          placeholder="搜姓名"
          clearable
          :prefix-icon="Search"
        />
        <el-button class="action-btn" :icon="RefreshRight" @click="fetchData">刷新</el-button>
        <el-button type="primary" class="action-btn" :icon="Plus" @click="openAddTopUser">新增账户</el-button>
      </div>
    </div>

    <div class="filter-panel">
      <div class="filter-left">
        <div class="filter-title">筛选</div>
        <el-input v-model="query.phone" placeholder="电话" clearable class="filter-item filter-phone" />
        <el-select v-model="query.status" placeholder="状态" clearable class="filter-item">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
        <el-select v-model="query.type" placeholder="账户类型" clearable class="filter-item">
          <el-option label="主账户" value="main" />
          <el-option label="子用户" value="child" />
        </el-select>
        <el-select v-model="query.hasCard" placeholder="绑卡情况" clearable class="filter-item">
          <el-option label="已绑卡" :value="1" />
          <el-option label="未绑卡" :value="0" />
        </el-select>
        <el-button class="action-btn" @click="resetQuery">重置</el-button>
      </div>

      <div class="filter-right">
        <span class="toolbar-hint">当前 {{ filteredCount }} 条记录</span>
        <el-button link type="primary" :icon="expandAll ? Fold : Expand" @click="toggleExpandAll">
          {{ expandAll ? '收起全部' : '展开全部' }}
        </el-button>
      </div>
    </div>

    <div class="workspace-grid">
      <aside class="side-panel card-shell">
        <div class="panel-head">
          <div>
            <div class="panel-title">快捷菜单</div>
            <div class="panel-desc">快速切换</div>
          </div>
        </div>

        <div class="menu-list">
          <button
            v-for="item in quickMenus"
            :key="item.key"
            type="button"
            class="menu-item"
            :class="{ active: activeCategory === item.key }"
            @click="activeCategory = item.key"
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
            <div class="panel-title">持卡人数据区</div>
            <div class="panel-desc">{{ activeCategoryLabel }} · 一屏优先</div>
          </div>
          <div class="inline-summary">
            <span>主 {{ visibleMainCount }}</span>
            <span>子 {{ visibleChildCount }}</span>
            <span>正 {{ visibleActiveCount }}</span>
          </div>
        </div>

        <div v-loading="loading" class="user-table-wrap">
          <el-table
            v-if="treeData.length"
            ref="tableRef"
            :data="treeData"
            row-key="id"
            border
            stripe
            height="100%"
            table-layout="fixed"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            :header-cell-style="{ background: '#f7f9fc', color: '#5b6472', fontWeight: '600' }"
            :row-class-name="rowClassName"
            :cell-style="{ padding: '2px 0' }"
            @expand-change="onExpandChange"
          >
            <el-table-column prop="name" label="用户信息" min-width="220">
              <template #default="{ row }">
                <div class="user-cell">
                  <div class="avatar-wrap" :class="{ 'avatar-child': !!row.parentId }">
                    <el-icon v-if="!row.parentId" :size="16"><UserFilled /></el-icon>
                    <el-icon v-else :size="15"><User /></el-icon>
                  </div>

                  <div class="user-info">
                    <div class="name-row">
                      <span class="name-text">{{ row.name }}</span>
                      <el-tag v-if="!row.parentId" type="warning" size="small" effect="light">主账户</el-tag>
                      <el-tag v-else type="info" size="small" effect="plain">子用户</el-tag>
                      <el-tooltip v-if="row.remark" :content="row.remark" placement="top">
                        <span class="remark-icon">
                          <el-icon :size="12"><Document /></el-icon>
                        </span>
                      </el-tooltip>
                    </div>
                    <div class="meta-row">
                      <span v-if="row.phone" class="meta-item">
                        <el-icon :size="12"><Phone /></el-icon>{{ row.phone }}
                      </span>
                      <span v-if="row.createTime" class="meta-item meta-time">
                        <el-icon :size="12"><Clock /></el-icon>{{ formatTime(row.createTime) }}
                      </span>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="费率" width="132" align="center">
              <template #default="{ row }">
                <div class="fee-cell">
                  <template v-if="editingFeeId === row.id && !row.parentId">
                    <div class="fee-edit-wrap">
                      <el-input-number
                        v-model="editingFeeRate"
                        :min="0"
                        :max="100"
                        :precision="2"
                        :controls="false"
                        size="small"
                        class="fee-input"
                      />
                      <span class="pct-sign">%</span>
                    </div>
                    <div class="fee-actions">
                      <el-button type="primary" link size="small" :loading="savingFee" @click="handleSaveFeeRate(row)">保存</el-button>
                      <el-button link size="small" @click="cancelEditFee">取消</el-button>
                    </div>
                  </template>
                  <template v-else>
                    <span v-if="!row.parentId" class="fee-badge" :class="{ 'has-rate': (row.effectiveFeeRate ?? row.feeRate) > 0 }">
                      {{ formatFeeRate(row.effectiveFeeRate ?? row.feeRate) }}%
                    </span>
                    <span v-else class="fee-inherit-badge">
                      <el-icon :size="12"><Connection /></el-icon>
                      继承 {{ formatFeeRate(row.effectiveFeeRate ?? row.feeRate) }}%
                    </span>
                    <el-button v-if="!row.parentId" type="primary" link size="small" @click="startEditFee(row)">修改</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="cardCount" label="卡数" width="72" align="center">
              <template #default="{ row }">
                <div class="card-count-cell">
                  <el-badge v-if="row.cardCount > 0" :value="row.cardCount" :max="99" type="primary" class="count-badge" />
                  <span v-else class="no-data">0</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" width="82" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
                    <span class="status-dot" :class="row.status === 0 ? 'dot-green' : 'dot-red'" />
                    {{ row.status === 0 ? '正常' : '停用' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="146" align="center">
              <template #default="{ row }">
                <div class="action-cell">
                  <el-button
                    v-if="!row.parentId"
                    type="success"
                    link
                    class="action-icon-btn"
                    title="添加子用户"
                    @click="openAddChild(row)"
                  >
                    <el-icon :size="15"><CirclePlus /></el-icon>
                  </el-button>

                  <el-button type="primary" link class="action-icon-btn" title="编辑" @click="openEditUser(row)">
                    <el-icon :size="15"><EditPen /></el-icon>
                  </el-button>

                  <el-popconfirm
                    :title="row.status === 0 ? '确认停用该用户？停用后将禁止继续新增关联业务' : '确认启用该用户？'"
                    confirm-button-text="确定"
                    cancel-button-text="取消"
                    :icon-color="row.status === 0 ? '#e6a23c' : '#67c23a'"
                    @confirm="handleToggleStatus(row)"
                  >
                    <template #reference>
                      <el-button
                        :type="row.status === 0 ? 'warning' : 'success'"
                        link
                        class="action-icon-btn"
                        :title="row.status === 0 ? '停用' : '启用'"
                      >
                        <el-icon :size="15"><component :is="row.status === 0 ? CircleClose : CircleCheck" /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>

                  <el-popconfirm
                    title="确认删除该用户？关联的子用户、银行卡及历史账单/流水需先处理。"
                    confirm-button-text="确定"
                    cancel-button-text="取消"
                    icon-color="#f56c6c"
                    @confirm="handleDeleteUser(row.id)"
                  >
                    <template #reference>
                      <el-button type="danger" link class="action-icon-btn" title="删除">
                        <el-icon :size="15"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else-if="!loading" description="暂无持卡人数据">
            <template #image>
              <el-icon :size="58" color="#c0c7d6"><UserFilled /></el-icon>
            </template>
            <el-button type="primary" :icon="Plus" @click="openAddTopUser">立即添加</el-button>
          </el-empty>
        </div>
      </section>

      <aside class="action-panel card-shell">
        <div class="panel-head">
          <div>
            <div class="panel-title">功能操作区</div>
            <div class="panel-desc">集中常用动作与视图摘要</div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">快捷操作</div>
          <div class="action-stack">
            <el-button type="primary" :icon="Plus" @click="openAddTopUser">新增</el-button>
            <el-button :icon="expandAll ? Fold : Expand" @click="toggleExpandAll">{{ expandAll ? '收起' : '展开' }}</el-button>
            <el-button :icon="RefreshRight" @click="fetchData">刷新</el-button>
            <el-button @click="resetQuery">清空</el-button>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">当前视图</div>
          <div class="summary-list">
            <div class="summary-item">
              <span>当前分类</span>
              <strong>{{ activeCategoryLabel }}</strong>
            </div>
            <div class="summary-item">
              <span>展示人数</span>
              <strong>{{ currentSummary.total }}</strong>
            </div>
            <div class="summary-item">
              <span>主账户</span>
              <strong>{{ currentSummary.main }}</strong>
            </div>
            <div class="summary-item">
              <span>子用户</span>
              <strong>{{ currentSummary.child }}</strong>
            </div>
          </div>
        </div>

        <div class="side-card">
          <div class="side-card-title">状态说明</div>
          <div class="legend-list">
            <div class="legend-item">
              <span class="legend-dot success" />
              <span>正常</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot danger" />
              <span>停用</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot primary" />
              <span>主账户</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot muted" />
              <span>子用户</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

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

import { ref, reactive, computed, watch, onMounted, onActivated, nextTick } from 'vue'
import {
  Plus,
  UserFilled,
  User,
  Phone,
  EditPen,
  Delete,
  CirclePlus,
  Clock,
  Document,
  Connection,
  CircleClose,
  CircleCheck,
  Search,
  RefreshRight,
  Fold,
  Expand,
  CreditCard
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getUserTreeApi,
  saveUserApi,
  updateUserApi,
  deleteUserApi,
  updateUserFeeRateApi,
  getUserListActiveApi
} from '@/api/card'

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

type QuickMenuKey = 'all' | 'main' | 'child' | 'active' | 'disabled' | 'withCard' | 'noCard'

const loading = ref(false)
const treeData = ref<UserData[]>([])
const rawData = ref<UserData[]>([])
const tableRef = ref()
const expandAll = ref(false)
const activeCategory = ref<QuickMenuKey>('all')
const expandedRowIds = ref<number[]>([])

const query = reactive({
  name: '',
  phone: '',
  status: undefined as number | undefined,
  type: '' as '' | 'main' | 'child',
  hasCard: '' as '' | 0 | 1
})

const editingFeeId = ref<number | null>(null)
const editingFeeRate = ref(0)
const savingFee = ref(false)

const editVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const editTitle = ref('新增主账户')
const inheritedRate = ref(0)
const isChildMode = ref(false)
const parentUserOptions = ref<{ id: number; name: string }[]>([])

const defaultForm: Record<string, any> = {
  id: undefined,
  parentId: null,
  name: '',
  phone: '',
  feeRate: 0,
  remark: '',
  status: 0
}

const editForm = reactive({ ...defaultForm })
const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  feeRate: [{ required: true, message: '请输入手续费率', trigger: 'blur' }]
}

const statCards = computed(() => {
  const flat = flattenTree(rawData.value)
  const total = flat.length
  const active = flat.filter((u) => u.status === 0).length
  const mainAccounts = flat.filter((u) => !u.parentId).length
  const totalCards = flat.reduce((sum, u) => sum + (u.cardCount || 0), 0)

  return [
    { value: total, label: '人数', icon: UserFilled, bg: '#eaf2ff', color: '#0958d9' },
    { value: active, label: '正常', icon: CircleCheck, bg: '#e8f7ed', color: '#2f9e44' },
    { value: mainAccounts, label: '主户', icon: User, bg: '#fff4db', color: '#d97706' },
    { value: totalCards, label: '卡片', icon: CreditCard, bg: '#fdebec', color: '#cf1322' }
  ]
})

const quickMenus = computed(() => {
  const flat = flattenTree(rawData.value)
  return [
    { key: 'all' as QuickMenuKey, label: '全部', count: flat.length, color: '#0958d9' },
    { key: 'main' as QuickMenuKey, label: '主账户', count: flat.filter((u) => !u.parentId).length, color: '#d97706' },
    { key: 'child' as QuickMenuKey, label: '子用户', count: flat.filter((u) => !!u.parentId).length, color: '#7c8799' },
    { key: 'active' as QuickMenuKey, label: '正常', count: flat.filter((u) => u.status === 0).length, color: '#2f9e44' },
    { key: 'disabled' as QuickMenuKey, label: '停用', count: flat.filter((u) => u.status === 1).length, color: '#cf1322' },
    { key: 'withCard' as QuickMenuKey, label: '已绑卡', count: flat.filter((u) => (u.cardCount || 0) > 0).length, color: '#5b8ff9' },
    { key: 'noCard' as QuickMenuKey, label: '未绑卡', count: flat.filter((u) => (u.cardCount || 0) === 0).length, color: '#98a2b3' }
  ]
})

const activeCategoryLabel = computed(() => quickMenus.value.find((item) => item.key === activeCategory.value)?.label || '全部')
const filteredCount = computed(() => flattenTree(treeData.value).length)
const currentSummary = computed(() => {
  const flat = flattenTree(treeData.value)
  return {
    total: flat.length,
    main: flat.filter((u) => !u.parentId).length,
    child: flat.filter((u) => !!u.parentId).length,
    active: flat.filter((u) => u.status === 0).length
  }
})
const visibleMainCount = computed(() => currentSummary.value.main)
const visibleChildCount = computed(() => currentSummary.value.child)
const visibleActiveCount = computed(() => currentSummary.value.active)

async function fetchData() {
  loading.value = true
  try {
    const [treeRes, activeRes] = await Promise.all([getUserTreeApi(), getUserListActiveApi()]) as any
    rawData.value = treeRes?.data || []

    if (activeRes?.data) {
      parentUserOptions.value = (activeRes.data as UserData[])
        .filter((u) => !u.parentId)
        .map((u) => ({ id: u.id, name: u.name }))
    }

    applyFilter()
  } finally {
    loading.value = false
  }
}

function normalizeTree(list: UserData[]): UserData[] {
  return list.map((item) => {
    const children = item.children?.length ? normalizeTree(item.children) : undefined
    return {
      ...item,
      children,
      hasChildren: !!children?.length
    }
  })
}

function filterTreeBy(list: UserData[], predicate: (item: UserData) => boolean): UserData[] {
  return list
    .map((item) => {
      const children = item.children?.length ? filterTreeBy(item.children, predicate) : undefined
      return {
        ...item,
        children: children?.length ? children : undefined,
        hasChildren: !!children?.length
      }
    })
    .filter((item) => predicate(item) || !!item.children?.length)
}

function matchesCurrentFilters(user: UserData) {
  const name = query.name.trim()
  const phone = query.phone.trim()

  if (name && !String(user.name || '').includes(name)) return false
  if (phone && !String(user.phone || '').includes(phone)) return false
  if (query.status !== undefined && user.status !== query.status) return false
  if (query.type === 'main' && !!user.parentId) return false
  if (query.type === 'child' && !user.parentId) return false
  if (query.hasCard !== '') {
    const hasCard = (user.cardCount || 0) > 0 ? 1 : 0
    if (hasCard !== query.hasCard) return false
  }

  switch (activeCategory.value) {
    case 'main':
      return !user.parentId
    case 'child':
      return !!user.parentId
    case 'active':
      return user.status === 0
    case 'disabled':
      return user.status === 1
    case 'withCard':
      return (user.cardCount || 0) > 0
    case 'noCard':
      return (user.cardCount || 0) === 0
    default:
      return true
  }
}

function applyFilter() {
  const normalized = normalizeTree(rawData.value)
  treeData.value = filterTreeBy(normalized, matchesCurrentFilters)
  expandedRowIds.value = expandAll.value ? collectExpandableIds(treeData.value) : []

  nextTick(() => {
    if (treeData.value.length) {
      setExpandRecursive(treeData.value, expandAll.value)
    }
  })
}

function flattenTree<T extends { children?: T[] }>(list: T[]): T[] {
  const result: T[] = []
  const walk = (items: T[]) => {
    items.forEach((item) => {
      result.push(item)
      if (item.children?.length) walk(item.children)
    })
  }
  walk(list)
  return result
}

function collectExpandableIds(list: UserData[]) {
  return flattenTree(list)
    .filter((item) => item.children?.length)
    .map((item) => item.id)
}

function resetQuery() {
  query.name = ''
  query.phone = ''
  query.status = undefined
  query.type = ''
  query.hasCard = ''
  activeCategory.value = 'all'
  expandAll.value = false
  cancelEditFee()
}

function onExpandChange(row: UserData, expanded: boolean) {
  if (expanded) {
    if (!expandedRowIds.value.includes(row.id)) expandedRowIds.value.push(row.id)
  } else {
    expandedRowIds.value = expandedRowIds.value.filter((id) => id !== row.id)
  }
  syncExpandState()
}

function syncExpandState() {
  const expandableIds = collectExpandableIds(treeData.value)
  expandAll.value = expandableIds.length > 0 && expandableIds.every((id) => expandedRowIds.value.includes(id))
}

function toggleExpandAll() {
  if (!treeData.value.length) return
  expandAll.value = !expandAll.value
  expandedRowIds.value = expandAll.value ? collectExpandableIds(treeData.value) : []
  nextTick(() => setExpandRecursive(treeData.value, expandAll.value))
}

function setExpandRecursive(rows: UserData[], expanded: boolean) {
  rows.forEach((row) => {
    tableRef.value?.toggleRowExpansion(row, expanded)
    if (row.children?.length) setExpandRecursive(row.children, expanded)
  })
}

function rowClassName({ row }: { row: UserData }) {
  return row.status === 1 ? 'row-disabled' : ''
}

function formatTime(time?: string) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

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
  } finally {
    savingFee.value = false
  }
}

function cancelEditFee() {
  editingFeeId.value = null
}

function formatFeeRate(rate: number | string | null | undefined) {
  return Number(rate ?? 0).toFixed(2)
}

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

function openAddTopUser() {
  isChildMode.value = false
  editTitle.value = '新增主账户'
  Object.assign(editForm, defaultForm)
  editVisible.value = true
}

function openAddChild(parentRow: UserData) {
  isChildMode.value = true
  editTitle.value = `添加子用户 - ${parentRow.name}`
  inheritedRate.value = Number(parentRow.effectiveFeeRate ?? parentRow.feeRate ?? 0)
  Object.assign(editForm, { id: undefined, parentId: parentRow.id, name: '', phone: '', feeRate: 0, remark: '', status: 0 })
  editVisible.value = true
}

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

async function handleSaveUser() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

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
  } finally {
    saving.value = false
  }
}

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

async function handleDeleteUser(id: number) {
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，请先处理关联数据')
  }
}

onMounted(() => fetchData())
onActivated(() => fetchData())
watch(query, () => applyFilter(), { deep: true })
watch(activeCategory, () => applyFilter())
</script>

<style scoped>
.card-user-page {
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
  grid-template-columns: minmax(150px, 0.72fr) minmax(300px, 1fr) auto;
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
.user-info {
  min-width: 0;
}

.header-title {
  font-size: 16px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  font-size: 16px;
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

.header-search {
  width: 150px;
}

.action-btn {
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
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.035);
  flex-shrink: 0;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.filter-title {
  padding: 0 2px;
  font-size: 11px;
  font-weight: 700;
  color: #526074;
  white-space: nowrap;
}

.filter-item {
  width: 90px;
}

.filter-phone {
  width: 118px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toolbar-hint {
  font-size: 11px;
  color: #7c8799;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr) 152px;
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
}

.side-panel,
.action-panel,
.data-panel {
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
  display: none;
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

.user-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #fff;
}

:deep(.el-table) {
  height: 100% !important;
  --el-table-border-color: #e5eaf1;
  font-size: 12px;
}

:deep(.el-table .cell) {
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

:deep(.el-table th.el-table__cell) {
  height: 34px;
  font-size: 11px;
}

:deep(.el-table td.el-table__cell) {
  height: 50px;
}

:deep(.el-table__body-wrapper) {
  overflow-y: auto;
  scrollbar-width: none;
}

:deep(.el-table__body-wrapper::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

:deep(.row-disabled) {
  opacity: 0.62;
  background: #fafbfc;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.avatar-wrap {
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

.avatar-child {
  background: linear-gradient(135deg, #f1f4f8 0%, #e4e9f1 100%);
  color: #6b7280;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.name-row :deep(.el-tag) {
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
}

.name-text {
  max-width: 92px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2a37;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark-icon {
  width: 16px;
  height: 16px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #7c8799;
  background: #f2f5f9;
  cursor: pointer;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  min-width: 0;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #667085;
}

.meta-time {
  display: none;
}

.fee-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.fee-edit-wrap {
  display: flex;
  align-items: center;
  gap: 3px;
}

.fee-input {
  width: 64px;
}

.fee-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}

.fee-badge,
.fee-inherit-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
  background: #f3f5f8;
  color: #526074;
  border: 1px solid #e5eaf1;
}

.fee-badge.has-rate {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  border-color: transparent;
  color: #fff;
}

.pct-sign {
  font-size: 11px;
  color: #667085;
}

.card-count-cell,
.status-cell,
.action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.count-badge :deep(.el-badge__content) {
  font-weight: 700;
}

.status-cell :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 700;
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.dot-green {
  background: #2f9e44;
  box-shadow: 0 0 5px rgba(47, 158, 68, 0.3);
}

.dot-red {
  background: #cf1322;
  box-shadow: 0 0 5px rgba(207, 19, 34, 0.3);
}

.action-cell {
  gap: 0;
}

.action-icon-btn {
  width: 24px;
  height: 24px;
  padding: 0;
}

.no-data {
  font-size: 12px;
  color: #c0c7d6;
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
.legend-list {
  display: grid;
  gap: 6px;
}

.summary-item,
.legend-item {
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

.legend-item {
  justify-content: flex-start;
}

.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot.success {
  background: #2f9e44;
}

.legend-dot.danger {
  background: #cf1322;
}

.legend-dot.primary {
  background: #0958d9;
}

.legend-dot.muted {
  background: #98a2b3;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #d97706;
}

.inherit-hint {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #667085;
  font-size: 12px;
}

@media (max-width: 1480px) {
  .page-header {
    grid-template-columns: minmax(140px, 0.72fr) minmax(260px, 1fr) auto;
  }

  .workspace-grid {
    grid-template-columns: 132px minmax(0, 1fr) 144px;
  }

  .header-search {
    width: 132px;
  }
}

@media (max-width: 1320px) {
  .header-subtitle,
  .inline-summary,
  .toolbar-hint {
    display: none;
  }

  .workspace-grid {
    grid-template-columns: 124px minmax(0, 1fr) 136px;
  }
}
</style>
