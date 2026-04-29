<template>
  <div class="card-user-page">
    <div class="page-header">
      <div class="header-copy">
        <div class="header-title">用户信息</div>
      </div>

      <div class="header-actions">
        <el-button class="action-btn" :icon="RefreshRight" @click="refreshData">刷新</el-button>
      </div>
    </div>

    <div class="app-search-panel card-shell user-search-panel">
      <div class="app-search-main">
        <div class="app-search-title">检索</div>
        <el-input
          v-model="query.name"
          class="app-search-item app-search-item-xl"
          placeholder="请输入用户名称或手机号"
          clearable
          maxlength="30"
        />
        <div class="app-search-actions">
          <el-button link type="primary" :icon="expandAll ? Fold : Expand" @click="toggleExpandAll">
            {{ expandAll ? '收起全部' : '展开全部' }}
          </el-button>
          <el-button class="app-search-btn" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <div class="app-search-extra quick-menu-bar">
        <div class="menu-list menu-list-inline">
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
      </div>
    </div>

    <div class="workspace-grid">
      <section class="data-panel card-shell">
        <div class="panel-head data-head">
          <div class="panel-title">用户列表</div>
          <div class="data-head-actions">
            <el-button type="primary" class="action-btn" :icon="Plus" @click="openAddTopUser">新增用户</el-button>
          </div>
        </div>

        <div ref="tableWrapRef" class="user-table-wrap" :class="{ expanded: expandAll }" :style="tableStyleVars">
          <div v-if="showTableSkeleton" class="user-table-skeleton">
            <el-skeleton animated>
              <template #template>
                <div class="user-skeleton-head">
                  <el-skeleton-item variant="text" style="width: 16%" />
                  <el-skeleton-item variant="text" style="width: 12%" />
                  <el-skeleton-item variant="text" style="width: 12%" />
                  <el-skeleton-item variant="text" style="width: 10%" />
                  <el-skeleton-item variant="text" style="width: 8%" />
                  <el-skeleton-item variant="text" style="width: 10%" />
                  <el-skeleton-item variant="text" style="width: 14%" />
                  <el-skeleton-item variant="text" style="width: 10%" />
                </div>
                <div v-for="n in 8" :key="n" class="user-skeleton-row">
                  <el-skeleton-item variant="rect" style="height: 100%; width: 100%" />
                </div>
              </template>
            </el-skeleton>
          </div>

          <el-table
            v-else-if="treeData.length"
            ref="tableRef"
            :data="treeData"
            row-key="id"
            border
            :height="tableHeight"
            table-layout="fixed"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            :header-cell-style="{ background: '#f7f9fc', color: '#5b6472', fontWeight: '600', padding: '0' }"
            :row-class-name="rowClassName"
            :cell-style="{ padding: '0' }"
            @expand-change="onExpandChange"
          >
            <el-table-column prop="name" label="姓名/角色">
              <template #default="{ row }">
                <div class="user-cell">
                  <div class="avatar-wrap" :class="{ 'avatar-child': !!row.parentId }">
                    <el-icon v-if="!row.parentId" :size="16"><UserFilled /></el-icon>
                    <el-icon v-else :size="15"><User /></el-icon>
                  </div>

                  <div class="user-info">
                    <div class="name-row">
                      <span class="name-text">{{ row.name }}</span>
                      <el-tag v-if="!row.parentId" type="warning" size="small" effect="light">洽谈人</el-tag>
                      <el-tag v-else type="info" size="small" effect="plain">名下持卡人</el-tag>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="phone" label="联系电话" align="center">
              <template #default="{ row }">
                <span class="plain-cell phone-cell">
                  <span class="phone-cell-inner">
                    <span class="phone-icon">
                      <el-icon :size="12"><Phone /></el-icon>
                    </span>
                    <span class="phone-text" :class="{ empty: !row.phone }">{{ row.phone || '—' }}</span>
                  </span>
                </span>
              </template>
            </el-table-column>

            <el-table-column label="归属洽谈人" align="center">
              <template #default="{ row }">
                <span class="plain-cell relation-cell">
                  {{ row.parentId ? (row.parentName || '未指定') : '本人' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="费率" align="center">
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

            <el-table-column prop="cardCount" label="卡数" align="center">
              <template #default="{ row }">
                <div class="card-count-cell">
                  <el-badge v-if="row.cardCount > 0" :value="row.cardCount" :max="99" type="primary" class="count-badge" />
                  <span v-else class="no-data">0</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
                    <span class="status-dot" :class="row.status === 0 ? 'dot-green' : 'dot-red'" />
                    {{ row.status === 0 ? '正常' : '停用' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="createTime" label="创建时间" align="center">
              <template #default="{ row }">
                <span class="plain-cell time-cell">
                  <el-icon :size="12"><Clock /></el-icon>{{ formatTime(row.createTime) || '—' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column prop="remark" label="备注" align="center">
              <template #default="{ row }">
                <span class="plain-cell remark-cell">{{ row.remark || '—' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" align="center">
              <template #default="{ row }">
                <div class="action-cell">
                  <el-button
                    v-if="!row.parentId"
                    type="success"
                    link
                    class="action-icon-btn"
                    title="添加名下持卡人"
                    @click="openAddChild(row)"
                  >
                    <el-icon :size="16"><CirclePlus /></el-icon>
                  </el-button>

                  <el-button type="primary" link class="action-icon-btn" title="编辑" @click="openEditUser(row)">
                    <el-icon :size="16"><EditPen /></el-icon>
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
                        <el-icon :size="16"><component :is="row.status === 0 ? CircleClose : CircleCheck" /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>

                  <el-popconfirm
                    title="确认删除该用户？关联的名下持卡人、银行卡及历史账单/流水需先处理。"
                    confirm-button-text="确定"
                    cancel-button-text="取消"
                    icon-color="#f56c6c"
                    @confirm="handleDeleteUser(row.id)"
                  >
                    <template #reference>
                      <el-button type="danger" link class="action-icon-btn" title="删除">
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else-if="dataReady" description="暂无用户信息">
            <template #image>
              <el-icon :size="58" color="#c0c7d6"><UserFilled /></el-icon>
            </template>
            <el-button type="primary" :icon="Plus" @click="openAddTopUser">立即添加</el-button>
          </el-empty>
        </div>

        <div v-if="filteredTotal > 0" class="user-pagination">
          <div class="pagination-meta">
            <span>一共 {{ filteredTotal }} 条</span>
            <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
            <span>一页 {{ query.pageSize }} 条</span>
          </div>
          <el-pagination
            background
            small
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            :page-sizes="pageSizeOptions"
            :total="filteredTotal"
            layout="sizes, prev, pager, next, jumper"
            @current-change="handleCurrentChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="editVisible" :title="editTitle" width="620px" class="user-edit-dialog" destroy-on-close :close-on-click-modal="false">
      <div class="user-edit-panel">
        <el-form class="user-edit-form" :model="editForm" label-width="90px" ref="formRef" :rules="formRules" size="large">
          <el-form-item label="用户类型" v-if="!editForm.id" class="type-form-item">
            <div class="user-type-block">
              <template v-if="childModeLocked">
                <div class="user-type-fixed">
                  <span class="user-type-fixed-tag">名下持卡人</span>
                  <span class="user-type-fixed-text">当前从洽谈人下创建，类型已固定。</span>
                </div>
              </template>
              <el-radio-group v-else v-model="isChildMode" :disabled="!!editForm.id" class="user-type-group">
                <el-radio-button :label="false">洽谈人</el-radio-button>
                <el-radio-button :label="true">名下持卡人</el-radio-button>
              </el-radio-group>
              <div class="type-inline-tip">
                {{ isChildMode ? '名下持卡人将继承所属洽谈人的手续费率。' : '洽谈人可单独设置手续费率，并同步名下持卡人。' }}
              </div>
            </div>
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="editForm.name" placeholder="请输入姓名" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item label="所属洽谈人" v-if="isChildMode || editForm.parentId">
            <el-select v-model="editForm.parentId" placeholder="选择洽谈人" clearable style="width:100%" :disabled="!!editForm.parentId">
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
            <div class="form-tip">修改此值将级联同步更新所有名下持卡人</div>
          </el-form-item>
          <el-form-item label="手续费率" v-else-if="isChildMode || editForm.parentId">
            <span class="inherit-hint">继承自洽谈人（{{ formatFeeRate(inheritedRate) }}%）</span>
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
      </div>
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

import { ref, reactive, computed, watch, onMounted, onActivated, onBeforeUnmount, nextTick } from 'vue'
import {
  Plus,
  UserFilled,
  User,
  Phone,
  EditPen,
  Delete,
  CirclePlus,
  Clock,
  Connection,
  CircleClose,
  CircleCheck,
  RefreshRight,
  Fold,
  Expand
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

interface CardUserViewCache {
  savedAt: number
  rawData: UserData[]
  parentUserOptions: { id: number; name: string }[]
}

type QuickMenuKey = 'all' | 'main' | 'child' | 'active' | 'disabled' | 'withCard' | 'noCard'

const DEFAULT_PAGE_SIZE = 10
const USER_HEADER_HEIGHT = 20
const USER_ROW_HEIGHT = 42
const USER_FONT_SIZE = 12
const USER_AVATAR_SIZE = 18
const VIEW_CACHE_TTL = 30 * 1000
const VIEW_CACHE_KEY = 'card-user-list-cache-v1'

function createDebouncedTask(fn: () => void, delay = 250) {
  let timer = 0
  const run = () => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      timer = 0
      fn()
    }, delay)
  }
  run.cancel = () => {
    if (timer) {
      window.clearTimeout(timer)
      timer = 0
    }
  }
  return run
}

function readViewCache(): CardUserViewCache | null {
  try {
    const raw = window.sessionStorage.getItem(VIEW_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as CardUserViewCache
    if (!parsed || !Array.isArray(parsed.rawData) || !Array.isArray(parsed.parentUserOptions)) return null
    return parsed
  } catch {
    return null
  }
}

function writeViewCache(rawData: UserData[], parentUserOptions: { id: number; name: string }[]) {
  try {
    const payload: CardUserViewCache = {
      savedAt: Date.now(),
      rawData,
      parentUserOptions
    }
    window.sessionStorage.setItem(VIEW_CACHE_KEY, JSON.stringify(payload))
  } catch {
    /* ignore cache write errors */
  }
}

const initialCache = readViewCache()
const loading = ref(false)
const hasActivatedOnce = ref(false)
const treeData = ref<UserData[]>([])
const rawData = ref<UserData[]>(initialCache?.rawData || [])
const filteredTotal = ref(0)
const tableRef = ref()
const tableWrapRef = ref<HTMLElement | null>(null)
const tableWrapHeight = ref(0)
const expandAll = ref(false)
const activeCategory = ref<QuickMenuKey>('all')
const expandedRowIds = ref<number[]>([])
const dataReady = ref(!!initialCache)
const lastFetchedAt = ref(initialCache?.savedAt || 0)
let tableResizeObserver: ResizeObserver | undefined
let tableLayoutFrame = 0
let dataLoadingPromise: Promise<void> | null = null
let dataRequestSeq = 0
let syncingFilterQuery = false
let syncingExpandedRows = false

const query = reactive({
  name: '',
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const triggerFilterSearch = createDebouncedTask(() => {
  handleFilterSearch()
})

const editingFeeId = ref<number | null>(null)
const editingFeeRate = ref(0)
const savingFee = ref(false)
const pageSizeManuallyChanged = ref(false)
const autoPageSize = ref(DEFAULT_PAGE_SIZE)

const editVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const editTitle = ref('新增用户')
const inheritedRate = ref(0)
const isChildMode = ref(false)
const childModeLocked = ref(false)
const parentUserOptions = ref<{ id: number; name: string }[]>(initialCache?.parentUserOptions || [])

const defaultForm: Record<string, any> = {
  id: undefined,
  parentId: null,
  name: '',
  phone: '',
  feeRate: 1,
  remark: '',
  status: 0
}

const editForm = reactive({ ...defaultForm })
const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  feeRate: [{ required: true, message: '请输入手续费率', trigger: 'blur' }]
}

const quickMenus = computed(() => {
  const flat = flattenTree(rawData.value)
  return [
    { key: 'all' as QuickMenuKey, label: '全部', count: flat.length, color: '#0958d9' },
    { key: 'main' as QuickMenuKey, label: '洽谈人', count: flat.filter((u) => !u.parentId).length, color: '#d97706' },
    { key: 'child' as QuickMenuKey, label: '名下持卡人', count: flat.filter((u) => !!u.parentId).length, color: '#7c8799' },
    { key: 'active' as QuickMenuKey, label: '正常', count: flat.filter((u) => u.status === 0).length, color: '#2f9e44' },
    { key: 'disabled' as QuickMenuKey, label: '停用', count: flat.filter((u) => u.status === 1).length, color: '#cf1322' },
    { key: 'withCard' as QuickMenuKey, label: '已绑卡', count: flat.filter((u) => (u.cardCount || 0) > 0).length, color: '#5b8ff9' },
    { key: 'noCard' as QuickMenuKey, label: '未绑卡', count: flat.filter((u) => (u.cardCount || 0) === 0).length, color: '#98a2b3' }
  ]
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredTotal.value / query.pageSize)))
const visibleRowCount = computed(() => Math.max(collectVisibleRows(treeData.value).length, 1))
const visibleRowIndexMap = computed(() => {
  const indexMap = new Map<number, number>()
  collectVisibleRows(treeData.value).forEach((row, index) => {
    indexMap.set(row.id, index)
  })
  return indexMap
})
const showTableSkeleton = computed(() => loading.value && !dataReady.value && treeData.value.length === 0)
const tableHeight = computed(() => '100%')
const pageSizeOptions = computed(() => Array.from(new Set([10, autoPageSize.value, 15, 20, 30].filter((size) => size > 0))).sort((a, b) => a - b))
const tableStyleVars = computed(() => {
  return {
    '--user-header-height': `${USER_HEADER_HEIGHT}px`,
    '--user-row-height': `${USER_ROW_HEIGHT}px`,
    '--user-font-size': `${USER_FONT_SIZE}px`,
    '--user-avatar-size': `${USER_AVATAR_SIZE}px`,
    '--user-line-height': '1.1'
  }
})

function calculateAutoPageSize() {
  if (!tableWrapHeight.value || expandAll.value) return DEFAULT_PAGE_SIZE
  const availableHeight = Math.max(tableWrapHeight.value - USER_HEADER_HEIGHT - 2, USER_ROW_HEIGHT * DEFAULT_PAGE_SIZE)
  return Math.max(DEFAULT_PAGE_SIZE, Math.floor(availableHeight / USER_ROW_HEIGHT))
}

function syncAutoPageSize() {
  autoPageSize.value = calculateAutoPageSize()
  if (pageSizeManuallyChanged.value || expandAll.value || query.pageSize === autoPageSize.value) return

  const firstItemIndex = Math.max((query.pageNum - 1) * query.pageSize, 0)
  query.pageSize = autoPageSize.value
  query.pageNum = Math.floor(firstItemIndex / autoPageSize.value) + 1
}

function updateTableLayout() {
  if (tableLayoutFrame) return
  tableLayoutFrame = window.requestAnimationFrame(() => {
    tableLayoutFrame = 0
    tableWrapHeight.value = tableWrapRef.value?.clientHeight || 0
    syncAutoPageSize()
    tableRef.value?.doLayout?.()
  })
}

function initTableResize() {
  tableResizeObserver?.disconnect()
  if (tableWrapRef.value) {
    tableResizeObserver = new ResizeObserver(() => updateTableLayout())
    tableResizeObserver.observe(tableWrapRef.value)
  }
  updateTableLayout()
}

type FetchDataOptions = { silent?: boolean; force?: boolean }

async function fetchData(options: FetchDataOptions = {}) {
  if (dataLoadingPromise && !options.force) return dataLoadingPromise

  const requestSeq = ++dataRequestSeq
  loading.value = true

  const promise = (async () => {
    try {
      const [treeRes, activeRes] = await Promise.all([getUserTreeApi(), getUserListActiveApi()]) as any
      if (requestSeq !== dataRequestSeq) return

      rawData.value = treeRes?.data || []

      if (activeRes?.data) {
        parentUserOptions.value = (activeRes.data as UserData[])
          .filter((u) => !u.parentId)
          .map((u) => ({ id: u.id, name: u.name }))
      }

      dataReady.value = true
      lastFetchedAt.value = Date.now()
      writeViewCache(rawData.value, parentUserOptions.value)
      applyFilter()
    } catch (error) {
      if (requestSeq === dataRequestSeq && !dataReady.value) {
        dataReady.value = true
      }
      throw error
    } finally {
      if (requestSeq === dataRequestSeq) {
        loading.value = false
        dataLoadingPromise = null
        nextTick(updateTableLayout)
      }
    }
  })()

  dataLoadingPromise = promise
  return promise
}

function refreshData() {
  fetchData({ force: true })
}

function syncLocalFeeRate(userId: number, feeRate: number) {
  let updated = false

  rawData.value = rawData.value.map((user) => {
    if (user.id !== userId) return user

    updated = true
    return {
      ...user,
      feeRate,
      effectiveFeeRate: feeRate,
      children: user.children?.map((child) => ({
        ...child,
        feeRate: 0,
        effectiveFeeRate: feeRate
      }))
    }
  })

  if (!updated) return

  writeViewCache(rawData.value, parentUserOptions.value)
  applyFilter()
}

function normalizeTree(list: UserData[]): UserData[] {
  return [...list]
    .sort(compareUsersByCreateTimeDesc)
    .map((item) => {
      const children = item.children?.length ? normalizeTree(item.children) : undefined
      return {
        ...item,
        children,
        hasChildren: !!children?.length
      }
    })
}

function compareUsersByCreateTimeDesc(a: UserData, b: UserData) {
  const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
  const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
  return timeB - timeA
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
  const keyword = query.name.trim()

  if (keyword) {
    const nameMatched = String(user.name || '').includes(keyword)
    const phoneMatched = String(user.phone || '').includes(keyword)
    if (!nameMatched && !phoneMatched) return false
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
  const filtered = filterTreeBy(normalized, matchesCurrentFilters)
  filteredTotal.value = filtered.length

  const maxPage = Math.max(1, Math.ceil(filteredTotal.value / query.pageSize))
  if (query.pageNum > maxPage) query.pageNum = maxPage

  const start = (query.pageNum - 1) * query.pageSize
  treeData.value = filtered.slice(start, start + query.pageSize)
  expandedRowIds.value = expandAll.value
    ? collectExpandableIds(treeData.value)
    : Array.from(new Set([
        ...collectContextExpandIds(treeData.value),
        ...collectNameMatchExpandIds(treeData.value, query.name)
      ]))

  nextTick(() => {
    if (treeData.value.length) {
      applyExpandedRows()
    }
    updateTableLayout()
  })
}

if (rawData.value.length) {
  applyFilter()
}

function collectVisibleRows(list: UserData[]) {
  const rows: UserData[] = []
  const walk = (items: UserData[]) => {
    items.forEach((item) => {
      rows.push(item)
      if (item.children?.length && expandedRowIds.value.includes(item.id)) walk(item.children)
    })
  }
  walk(list)
  return rows
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

function collectNameMatchExpandIds(list: UserData[], keyword: string) {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) return []

  const matchedParentIds = new Set<number>()
  const walk = (item: UserData): boolean => {
    const selfMatched = String(item.name || '').includes(normalizedKeyword) || String(item.phone || '').includes(normalizedKeyword)
    const childMatched = !!item.children?.some((child) => walk(child))
    if (childMatched && item.children?.length) matchedParentIds.add(item.id)
    return selfMatched || childMatched
  }

  list.forEach((item) => walk(item))
  return Array.from(matchedParentIds)
}

function collectContextExpandIds(list: UserData[]) {
  if (activeCategory.value !== 'child') {
    return []
  }
  return collectExpandableIds(list)
}

function resetQuery() {
  triggerFilterSearch.cancel()
  syncingFilterQuery = true
  query.name = ''
  query.pageNum = 1
  pageSizeManuallyChanged.value = false
  query.pageSize = autoPageSize.value
  activeCategory.value = 'all'
  expandAll.value = false
  syncingFilterQuery = false
  cancelEditFee()
  applyFilter()
}

function handleFilterSearch() {
  cancelEditFee()
  if (query.pageNum !== 1) {
    query.pageNum = 1
    return
  }
  applyFilter()
}

function handleCurrentChange(pageNum: number) {
  query.pageNum = pageNum
}

function handlePageSizeChange(pageSize: number) {
  pageSizeManuallyChanged.value = true
  query.pageSize = pageSize
  query.pageNum = 1
}

function onExpandChange(row: UserData, expanded: boolean) {
  if (syncingExpandedRows) return
  if (expanded) {
    if (!expandedRowIds.value.includes(row.id)) expandedRowIds.value.push(row.id)
  } else {
    expandedRowIds.value = expandedRowIds.value.filter((id) => id !== row.id)
  }
  syncExpandState()
  nextTick(() => {
    updateTableLayout()
    if (expanded) {
      window.requestAnimationFrame(() => ensureExpandedRowVisible(row))
    }
  })
}

function syncExpandState() {
  expandAll.value = expandedRowIds.value.length > 0
}

function toggleExpandAll() {
  if (!treeData.value.length) return
  expandAll.value = !expandAll.value
  expandedRowIds.value = expandAll.value ? collectExpandableIds(treeData.value) : []
  nextTick(() => {
    window.requestAnimationFrame(() => {
      if (expandAll.value) {
        applyExpandedRows()
      } else {
        collapseAllExpandedRows()
      }
      updateTableLayout()
    })
  })
}

function applyExpandedRows() {
  const expandedIds = new Set(expandedRowIds.value)
  const rows = flattenTree(treeData.value).filter((row) => row.children?.length)

  syncingExpandedRows = true
  try {
    rows.forEach((row) => {
      tableRef.value?.toggleRowExpansion(row, expandedIds.has(row.id))
    })
  } finally {
    syncingExpandedRows = false
  }
}

function collapseAllExpandedRows() {
  const rows = flattenTree(treeData.value)
    .filter((row) => row.children?.length)
    .reverse()

  syncingExpandedRows = true
  try {
    rows.forEach((row) => {
      tableRef.value?.toggleRowExpansion(row, false)
    })
  } finally {
    syncingExpandedRows = false
  }
}

function getTableScrollContainer() {
  return (tableWrapRef.value?.querySelector('.el-scrollbar__wrap') as HTMLElement | null) || tableWrapRef.value
}

function ensureExpandedRowVisible(row: UserData) {
  const container = getTableScrollContainer()
  const rowEl = tableWrapRef.value?.querySelector(`.user-row-${row.id}`) as HTMLElement | null
  if (!container || !rowEl) return

  const containerRect = container.getBoundingClientRect()
  const rowRect = rowEl.getBoundingClientRect()
  const childCount = row.children?.length || 0
  if (!childCount) return

  const previewChildCount = Math.min(childCount, 3)
  const desiredBottom = rowRect.bottom + previewChildCount * USER_ROW_HEIGHT + 8

  if (desiredBottom > containerRect.bottom) {
    container.scrollTop += desiredBottom - containerRect.bottom
  }
}

function rowClassName({ row }: { row: UserData }) {
  const visibleIndex = visibleRowIndexMap.value.get(row.id) ?? 0
  const classNames = [`user-row-${row.id}`, visibleIndex % 2 === 0 ? 'row-even' : 'row-odd']
  if (row.status === 1) classNames.push('row-disabled')
  return classNames.join(' ')
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
    const nextFeeRate = Number(formatFeeRate(editingFeeRate.value))
    await updateUserFeeRateApi({ userId: row.id, feeRate: nextFeeRate })
    syncLocalFeeRate(row.id, nextFeeRate)
    ElMessage.success('费率已更新并同步到所有名下持卡人')
    cancelEditFee()
    void fetchData({ force: true })
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
  childModeLocked.value = false
  isChildMode.value = false
  editTitle.value = '新增用户'
  Object.assign(editForm, defaultForm)
  editVisible.value = true
}

function openAddChild(parentRow: UserData) {
  childModeLocked.value = true
  isChildMode.value = true
  editTitle.value = `添加名下持卡人 - ${parentRow.name}`
  inheritedRate.value = Number(parentRow.effectiveFeeRate ?? parentRow.feeRate ?? 0)
  Object.assign(editForm, { id: undefined, parentId: parentRow.id, name: '', phone: '', feeRate: 0, remark: '', status: 0 })
  editVisible.value = true
}

function openEditUser(row: UserData) {
  childModeLocked.value = false
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
    fetchData({ force: true })
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
  fetchData({ force: true })
}

async function handleDeleteUser(id: number) {
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    fetchData({ force: true })
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，请先处理关联数据')
  }
}

onMounted(() => {
  // 始终拉取最新数据，sessionStorage 缓存仅用于渲染初始占位，不阻止请求
  fetchData({ silent: true })
  nextTick(initTableResize)
  window.addEventListener('resize', updateTableLayout)
})
onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true
    nextTick(updateTableLayout)
    return
  }
  // 切换回此页面时始终刷新数据
  fetchData({ silent: true })
  nextTick(updateTableLayout)
})
onBeforeUnmount(() => {
  triggerFilterSearch.cancel()
  tableResizeObserver?.disconnect()
  if (tableLayoutFrame) {
    window.cancelAnimationFrame(tableLayoutFrame)
    tableLayoutFrame = 0
  }
  window.removeEventListener('resize', updateTableLayout)
})
watch(
  () => activeCategory.value,
  () => {
    query.pageNum = 1
    applyFilter()
  }
)
watch(
  () => query.name,
  () => {
    if (syncingFilterQuery) return
    triggerFilterSearch()
  },
  { flush: 'sync' }
)
watch(() => [query.pageNum, query.pageSize], () => applyFilter())
watch(visibleRowCount, () => nextTick(updateTableLayout))
</script>

<style scoped>
.card-user-page {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 0;
  background: #f5f7fb;
  overflow: hidden;
  box-sizing: border-box;
  --user-header-height: 22px;
  --user-row-height: 24px;
  --user-font-size: 10px;
  --user-avatar-size: 18px;
  --user-line-height: 1.1;
}

.card-shell {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.035);
}

.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98) 0%, rgba(248,250,253,0.98) 100%);
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.03);
  flex-shrink: 0;
}

.header-copy,
.user-info {
  min-width: 0;
}

.header-title {
  font-size: 14px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-search-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex-shrink: 0;
  gap: 6px 10px;
  padding: 6px 8px;
}

.user-search-panel .app-search-main {
  justify-content: space-between;
  gap: 6px;
}

.user-search-panel .app-search-extra,
.user-search-panel .app-search-actions {
  gap: 6px;
}

.user-search-panel .app-search-title {
  min-height: 28px;
  font-size: 11px;
}

.user-search-panel .app-search-item-xl {
  width: auto;
  flex: 1 1 360px;
  min-width: 220px;
}

.user-search-panel .app-search-actions {
  margin-left: auto;
}

.quick-menu-bar {
  align-items: stretch;
  justify-content: flex-start;
  padding-top: 4px;
  border-top: 1px dashed #e5eaf1;
}

.user-search-panel .app-search-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
}

/*noinspection CssUnusedSymbol*/
.user-search-panel :deep(.el-input__wrapper),
.user-search-panel :deep(.el-select__wrapper),
.user-search-panel :deep(.el-date-editor.el-input__wrapper),
.user-search-panel :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
  border-radius: 8px;
}

/*noinspection CssUnusedSymbol*/
.user-search-panel :deep(.el-input__inner),
.user-search-panel :deep(.el-select__selected-item),
.user-search-panel :deep(.el-range-input),
.user-search-panel :deep(.el-input-number__input),
.user-search-panel :deep(.el-date-editor .el-range-separator) {
  font-size: 12px;
}

.action-btn {
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 13px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.data-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.data-panel {
  padding: 4px;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 4px;
  flex-shrink: 0;
}

.panel-title {
  font-size: 12px;
  font-weight: 700;
  color: #1f2a37;
  line-height: 1.1;
}

.panel-desc {
  display: none;
}

.menu-list {
  display: grid;
  gap: 3px;
  overflow: hidden;
}

.menu-list-inline {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
  width: 100%;
}

.menu-item {
  display: grid;
  grid-template-columns: 8px 1fr auto;
  align-items: center;
  gap: 6px;
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(218, 226, 236, 0.92);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.99) 0%, rgba(248,250,253,0.97) 100%);
  color: #55657b;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 6px 16px rgba(15,23,42,0.05);
}

.menu-item:hover {
  transform: translateY(-1px);
  border-color: rgba(180, 206, 255, 0.92);
  box-shadow: 0 10px 18px rgba(15,23,42,0.07);
}

.menu-item.active {
  transform: translateY(-1px);
  border-color: transparent;
  background: linear-gradient(180deg, rgba(255,255,255,0.99) 0%, rgba(234,242,255,0.95) 160%);
  color: #0958d9;
  box-shadow: inset 0 0 0 1.5px rgba(180, 206, 255, 0.85), 0 12px 22px rgba(15,23,42,0.08);
}

.menu-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.menu-label {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-count {
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 999px;
  background: rgba(9, 88, 217, 0.1);
  color: inherit;
  font-size: 11px;
  line-height: 20px;
  text-align: center;
}

.data-head {
  align-items: center;
}

.data-head-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-table-wrap {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #fff;
  scrollbar-width: thin;
  scrollbar-color: #cdd6e3 transparent;
  scroll-behavior: smooth;
}

.user-table-wrap.expanded {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

.user-table-wrap::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.user-table-wrap::-webkit-scrollbar-thumb {
  background: #cdd6e3;
  border-radius: 999px;
}

.user-table-wrap::-webkit-scrollbar-track {
  background: transparent;
}

.user-table-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  padding: 10px 12px;
  box-sizing: border-box;
}

.user-skeleton-head {
  display: grid;
  grid-template-columns: 1.6fr 1.1fr 1fr .9fr .7fr .9fr 1.2fr .9fr;
  align-items: center;
  gap: 10px;
  height: 20px;
}

.user-skeleton-row {
  height: 34px;
  border-radius: 8px;
  overflow: hidden;
}

.user-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  padding-top: 3px;
  flex-shrink: 0;
}

.pagination-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  color: #667085;
  white-space: nowrap;
}

/*noinspection CssUnusedSymbol*/
.user-pagination :deep(.el-pagination) {
  --el-pagination-button-height: 20px;
  --el-pagination-button-width: 20px;
  font-size: 10px;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table) {
  --el-table-border-color: #e5eaf1;
  font-size: var(--user-font-size);
  overflow: visible !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__inner-wrapper),
:deep(.el-table__header-wrapper),
:deep(.el-table__body-wrapper),
:deep(.el-scrollbar),
:deep(.el-scrollbar__wrap),
:deep(.el-scrollbar__view) {
  overflow: visible !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__body),
:deep(.el-table__header) {
  width: 100% !important;
  table-layout: fixed !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  width: 100%;
  height: 100%;
  padding: 0 2px;
  line-height: var(--user-line-height);
  white-space: normal;
  word-break: break-all;
  overflow: visible;
  text-overflow: clip;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table .el-icon) {
  width: max(6px, min(12px, var(--user-font-size))) !important;
  height: max(6px, min(12px, var(--user-font-size))) !important;
  font-size: max(6px, min(12px, var(--user-font-size))) !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table .el-button) {
  min-height: 0;
  line-height: 1;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table .el-button > span) {
  gap: 0;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table th.el-table__cell) {
  height: var(--user-header-height) !important;
  font-size: max(8px, calc(var(--user-font-size) + 1px));
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table td.el-table__cell) {
  height: var(--user-row-height) !important;
  padding: 0 !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__row) {
  height: var(--user-row-height) !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__cell .el-table__expand-icon) {
  margin-right: 1px;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__indent) {
  padding-left: 6px !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-scrollbar__bar) {
  display: none !important;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__body tr.row-even > td.el-table__cell) {
  background: #ffffff;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__body tr.row-odd > td.el-table__cell) {
  background: #f8fafc;
}

/*noinspection CssUnusedSymbol*/
:deep(.row-disabled) {
  opacity: 0.62;
}

/*noinspection CssUnusedSymbol*/
:deep(.el-table__body tr.row-disabled > td.el-table__cell) {
  background: #f2f4f7 !important;
}

.user-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 100%;
  min-width: 0;
  overflow: visible;
}

.avatar-wrap {
  width: var(--user-avatar-size);
  height: var(--user-avatar-size);
  border-radius: 5px;
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
  justify-content: center;
  gap: 2px;
  min-width: 0;
  flex-wrap: wrap;
  overflow: visible;
}

/*noinspection CssUnusedSymbol*/
.name-row :deep(.el-tag) {
  height: 20px;
  padding: 0 3px;
  font-size: 10px;
  line-height: 18px;
}

.name-text {
  max-width: none;
  font-size: max(8px, calc(var(--user-font-size) + 1px));
  font-weight: 600;
  color: #1f2a37;
  white-space: normal;
  word-break: break-all;
}

.plain-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
  min-width: 0;
  font-size: var(--user-font-size);
  line-height: var(--user-line-height);
  color: #667085;
  white-space: normal;
  word-break: break-all;
  overflow: visible;
}

.phone-cell-inner {
  display: inline-flex;
  align-items: center;
  width: 98px;
  max-width: 100%;
}

.phone-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  margin-right: 4px;
  flex-shrink: 0;
}

.phone-text {
  min-width: 0;
  text-align: left;
  font-variant-numeric: tabular-nums;
}

.phone-text.empty {
  letter-spacing: 0;
}

.fee-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  width: 100%;
  min-width: 0;
}

.fee-edit-wrap {
  display: flex;
  align-items: center;
  gap: 3px;
}

.fee-input {
  width: 42px;
}

/*noinspection CssUnusedSymbol*/
.fee-input :deep(.el-input__wrapper) {
  min-height: 0;
  height: 22px;
  padding: 0 2px;
}

/*noinspection CssUnusedSymbol*/
.fee-input :deep(.el-input__inner) {
  height: 100%;
  font-size: var(--user-font-size);
  line-height: 1;
}

.fee-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  line-height: 1;
}

.fee-badge,
.fee-inherit-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 20px;
  padding: 0 4px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.05;
  background: #f3f5f8;
  color: #526074;
  border: 1px solid #e5eaf1;
  white-space: normal;
  word-break: keep-all;
}

.fee-badge.has-rate {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  border-color: transparent;
  color: #fff;
}

.pct-sign {
  font-size: var(--user-font-size);
  color: #667085;
}

.card-count-cell,
.status-cell,
.action-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
}

/*noinspection CssUnusedSymbol*/
.count-badge :deep(.el-badge__content) {
  font-weight: 700;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  font-size: var(--user-font-size);
  line-height: 14px;
}

/*noinspection CssUnusedSymbol*/
.status-cell :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  min-height: 20px;
  padding: 0 4px;
  font-size: 10px;
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
  flex-wrap: wrap;
}

.action-icon-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  margin-left: 0 !important;
  min-height: 0;
}

.no-data {
  font-size: var(--user-font-size);
  color: #c0c7d6;
}

.user-edit-panel {
  padding: 4px 2px 0;
}

.user-edit-form {
  padding: 2px 4px 0;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-form-item:last-child) {
  margin-bottom: 8px;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #344054;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-form-item__content) {
  min-width: 0;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-input__wrapper),
.user-edit-form :deep(.el-select__wrapper),
.user-edit-form :deep(.el-textarea__inner) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d7dee8 inset;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-input__wrapper:hover),
.user-edit-form :deep(.el-select__wrapper:hover),
.user-edit-form :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #b8c7da inset;
}

/*noinspection CssUnusedSymbol*/
.user-edit-form :deep(.el-input__wrapper.is-focus),
.user-edit-form :deep(.el-select__wrapper.is-focused),
.user-edit-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #1677ff inset, 0 0 0 3px rgba(22, 119, 255, 0.08);
}

.user-type-block {
  width: 100%;
}

.user-type-fixed {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 50px;
  padding: 10px 14px;
  border: 1px solid #dbe7ff;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(234, 242, 255, 0.95) 160%);
}

.user-type-fixed-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  min-height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  background: #0958d9;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.user-type-fixed-text {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #526074;
  text-align: right;
}

.user-type-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  width: 100%;
  padding: 4px;
  border-radius: 14px;
  background: #f6f8fb;
}

/*noinspection CssUnusedSymbol*/
.user-type-group :deep(.el-radio-button) {
  margin: 0;
}

/*noinspection CssUnusedSymbol*/
.user-type-group :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 42px;
  padding: 0 16px;
  border: none !important;
  border-radius: 10px !important;
  background: transparent;
  color: #526074;
  font-size: 13px;
  font-weight: 700;
  box-shadow: none;
  white-space: nowrap;
}

/*noinspection CssUnusedSymbol*/
.user-type-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99) 0%, rgba(234, 242, 255, 0.95) 160%);
  color: #0958d9;
  box-shadow: inset 0 0 0 1.5px rgba(180, 206, 255, 0.85), 0 8px 16px rgba(15, 23, 42, 0.08);
}

.type-inline-tip {
  margin-top: 8px;
  padding: 0 2px;
  font-size: 12px;
  line-height: 1.5;
  color: #667085;
}

.form-tip {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff7e6;
  font-size: 12px;
  line-height: 1.5;
  color: #d97706;
}

.inherit-hint {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  background: #f5f7fa;
  border: 1px solid #e5eaf1;
  color: #526074;
  font-size: 12px;
}

@media (max-width: 1480px) {
  .page-header {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .menu-list-inline {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1320px) {
  .pagination-meta {
    display: none;
  }

  .menu-list-inline {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
