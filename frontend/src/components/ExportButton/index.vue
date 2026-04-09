<template>
  <el-button
    :type="type"
    :icon="Download"
    :loading="loading"
    :disabled="disabled"
    @click="handleClick"
  >
    {{ text }}
  </el-button>
</template>

<script setup lang="ts">
import { Download } from '@element-plus/icons-vue'

interface Props {
  text?: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | ''
  loading?: boolean
  disabled?: boolean
}

interface Emits {
  (e: 'click'): void
}

const props = withDefaults(defineProps<Props>(), {
  text: '导出',
  type: 'success',
  loading: false,
  disabled: false
})

const emit = defineEmits<Emits>()

const handleClick = () => {
  emit('click')
}
</script>
