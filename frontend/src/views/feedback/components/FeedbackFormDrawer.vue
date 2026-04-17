<template>
  <el-drawer
    v-model="innerVisible"
    :title="drawerTitle"
    size="680px"
    destroy-on-close
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        class="feedback-form"
      >
        <el-form-item label="问题标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请简要描述您的问题"
            maxlength="200"
            show-word-limit
            clearable
          />
        </el-form-item>

        <el-form-item label="详细描述" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请详细描述问题现象、复现步骤、期望结果等"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题类型" prop="feedbackType">
          <el-radio-group v-model="form.feedbackType">
            <el-radio-button
              v-for="opt in FEEDBACK_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button :value="0">低</el-radio-button>
            <el-radio-button :value="1">中</el-radio-button>
            <el-radio-button :value="2">高</el-radio-button>
            <el-radio-button :value="3">紧急</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="isEditMode">
          <el-divider content-position="left">扩展字段</el-divider>

          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择状态" style="width: 220px">
              <el-option
                v-for="opt in FEEDBACK_STATUS_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="提交人" prop="submitter">
            <el-input v-model="form.submitter" maxlength="50" clearable />
          </el-form-item>

          <el-form-item label="处理人" prop="assignee">
            <el-input v-model="form.assignee" maxlength="50" clearable placeholder="可为空" />
          </el-form-item>

          <el-form-item label="最近备注" prop="latestRemark">
            <el-input
              v-model="form.latestRemark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="最近处理备注，可为空"
            />
          </el-form-item>

          <el-form-item label="解决时间">
            <el-date-picker
              v-model="form.resolvedTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择解决时间"
              clearable
              style="width: 220px"
            />
          </el-form-item>

          <el-form-item label="关闭时间">
            <el-date-picker
              v-model="form.closedTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择关闭时间"
              clearable
              style="width: 220px"
            />
          </el-form-item>

          <el-form-item label="已有附件" v-if="existingAttachments.length > 0">
            <div class="file-list">
              <div
                v-for="att in existingAttachments"
                :key="att.id"
                class="file-item"
                :class="{ 'is-deleted': deletedAttachmentIds.includes(att.id) }"
              >
                <el-icon class="file-icon"><component :is="getFileIcon(att.fileName)" /></el-icon>
                <div class="file-info">
                  <el-link type="primary" :underline="false" class="file-name-link" @click="handlePreviewExistingAttachment(att)">
                    <span class="file-name" :title="att.fileName">{{ att.fileName }}</span>
                  </el-link>
                  <span class="file-size">{{ formatFileSize(att.fileSize) }}</span>
                </div>
                <div class="file-actions">
                  <el-button link size="small" @click="handlePreviewExistingAttachment(att)">预览</el-button>
                  <el-button link size="small" @click="handleDownloadExistingAttachment(att)">下载</el-button>
                  <el-button
                    :type="deletedAttachmentIds.includes(att.id) ? 'info' : 'danger'"
                    link
                    size="small"
                    @click="toggleDeleteAttachment(att.id)"
                  >{{ deletedAttachmentIds.includes(att.id) ? '撤销删除' : '删除' }}</el-button>
                </div>
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="上传附件">
          <div class="upload-area">
            <div
              class="upload-drop-zone"
              :class="{ dragging: isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="handleDrop"
            >
              <el-icon class="upload-icon"><Upload /></el-icon>
              <div class="upload-tip">
                拖拽文件到此处，或
                <label class="upload-btn-label" for="file-input">点击选择</label>
              </div>
              <div class="upload-hint">
                支持 jpg/png/pdf/doc/xlsx 等格式，单文件 ≤10MB，最多 {{ MAX_FILE_COUNT }} 个
              </div>
              <input
                id="file-input"
                ref="fileInputRef"
                type="file"
                multiple
                accept=".jpg,.jpeg,.png,.webp,.gif,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar"
                style="display: none"
                @change="handleFileSelect"
              />
            </div>

            <div v-if="selectedFiles.length > 0" class="file-list">
              <div
                v-for="(f, idx) in selectedFiles"
                :key="`${f.name}-${f.size}-${idx}`"
                class="file-item"
              >
                <el-icon class="file-icon"><component :is="getFileIcon(f.name)" /></el-icon>
                <div class="file-info">
                  <span class="file-name" :title="f.name">{{ f.name }}</span>
                  <span class="file-size">{{ formatFileSize(f.size) }}</span>
                </div>
                <div class="file-actions">
                  <el-button
                    type="danger"
                    link
                    :icon="Close"
                    size="small"
                    @click="removeFile(idx)"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? '保存修改' : '提交反馈' }}
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Upload, Close, Document, Picture, Files } from '@element-plus/icons-vue'
import {
  createFeedbackApi,
  updateFeedbackApi,
  getFeedbackDetailApi,
  downloadFeedbackAttachmentApi,
  type FeedbackCreateForm,
  type FeedbackUpdateForm,
  type FeedbackAttachment
} from '@/api/feedback'
import { FEEDBACK_STATUS_OPTIONS, FEEDBACK_TYPE_OPTIONS } from '@/constants/dict'

const props = withDefaults(defineProps<{
  visible: boolean
  mode?: 'create' | 'edit'
  feedbackId?: number
}>(), {
  mode: 'create',
  feedbackId: 0
})

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'success'): void
}>()

const innerVisible = ref(props.visible)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const loading = ref(false)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const selectedFiles = ref<File[]>([])
const existingAttachments = ref<FeedbackAttachment[]>([])
const deletedAttachmentIds = ref<number[]>([])

const MAX_FILE_COUNT = 5
const MAX_FILE_SIZE = 10 * 1024 * 1024
const ALLOWED_SUFFIX = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'zip', 'rar']

const isEditMode = computed(() => props.mode === 'edit')
const drawerTitle = computed(() => isEditMode.value ? '编辑反馈' : '提交反馈')

const createDefaults = (): FeedbackUpdateForm => ({
  title: '',
  content: '',
  feedbackType: 0,
  priority: 1,
  status: 0,
  submitter: '',
  assignee: '',
  latestRemark: '',
  resolvedTime: '',
  closedTime: '',
  deleteAttachmentIds: []
})

const form = ref<FeedbackUpdateForm>(createDefaults())

const rules: FormRules = {
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { max: 200, message: '标题不超过200字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入详细描述', trigger: 'blur' },
    { max: 5000, message: '描述不超过5000字', trigger: 'blur' }
  ],
  feedbackType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  submitter: [
    { required: true, message: '请输入提交人', trigger: 'blur' },
    { max: 50, message: '提交人不超过50字', trigger: 'blur' }
  ],
  assignee: [{ max: 50, message: '处理人不超过50字', trigger: 'blur' }],
  latestRemark: [{ max: 500, message: '最近备注不超过500字', trigger: 'blur' }]
}

watch(() => props.visible, (v) => {
  innerVisible.value = v
  if (v) {
    if (isEditMode.value && props.feedbackId) {
      void loadDetail()
    } else {
      resetState()
    }
  }
})

watch(innerVisible, (v) => {
  emit('update:visible', v)
  if (!v) {
    resetState()
  }
})

function resetState() {
  form.value = createDefaults()
  selectedFiles.value = []
  existingAttachments.value = []
  deletedAttachmentIds.value = []
  isDragging.value = false
  formRef.value?.clearValidate()
}

async function loadDetail() {
  if (!props.feedbackId) return
  loading.value = true
  try {
    const res = await getFeedbackDetailApi(props.feedbackId)
    const detail = res.data
    form.value = {
      title: detail.title || '',
      content: detail.content || '',
      feedbackType: detail.feedbackType,
      priority: detail.priority,
      status: detail.status,
      submitter: detail.submitter || '',
      assignee: detail.assignee || '',
      latestRemark: detail.latestRemark || '',
      resolvedTime: detail.resolvedTime || '',
      closedTime: detail.closedTime || '',
      deleteAttachmentIds: []
    }
    existingAttachments.value = detail.attachments || []
    deletedAttachmentIds.value = []
    selectedFiles.value = []
    formRef.value?.clearValidate()
  } finally {
    loading.value = false
  }
}

function validateFile(file: File): string | null {
  const suffix = file.name.split('.').pop()?.toLowerCase() || ''
  if (!ALLOWED_SUFFIX.includes(suffix)) {
    return `不支持的文件类型：${suffix}`
  }
  if (file.size > MAX_FILE_SIZE) {
    return `文件 ${file.name} 超过10MB限制`
  }
  return null
}

function currentAttachmentTotal() {
  const remainCount = existingAttachments.value.filter(att => !deletedAttachmentIds.value.includes(att.id)).length
  return remainCount + selectedFiles.value.length
}

function addFiles(files: FileList | File[]) {
  const arr = Array.from(files)
  for (const f of arr) {
    if (currentAttachmentTotal() >= MAX_FILE_COUNT) {
      ElMessage.warning(`最多上传 ${MAX_FILE_COUNT} 个文件`)
      break
    }
    const err = validateFile(f)
    if (err) {
      ElMessage.warning(err)
      continue
    }
    if (!selectedFiles.value.find(sf => sf.name === f.name && sf.size === f.size)) {
      selectedFiles.value.push(f)
    }
  }
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    addFiles(input.files)
    input.value = ''
  }
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  if (e.dataTransfer?.files) {
    addFiles(e.dataTransfer.files)
  }
}

function removeFile(idx: number) {
  selectedFiles.value.splice(idx, 1)
}

function toggleDeleteAttachment(id: number) {
  if (deletedAttachmentIds.value.includes(id)) {
    deletedAttachmentIds.value = deletedAttachmentIds.value.filter(item => item !== id)
  } else {
    deletedAttachmentIds.value.push(id)
  }
}

function getFileIcon(name: string) {
  const suffix = name.split('.').pop()?.toLowerCase() || ''
  if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(suffix)) return Picture
  if (['zip', 'rar'].includes(suffix)) return Files
  return Document
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

async function openAttachmentPreview(att: FeedbackAttachment) {
  const blob = await downloadFeedbackAttachmentApi(att.id, att.fileName)
  const url = window.URL.createObjectURL(blob)
  const win = window.open(url, '_blank', 'noopener,noreferrer')
  if (!win) {
    window.URL.revokeObjectURL(url)
    ElMessage.warning('请允许浏览器弹出窗口')
    return
  }
  window.setTimeout(() => window.URL.revokeObjectURL(url), 60000)
}

async function handlePreviewExistingAttachment(att: FeedbackAttachment) {
  await openAttachmentPreview(att)
}

async function handleDownloadExistingAttachment(att: FeedbackAttachment) {
  const blob = await downloadFeedbackAttachmentApi(att.id, att.fileName)
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = att.fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEditMode.value) {
      if (!props.feedbackId) return
      await updateFeedbackApi(props.feedbackId, {
        ...form.value,
        deleteAttachmentIds: deletedAttachmentIds.value
      }, selectedFiles.value)
      ElMessage.success('反馈修改成功')
    } else {
      const payload: FeedbackCreateForm = {
        title: form.value.title,
        content: form.value.content,
        feedbackType: form.value.feedbackType,
        priority: form.value.priority
      }
      await createFeedbackApi(payload, selectedFiles.value)
      ElMessage.success('反馈提交成功')
    }
    emit('success')
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  innerVisible.value = false
}
</script>

<style scoped lang="scss">
.feedback-form {
  padding: 0 4px;
}

.upload-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-drop-zone {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fafafa;

  &:hover,
  &.dragging {
    border-color: #1677ff;
    background: #f0f7ff;
  }

  .upload-icon {
    font-size: 32px;
    color: #bfbfbf;
    margin-bottom: 8px;
  }

  .upload-tip {
    font-size: 14px;
    color: #595959;

    .upload-btn-label {
      color: #1677ff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .upload-hint {
    font-size: 12px;
    color: #bfbfbf;
    margin-top: 4px;
  }
}

.file-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;

  &.is-deleted {
    opacity: 0.6;
    background: #fff1f0;
    border-color: #ffccc7;

    .file-name {
      text-decoration: line-through;
    }
  }

  .file-icon {
    color: #1677ff;
    font-size: 16px;
    flex-shrink: 0;
  }

  .file-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;

    .file-name-link {
      min-width: 0;
      max-width: 100%;
    }

    .file-name {
      flex: 1;
      font-size: 13px;
      color: #262626;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file-size {
      font-size: 12px;
      color: #8c8c8c;
      flex-shrink: 0;
    }
  }

  .file-actions {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }
}
</style>
