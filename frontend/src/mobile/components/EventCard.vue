<template>
  <div
    class="event-card btn-bounce"
    :class="[
      `status-${props.event.status}`,
      {
        selected: props.selected,
        'is-batch': props.batchMode,
        'is-done': props.event.status === EVENT_STATUS_VALUE.DONE,
        'is-cancelled': props.event.status === EVENT_STATUS_VALUE.CANCELLED,
      },
    ]"
    :style="cardAccentStyle"
    @click="handleCardClick"
  >
    <button v-if="props.batchMode" class="select-box" type="button" @click.stop="emit('toggleSelect', props.event.id)">
      <span :class="['select-inner', { active: props.selected }]">
        <i v-if="props.selected">✓</i>
      </span>
    </button>

    <div class="event-main">
      <div class="event-top">
        <div class="event-time-panel">
          <span class="event-time-label">{{ getTimeBadgeLabel(props.event) }}</span>
          <strong>{{ getTimePrimary(props.event) }}</strong>
          <small>{{ getTimeSecondary(props.event) }}</small>
        </div>

        <div class="event-body">
          <div class="event-title-row">
            <span class="event-title">{{ props.event.title }}</span>
            <el-tag size="small" :type="statusTagType(props.event.status)" round effect="light">
              {{ STATUS_LABEL[props.event.status] }}
            </el-tag>
          </div>

          <div class="event-meta-row">
            <span class="time-chip">
              {{ formatTime(props.event.startTime) || '全天' }}{{ props.event.endTime ? ` - ${formatTime(props.event.endTime)}` : '' }}
            </span>
            <span class="cat-chip" :style="{ color: CATEGORY_COLOR[props.event.category], background: CATEGORY_COLOR_LIGHT[props.event.category] }">
              {{ EVENT_CATEGORY_MAP[props.event.category] }}
            </span>
            <span v-if="props.event.priority === 2" class="priority-chip danger">紧急</span>
            <span v-else-if="props.event.priority === 1" class="priority-chip warning">重要</span>
          </div>

          <p v-if="getRemarkPreview(props.event)" class="event-remark">{{ getRemarkPreview(props.event) }}</p>
        </div>
      </div>

      <div class="event-foot" :class="{ 'only-tip': props.batchMode || !quickActions(props.event).length }">
        <span class="event-tip">{{ getStatusHint(props.event) }}</span>

        <div v-if="!props.batchMode && quickActions(props.event).length" class="event-actions" @click.stop>
          <button
            v-for="action in quickActions(props.event)"
            :key="action.key"
            type="button"
            :class="['quick-btn', `quick-${action.key}`, { pending: props.pendingId === props.event.id }]"
            :disabled="props.pendingId === props.event.id"
            @click="emit('statusChange', props.event.id, action.value)"
          >
            {{ action.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  EVENT_CATEGORY_MAP,
  EVENT_STATUS_TAG_TYPE,
  EVENT_STATUS_VALUE,
} from '@/constants/dict'
import {
  CATEGORY_COLOR,
  CATEGORY_COLOR_LIGHT,
  STATUS_LABEL,
  formatTime,
} from '../utils/constants'

const props = withDefaults(defineProps<{
  event: any
  pendingId?: number | null
  batchMode?: boolean
  selected?: boolean
}>(), {
  pendingId: null,
  batchMode: false,
  selected: false,
})

const emit = defineEmits<{
  click: [event: any]
  statusChange: [id: number, status: number]
  toggleSelect: [id: number]
}>()

const cardAccentStyle = computed(() => {
  const accent = CATEGORY_COLOR[props.event.category] || '#0958d9'
  const accentSoft = CATEGORY_COLOR_LIGHT[props.event.category] || '#eaf2ff'
  return {
    '--accent': accent,
    '--accent-soft': accentSoft,
  }
})

function statusTagType(status: number): string {
  return EVENT_STATUS_TAG_TYPE[status] || 'info'
}

function getRemarkPreview(event: any) {
  return String(event?.remark || event?.description || '').trim()
}

function getTimePrimary(event: any) {
  return formatTime(event?.startTime) || '全天'
}

function getTimeSecondary(event: any) {
  const start = formatTime(event?.startTime)
  const end = formatTime(event?.endTime)
  if (!start && !end) return '灵活安排'
  if (start && end) return `至 ${end}`
  if (start) return '待定结束'
  return `截至 ${end}`
}

function getTimeBadgeLabel(event: any) {
  const start = formatTime(event?.startTime)
  if (!start) return '全天'
  const hour = Number(start.split(':')[0])
  if (Number.isNaN(hour)) return '日程'
  if (hour < 12) return '上午'
  if (hour < 18) return '下午'
  return '晚间'
}

function getStatusHint(event: any) {
  if (event.status === EVENT_STATUS_VALUE.DOING) return '进行中，建议优先跟进'
  if (event.status === EVENT_STATUS_VALUE.DONE) return '已完成，可点击查看详情'
  if (event.status === EVENT_STATUS_VALUE.CANCELLED) return '已取消，可在详情页重新开启'
  return '待处理，点击可查看完整信息'
}

function quickActions(event: any) {
  if (event.status === EVENT_STATUS_VALUE.TODO) {
    return [{ key: 'done', value: EVENT_STATUS_VALUE.DONE, label: '完成' }]
  }
  if (event.status === EVENT_STATUS_VALUE.DOING) {
    return [
      { key: 'todo', value: EVENT_STATUS_VALUE.TODO, label: '待办' },
      { key: 'done', value: EVENT_STATUS_VALUE.DONE, label: '完成' },
    ]
  }
  if (event.status === EVENT_STATUS_VALUE.DONE) {
    return [{ key: 'undo', value: EVENT_STATUS_VALUE.TODO, label: '撤销' }]
  }
  return []
}

function handleCardClick() {
  if (props.batchMode) {
    emit('toggleSelect', props.event.id)
    return
  }
  emit('click', props.event)
}
</script>

<style scoped lang="scss">
.event-card {
  --accent: #0958d9;
  --accent-soft: #eaf2ff;
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 20px;
  border: 1px solid rgba(219, 226, 234, 0.88);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.event-card.selected {
  border-color: rgba(9, 88, 217, 0.36);
  box-shadow: 0 16px 32px rgba(9, 88, 217, 0.14);
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
}

.event-card.status-0 {
  border-left: 4px solid #d97706;
}

.event-card.status-1 {
  border-left: 4px solid #2563eb;
}

.event-card.status-2 {
  border-left: 4px solid #2f9e44;
}

.event-card.status-3 {
  border-left: 4px solid #94a3b8;
}

.select-box {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.select-inner {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  border: 1.5px solid #c5cfdb;
  background: #fff;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.select-inner.active {
  border-color: #0958d9;
  background: #0958d9;
}

.event-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-top {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.event-time-panel {
  width: 76px;
  flex-shrink: 0;
  border-radius: 16px;
  padding: 10px 8px;
  background: linear-gradient(180deg, var(--accent-soft) 0%, rgba(255, 255, 255, 0.96) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 4px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.event-time-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
}

.event-time-panel strong {
  font-size: 18px;
  line-height: 1;
  color: var(--color-text-primary);
}

.event-time-panel small {
  font-size: 11px;
  line-height: 1.45;
  color: var(--color-text-secondary);
}

.event-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-title-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  justify-content: space-between;
}

.event-title {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--color-text-primary);
  word-break: break-word;
}

.is-done .event-title,
.is-cancelled .event-title {
  text-decoration: line-through;
  color: #94a3b8;
}

.event-meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.time-chip,
.cat-chip,
.priority-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.time-chip {
  background: #f3f6fa;
  color: var(--color-text-secondary);
}

.cat-chip {
  white-space: nowrap;
}

.priority-chip.warning {
  background: #fff4db;
  color: #d97706;
}

.priority-chip.danger {
  background: #fff1f0;
  color: #cf1322;
}

.event-remark {
  margin: 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(245, 247, 251, 0.94);
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.event-foot.only-tip {
  align-items: flex-start;
}

.event-tip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.04);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 600;
}

.event-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-left: auto;
}

.quick-btn {
  min-width: 64px;
  min-height: 34px;
  padding: 0 12px;
  border: none;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.1);
}

.quick-btn.quick-done {
  background: #2f9e44;
}

.quick-btn.quick-todo,
.quick-btn.quick-undo {
  background: #64748b;
}

.quick-btn.pending {
  opacity: 0.6;
}

.quick-btn:disabled {
  pointer-events: none;
}

@media (max-width: 420px) {
  .event-top {
    gap: 10px;
  }

  .event-time-panel {
    width: 68px;
    padding: 10px 7px;
  }

  .event-time-panel strong {
    font-size: 16px;
  }
}

@media (max-width: 360px) {
  .event-card {
    padding: 12px;
    border-radius: 18px;
  }

  .event-top {
    flex-direction: column;
  }

  .event-time-panel {
    width: 100%;
    flex-direction: row;
    align-items: center;
  }

  .event-time-panel strong,
  .event-time-panel small {
    flex: 1;
  }

  .event-time-panel small {
    text-align: right;
  }

  .event-title {
    font-size: 14px;
  }

  .event-actions {
    width: 100%;
    margin-left: 0;
  }

  .quick-btn {
    flex: 1;
  }
}
</style>
