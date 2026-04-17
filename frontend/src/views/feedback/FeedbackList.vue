<template>
  <div class="feedback-list-page">
    <!-- 顶部统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-pending">
        <div class="stat-num">{{ stats.pendingCount }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card stat-progress">
        <div class="stat-num">{{ stats.inProgressCount }}</div>
        <div class="stat-label">修复中</div>
      </div>
      <div class="stat-card stat-resolved">
        <div class="stat-num">{{ stats.resolvedCount }}</div>
        <div class="stat-label">已解决</div>
      </div>
      <div class="stat-card stat-week">
        <div class="stat-num">{{ stats.weekNewCount }}</div>
        <div class="stat-label">本周新增</div>
      </div>
    </div>

    <!-- 搜索区 -->
    <el-card class="search-card" :body-style="{ padding: '16px 20px 4px' }">
      <el-form :model="query" inline>
        <el-form-item label="标题关键词">
          <el-input
            v-model="query.titleKeyword"
            placeholder="请输入标题关键词，自动查询"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="opt in FEEDBACK_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="全部" clearable style="width: 100px">
            <el-option
              v-for="opt in FEEDBACK_PRIORITY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.feedbackType" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="opt in FEEDBACK_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
            :shortcuts="DATE_RANGE_SHORTCUTS"
          />
        </el-form-item>
        <el-form-item class="search-action-item">
          <span class="search-tip">条件变更后自动查询</span>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="primary" plain :icon="Plus" @click="openCreateDrawer">提交反馈</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区 -->
    <el-card class="table-card" :body-style="{ padding: '0' }">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column label="反馈编号" prop="feedbackNo" width="190" fixed="left">
          <template #default="{ row }">
            <span class="feedback-no">{{ row.feedbackNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span
              class="title-link"
              @click="openDetailDrawer(row)"
              v-html="highlightKeyword(row.title)"
            />
          </template>
        </el-table-column>

        <el-table-column label="类型" prop="feedbackTypeDesc" width="110">
          <template #default="{ row }">
            <el-tag
              :type="(FEEDBACK_TYPE_TAG_TYPE[row.feedbackType] as any)"
              size="small"
            >{{ row.feedbackTypeDesc }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="优先级" prop="priorityDesc" width="90">
          <template #default="{ row }">
            <el-tag
              :type="(FEEDBACK_PRIORITY_TAG_TYPE[row.priority] as any)"
              size="small"
            >{{ row.priorityDesc }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="statusDesc" width="110">
          <template #default="{ row }">
            <el-tag
              :type="(FEEDBACK_STATUS_TAG_TYPE[row.status] as any)"
              size="small"
            >{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="附件" min-width="260">
          <template #default="{ row }">
            <div v-if="row.attachments?.length" class="attachment-cell">
              <div class="attachment-chip attachment-chip--single">
                <div class="attachment-popover-icon">
                  <el-icon>
                    <component :is="getFileIcon(row.attachments[0].fileSuffix)" />
                  </el-icon>
                </div>
                <el-link
                  type="primary"
                  :underline="false"
                  class="attachment-link"
                  @click="downloadAttachment(row.attachments[0])"
                >
                  <span class="attachment-name" :title="row.attachments[0].fileName">
                    {{ row.attachments[0].fileName }}
                  </span>
                </el-link>
              </div>

              <el-popover
                v-if="row.attachments.length > 1"
                placement="top"
                :width="380"
                trigger="hover"
              >
                <template #reference>
                  <el-tag type="info" effect="plain" class="attachment-more-tag">
                    +{{ row.attachments.length - 1 }}
                  </el-tag>
                </template>

                <div class="attachment-popover">
                  <div
                    v-for="att in row.attachments"
                    :key="att.id"
                    class="attachment-popover-item"
                  >
                    <div class="attachment-popover-icon">
                      <el-icon>
                        <component :is="getFileIcon(att.fileSuffix)" />
                      </el-icon>
                    </div>

                    <el-link
                      type="primary"
                      :underline="false"
                      class="attachment-popover-link"
                      @click="downloadAttachment(att)"
                    >
                      <span class="attachment-popover-name" :title="att.fileName">{{ att.fileName }}</span>
                    </el-link>
                    <span class="attachment-popover-size">{{ formatFileSize(att.fileSize) }}</span>
                  </div>
                </div>
              </el-popover>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="提交人" prop="submitter" width="100" show-overflow-tooltip />

        <el-table-column label="最近备注" prop="latestRemark" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.latestRemark" class="remark-text">{{ row.latestRemark }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="最近更新" prop="updateTime" width="170">
          <template #default="{ row }">
            {{ formatTime(row.updateTime as any) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetailDrawer(row)">
              详情
            </el-button>
            <el-button type="warning" link size="small" @click="openEditDrawer(row)">
              编辑
            </el-button>
            <el-button type="primary" link size="small" @click="openProcessDrawer(row)">
              处理
            </el-button>
            <el-popconfirm
              title="确认删除该反馈记录？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 提交反馈抽屉 -->
    <FeedbackFormDrawer
      v-model:visible="createDrawerVisible"
      mode="create"
      @success="onCreateSuccess"
    />

    <FeedbackFormDrawer
      v-model:visible="editDrawerVisible"
      mode="edit"
      :feedback-id="editFeedbackId"
      @success="onEditSuccess"
    />

    <!-- 详情抽屉 -->
    <FeedbackDetailDrawer
      v-model:visible="detailDrawerVisible"
      :feedback-id="selectedId"
      @edit="openEditDrawerById"
      @process="openProcessDrawerById"
    />

    <!-- 处理抽屉 -->
    <FeedbackProcessDrawer
      v-model:visible="processDrawerVisible"
      :feedback="selectedFeedback"
      @success="onProcessSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Plus, Document, Picture, Files } from '@element-plus/icons-vue'
import {
  getFeedbackPageApi,
  getFeedbackStatsApi,
  deleteFeedbackApi,
  downloadFeedbackAttachmentApi,
  type FeedbackVO,
  type FeedbackStats,
  type FeedbackQuery,
  type FeedbackAttachment
} from '@/api/feedback'
import {
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_STATUS_TAG_TYPE,
  FEEDBACK_PRIORITY_OPTIONS,
  FEEDBACK_PRIORITY_TAG_TYPE,
  FEEDBACK_TYPE_OPTIONS,
  FEEDBACK_TYPE_TAG_TYPE,
  DATE_RANGE_SHORTCUTS
} from '@/constants/dict'
import FeedbackFormDrawer from './components/FeedbackFormDrawer.vue'
import FeedbackDetailDrawer from './components/FeedbackDetailDrawer.vue'
import FeedbackProcessDrawer from './components/FeedbackProcessDrawer.vue'
import { formatTime } from '@/utils/formatters'

// ==================== 状态 ====================
const loading = ref(false)
const tableData = ref<FeedbackVO[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const stats = ref<FeedbackStats>({
  pendingCount: 0,
  inProgressCount: 0,
  resolvedCount: 0,
  weekNewCount: 0,
  totalCount: 0
})

const query = reactive<FeedbackQuery>({
  pageNum: 1,
  pageSize: 10,
  titleKeyword: '',
  status: null,
  priority: null,
  feedbackType: null,
  submitter: ''
})

const createDrawerVisible = ref(false)
const editDrawerVisible = ref(false)
const detailDrawerVisible = ref(false)
const processDrawerVisible = ref(false)
const selectedId = ref<number>(0)
const editFeedbackId = ref<number>(0)
const selectedFeedback = ref<FeedbackVO | null>(null)
const suppressAutoQuery = ref(false)
let keywordTimer: ReturnType<typeof setTimeout> | null = null

// ==================== 关键词高亮 ====================
const highlightKeyword = (title: string) => {
  const kw = query.titleKeyword?.trim()
  if (!kw) return title
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return title.replace(new RegExp(escaped, 'gi'), (m) => `<mark class="kw-highlight">${m}</mark>`)
}

function isImage(suffix?: string) {
  return ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes((suffix || '').toLowerCase())
}

function getFileIcon(suffix?: string) {
  if (isImage(suffix)) return Picture
  if (['zip', 'rar'].includes((suffix || '').toLowerCase())) return Files
  return Document
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

async function downloadAttachment(att: FeedbackAttachment) {
  try {
    const blob = await downloadFeedbackAttachmentApi(att.id, att.fileName)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch {}
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    if (dateRange.value) {
      query.startTime = dateRange.value[0]
      query.endTime = dateRange.value[1]
    } else {
      query.startTime = undefined
      query.endTime = undefined
    }
    const res = await getFeedbackPageApi(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getFeedbackStatsApi()
  stats.value = res.data
}

// ==================== 自动查询 ====================
function triggerAutoQuery() {
  if (suppressAutoQuery.value) return
  query.pageNum = 1
  loadData()
}

watch(() => query.titleKeyword, () => {
  if (suppressAutoQuery.value) return
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  keywordTimer = setTimeout(() => {
    triggerAutoQuery()
  }, 300)
})

watch([
  () => query.status,
  () => query.priority,
  () => query.feedbackType,
  dateRange
], () => {
  triggerAutoQuery()
})

// ==================== 操作方法 ====================
async function handleReset() {
  suppressAutoQuery.value = true
  if (keywordTimer) {
    clearTimeout(keywordTimer)
    keywordTimer = null
  }
  query.titleKeyword = ''
  query.status = null
  query.priority = null
  query.feedbackType = null
  query.submitter = ''
  dateRange.value = null
  query.startTime = undefined
  query.endTime = undefined
  query.pageNum = 1
  await nextTick()
  suppressAutoQuery.value = false
  loadData()
}

function openCreateDrawer() {
  createDrawerVisible.value = true
}

function openDetailDrawer(row: FeedbackVO) {
  selectedId.value = row.id
  detailDrawerVisible.value = true
}

function openEditDrawer(row: FeedbackVO) {
  editFeedbackId.value = row.id
  editDrawerVisible.value = true
}

function openProcessDrawer(row: FeedbackVO) {
  selectedFeedback.value = row
  processDrawerVisible.value = true
}

function openEditDrawerById(id: number) {
  editFeedbackId.value = id
  editDrawerVisible.value = true
}

function openProcessDrawerById(id: number) {
  const row = tableData.value.find(r => r.id === id)
  if (row) openProcessDrawer(row)
}

async function handleDelete(id: number) {
  await deleteFeedbackApi(id)
  ElMessage.success('删除成功')
  loadData()
  loadStats()
}

function onCreateSuccess() {
  createDrawerVisible.value = false
  loadData()
  loadStats()
}

function onEditSuccess() {
  editDrawerVisible.value = false
  detailDrawerVisible.value = false
  loadData()
  loadStats()
}

function onProcessSuccess() {
  processDrawerVisible.value = false
  loadData()
  loadStats()
}

// ==================== 初始化 ====================
onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped lang="scss">
.feedback-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 统计卡片行
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border-left: 4px solid transparent;

  .stat-num {
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }

  .stat-label {
    font-size: 13px;
    color: #8c8c8c;
  }

  &.stat-pending  { border-color: #faad14; .stat-num { color: #faad14; } }
  &.stat-progress { border-color: #1677ff; .stat-num { color: #1677ff; } }
  &.stat-resolved { border-color: #52c41a; .stat-num { color: #52c41a; } }
  &.stat-week     { border-color: #722ed1; .stat-num { color: #722ed1; } }
}

.search-card,
.table-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.search-action-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-right: 4px;
}

.feedback-no {
  font-family: monospace;
  color: #595959;
  font-size: 12px;
}

.title-link {
  color: #1677ff;
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

.attachment-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 6px 0;
}

.attachment-chip,
.attachment-popover-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.attachment-chip {
  max-width: calc(100% - 40px);
  padding: 4px 8px;
  overflow: hidden;
}

.attachment-chip--single {
  max-width: none;
  flex: 1;
}

.attachment-popover-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
  background: #ecf5ff;
}

.attachment-link,
.attachment-popover-link {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

.attachment-name,
.attachment-popover-name {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-more-tag {
  cursor: pointer;
  flex-shrink: 0;
}

.attachment-popover {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.attachment-popover-item {
  padding: 6px 8px;
}

.attachment-popover-size {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.remark-text {
  color: #595959;
  font-size: 12px;
}

.text-muted {
  color: #bfbfbf;
  font-size: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
}

:deep(.kw-highlight) {
  background: #fff566;
  color: #d4380d;
  border-radius: 2px;
  padding: 0 1px;
}
</style>
