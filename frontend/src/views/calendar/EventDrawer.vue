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
          <span class="drawer-title-text">{{ eventId ? '编辑日程' : '新建日程' }}</span>
        </div>
        <span v-if="form.eventDate" class="drawer-date-badge">{{ form.eventDate }}</span>
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
          <el-time-picker
            v-model="form.startTime"
            placeholder="开始时间"
            value-format="HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" class="form-col">
          <el-time-picker
            v-model="form.endTime"
            placeholder="结束时间"
            value-format="HH:mm:ss"
            style="width: 100%"
          />
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

      <!-- 提醒 -->
      <el-form-item label="提前提醒">
        <el-select
          v-model="form.remindBeforeMin"
          placeholder="不设置提醒"
          clearable
          style="width: 100%"
        >
          <el-option label="提前 5 分钟" :value="5" />
          <el-option label="提前 15 分钟" :value="15" />
          <el-option label="提前 30 分钟" :value="30" />
          <el-option label="提前 1 小时" :value="60" />
          <el-option label="提前 1 天" :value="1440" />
        </el-select>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="添加备注信息（选填）"
          maxlength="500"
          show-word-limit
          resize="none"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="$emit('update:visible', false)" style="border-radius:8px">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave" style="border-radius:8px;min-width:88px">
          {{ saving ? '保存中...' : (eventId ? '保存修改' : '创建日程') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, h } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getEventDetailApi, saveEventApi, updateEventApi
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
  0: '#1677ff', 1: '#52c41a', 2: '#fa8c16', 3: '#f5222d', 4: '#8c8c8c', 5: '#722ed1',
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
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  title: '',
  eventDate: '',
  startTime: '' as any,
  endTime: '' as any,
  category: EVENT_CATEGORY_VALUE.WORK,
  priority: EVENT_PRIORITY_VALUE.MEDIUM,
  status: EVENT_STATUS_VALUE.TODO,
  remark: '',
  remindBeforeMin: null as number | null
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      resetForm()
      if (props.defaultDate) {
        form.eventDate = props.defaultDate
      }
      if (props.eventId) {
        await loadDetail(props.eventId)
      }
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
      startTime: d.startTime || '',
      endTime: d.endTime || '',
      category: d.category ?? EVENT_CATEGORY_VALUE.WORK,
      priority: d.priority ?? EVENT_PRIORITY_VALUE.MEDIUM,
      status: d.status ?? EVENT_STATUS_VALUE.TODO,
      remark: d.remark || d.description || '',
      remindBeforeMin: d.remindBeforeMin ?? null
    })
  } catch {}
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

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
      remindBeforeMin: form.remindBeforeMin,
      startTime: form.startTime || null,
      endTime: form.endTime || null
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

function resetForm() {
  form.title = ''
  form.eventDate = ''
  form.startTime = ''
  form.endTime = ''
  form.category = EVENT_CATEGORY_VALUE.WORK
  form.priority = EVENT_PRIORITY_VALUE.MEDIUM
  form.status = EVENT_STATUS_VALUE.TODO
  form.remark = ''
  form.remindBeforeMin = null
  formRef.value?.clearValidate()
}
</script>

<style lang="scss">
/* Drawer 全局样式覆盖（不用 scoped） */
.event-drawer {
  .el-drawer__header {
    padding: 0 !important;
    margin-bottom: 0 !important;
    border-bottom: 1px solid #e5e6eb;
  }
  .el-drawer__body {
    padding: 20px 24px 0;
    overflow-y: auto;
    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: #d0d3da; border-radius: 4px; }
  }
  .el-drawer__footer {
    padding: 14px 24px;
    border-top: 1px solid #e5e6eb;
    background: #fafbfc;
  }
  .el-form-item__label {
    font-size: 12.5px !important;
    font-weight: 600 !important;
    color: #4e5969 !important;
    padding-bottom: 6px !important;
    line-height: 1 !important;
  }
  .el-form-item {
    margin-bottom: 18px;
  }
  .el-input__inner { font-size: 13.5px; }
  .el-textarea__inner { font-size: 13.5px; line-height: 1.6; }
}
</style>

<style scoped lang="scss">
$primary: #1677ff;
$primary-light: #e6f4ff;
$ink: #1d2129;
$ink2: #4e5969;
$sub: #86909c;
$border: #e5e6eb;
$bg: #f0f2f5;
$danger: #f5222d;
$warning: #fa8c16;
$rs: 8px;

/* Drawer 头部 */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  width: 100%;
}
.drawer-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}
.drawer-icon {
  width: 30px;
  height: 30px;
  border-radius: $rs;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  &.icon-add { background: #e6f4ff; color: $primary; }
  &.icon-edit { background: #fff7e6; color: $warning; }
}
.drawer-title-text {
  font-size: 16px;
  font-weight: 700;
  color: $ink;
}
.drawer-date-badge {
  font-size: 12px;
  color: $sub;
  background: $bg;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

/* 表单 */
.event-form { padding-bottom: 8px; }
.form-section-label {
  font-size: 11.5px;
  font-weight: 700;
  color: $sub;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 10px;
  padding-left: 1px;
  border-left: 3px solid $primary;
  padding-left: 8px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 0;
}
.form-col { flex: 1; min-width: 0; }

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
  border-radius: 7px;
  cursor: pointer;
  transition: all .18s;
  border: 1.5px solid #e5e6eb;
  background: #f7f8fa;
  color: #86909c;
  &:hover { border-color: currentColor; background: #fff; }
  &.pri-0 {
    &.active { border-color: #8c8c8c; background: #fafafa; color: #595959; }
  }
  &.pri-1 {
    color: #fa8c16;
    &.active { border-color: #fa8c16; background: #fff7e6; color: #fa8c16; }
  }
  &.pri-2 {
    color: #f5222d;
    &.active { border-color: #f5222d; background: #fff1f0; color: #f5222d;
      svg { fill: currentColor; }
    }
  }
}

/* 底部按钮 */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
