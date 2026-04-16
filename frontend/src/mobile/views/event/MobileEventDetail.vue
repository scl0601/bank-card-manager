<template>
  <div class="mobile-page mobile-event-page">
    <transition name="view-fade" mode="out-in">
      <div v-if="!isEditing && eventData" key="view" class="detail-page">
        <section class="summary-card" :class="`status-${eventData.status}`">
          <div class="summary-top">
            <span class="cat-badge" :style="{ background: CATEGORY_COLOR[eventData.category], color: '#fff' }">
              {{ EVENT_CATEGORY_MAP[eventData.category] }}
            </span>
            <el-tag size="small" :type="statusTagType(eventData.status)" round effect="light">
              {{ STATUS_LABEL[eventData.status] }}
            </el-tag>
            <el-tag v-if="eventData.priority === 2" size="small" type="danger" effect="dark" round>紧急</el-tag>
            <el-tag v-else-if="eventData.priority === 1" size="small" type="warning" effect="plain" round>重要</el-tag>
          </div>

          <h1 class="summary-title">{{ eventData.title }}</h1>
          <p class="summary-subtitle">{{ detailStatusHint }}</p>

          <div class="summary-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDisplayDate(eventData.eventDate) }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ timeRangeLabel }}</span>
            </div>
          </div>

          <div class="summary-chips">
            <span class="summary-chip summary-chip--primary">{{ relativeDateLabel }}</span>
            <span class="summary-chip">{{ periodLabel }}</span>
            <span class="summary-chip">{{ PRIORITY_LABEL[eventData.priority ?? 0] }}优先级</span>
          </div>
        </section>

        <section v-if="remarkLines.length" class="info-card remark-card mobile-card mobile-card--flat">
          <div class="block-head">
            <div>
              <h3 class="info-title">执行步骤</h3>
              <p>按行拆分展示，移动端查看更清晰</p>
            </div>
            <span class="block-badge">{{ remarkLines.length }} 条</span>
          </div>

          <ul class="remark-list">
            <li v-for="(line, index) in remarkLines" :key="`${index}-${line}`" class="remark-item">
              <span class="remark-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="remark-text">{{ line }}</span>
            </li>
          </ul>
        </section>

        <section class="info-card summary-grid-card mobile-card mobile-card--flat">
          <div class="block-head">
            <div>
              <h3 class="info-title">日程摘要</h3>
              <p>关键信息分块展示，方便快速核对</p>
            </div>
          </div>

          <div class="detail-grid">
            <div v-for="item in detailFacts" :key="item.label" :class="['detail-tile', item.tone]">
              <span class="detail-tile-label">{{ item.label }}</span>
              <strong class="detail-tile-value">{{ item.value }}</strong>
              <small class="detail-tile-hint">{{ item.hint }}</small>
            </div>
          </div>
        </section>

        <section class="action-card mobile-card mobile-card--flat">
          <div class="block-head">
            <div>
              <h3 class="info-title">快捷操作</h3>
              <p>支持状态更新、编辑和取消当前日程</p>
            </div>
          </div>

          <div class="status-actions">
            <button
              v-for="action in statusActions"
              :key="action.key"
              :class="['sa-btn btn-bounce', `sa-${action.key}`, { active: eventData.status === action.value, pending: statusChanging }]"
              @click="changeStatus(action.value)"
              :disabled="statusChanging"
            >
              {{ action.label }}
            </button>
          </div>

          <div class="edit-actions">
            <button class="ea-btn ea-edit btn-bounce" @click="startEdit">
              <el-icon><EditPen /></el-icon>
              编辑
            </button>
            <button
              v-if="eventData.status === EVENT_STATUS_VALUE.CANCELLED"
              class="ea-btn ea-delete btn-bounce"
              :disabled="deleting"
              @click="handleDelete"
            >
              {{ deleting ? '删除中...' : '删除' }}
            </button>
            <button v-else class="ea-btn ea-danger btn-bounce" @click="handleCancelStatus">
              取消日程
            </button>
          </div>
        </section>
      </div>

      <div v-else-if="loading" key="loading" class="skeleton-detail">
        <div class="sk-block sk-summary shimmer-bg"></div>
        <div class="sk-block sk-info shimmer-bg"></div>
      </div>

      <div v-else key="empty" class="mobile-empty">
        <div class="empty-icon">⚠️</div>
        <div class="empty-text">日程不存在或已被删除</div>
      </div>
    </transition>

    <transition name="edit-slide" mode="out-in">
      <div v-if="isEditing" key="edit" class="edit-page">
        <section class="edit-intro mobile-card mobile-card--flat">
          <div>
            <strong>{{ isEditMode ? '编辑日程' : '快速新建日程' }}</strong>
            <p>保留 PC 端字段结构，优先优化移动端单手录入与触控效率。</p>
          </div>
          <span class="intro-badge">{{ form.eventDate || '未选择日期' }}</span>
        </section>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="event-form mobile-card mobile-card--flat">
          <el-form-item label="日程标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入日程标题..." maxlength="100" show-word-limit />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="日期" prop="eventDate" class="form-col">
              <el-date-picker v-model="form.eventDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
            <el-form-item label="分类" prop="category" class="form-col">
              <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
                <el-option v-for="c in EVENT_CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value">
                  <span class="cat-opt"><i class="cat-dot" :style="{ background: CATEGORY_COLOR[c.value] }"></i>{{ c.label }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </div>

          <div class="section-label">时间安排</div>
          <div class="preset-group">
            <button
              v-for="preset in quickTimePresets"
              :key="preset.key"
              type="button"
              class="preset-btn btn-bounce"
              @click="applyTimePreset(preset)"
            >
              <strong>{{ preset.label }}</strong>
              <small>{{ preset.hint }}</small>
            </button>
          </div>
          <div class="form-row">
            <el-form-item label="开始时间" class="form-col">
              <el-select v-model="form.startTime" placeholder="选择开始时间" clearable filterable style="width:100%">
                <el-option v-for="opt in timeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="结束时间" class="form-col">
              <el-select v-model="form.endTime" placeholder="选择结束时间" clearable filterable style="width:100%">
                <el-option v-for="opt in timeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </div>

          <div class="section-label">属性设置</div>
          <div class="priority-group">
            <button
              v-for="p in EVENT_PRIORITY_OPTIONS"
              :key="p.value"
              :class="['pri-btn btn-bounce', { active: form.priority === p.value }]"
              type="button"
              @click="form.priority = p.value"
            >
              {{ p.label }}
            </button>
          </div>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in EVENT_STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="步骤详情">
            <el-input
              v-model="form.remark"
              type="textarea"
              :autosize="{ minRows: 4, maxRows: 12 }"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="请输入步骤详情，每行一条"
            />
          </el-form-item>
        </el-form>

        <div class="submit-bar">
          <button class="sb-btn sb-cancel btn-bounce" @click="cancelEdit">取消</button>
          <button class="sb-btn sb-save btn-bounce" :disabled="saving" @click="handleSave">
            {{ saving ? '处理中...' : (isEditMode ? '保存修改' : '创建日程') }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, EditPen } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  deleteEventApi,
  getEventDetailApi,
  saveEventApi,
  updateEventApi,
  updateEventStatusApi,
} from '@/api/calendar'
import {
  EVENT_CATEGORY_MAP,
  EVENT_CATEGORY_OPTIONS,
  EVENT_CATEGORY_VALUE,
  EVENT_PRIORITY_OPTIONS,
  EVENT_PRIORITY_VALUE,
  EVENT_STATUS_OPTIONS,
  EVENT_STATUS_TAG_TYPE,
  EVENT_STATUS_VALUE,
} from '@/constants/dict'
import {
  CATEGORY_COLOR,
  PRIORITY_LABEL,
  STATUS_LABEL,
  formatDate,
  formatDisplayDate,
  formatTime,
  getCurrentTime,
} from '../../utils/constants'

interface TimeOption {
  value: string
  label: string
}

interface TimePreset {
  key: string
  label: string
  hint: string
  startTime: string
  endTime: string
}

interface DetailFact {
  label: string
  value: string
  hint: string
  tone: string
}

const route = useRoute()
const router = useRouter()

const quickTimePresets: TimePreset[] = [
  { key: 'all-day', label: '全天', hint: '清空时间字段', startTime: '', endTime: '' },
  { key: 'morning', label: '上午', hint: '09:00 - 10:00', startTime: '09:00', endTime: '10:00' },
  { key: 'afternoon', label: '下午', hint: '14:00 - 15:00', startTime: '14:00', endTime: '15:00' },
  { key: 'evening', label: '晚间', hint: '19:00 - 20:00', startTime: '19:00', endTime: '20:00' },
]

const calendarBackQuery = computed(() => {
  const query: Record<string, string> = {}
  ;['date', 'view', 'category', 'quick', 'keyword'].forEach((key) => {
    const value = route.query[key]
    if (typeof value === 'string' && value) {
      query[key] = value
    }
  })
  return query
})

function goBackToCalendar() {
  router.replace({ path: '/m/calendar', query: calendarBackQuery.value })
}

function statusTagType(status: number): string {
  return EVENT_STATUS_TAG_TYPE[status] || 'info'
}

function buildTimeOptions(): TimeOption[] {
  const opts: TimeOption[] = []
  for (let m = 0; m <= 23 * 60 + 45; m += 15) {
    const h = Math.floor(m / 60)
    const min = m % 60
    opts.push({ value: `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`, label: `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}` })
  }
  opts.push({ value: '23:59', label: '23:59' })
  return opts
}

const timeOptions = buildTimeOptions()

const eventId = computed(() => {
  const id = Number(route.params.id)
  return Number.isNaN(id) ? null : id
})
const isEditMode = computed(() => !!eventId.value)

const isEditing = ref(!isEditMode.value)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const statusChanging = ref(false)
const eventData = ref<any>(null)

const remarkLines = computed(() => {
  const text = String(eventData.value?.remark || eventData.value?.description || '').trim()
  if (!text) return []
  return text.split(/\r?\n/).map((item) => item.trim()).filter(Boolean)
})

const timeRangeLabel = computed(() => {
  if (!eventData.value) return ''
  const start = formatTime(eventData.value.startTime)
  const end = formatTime(eventData.value.endTime)
  if (!start && !end) return '全天安排'
  if (start && end) return `${start} - ${end}`
  if (start) return `${start} 开始`
  return `截至 ${end}`
})

const periodLabel = computed(() => {
  if (!eventData.value) return ''
  return getPeriodLabel(eventData.value.startTime)
})

const relativeDateLabel = computed(() => {
  if (!eventData.value?.eventDate) return ''
  return getRelativeDateLabel(eventData.value.eventDate)
})

const detailStatusHint = computed(() => {
  if (!eventData.value) return ''
  return getStatusHint(eventData.value.status)
})

const detailFacts = computed<DetailFact[]>(() => {
  if (!eventData.value) return []
  return [
    {
      label: '日期',
      value: formatDisplayDate(eventData.value.eventDate),
      hint: relativeDateLabel.value || '已安排日程日期',
      tone: 'primary',
    },
    {
      label: '时段',
      value: timeRangeLabel.value,
      hint: periodLabel.value,
      tone: 'info',
    },
    {
      label: '优先级',
      value: PRIORITY_LABEL[eventData.value.priority ?? 0] || '低',
      hint: getPriorityHint(eventData.value.priority),
      tone: eventData.value.priority === 2 ? 'danger' : eventData.value.priority === 1 ? 'warning' : 'neutral',
    },
    {
      label: '当前状态',
      value: STATUS_LABEL[eventData.value.status] || '待办',
      hint: getStatusHint(eventData.value.status),
      tone: eventData.value.status === EVENT_STATUS_VALUE.DONE ? 'success' : eventData.value.status === EVENT_STATUS_VALUE.CANCELLED ? 'neutral' : eventData.value.status === EVENT_STATUS_VALUE.DOING ? 'info' : 'warning',
    },
  ]
})

const formRef = ref<FormInstance>()
const form = reactive({
  title: '',
  eventDate: '',
  startTime: '' as string,
  endTime: '' as string,
  category: EVENT_CATEGORY_VALUE.WORK as number,
  priority: EVENT_PRIORITY_VALUE.MEDIUM as number,
  status: EVENT_STATUS_VALUE.TODO as number,
  remark: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const statusActions = computed(() => {
  if (!eventData.value) return []
  const s = eventData.value.status
  if (s === EVENT_STATUS_VALUE.TODO) return [
    { key: 'doing', value: EVENT_STATUS_VALUE.DOING, label: '标记进行中' },
    { key: 'done', value: EVENT_STATUS_VALUE.DONE, label: '标记完成' },
  ]
  if (s === EVENT_STATUS_VALUE.DOING) return [
    { key: 'todo', value: EVENT_STATUS_VALUE.TODO, label: '退回待办' },
    { key: 'done', value: EVENT_STATUS_VALUE.DONE, label: '标记完成' },
  ]
  if (s === EVENT_STATUS_VALUE.DONE) return [
    { key: 'todo', value: EVENT_STATUS_VALUE.TODO, label: '重新开启' },
  ]
  return [
    { key: 'todo', value: EVENT_STATUS_VALUE.TODO, label: '重新开启' },
    { key: 'doing', value: EVENT_STATUS_VALUE.DOING, label: '重新进行' },
  ]
})

function parseTimeValue(value: unknown) {
  if (typeof value !== 'string') return ''
  return /^\d{2}:\d{2}(:\d{2})?$/.test(value) ? value.slice(0, 5) : ''
}

function parseEnumValue(value: unknown, candidates: number[]) {
  const parsed = Number(value)
  return candidates.includes(parsed) ? parsed : undefined
}

function addMinutes(time: string, minutesToAdd: number) {
  const [hourText, minuteText = '0'] = time.split(':')
  const minutes = Number(hourText) * 60 + Number(minuteText) + minutesToAdd
  if (Number.isNaN(minutes)) return ''
  const normalized = Math.min(Math.max(minutes, 0), 23 * 60 + 59)
  const hour = Math.floor(normalized / 60)
  const minute = normalized % 60
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function compareTime(a?: string, b?: string) {
  if (!a || !b) return 0
  return a.localeCompare(b)
}

function getPeriodLabel(startTime?: string) {
  const time = formatTime(startTime || '')
  if (!time) return '全天安排'
  const hour = Number(time.split(':')[0])
  if (Number.isNaN(hour)) return '时间待定'
  if (hour < 12) return '上午时段'
  if (hour < 18) return '下午时段'
  return '晚间时段'
}

function getRelativeDateLabel(dateKey: string) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(`${dateKey}T00:00:00`)
  const diff = Math.round((target.getTime() - today.getTime()) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === -1) return '昨天'
  if (diff > 0) return `${diff}天后`
  return `${Math.abs(diff)}天前`
}

function getPriorityHint(priority?: number) {
  if (priority === 2) return '建议优先处理，避免遗漏'
  if (priority === 1) return '重要事项，建议预留执行时间'
  return '常规事项，可灵活安排'
}

function getStatusHint(status?: number) {
  if (status === EVENT_STATUS_VALUE.DOING) return '当前正在推进，可继续跟进处理'
  if (status === EVENT_STATUS_VALUE.DONE) return '事项已完成，建议保留结果记录'
  if (status === EVENT_STATUS_VALUE.CANCELLED) return '事项已取消，如有需要可重新开启'
  return '待处理事项，建议尽快安排执行'
}

async function loadEventDetail() {
  if (!eventId.value) {
    initNewForm()
    isEditing.value = true
    return
  }
  loading.value = true
  try {
    const res: any = await getEventDetailApi(eventId.value)
    eventData.value = res.data
    syncFormFromData(res.data)
    isEditing.value = false
  } catch {
    eventData.value = null
  } finally {
    loading.value = false
  }
}

function initNewForm() {
  const startTime = parseTimeValue(route.query.startTime) || getCurrentTime()
  const endTime = parseTimeValue(route.query.endTime) || (startTime ? addMinutes(startTime, 60) : '')
  const category = parseEnumValue(route.query.category, EVENT_CATEGORY_OPTIONS.map((item) => item.value))
  const priority = parseEnumValue(route.query.priority, EVENT_PRIORITY_OPTIONS.map((item) => item.value))
  const status = parseEnumValue(route.query.status, EVENT_STATUS_OPTIONS.map((item) => item.value))

  Object.assign(form, {
    title: '',
    eventDate: (route.query.date as string) || formatDate(new Date()),
    startTime,
    endTime,
    category: category ?? EVENT_CATEGORY_VALUE.WORK,
    priority: priority ?? EVENT_PRIORITY_VALUE.MEDIUM,
    status: status ?? EVENT_STATUS_VALUE.TODO,
    remark: '',
  })
}

function syncFormFromData(data: any) {
  Object.assign(form, {
    title: data.title || '',
    eventDate: data.eventDate || '',
    startTime: data.startTime ? String(data.startTime).slice(0, 5) : '',
    endTime: data.endTime ? String(data.endTime).slice(0, 5) : '',
    category: data.category ?? EVENT_CATEGORY_VALUE.WORK,
    priority: data.priority ?? EVENT_PRIORITY_VALUE.MEDIUM,
    status: data.status ?? EVENT_STATUS_VALUE.TODO,
    remark: data.remark || data.description || '',
  })
}

function startEdit() {
  isEditing.value = true
}

function cancelEdit() {
  if (isEditMode.value && eventData.value) {
    syncFormFromData(eventData.value)
    isEditing.value = false
  } else {
    goBackToCalendar()
  }
}

function applyTimePreset(preset: TimePreset) {
  form.startTime = preset.startTime
  form.endTime = preset.endTime
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.startTime && form.endTime && compareTime(form.startTime, form.endTime) > 0) {
    ElMessage.warning('结束时间不能早于开始时间')
    return
  }

  saving.value = true
  try {
    const payload: any = {
      title: form.title,
      eventDate: form.eventDate,
      category: form.category,
      priority: form.priority,
      status: form.status,
      remark: form.remark || null,
      description: form.remark || null,
      startTime: form.startTime ? `${form.startTime}:00` : null,
      endTime: form.endTime ? `${form.endTime}:00` : null,
    }

    if (isEditMode.value) {
      await updateEventApi(eventId.value!, payload)
      ElMessage.success('日程已更新')
    } else {
      await saveEventApi(payload)
      ElMessage.success('日程已创建')
    }
    goBackToCalendar()
  } catch {
  } finally {
    saving.value = false
  }
}

async function changeStatus(newStatus: number) {
  if (!eventId.value || !eventData.value || eventData.value.status === newStatus) return
  statusChanging.value = true
  try {
    await updateEventStatusApi(eventId.value, newStatus)
    eventData.value = { ...eventData.value, status: newStatus }
    ElMessage.success('状态已更新')
  } catch {
  } finally {
    statusChanging.value = false
  }
}

async function handleCancelStatus() {
  try {
    await ElMessageBox.confirm('确定要取消此日程吗？取消后可重新开启。', '确认取消', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      type: 'warning',
    })
    await changeStatus(EVENT_STATUS_VALUE.CANCELLED)
  } catch {
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除此日程吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    deleting.value = true
    try {
      await deleteEventApi(eventId.value!)
      ElMessage.success('已删除')
      goBackToCalendar()
    } finally {
      deleting.value = false
    }
  } catch {
  }
}

onMounted(() => loadEventDetail())
</script>

<style scoped lang="scss">
@import '../../../styles/index.scss';
@import '../../styles/mobile.scss';

.view-fade-enter-active,
.view-fade-leave-active {
  transition: all 0.22s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.view-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.view-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.edit-slide-enter-active {
  transition: all 0.28s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.edit-slide-leave-active {
  transition: all 0.18s ease-in;
}

.edit-slide-enter-from {
  opacity: 0;
  transform: translateX(16px);
}

.edit-slide-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}

.mobile-event-page {
  padding-bottom: calc(24px + var(--mobile-safe-bottom));
}

.detail-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-card,
.info-card,
.action-card {
  margin: 0;
}

.summary-card {
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.98) 0%, rgba(244, 248, 255, 0.98) 100%);
  border: 1px solid rgba(221, 228, 238, 0.95);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);

  &.status-0 { border-left: 4px solid #d97706; }
  &.status-1 { border-left: 4px solid #2563eb; }
  &.status-2 { border-left: 4px solid #2f9e44; }
  &.status-3 { border-left: 4px solid #c9cdd4; opacity: 0.82; }
}

.summary-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.cat-badge {
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  min-height: 26px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.summary-title {
  margin: 0;
  font-size: clamp(22px, 5.5vw, 28px);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.35;
  word-break: break-word;
}

.summary-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.summary-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(245, 247, 251, 0.92);
  font-size: var(--mobile-font-sm);
  color: var(--color-text-secondary);
}

.summary-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.summary-chip--primary {
  background: #eaf2ff;
  color: #0958d9;
}

.block-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  p {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: var(--color-text-secondary);
  }
}

.info-title {
  margin: 0;
  font-size: var(--mobile-font-md);
  font-weight: 700;
  color: var(--color-text-primary);
}

.block-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef5ff;
  color: #0958d9;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.remark-card {
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.remark-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.remark-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 12px;
  border-radius: 16px;
  background: rgba(245, 247, 251, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.92);
}

.remark-index {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #0958d9;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.remark-text {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-regular);
  word-break: break-word;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-tile {
  min-height: 118px;
  padding: 14px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: #f8fbff;
}

.detail-tile.primary {
  background: linear-gradient(180deg, #eef5ff 0%, #f9fbff 100%);
}

.detail-tile.info {
  background: linear-gradient(180deg, #eff6ff 0%, #fbfdff 100%);
}

.detail-tile.success {
  background: linear-gradient(180deg, #eefbf1 0%, #fcfffd 100%);
}

.detail-tile.warning {
  background: linear-gradient(180deg, #fff7e8 0%, #fffdfa 100%);
}

.detail-tile.danger {
  background: linear-gradient(180deg, #fff3f1 0%, #fffdfd 100%);
}

.detail-tile.neutral {
  background: linear-gradient(180deg, #f5f7fa 0%, #fcfdff 100%);
}

.detail-tile-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.detail-tile-value {
  font-size: 18px;
  line-height: 1.4;
  color: var(--color-text-primary);
  word-break: break-word;
}

.detail-tile-hint {
  font-size: 11px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.action-card {
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.status-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.sa-btn {
  flex-shrink: 0;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  border: none;
  font-size: 13px;
  font-weight: 700;

  &:disabled {
    opacity: 0.5;
  }

  &.sa-doing {
    background: #eaf2ff;
    color: #2563eb;

    &.active {
      background: #2563eb;
      color: #fff;
      box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
    }
  }

  &.sa-done {
    background: #e8f7ed;
    color: #2f9e44;

    &.active {
      background: #2f9e44;
      color: #fff;
      box-shadow: 0 8px 18px rgba(47, 158, 68, 0.22);
    }
  }

  &.sa-todo {
    background: #fff4db;
    color: #d97706;

    &.active {
      background: #d97706;
      color: #fff;
      box-shadow: 0 8px 18px rgba(217, 119, 6, 0.22);
    }
  }

  &.pending {
    opacity: 0.65;
  }
}

.edit-actions {
  display: flex;
  gap: 8px;
}

.ea-btn {
  flex: 1;
  min-height: 46px;
  border-radius: 14px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &.ea-edit {
    background: var(--color-primary);
    color: #fff;
  }

  &.ea-danger {
    background: #fff1f0;
    color: #cf1322;
    border: 1px solid #ffccc7;
  }

  &.ea-delete {
    background: #cf1322;
    color: #fff;
  }
}

.edit-page {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-intro {
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  strong {
    display: block;
    font-size: 16px;
    color: var(--color-text-primary);
  }

  p {
    margin: 6px 0 0;
    font-size: 12px;
    line-height: 1.6;
    color: var(--color-text-secondary);
  }
}

.intro-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #0958d9;
  font-size: 12px;
  font-weight: 700;
}

.event-form {
  margin: 0;
}

.event-form .el-form-item {
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  gap: 10px;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.section-label {
  margin: 18px 0 10px;
  font-size: var(--mobile-font-sm);
  font-weight: 700;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.preset-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.preset-btn {
  min-height: 60px;
  padding: 12px;
  border: none;
  border-radius: 16px;
  background: rgba(245, 247, 251, 0.95);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  text-align: left;

  strong {
    font-size: 14px;
    color: var(--color-text-primary);
  }

  small {
    font-size: 11px;
    color: var(--color-text-secondary);
  }
}

.priority-group {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.pri-btn {
  flex: 1;
  min-height: 40px;
  border-radius: 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  font-size: 13px;
  color: var(--color-text-regular);

  &.active {
    border-color: var(--color-primary);
    background: #eaf2ff;
    color: var(--color-primary);
    font-weight: 700;
    box-shadow: 0 0 0 2px rgba(9, 88, 217, 0.08);
  }
}

.cat-opt {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.submit-bar {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 10px;
  padding: 12px 0 calc(4px + var(--mobile-safe-bottom));
  background: linear-gradient(180deg, rgba(244, 247, 251, 0.18) 0%, rgba(244, 247, 251, 0.98) 36%);
  z-index: 50;
}

.sb-btn {
  flex: 1;
  min-height: 48px;
  border-radius: 14px;
  border: none;
  font-size: 16px;
  font-weight: 700;

  &.sb-cancel {
    background: #f5f7fa;
    color: var(--color-text-secondary);
  }

  &.sb-save {
    background: var(--color-primary);
    color: #fff;

    &:disabled {
      opacity: 0.55;
    }
  }
}

.skeleton-detail {
  padding: 12px;

  .sk-block {
    border-radius: 16px;
    margin-bottom: 12px;
  }

  .sk-summary {
    height: 170px;
  }

  .sk-info {
    height: 240px;
  }
}

@media (max-width: 420px) {
  .detail-page,
  .edit-page {
    padding: 10px;
  }

  .detail-grid,
  .edit-actions,
  .form-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .preset-group {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 360px) {
  .summary-card {
    padding: 16px;
    border-radius: 20px;
  }

  .summary-chip,
  .meta-item {
    width: 100%;
    justify-content: center;
  }

  .remark-item {
    padding: 10px;
  }

  .detail-tile {
    min-height: 104px;
  }
}
</style>
