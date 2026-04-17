<template>
  <el-drawer
    v-model="innerVisible"
    title="处理反馈"
    size="480px"
    destroy-on-close
    @close="handleClose"
  >
    <template v-if="feedback">
      <!-- 反馈摘要 -->
      <div class="feedback-summary">
        <div class="summary-header">
          <span class="feedback-no">{{ feedback.feedbackNo }}</span>
          <el-tag :type="(FEEDBACK_STATUS_TAG_TYPE[feedback.status] as any)" size="small">
            {{ feedback.statusDesc }}
          </el-tag>
          <el-tag :type="(FEEDBACK_PRIORITY_TAG_TYPE[feedback.priority] as any)" size="small">
            {{ feedback.priorityDesc }}
          </el-tag>
        </div>
        <div class="summary-title">{{ feedback.title }}</div>
        <div class="summary-meta">提交人：{{ feedback.submitter }}　提交时间：{{ formatTime(feedback.createTime as any) }}</div>
      </div>

      <el-divider />

      <!-- 处理面板 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="process-form"
      >
        <!-- 快捷操作按钮 -->
        <el-form-item label="快捷操作">
          <div class="quick-actions">
            <el-button
              v-for="action in quickActions"
              :key="action.status"
              :type="action.btnType"
              plain
              size="small"
              :disabled="feedback.status === action.status"
              @click="quickSetStatus(action.status)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="修改状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择目标状态" style="width: 200px">
            <el-option
              v-for="opt in FEEDBACK_STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="处理备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="5"
            placeholder="请填写处理说明（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left" style="margin: 8px 0 16px;">仅追加备注</el-divider>

        <el-form-item label="追加备注" prop="remarkOnly">
          <el-input
            v-model="remarkOnlyContent"
            type="textarea"
            :rows="3"
            placeholder="不改变状态，仅追加一条处理备注"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label=" ">
          <el-button
            :loading="addingRemark"
            @click="handleAddRemarkOnly"
          >追加备注</el-button>
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        保存状态变更
      </el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  updateFeedbackStatusApi,
  addFeedbackRemarkApi,
  type FeedbackVO
} from '@/api/feedback'
import {
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_STATUS_TAG_TYPE,
  FEEDBACK_PRIORITY_TAG_TYPE
} from '@/constants/dict'
import { formatTime } from '@/utils/formatters'

const props = defineProps<{ visible: boolean; feedback: FeedbackVO | null }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'success'): void
}>()

const innerVisible = ref(props.visible)
watch(() => props.visible, v => { innerVisible.value = v })
watch(innerVisible, v => emit('update:visible', v))

const formRef = ref<FormInstance>()
const submitting = ref(false)
const addingRemark = ref(false)
const remarkOnlyContent = ref('')

const form = ref({ status: props.feedback?.status ?? 0, remark: '' })

watch(() => props.feedback, f => {
  if (f) form.value.status = f.status
})

const rules: FormRules = {
  status: [{ required: true, message: '请选择目标状态', trigger: 'change' }],
  remark: [
    { required: true, message: '处理备注为必填项', trigger: 'blur' },
    { max: 500, message: '备注不超过500字', trigger: 'blur' }
  ]
}

const quickActions = [
  { status: 0, label: '标记待处理', btnType: 'warning' as const },
  { status: 1, label: '开始修复',   btnType: 'primary' as const },
  { status: 2, label: '标记已解决', btnType: 'success' as const },
  { status: 3, label: '关闭反馈',   btnType: 'info'    as const }
]

function quickSetStatus(status: number) {
  form.value.status = status
}

async function handleSubmit() {
  if (!props.feedback) return
  await formRef.value?.validate()
  submitting.value = true
  try {
    await updateFeedbackStatusApi(props.feedback.id, {
      status: form.value.status,
      remark: form.value.remark
    })
    ElMessage.success('处理成功')
    emit('success')
  } finally {
    submitting.value = false
  }
}

async function handleAddRemarkOnly() {
  if (!props.feedback) return
  if (!remarkOnlyContent.value.trim()) {
    ElMessage.warning('请填写备注内容')
    return
  }
  addingRemark.value = true
  try {
    await addFeedbackRemarkApi(props.feedback.id, {
      remark: remarkOnlyContent.value
    })
    ElMessage.success('备注已追加')
    remarkOnlyContent.value = ''
    emit('success')
  } finally {
    addingRemark.value = false
  }
}

function handleClose() {
  innerVisible.value = false
  formRef.value?.resetFields()
  remarkOnlyContent.value = ''
}
</script>

<style scoped lang="scss">
.feedback-summary {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 4px;

  .summary-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .feedback-no {
      font-family: monospace;
      font-size: 12px;
      color: #8c8c8c;
    }
  }

  .summary-title {
    font-size: 15px;
    font-weight: 600;
    color: #262626;
    margin-bottom: 6px;
    line-height: 1.4;
  }

  .summary-meta {
    font-size: 12px;
    color: #8c8c8c;
  }
}

.process-form {
  padding: 0 4px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
