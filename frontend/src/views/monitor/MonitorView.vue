<template>
  <div class="monitor-page">
    <div class="monitor-header">
      <div>
        <div class="page-title">监控列表</div>
        <div class="page-subtitle">{{ displayDate }} 今日动态</div>
      </div>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        value-format="YYYY-MM-DD"
        :clearable="false"
        @change="fetchMonitor"
      />
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">操作总数</span>
        <strong>{{ summary.operationCount }}</strong>
      </div>
      <div class="summary-card is-success">
        <span class="summary-label">成功操作</span>
        <strong>{{ summary.successCount }}</strong>
      </div>
      <div class="summary-card is-danger">
        <span class="summary-label">失败操作</span>
        <strong>{{ summary.failCount }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">今日日程</span>
        <strong>{{ summary.calendarCount }}</strong>
      </div>
      <div class="summary-card is-success">
        <span class="summary-label">已完成日程</span>
        <strong>{{ summary.completedCalendarCount }}</strong>
      </div>
    </div>

    <div class="timeline-panel" v-loading="loading">
      <div class="panel-title">时间线</div>
      <el-empty v-if="!loading && timeline.length === 0" description="暂无监控记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in timeline"
          :key="`${item.type}-${item.sourceId}-${item.time}`"
          :timestamp="formatTime(item.time)"
          placement="top"
          :type="timelineType(item)"
        >
          <div class="timeline-item">
            <div class="timeline-main">
              <div class="timeline-title">
                <el-tag size="small" :type="item.type === 'OPERATION' ? 'primary' : 'warning'">
                  {{ item.type === 'OPERATION' ? '系统操作' : '日程计划' }}
                </el-tag>
                <span>{{ item.title }}</span>
              </div>
              <div v-if="item.description" class="timeline-desc">{{ item.description }}</div>
            </div>
            <div class="timeline-meta">
              <span>{{ item.operator || '-' }}</span>
              <span>{{ item.module }}</span>
              <span>{{ item.action || '-' }}</span>
              <StatusTag
                v-if="item.type === 'OPERATION'"
                :value="item.result"
                :label-map="LOG_RESULT_MAP"
                :type-map="LOG_RESULT_TAG_TYPE"
                size="small"
              />
              <StatusTag
                v-else
                :value="item.result"
                :label-map="EVENT_STATUS_MAP"
                :type-map="EVENT_STATUS_TAG_TYPE"
                size="small"
              />
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Monitor' })

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag/index.vue'
import { getMonitorTodayApi } from '@/api/monitor'
import { formatTime } from '@/utils/formatters'
import {
  EVENT_STATUS_MAP,
  EVENT_STATUS_TAG_TYPE,
  LOG_RESULT_MAP,
  LOG_RESULT_TAG_TYPE
} from '@/constants/dict'

interface MonitorSummary {
  operationCount: number
  successCount: number
  failCount: number
  calendarCount: number
  completedCalendarCount: number
}

interface MonitorTimelineItem {
  type: 'OPERATION' | 'CALENDAR'
  operator: string
  time: string
  module: string
  action: string
  title: string
  description: string
  result: number
  sourceId: number
}

const loading = ref(false)
const selectedDate = ref(todayText())
const timeline = ref<MonitorTimelineItem[]>([])
const summary = reactive<MonitorSummary>({
  operationCount: 0,
  successCount: 0,
  failCount: 0,
  calendarCount: 0,
  completedCalendarCount: 0
})

const displayDate = computed(() => selectedDate.value || todayText())

function todayText() {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function fetchMonitor() {
  loading.value = true
  try {
    const res = await getMonitorTodayApi({ date: selectedDate.value })
    const data = res.data || {}
    Object.assign(summary, {
      operationCount: data.summary?.operationCount || 0,
      successCount: data.summary?.successCount || 0,
      failCount: data.summary?.failCount || 0,
      calendarCount: data.summary?.calendarCount || 0,
      completedCalendarCount: data.summary?.completedCalendarCount || 0
    })
    timeline.value = data.timeline || []
  } catch (error) {
    ElMessage.error('监控数据加载失败')
  } finally {
    loading.value = false
  }
}

function timelineType(item: MonitorTimelineItem) {
  if (item.type === 'CALENDAR') return 'warning'
  return item.result === 1 ? 'danger' : 'success'
}

onMounted(fetchMonitor)
</script>

<style scoped lang="scss">
.monitor-page {
  min-width: 0;
}

.monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-subtitle {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 16px;

  .summary-label {
    display: block;
    color: #667085;
    font-size: 13px;
    margin-bottom: 8px;
  }

  strong {
    color: #1f2329;
    font-size: 28px;
    line-height: 1;
  }

  &.is-success strong {
    color: #16a34a;
  }

  &.is-danger strong {
    color: #dc2626;
  }
}

.timeline-panel {
  min-height: 360px;
  background: #fff;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 18px 20px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.timeline-item {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 14px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  background: #fafafa;
}

.timeline-main {
  min-width: 0;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2329;
}

.timeline-desc {
  margin-top: 8px;
  color: #667085;
  font-size: 13px;
  word-break: break-word;
}

.timeline-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  min-width: 240px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }

  .timeline-item {
    flex-direction: column;
  }

  .timeline-meta {
    justify-content: flex-start;
    min-width: 0;
  }
}
</style>
