<template>
  <span class="card-number-display" :class="{ 'is-compact': compact }">
    <span class="card-last4">{{ displayCardNo }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 卡号后四位 */
  last4?: string | null
  /** 保留兼容旧调用，当前不再显示脱敏前缀 */
  maskPrefix?: string
  /** 完整卡号（如果传入则优先展示完整卡号） */
  fullCardNo?: string | null
  /** 紧凑模式（适用于表格等窄列场景） */
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  last4: '',
  maskPrefix: '',
  fullCardNo: '',
  compact: false
})

const displayCardNo = computed(() => {
  if (props.fullCardNo) {
    return props.fullCardNo
  }
  if (props.last4) {
    return props.last4
  }
  return '-'
})
</script>

<style scoped lang="scss">
.card-number-display {
  font-family: 'Courier New', Courier, monospace;
  letter-spacing: 0.5px;
  white-space: nowrap;

  .card-last4 {
    color: #303133;
    font-weight: 600;
  }

  &.is-compact {
    display: inline-flex;
    align-items: baseline;
    letter-spacing: 0;
    font-size: 13px;

    .card-last4 {
      color: #1d2129;
      font-weight: 700;
      font-size: 13px;
      letter-spacing: 0.8px;
    }
  }
}
</style>
