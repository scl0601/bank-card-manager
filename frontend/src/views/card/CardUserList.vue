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
        <el-button class="action-btn" :icon="RefreshRight" @click="refreshData">刷新</el-button>
        <el-button type="primary" class="action-btn" :icon="Plus" @click="openAddTopUser">新增账户</el-button>
      </div>
    </div>

    <div class="app-search-panel card-shell user-search-panel">
      <div class="app-search-main">
        <div class="app-search-title">筛选</div>
        <el-input
          v-model="query.name"
          class="app-search-item app-search-item-lg"
          placeholder="请输入持卡人姓名模糊查询"
          clearable
          maxlength="20"
        />
        <el-input
          v-model="query.phone"
          class="app-search-item app-search-item-md"
          placeholder="请输入联系电话模糊查询"
          clearable
          maxlength="11"
        />
        <el-select v-model="query.status" class="app-search-item app-search-item-sm" placeholder="请选择持卡人状态" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
        <el-select v-model="query.type" class="app-search-item app-search-item-sm" placeholder="请选择账户类型" clearable>
          <el-option label="主账户" value="main" />
          <el-option label="子用户" value="child" />
        </el-select>
        <el-select v-model="query.hasCard" class="app-search-item app-search-item-sm" placeholder="请选择绑卡情况" clearable>
          <el-option label="已绑卡" :value="1" />
          <el-option label="未绑卡" :value="0" />
        </el-select>
      </div>

      <div class="app-search-extra">
        <span class="app-search-meta">当前共 {{ filteredCount }} 条持卡人记录</span>
        <span class="app-search-meta">筛选条件变化后自动刷新列表</span>
        <el-button link type="primary" :icon="expandAll ? Fold : Expand" @click="toggleExpandAll">
          {{ expandAll ? '收起全部' : '展开全部' }}
        </el-button>
        <div class="app-search-actions">
          <el-button class="app-search-btn" @click="resetQuery">重置</el-button>
        </div>
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

        <div ref="tableWrapRef" class="user-table-wrap" :style="tableStyleVars">
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
            stripe
            height="100%"
            table-layout="fixed"
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
            :header-cell-style="{ background: '#f7f9fc', color: '#5b6472', fontWeight: '600', padding: '0' }"
            :row-class-name="rowClassName"
            :cell-style="{ padding: '0' }"
            @expand-change="onExpandChange"
          >
            <el-table-column prop="name" label="姓名/类型">
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
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="phone" label="联系电话" align="center">
              <template #default="{ row }">
                <span class="plain-cell phone-cell">
                  <el-icon :size="12"><Phone /></el-icon>{{ row.phone || '—' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="所属关系" align="center">
              <template #default="{ row }">
                <span class="plain-cell relation-cell">
                  {{ row.parentId ? (row.parentName || '子用户') : '主账户' }}
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

          <el-empty v-else-if="dataReady" description="暂无持卡人数据">
            <template #image>
              <el-icon :size="58" color="#c0c7d6"><UserFilled /></el-icon>
            </template>
            <el-button type="primary" :icon="Plus" @click="openAddTopUser">立即添加</el-button>
          </el-empty>
        </div>

        <div v-if="filteredTotal > 0" class="user-pagination">
          <el-pagination
            background
            small
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            :total="filteredTotal"
            layout="total, prev, pager, next"
            @current-change="handleCurrentChange"
          />
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

interface CardUserViewCache {
  savedAt: number
  rawData: UserData[]
  parentUserOptions: { id: number; name: string }[]
}

type QuickMenuKey = 'all' | 'main' | 'child' | 'active' | 'disabled' | 'withCard' | 'noCard'

const DEFAULT_PAGE_SIZE = 10
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
  phone: '',
  status: undefined as number | undefined,
  type: '' as '' | 'main' | 'child',
  hasCard: '' as '' | 0 | 1,
  pageNum: 1,
  pageSize: DEFAULT_PAGE_SIZE
})

const triggerFilterSearch = createDebouncedTask(() => {
  handleFilterSearch()
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
const parentUserOptions = ref<{ id: number; name: string }[]>(initialCache?.parentUserOptions || [])

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
const filteredCount = computed(() => filteredTotal.value)
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
const visibleRowCount = computed(() => Math.max(collectVisibleRows(treeData.value).length, 1))
const showTableSkeleton = computed(() => loading.value && !dataReady.value && treeData.value.length === 0)
const tableStyleVars = computed(() => {
  const headerHeight = visibleRowCount.value > 16 ? 20 : 22
  const availableHeight = Math.max(tableWrapHeight.value - headerHeight - 2, 120)
  const rowHeight = Math.max(9, Math.min(30, Math.floor(availableHeight / visibleRowCount.value)))
  const fontSize = Math.max(5, Math.min(12, Math.floor(rowHeight * 0.48)))
  const avatarSize = Math.max(8, Math.min(20, rowHeight - 3))

  return {
    '--user-header-height': `${headerHeight}px`,
    '--user-row-height': `${rowHeight}px`,
    '--user-font-size': `${fontSize}px`,
    '--user-avatar-size': `${avatarSize}px`,
    '--user-line-height': `${rowHeight <= 18 ? 1.05 : 1.15}`
  }
})

function updateTableLayout() {
  if (tableLayoutFrame) return
  tableLayoutFrame = window.requestAnimationFrame(() => {
    tableLayoutFrame = 0
    tableWrapHeight.value = tableWrapRef.value?.clientHeight || 0
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
  const filtered = filterTreeBy(normalized, matchesCurrentFilters)
  filteredTotal.value = filtered.length

  const maxPage = Math.max(1, Math.ceil(filteredTotal.value / query.pageSize))
  if (query.pageNum > maxPage) query.pageNum = maxPage

  const start = (query.pageNum - 1) * query.pageSize
  treeData.value = filtered.slice(start, start + query.pageSize)
  expandedRowIds.value = expandAll.value
    ? collectExpandableIds(treeData.value)
    : collectNameMatchExpandIds(treeData.value, query.name)

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
  const name = keyword.trim()
  if (!name) return []

  const matchedParentIds = new Set<number>()
  const walk = (item: UserData): boolean => {
    const selfMatched = String(item.name || '').includes(name)
    const childMatched = !!item.children?.some((child) => walk(child))
    if (childMatched && item.children?.length) matchedParentIds.add(item.id)
    return selfMatched || childMatched
  }

  list.forEach((item) => walk(item))
  return Array.from(matchedParentIds)
}

function resetQuery() {
  triggerFilterSearch.cancel()
  syncingFilterQuery = true
  query.name = ''
  query.phone = ''
  query.status = undefined
  query.type = ''
  query.hasCard = ''
  query.pageNum = 1
  query.pageSize = DEFAULT_PAGE_SIZE
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

function onExpandChange(row: UserData, expanded: boolean) {
  if (syncingExpandedRows) return
  if (expanded) {
    if (!expandedRowIds.value.includes(row.id)) expandedRowIds.value.push(row.id)
  } else {
    expandedRowIds.value = expandedRowIds.value.filter((id) => id !== row.id)
  }
  syncExpandState()
  nextTick(updateTableLayout)
}

function syncExpandState() {
  const expandableIds = collectExpandableIds(treeData.value)
  expandAll.value = expandableIds.length > 0 && expandableIds.every((id) => expandedRowIds.value.includes(id))
}

function toggleExpandAll() {
  if (!treeData.value.length) return
  expandAll.value = !expandAll.value
  expandedRowIds.value = expandAll.value ? collectExpandableIds(treeData.value) : []
  nextTick(() => {
    applyExpandedRows()
    updateTableLayout()
  })
}

function applyExpandedRows() {
  syncingExpandedRows = true
  try {
    setRowExpansionByIds(treeData.value, new Set(expandedRowIds.value))
  } finally {
    syncingExpandedRows = false
  }
}

function setRowExpansionByIds(rows: UserData[], expandedIds: Set<number>) {
  rows.forEach((row) => {
    tableRef.value?.toggleRowExpansion(row, expandedIds.has(row.id))
    if (row.children?.length) setRowExpansionByIds(row.children, expandedIds)
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
    fetchData({ force: true })
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
  if (!lastFetchedAt.value || Date.now() - lastFetchedAt.value > VIEW_CACHE_TTL) {
    fetchData({ silent: true })
  }
  nextTick(initTableResize)
  window.addEventListener('resize', updateTableLayout)
})
onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true
    nextTick(updateTableLayout)
    return
  }
  if (!lastFetchedAt.value || Date.now() - lastFetchedAt.value > VIEW_CACHE_TTL) {
    fetchData({ silent: true })
  }
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
  () => [query.name, query.phone, query.status, query.type, query.hasCard],
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

.card-user-page,
.card-user-page * {
  scrollbar-width: none;
}

.card-user-page ::-webkit-scrollbar {
  width: 0 !important;
  height: 0 !important;
  display: none;
}

.card-shell {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.035);
}

.page-header {
  display: grid;
  grid-template-columns: minmax(122px, 0.58fr) minmax(232px, 1fr) auto;
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
.header-stat-body,
.user-info {
  min-width: 0;
}

.header-title {
  font-size: 14px;
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.header-subtitle {
  margin-top: 1px;
  font-size: 10px;
  line-height: 1.2;
  color: #8a94a6;
  white-space: nowrap;
}

.header-stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 3px;
  min-width: 0;
}

.header-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 5px;
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  min-width: 0;
}

.header-stat-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-stat-value {
  font-size: 13px;
  line-height: 1;
  font-weight: 700;
  color: #1f2a37;
}

.header-stat-label {
  margin-top: 0;
  font-size: 9px;
  color: #7c8799;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-search-panel {
  flex-shrink: 0;
  gap: 6px 10px;
  padding: 6px 8px;
}

.user-search-panel .app-search-main {
  gap: 6px;
}

.user-search-panel .app-search-extra,
.user-search-panel .app-search-actions {
  gap: 6px;
}

.user-search-panel .app-search-title,
.user-search-panel .app-search-meta {
  min-height: 28px;
  font-size: 11px;
}

.user-search-panel .app-search-item-sm {
  width: 128px;
}

.user-search-panel .app-search-item-md {
  width: 156px;
}

.user-search-panel .app-search-item-lg {
  width: 190px;
}

.user-search-panel .app-search-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 8px;
  font-size: 12px;
}

.user-search-panel :deep(.el-input__wrapper),
.user-search-panel :deep(.el-select__wrapper),
.user-search-panel :deep(.el-date-editor.el-input__wrapper),
.user-search-panel :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
  border-radius: 8px;
}

.user-search-panel :deep(.el-input__inner),
.user-search-panel :deep(.el-select__selected-item),
.user-search-panel :deep(.el-range-input),
.user-search-panel :deep(.el-input-number__input),
.user-search-panel :deep(.el-date-editor .el-range-separator) {
  font-size: 12px;
}

.header-search {
  display: none !important;
  width: 116px;
}

.action-btn {
  height: 24px;
  padding: 0 7px;
  border-radius: 7px;
}

.filter-panel {
  display: none !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 3px 6px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  flex-shrink: 0;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.filter-title {
  padding: 0 1px;
  font-size: 10px;
  font-weight: 700;
  color: #526074;
  white-space: nowrap;
}

.filter-item {
  width: 82px;
}

.filter-phone {
  width: 104px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

.toolbar-hint {
  font-size: 10px;
  color: #7c8799;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr) 112px;
  gap: 4px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
  font-size: 11px;
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

.menu-item {
  display: grid;
  grid-template-columns: 8px 1fr auto;
  align-items: center;
  gap: 4px;
  height: 24px;
  padding: 0 5px;
  border: 1px solid #e7edf4;
  border-radius: 7px;
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
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.menu-count {
  min-width: 16px;
  height: 15px;
  padding: 0 3px;
  border-radius: 999px;
  background: rgba(9, 88, 217, 0.08);
  color: inherit;
  font-size: 9px;
  line-height: 15px;
  text-align: center;
}

.data-head {
  align-items: center;
}

.inline-summary {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 5px;
  height: 20px;
  border-radius: 6px;
  background: #f5f8fc;
  color: #667085;
  font-size: 10px;
  white-space: nowrap;
}

.user-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #fff;
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
  justify-content: flex-end;
  align-items: center;
  height: 26px;
  padding-top: 3px;
  flex-shrink: 0;
}

.user-pagination :deep(.el-pagination) {
  --el-pagination-button-height: 20px;
  --el-pagination-button-width: 20px;
  font-size: 10px;
}

:deep(.el-table) {
  height: 100% !important;
  --el-table-border-color: #e5eaf1;
  font-size: var(--user-font-size);
  overflow: hidden !important;
}

:deep(.el-table__inner-wrapper),
:deep(.el-table__header-wrapper),
:deep(.el-table__body-wrapper),
:deep(.el-scrollbar),
:deep(.el-scrollbar__wrap),
:deep(.el-scrollbar__view) {
  overflow: hidden !important;
}

:deep(.el-table__body),
:deep(.el-table__header) {
  width: 100% !important;
  table-layout: fixed !important;
}

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

:deep(.el-table .el-icon) {
  width: max(6px, min(12px, var(--user-font-size))) !important;
  height: max(6px, min(12px, var(--user-font-size))) !important;
  font-size: max(6px, min(12px, var(--user-font-size))) !important;
}

:deep(.el-table .el-button) {
  min-height: 0;
  line-height: 1;
}

:deep(.el-table .el-button > span) {
  gap: 0;
}

:deep(.el-table th.el-table__cell) {
  height: var(--user-header-height) !important;
  font-size: max(8px, calc(var(--user-font-size) + 1px));
}

:deep(.el-table td.el-table__cell) {
  height: var(--user-row-height) !important;
  padding: 0 !important;
}

:deep(.el-table__row) {
  height: var(--user-row-height) !important;
}

:deep(.el-table__cell .el-table__expand-icon) {
  margin-right: 1px;
}

:deep(.el-table__indent) {
  padding-left: 6px !important;
}

:deep(.el-scrollbar__bar) {
  display: none !important;
}

:deep(.row-disabled) {
  opacity: 0.62;
  background: #fafbfc;
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

.name-row :deep(.el-tag) {
  height: max(7px, calc(var(--user-row-height) - 8px));
  padding: 0 3px;
  font-size: max(5px, calc(var(--user-font-size) - 1px));
  line-height: 1;
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

.fee-input :deep(.el-input__wrapper) {
  min-height: 0;
  height: max(8px, calc(var(--user-row-height) - 8px));
  padding: 0 2px;
}

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
  min-height: max(7px, calc(var(--user-row-height) - 8px));
  padding: 0 4px;
  border-radius: 999px;
  font-size: var(--user-font-size);
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

.count-badge :deep(.el-badge__content) {
  font-weight: 700;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  font-size: var(--user-font-size);
  line-height: 14px;
}

.status-cell :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  min-height: max(7px, calc(var(--user-row-height) - 8px));
  padding: 0 4px;
  font-size: var(--user-font-size);
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
  width: max(8px, calc(var(--user-row-height) - 5px));
  height: max(8px, calc(var(--user-row-height) - 5px));
  padding: 0;
  margin-left: 0 !important;
  min-height: 0;
}

.no-data {
  font-size: var(--user-font-size);
  color: #c0c7d6;
}

.side-card {
  padding: 5px;
  background: #f8fafc;
  border: 1px solid #e7edf4;
  border-radius: 8px;
}

.side-card + .side-card {
  margin-top: 4px;
}

.side-card-title {
  margin-bottom: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #1f2a37;
}

.action-stack {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px;
}

.action-stack :deep(.el-button) {
  width: 100%;
  height: 23px;
  margin-left: 0;
  border-radius: 7px;
  font-size: 10px;
}

.summary-list,
.legend-list {
  display: grid;
  gap: 3px;
}

.summary-item,
.legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  font-size: 9px;
  color: #667085;
}

.summary-item strong {
  color: #1f2a37;
  font-size: 10px;
  word-break: break-all;
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
    grid-template-columns: minmax(112px, 0.58fr) minmax(210px, 1fr) auto;
  }

  .workspace-grid {
    grid-template-columns: 96px minmax(0, 1fr) 104px;
  }

  .header-search {
    width: 104px;
  }
}

@media (max-width: 1320px) {
  .inline-summary,
  .toolbar-hint {
    display: none;
  }

  .workspace-grid {
    grid-template-columns: 88px minmax(0, 1fr) 96px;
  }
}
</style>
