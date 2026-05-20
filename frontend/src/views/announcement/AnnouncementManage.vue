<template>
  <div class="announcement-page">
    <div class="page-title">公告管理</div>

    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="handleReset">
      <el-form-item label="内容">
        <el-input v-model="query.content" placeholder="更新内容关键词" clearable class="app-search-item-md" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable class="app-search-item-sm">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已下线" :value="2" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" @click="openCreate">新增公告</el-button>
      </template>
    </SearchBar>

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
      <el-table-column prop="content" label="公告内容" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ firstLine(row.content) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.pinned === 1" type="warning" size="small">置顶</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="publishTime" label="发布时间" width="170">
        <template #default="{ row }">
          <span v-if="row.publishTime">{{ formatTime(row.publishTime) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170">
        <template #default="{ row }">
          <span v-if="row.updateTime">{{ formatTime(row.updateTime) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-divider direction="vertical" />
          <el-button v-if="row.status !== 1" link type="success" size="small" @click="handlePublish(row.id)">发布</el-button>
          <el-button v-else link type="warning" size="small" @click="handleOffline(row.id)">下线</el-button>
          <el-divider direction="vertical" />
          <el-popconfirm title="确认删除该公告？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </PageTable>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '新增公告'"
      width="640px"
      align-center
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="置顶">
          <el-switch v-model="pinnedSwitch" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="5000"
            show-word-limit
            placeholder="每行一条更新内容，用户端会按换行展示"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Announcements' })
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from '@/plugins/element-feedback'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import { usePageTable } from '@/composables/usePageTable'
import type { Announcement, AnnouncementSavePayload } from '@/api/announcement'
import {
  createAnnouncementApi,
  deleteAnnouncementApi,
  getAnnouncementAdminPageApi,
  offlineAnnouncementApi,
  publishAnnouncementApi,
  updateAnnouncementApi
} from '@/api/announcement'

const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()

const form = reactive<AnnouncementSavePayload>({
  content: '',
  pinned: 0,
  sortOrder: 0
})

const pinnedSwitch = computed({
  get: () => form.pinned === 1,
  set: (value: boolean) => { form.pinned = value ? 1 : 0 }
})

const rules: FormRules = {
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange, refresh
} = usePageTable<Announcement>({
  fetchApi: (params) => {
    const clean: Record<string, any> = {}
    for (const key in params) {
      const val = (params as any)[key]
      if (val !== undefined && val !== null && val !== '') clean[key] = val
    }
    return getAnnouncementAdminPageApi(clean)
  },
  defaultQuery: { content: '', status: undefined as number | undefined },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  }
})

function handleReset() {
  resetQuery()
}

function resetForm() {
  editingId.value = undefined
  form.content = ''
  form.pinned = 0
  form.sortOrder = 0
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Announcement) {
  isEdit.value = true
  editingId.value = row.id
  form.content = row.content
  form.pinned = row.pinned || 0
  form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      content: form.content,
      pinned: form.pinned,
      sortOrder: form.sortOrder
    }
    if (isEdit.value && editingId.value) {
      await updateAnnouncementApi(editingId.value, payload)
    } else {
      await createAnnouncementApi(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

async function handlePublish(id: number) {
  await publishAnnouncementApi(id)
  ElMessage.success('发布成功')
  refresh()
}

async function handleOffline(id: number) {
  await offlineAnnouncementApi(id)
  ElMessage.success('下线成功')
  refresh()
}

async function handleDelete(id: number) {
  await deleteAnnouncementApi(id)
  ElMessage.success('删除成功')
  refresh()
}

function statusTagType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'info'
  return 'warning'
}

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

function firstLine(value: string) {
  return value.split(/\r?\n/).map(line => line.trim()).filter(Boolean)[0] || '-'
}
</script>

<style scoped lang="scss">
.announcement-page {
  min-width: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-footer .el-button + .el-button {
  margin-left: 0;
}

.text-muted {
  color: var(--color-text-secondary);
}
</style>
