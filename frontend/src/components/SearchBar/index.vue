<template>
  <div class="search-bar">
    <el-form
      ref="formRef"
      :model="modelValue"
      :inline="true"
      :label-width="labelWidth"
      class="search-form"
    >
      <slot />
      <el-form-item v-if="!hideSearchButton">
        <el-button type="primary" :icon="Search" @click="handleSearch">
          查询
        </el-button>
        <el-button :icon="Refresh" @click="handleReset">
          重置
        </el-button>
        <slot name="extra-buttons" />
      </el-form-item>
      <el-form-item v-else>
        <el-button :icon="Refresh" @click="handleReset">
          重置
        </el-button>
        <slot name="extra-buttons" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

interface Props {
  modelValue: Record<string, any>
  labelWidth?: string | number
  /** 是否隐藏查询按钮（autoSearch 模式下使用） */
  hideSearchButton?: boolean
}

interface Emits {
  (e: 'search'): void
  (e: 'reset'): void
  (e: 'update:modelValue', value: Record<string, any>): void
}

const props = withDefaults(defineProps<Props>(), {
  labelWidth: 'auto',
  hideSearchButton: false
})

const emit = defineEmits<Emits>()

const formRef = ref()

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  emit('reset')
}

defineExpose({
  formRef
})
</script>

<style scoped lang="scss">
.search-bar {
  background: var(--color-card);
  padding: 16px 20px 0;
  border-radius: var(--border-radius);
  margin-bottom: 16px;
  box-shadow: var(--shadow-card);

  .search-form {
    display: flex;
    flex-wrap: wrap;

    :deep(.el-form-item) {
      margin-bottom: 12px;
      margin-right: 16px;
    }

    :deep(.el-form-item:last-child) {
      margin-right: 0;
    }
  }
}
</style>
