<template>
  <el-drawer
    v-model="innerVisible"
    title="反馈详情"
    size="680px"
    destroy-on-close
  >
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="8" animated />
    </div>

    <template v-else-if="detail">
      <!-- 基本信息摘要 -->
      <div class="detail-header">
        <div class="header-top">
          <span class="feedback-no">{{ detail.feedbackNo }}</span>
          <el-tag :type="(FEEDBACK_STATUS_TAG_TYPE[detail.status] as any)" size="small">
            {{ detail.statusDesc }}
          </el-tag>
          <el-tag :type="(FEEDBACK_PRIORITY_TAG_TYPE[detail.priority] as any)" size="small">
            {{ detail.priorityDesc }}
          </el-tag>
          <el-tag :type="(FEEDBACK_TYPE_TAG_TYPE[detail.feedbackType] as any)" size="small">
            {{ detail.feedbackTypeDesc }}
          </el-tag>
        </div>
        <h3 class="feedback-title">{{ detail.title }}</h3>
        <div class="meta-row">
          <span><el-icon><User /></el-icon> 提交人：{{ detail.submitter }}</span>
          <span v-if="detail.assignee"><el-icon><Avatar /></el-icon> 处理人：{{ detail.assignee }}</span>
          <span><el-icon><Clock /></el-icon> 提交时间：{{ formatTime(detail.createTime as any) }}</span>
          <span v-if="detail.resolvedTime"><el-icon><Check /></el-icon> 解决时间：{{ formatTime(detail.resolvedTime as any) }}</span>
        </div>
      </div>

      <el-divider />

      <!-- 详细描述 -->
      <div class="section-block">
        <div class="section-title">问题描述</div>
        <div class="content-box">{{ detail.content }}</div>
      </div>

      <!-- 附件列表 -->
      <div v-if="detail.attachments && detail.attachments.length > 0" class="section-block">
        <div class="section-title">附件（{{ detail.attachments.length }}）</div>
        <div class="attachment-list">
          <div
            v-for="att in detail.attachments"
            :key="att.id"
            class="attachment-item"
          >
            <div class="att-left">
              <div class="att-file-icon">
                <el-icon class="att-icon"><component :is="getFileIcon(att.fileSuffix)" /></el-icon>
              </div>
              <div class="att-info">
                <el-link
                  type="primary"
                  :underline="false"
                  class="att-name-link"
                  @click="downloadAttachment(att)"
                >
                  <span class="att-name" :title="att.fileName">{{ att.fileName }}</span>
                </el-link>
                <span class="att-size">{{ formatFileSize(att.fileSize) }}</span>
              </div>
            </div>
            <div class="att-actions">
              <el-button
                type="primary"
                link
                size="small"
                :icon="Download"
                @click="downloadAttachment(att)"
              >下载</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 处理轨迹时间线 -->
      <div class="section-block">
        <div class="section-title">处理轨迹</div>
        <el-timeline v-if="detail.processLogs && detail.processLogs.length > 0">
          <el-timeline-item
            v-for="log in detail.processLogs"
            :key="log.id"
            :timestamp="formatTime(log.operateTime as any)"
            :type="getTimelineType(log.actionType)"
            placement="top"
          >
            <div class="timeline-content">
              <div class="timeline-header">
                <strong>{{ log.actionDesc }}</strong>
                <span class="timeline-operator">{{ log.operator }}</span>
              </div>
              <div v-if="log.actionType === 'STATUS_CHANGE'" class="status-change">
                <el-tag size="small" type="info">{{ log.fromStatusDesc }}</el-tag>
                <el-icon><ArrowRight /></el-icon>
                <el-tag size="small" type="success">{{ log.toStatusDesc }}</el-tag>
              </div>
              <div v-if="log.remark" class="timeline-remark">{{ log.remark }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无处理记录" :image-size="60" />
      </div>
    </template>

    <template #footer>
      <el-button @click="innerVisible = false">关闭</el-button>
      <el-button type="warning" @click="handleEdit">编辑</el-button>
      <el-button type="primary" @click="handleProcess">去处理</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { User, Clock, Check, Download, ArrowRight } from '@element-plus/icons-vue'
import { Avatar } from '@element-plus/icons-vue'
import { Document, Picture, Files } from '@element-plus/icons-vue'
import {
  getFeedbackDetailApi,
  downloadFeedbackAttachmentApi,
  type FeedbackVO,
  type FeedbackAttachment
} from '@/api/feedback'
import {
  FEEDBACK_STATUS_TAG_TYPE,
  FEEDBACK_PRIORITY_TAG_TYPE,
  FEEDBACK_TYPE_TAG_TYPE
} from '@/constants/dict'
import { formatTime } from '@/utils/formatters'

const props = defineProps<{ visible: boolean; feedbackId: number }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'process', id: number): void
  (e: 'edit', id: number): void
}>()

const innerVisible = ref(props.visible)
const loading = ref(false)
const detail = ref<FeedbackVO | null>(null)

watch(() => props.visible, v => {
  innerVisible.value = v
  if (v && props.feedbackId) loadDetail()
})
watch(innerVisible, v => emit('update:visible', v))

async function loadDetail() {
  if (!props.feedbackId) return
  loading.value = true
  try {
    const res = await getFeedbackDetailApi(props.feedbackId)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function isImage(suffix: string) {
  return ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes((suffix || '').toLowerCase())
}

function getFileIcon(suffix: string) {
  if (isImage(suffix)) return Picture
  if (['zip', 'rar'].includes((suffix || '').toLowerCase())) return Files
  return Document
}

function formatFileSize(size: number): string {
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
  } catch {
  }
}

function getTimelineType(actionType: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    SUBMIT: 'primary',
    STATUS_CHANGE: 'success',
    REMARK: 'info',
    ATTACHMENT: 'warning',
    CLOSE: 'danger'
  }
  return map[actionType] || 'primary'
}

function handleEdit() {
  emit('edit', props.feedbackId)
  innerVisible.value = false
}

function handleProcess() {
  emit('process', props.feedbackId)
  innerVisible.value = false
}
</script>

<style scoped lang="scss">
.loading-box {
  padding: 16px;
}

.detail-header {
  padding: 0 0 4px;

  .header-top {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .feedback-no {
    font-family: monospace;
    color: #8c8c8c;
    font-size: 12px;
  }

  .feedback-title {
    font-size: 18px;
    font-weight: 600;
    color: #262626;
    margin: 0 0 10px;
    line-height: 1.4;
  }

  .meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 13px;
    color: #8c8c8c;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.section-block {
  margin-bottom: 20px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #262626;
    margin-bottom: 12px;
    padding-left: 10px;
    border-left: 3px solid #1677ff;
  }
}

.content-box {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 14px 16px;
  font-size: 14px;
  color: #434343;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  gap: 12px;

  .att-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;
    min-width: 0;

    .att-file-icon {
      width: 40px;
      height: 40px;
      border-radius: 4px;
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: #1677ff;
      background: #ecf5ff;
    }

    .att-file-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      color: #1677ff;
      background: #ecf5ff;
    }

    .att-icon {
      font-size: 18px;
      color: #1677ff;
      flex-shrink: 0;
    }

    .att-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;

      .att-name-link {
        display: inline-flex;
        max-width: 100%;
      }

      .att-name {
        font-size: 13px;
        color: #262626;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 360px;
      }

      .att-size {
        font-size: 12px;
        color: #8c8c8c;
        flex-shrink: 0;
      }
    }
  }

  .att-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
}

.timeline-content {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 10px 14px;

  .timeline-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;

    .timeline-operator {
      font-size: 12px;
      color: #8c8c8c;
    }
  }

  .status-change {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .timeline-remark {
    font-size: 13px;
    color: #434343;
    background: #fff;
    border-radius: 4px;
    padding: 6px 10px;
    border-left: 3px solid #1677ff;
    margin-top: 4px;
  }
}
</style>
