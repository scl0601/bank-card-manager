<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :destroy-on-close="destroyOnClose"
    :before-close="handleBeforeClose"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-width="labelWidth"
      :label-position="labelPosition"
      :disabled="disabled"
    >
      <slot :form="formData" :is-edit="isEdit" />
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel" :disabled="loading">
          取消
        </el-button>
        <el-button type="primary" @click="handleConfirm" :loading="loading">
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

interface Props {
  modelValue: boolean
  title?: string
  width?: string | number
  formData: Record<string, any>
  rules?: FormRules
  labelWidth?: string | number
  labelPosition?: 'left' | 'right' | 'top'
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
  destroyOnClose?: boolean
  loading?: boolean
  disabled?: boolean
  isEdit?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'closed'): void
}

const props = withDefaults(defineProps<Props>(), {
  title: '对话框',
  width: '500px',
  labelWidth: '100px',
  labelPosition: 'right',
  closeOnClickModal: false,
  closeOnPressEscape: true,
  destroyOnClose: true,
  loading: false,
  disabled: false,
  isEdit: false
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()

// 双向绑定 visible
const visible = ref(props.modelValue)
watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
  }
)
watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 确认
const handleConfirm = async () => {
  if (!formRef.value) {
    emit('confirm')
    return
  }

  try {
    await formRef.value.validate()
    emit('confirm')
  } catch {
    // 验证失败
  }
}

// 取消
const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

// 关闭前
const handleBeforeClose = (done: () => void) => {
  if (props.loading) {
    return
  }
  done()
}

// 关闭后
const handleClosed = () => {
  formRef.value?.resetFields()
  emit('closed')
}

// 暴露方法
defineExpose({
  formRef,
  validate: () => formRef.value?.validate(),
  validateField: (props?: string[]) => formRef.value?.validateField(props),
  resetFields: () => formRef.value?.resetFields(),
  clearValidate: (props?: string[]) => formRef.value?.clearValidate(props)
})
</script>

<style scoped lang="scss">
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
