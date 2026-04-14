<template>
  <el-drawer
    :model-value="visible"
    :title="eventId ? '编辑日程' : '新建日程'"
    direction="rtl"
    size="552px"
    :close-on-click-modal="true"
    @update:model-value="$emit('update:visible', $event)"
    @closed="resetForm"
    class="event-drawer"
  >
    <template #header>
      <div class="drawer-header">
        <div class="drawer-title-wrap">
          <span class="drawer-icon" :class="eventId ? 'icon-edit' : 'icon-add'">
            <svg v-if="!eventId" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </span>
          <div class="drawer-title-copy">
            <span class="drawer-title-text">{{ eventId ? '编辑日程' : '新建日程' }}</span>
          </div>
        </div>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="default"
      class="event-form"
    >
      <!-- 标题 -->
      <el-form-item label="日程标题" prop="title" class="form-item-title">
        <el-input
          v-model="form.title"
          placeholder="请输入日程标题..."
          maxlength="100"
          show-word-limit
          :prefix-icon="TitleIcon"
        />
      </el-form-item>

      <!-- 日期 + 分类 -->
      <div class="form-row">
        <el-form-item label="日期" prop="eventDate" class="form-col">
          <el-date-picker
            v-model="form.eventDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类" prop="category" class="form-col">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option
              v-for="c in EVENT_CATEGORY_OPTIONS"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            >
              <div class="cat-option">
                <span class="cat-dot" :style="{ background: categoryColor[c.value] }"></span>
                {{ c.label }}
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </div>

      <!-- 时间段 -->
      <div class="form-section-label">时间安排</div>
      <div class="form-row">
        <el-form-item label="开始时间" class="form-col">
          <div class="time-field">
            <el-select
              v-model="form.startTime"
              placeholder="选择开始时间"
              clearable
              filterable
              default-first-option
              popper-class="event-drawer-time-popper"
              style="width: 100%"
            >
              <el-option
                v-for="option in startTimeOptions"
                :key="`start-${option.value}`"
                :label="option.label"
                :value="option.value"
              >
                <div class="time-option">
                  <span>{{ option.label }}</span>
                  <span v-if="option.badge" class="time-option-badge">{{ option.badge }}</span>
                </div>
              </el-option>
            </el-select>
            <div class="time-shortcuts">
              <button type="button" class="time-shortcut-btn is-primary" @click="applyCurrentStartTime">当前时间</button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="结束时间" class="form-col">
          <div class="time-field">
            <el-select
              v-model="form.endTime"
              placeholder="选择结束时间"
              clearable
              filterable
              default-first-option
              popper-class="event-drawer-time-popper"
              style="width: 100%"
            >
              <el-option
                v-for="option in endTimeOptions"
                :key="`end-${option.value}`"
                :label="option.label"
                :value="option.value"
              >
                <div class="time-option">
                  <span>{{ option.label }}</span>
                  <span v-if="option.badge" class="time-option-badge">{{ option.badge }}</span>
                </div>
              </el-option>
            </el-select>
            <div class="time-shortcuts">
              <button type="button" class="time-shortcut-btn" :disabled="!form.startTime" @click="applyEndTimeOffset(30)">+30 分钟</button>
              <button type="button" class="time-shortcut-btn" :disabled="!form.startTime" @click="applyEndTimeOffset(60)">+1 小时</button>
            </div>
          </div>
        </el-form-item>
      </div>

      <!-- 优先级 + 状态 -->
      <div class="form-section-label">属性设置</div>
      <div class="form-row">
        <el-form-item label="优先级" prop="priority" class="form-col">
          <div class="priority-group">
            <button
              v-for="p in EVENT_PRIORITY_OPTIONS"
              :key="p.value"
              :class="['pri-btn', `pri-${p.value}`, { active: form.priority === p.value }]"
              type="button"
              @click="form.priority = p.value"
            >
              <svg v-if="p.value === 2" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
              {{ p.label }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status" class="form-col">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="s in EVENT_STATUS_OPTIONS"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            >
              <div class="status-option">
                <span class="status-dot" :class="`s-${s.value}`"></span>
                {{ s.label }}
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </div>

      <!-- 步骤详情 -->
      <el-form-item label="步骤详情">
        <div class="step-editor">
          <div class="step-textarea-wrap">
            <el-input
              v-model="form.remark"
              type="textarea"
              class="step-textarea"
              :autosize="{ minRows: 6, maxRows: 20 }"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="请输入步骤详情，每行一条"
              @input="limitRemarkLines"
            />
          </div>
          <div class="step-footer-hint">
            <span>{{ remarkLineCount }} 行</span>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="$emit('update:visible', false)" style="border-radius:8px">取消</el-button>
        <el-button
          v-if="eventId && form.status === EVENT_STATUS_VALUE.CANCELLED"
          type="danger"
          plain
          :loading="deleting"
          @click="handleDelete"
          style="border-radius:8px"
        >删除此日程</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave" style="border-radius:8px;min-width:88px">
          {{ saving ? '处理中...' : (eventId ? '保存修改' : '完成') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, h } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import type { FormInstance, FormRules } from 'element-plus'
import {
  getEventDetailApi, saveEventApi, updateEventApi, deleteEventApi
} from '@/api/calendar'
import {
  EVENT_CATEGORY_OPTIONS, EVENT_STATUS_OPTIONS, EVENT_PRIORITY_OPTIONS,
  EVENT_STATUS_VALUE, EVENT_PRIORITY_VALUE, EVENT_CATEGORY_VALUE
} from '@/constants/dict'

// 标题前缀图标组件
const TitleIcon = { render: () => h('svg', { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
  h('line', { x1: 8, y1: 6, x2: 21, y2: 6 }), h('line', { x1: 8, y1: 12, x2: 21, y2: 12 }), h('line', { x1: 8, y1: 18, x2: 21, y2: 18 }),
  h('line', { x1: 3, y1: 6, x2: 3.01, y2: 6 }), h('line', { x1: 3, y1: 12, x2: 3.01, y2: 12 }), h('line', { x1: 3, y1: 18, x2: 3.01, y2: 18 }),
]) }

const categoryColor: Record<number, string> = {
  0: '#0958d9', 1: '#2f9e44', 2: '#d97706', 3: '#cf1322', 4: '#64748b', 5: '#6d28d9',
}

interface Props {
  visible: boolean
  eventId: number | null
  defaultDate: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
  (e: 'deleted'): void
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const deleting = ref(false)
const currentTimeSnapshot = ref('')


const form = reactive({
  title: '',
  eventDate: '',
  startTime: '',
  endTime: '',
  category: EVENT_CATEGORY_VALUE.WORK,
  priority: EVENT_PRIORITY_VALUE.MEDIUM,
  status: EVENT_STATUS_VALUE.TODO,
  remark: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

/* ============ 步骤详情逻辑 ============ */
const MAX_REMARK_LENGTH = 500
const MAX_REMARK_LINES = 50

const remarkLineCount = computed(() => {
  if (!form.remark) return 0
  return String(form.remark).split(/\r?\n/).length
})

function normalizeRemarkLines(remark: string): string {
  return String(remark || '')
    .replace(/\r\n/g, '\n')
    .split('\n')
    .slice(0, MAX_REMARK_LINES)
    .join('\n')
    .slice(0, MAX_REMARK_LENGTH)
}

function limitRemarkLines(value: string) {
  const nextRemark = normalizeRemarkLines(value)
  if (nextRemark !== form.remark) {
    form.remark = nextRemark
  }
}

function normalizePickerTimeValue(time?: string | null): string {
  const value = String(time || '').trim()
  if (!value) return ''
  return value.length >= 5 ? value.slice(0, 5) : value
}

function normalizeSubmitTimeValue(time?: string | null): string | null {
  const value = normalizePickerTimeValue(time)
  if (!value) return null
  return value.length === 5 ? `${value}:00` : value
}

interface TimeOption {
  value: string
  label: string
  badge?: string
}

const TIME_STEP_MINUTES = 15

function formatMinuteTime(totalMinutes: number): string {
  const safeMinutes = Math.min(Math.max(totalMinutes, 0), 23 * 60 + 59)
  const hours = Math.floor(safeMinutes / 60)
  const minutes = safeMinutes % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

function toTimeMinutes(time?: string | null): number {
  const value = normalizePickerTimeValue(time)
  if (!value) return -1
  const [hourText, minuteText = '0'] = value.split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText)
  if (Number.isNaN(hour) || Number.isNaN(minute)) return -1
  return hour * 60 + minute
}

function buildTimeOptions(): TimeOption[] {
  const options: TimeOption[] = []
  for (let minutes = 0; minutes <= 23 * 60 + 45; minutes += TIME_STEP_MINUTES) {
    const value = formatMinuteTime(minutes)
    options.push({ value, label: value })
  }
  options.push({ value: '23:59', label: '23:59' })
  return options
}

function appendCustomTimeOption(options: TimeOption[], time?: string | null, badge?: string): TimeOption[] {
  const value = normalizePickerTimeValue(time)
  if (!value) return options
  const matched = options.find(item => item.value === value)
  if (matched) {
    return badge ? options.map(item => item.value === value ? { ...item, badge } : item) : options
  }

  const nextOptions = [...options, { value, label: value, badge }]
  nextOptions.sort((a, b) => toTimeMinutes(a.value) - toTimeMinutes(b.value))
  return nextOptions
}

function getDefaultStartTime(): string {
  const now = new Date()
  return formatMinuteTime(now.getHours() * 60 + now.getMinutes())
}

function addMinutesToTime(time: string, deltaMinutes: number): string {
  const minutes = toTimeMinutes(time)
  if (minutes < 0) return ''
  return formatMinuteTime(minutes + deltaMinutes)
}

const baseTimeOptions = buildTimeOptions()

const startTimeOptions = computed(() => {
  let options = appendCustomTimeOption(baseTimeOptions, form.startTime)
  if (!props.eventId && currentTimeSnapshot.value) {
    options = appendCustomTimeOption(options, currentTimeSnapshot.value, '当前')
  }
  return options
})

const endTimeOptions = computed(() => {
  const startMinutes = toTimeMinutes(form.startTime)
  const filtered = startMinutes < 0
    ? [...baseTimeOptions]
    : baseTimeOptions.filter(item => toTimeMinutes(item.value) > startMinutes)
  return appendCustomTimeOption(filtered, form.endTime)
})

function applyCurrentStartTime() {
  currentTimeSnapshot.value = getDefaultStartTime()
  form.startTime = currentTimeSnapshot.value
}

function applyEndTimeOffset(deltaMinutes: number) {
  if (!form.startTime) return
  form.endTime = addMinutesToTime(form.startTime, deltaMinutes)
}
/* ============ 步骤详情逻辑结束 ============ */

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      resetForm()
      currentTimeSnapshot.value = getDefaultStartTime()
      if (props.defaultDate) {
        form.eventDate = props.defaultDate
      }
      if (props.eventId) {
        await loadDetail(props.eventId)
      } else {
        form.startTime = currentTimeSnapshot.value
      }
    }
  }
)

watch(
  () => form.startTime,
  (value) => {
    if (!value || !form.endTime) return
    if (toTimeMinutes(form.endTime) <= toTimeMinutes(value)) {
      form.endTime = ''
    }
  }
)



async function loadDetail(id: number) {
  try {
    const res: any = await getEventDetailApi(id)
    const d = res.data
    Object.assign(form, {
      title: d.title || '',
      eventDate: d.eventDate || '',
      startTime: normalizePickerTimeValue(d.startTime),
      endTime: normalizePickerTimeValue(d.endTime),
      category: d.category ?? EVENT_CATEGORY_VALUE.WORK,
      priority: d.priority ?? EVENT_PRIORITY_VALUE.MEDIUM,
      status: d.status ?? EVENT_STATUS_VALUE.TODO,
      remark: normalizeRemarkLines(d.remark || d.description || '')
    })
  } catch {}
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const remark = normalizeRemarkLines(form.remark)
    form.remark = remark

    const payload: any = {
      title: form.title,
      eventDate: form.eventDate,
      category: form.category,
      priority: form.priority,
      status: form.status,
      remark: remark || null,
      description: remark || null,
      startTime: normalizeSubmitTimeValue(form.startTime),
      endTime: normalizeSubmitTimeValue(form.endTime)
    }

    if (props.eventId) {
      await updateEventApi(props.eventId, payload)
      ElMessage.success('日程已更新')
    } else {
      await saveEventApi(payload)
      ElMessage.success('日程已创建')
    }

    emit('update:visible', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!props.eventId) return
  try {
    await ElMessageBox.confirm(
      '确定要删除此已取消的日程吗？删除后不可恢复。',
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    deleting.value = true
    try {
      await deleteEventApi(props.eventId)
      ElMessage.success('日程已删除')
      emit('update:visible', false)
      emit('deleted')
    } finally {
      deleting.value = false
    }
  } catch {
    // 用户取消
  }
}

function resetForm() {
  form.title = ''
  form.eventDate = ''
  form.startTime = ''
  form.endTime = ''
  form.category = EVENT_CATEGORY_VALUE.WORK
  form.priority = EVENT_PRIORITY_VALUE.MEDIUM
  form.status = EVENT_STATUS_VALUE.TODO
  form.remark = ''
  formRef.value?.clearValidate()
}
</script>

<style lang="scss">
.event-drawer {
  .el-drawer__header {
    padding: 0 !important;
    margin-bottom: 0 !important;
    border-bottom: 1px solid #dbe2ea;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  }

  .el-drawer__body {
    padding: 14px 20px 0;
    overflow-y: auto;
    background: #f8fafc;

    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: #d0d7e2; border-radius: 4px; }
  }

  .el-drawer__footer {
    padding: 12px 20px;
    border-top: 1px solid #dbe2ea;
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(8px);
  }

  .el-form-item__label {
    font-size: 12.5px !important;
    font-weight: 600 !important;
    color: #5b6475 !important;
    padding-bottom: 6px !important;
    line-height: 1 !important;
  }

  .el-form-item {
    margin-bottom: 14px;
  }

  .el-input__wrapper,
  .el-select__wrapper,
  .el-textarea__inner,
  .el-date-editor.el-input__wrapper,
  .el-date-editor .el-input__wrapper {
    background: #ffffff;
    box-shadow: 0 0 0 1px #dbe2ea inset !important;
    border-radius: 10px;
    transition: box-shadow .18s ease, transform .18s ease;
  }

  .el-input__wrapper:hover,
  .el-select__wrapper:hover,
  .el-textarea__inner:hover,
  .el-date-editor.el-input__wrapper:hover,
  .el-date-editor .el-input__wrapper:hover {
    box-shadow: 0 0 0 1px #bdd3f7 inset !important;
  }

  .is-focus .el-input__wrapper,
  .is-focus.el-select__wrapper,
  .el-textarea__inner:focus,
  .el-date-editor.is-active,
  .el-date-editor.is-active .el-input__wrapper {
    box-shadow: 0 0 0 1.5px #0958d9 inset, 0 0 0 4px rgba(9, 88, 217, 0.08) !important;
  }

  .el-input__inner,
  .el-textarea__inner {
    font-size: 13.5px;
    color: #1f2937;
  }

  .el-textarea__inner {
    line-height: 1.6;
    min-height: 96px !important;
  }
}

.event-drawer-time-popper {
  border: 1px solid #dbe2ea !important;
  border-radius: 14px !important;
  padding: 6px !important;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12) !important;

  .el-popper__arrow {
    display: none !important;
  }

  .el-select-dropdown__item {
    margin: 2px 0;
    padding: 0 10px !important;
    min-height: 38px;
    border-radius: 10px;
  }

  .el-select-dropdown__item.is-hovering,
  .el-select-dropdown__item:hover {
    background: #f5f8ff !important;
    color: #0958d9 !important;
  }

  .el-select-dropdown__item.is-selected {
    background: #eaf2ff !important;
    color: #0958d9 !important;
    font-weight: 700;
  }
}
</style>

<style scoped lang="scss">
$primary: #0958d9;
$primary-light: #eaf2ff;
$ink: #1f2937;
$ink2: #5b6475;
$sub: #7b8798;
$border: #dbe2ea;
$bg: #f5f7fb;
$danger: #cf1322;
$warning: #d97706;
$rs: 10px;

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  width: 100%;
  gap: 12px;
}

.drawer-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.drawer-title-copy {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
}

.drawer-icon {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);

  &.icon-add { background: $primary-light; color: $primary; }
  &.icon-edit { background: #fff4db; color: $warning; }
}

.drawer-title-text {
  font-size: 16px;
  font-weight: 700;
  color: $ink;
}

.drawer-subtitle {
  font-size: 12px;
  line-height: 1.5;
  color: $sub;
}

.drawer-date-badge {
  font-size: 12px;
  color: $primary;
  background: $primary-light;
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 表单 */
.event-form {
  padding-bottom: 8px;
  padding-top: 2px;
}

.form-section-label {
  font-size: 11.5px;
  font-weight: 700;
  color: $ink2;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 10px;
  border-left: 3px solid $primary;
  padding-left: 8px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 0;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.time-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.time-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  font-size: 13px;
}

.time-option-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  height: 20px;
  border-radius: 999px;
  background: #eaf2ff;
  color: $primary;
  font-size: 11px;
  font-weight: 700;
}

.time-shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.time-shortcut-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(219, 226, 234, 0.96);
  border-radius: 999px;
  background: #ffffff;
  color: $ink2;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all .18s ease;

  &:hover:not(:disabled) {
    color: $primary;
    border-color: rgba(9, 88, 217, 0.2);
    background: #f5f8ff;
  }

  &:disabled {
    cursor: not-allowed;
    color: #a8b2c1;
    background: #f8fafc;
  }

  &.is-primary {
    color: $primary;
    background: $primary-light;
    border-color: rgba(9, 88, 217, 0.14);
  }
}

/* 分类选项 */
.cat-option {
  display: flex;
  align-items: center;
  gap: 8px;
  .cat-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }
}

/* 状态选项 */
.status-option {
  display: flex;
  align-items: center;
  gap: 8px;
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
    &.s-0 { background: #fa8c16; }
    &.s-1 { background: #1677ff; }
    &.s-2 { background: #52c41a; }
    &.s-3 { background: #8c8c8c; }
  }
}

/* 优先级按钮组 */
.priority-group {
  display: flex;
  gap: 6px;
  width: 100%;
}

.pri-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12.5px;
  font-weight: 600;
  padding: 8px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all .18s;
  border: 1.5px solid $border;
  background: #f8fafc;
  color: $sub;

  &:hover {
    border-color: currentColor;
    background: #fff;
    transform: translateY(-1px);
  }

  &.pri-0 {
    &.active { border-color: #64748b; background: #f8fafc; color: #475569; }
  }

  &.pri-1 {
    color: $warning;
    &.active { border-color: $warning; background: #fff4db; color: $warning; }
  }

  &.pri-2 {
    color: $danger;
    &.active {
      border-color: $danger;
      background: #fff1f0;
      color: $danger;
      box-shadow: 0 8px 18px rgba(207, 19, 34, 0.08);

      svg { fill: currentColor; }
    }
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 640px) {
  .drawer-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-date-badge {
    align-self: flex-start;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .priority-group {
    flex-direction: column;
  }
}

/* ============ 步骤详情 ============ */
.step-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-textarea-wrap {
  width: 100%;
  overflow: visible;
}

.step-textarea {
  width: 100%;
}

.step-textarea :deep(.el-textarea__inner) {
  min-height: 120px !important;
  line-height: 1.6;
  padding: 10px 12px;
  /* 完全自适应高度，不出现滚动条 */
  overflow: hidden !important;

  &::-webkit-scrollbar {
    display: none !important;
    width: 0 !important;
    height: 0 !important;
  }
}

.step-footer-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 2px;

  span {
    font-size: 11.5px;
    color: #b0bac9;
  }
}

@media (max-width: 640px) {
  .step-textarea :deep(.el-textarea__inner) {
    min-height: 100px !important;
  }
}


</style>
