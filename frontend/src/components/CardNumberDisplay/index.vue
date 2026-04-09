<template>
  <span class="card-number-display" :class="{ 'is-compact': compact }">
    <span class="card-mask">{{ maskPrefix }}</span>
    <span class="card-last4">{{ displayLast4 }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 卡号后四位 */
  last4?: string | null
  /** 脱敏前缀（compact模式默认 ●●●●，完整模式默认 **** **** ****） */
  maskPrefix?: string
  /** 完整卡号（如果传入，会自动取后四位） */
  fullCardNo?: string | null
  /** 紧凑模式（适用于表格等窄列场景，显示为 ●●●● 1234） */
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  last4: '',
  maskPrefix: '',
  fullCardNo: '',
  compact: false
})

// 显示的后四位
const displayLast4 = computed(() => {
  if (props.last4) {
    return props.last4
  }
  if (props.fullCardNo && props.fullCardNo.length >= 4) {
    return props.fullCardNo.slice(-4)
  }
  return '-'
})

// 脱敏前缀
const maskPrefix = computed(() => {
  if (!displayLast4.value || displayLast4.value === '-') {
    return ''
  }
  // 如果传入了自定义前缀则使用，否则根据模式决定
  if (props.maskPrefix) {
    return props.maskPrefix + (props.compact ? '' : ' ')
  }
  return props.compact ? '\u2022\u2022\u2022\u2022 ' : '**** **** **** '
})
</script>

<style scoped lang="scss">
.card-number-display {
  font-family: 'Courier New', Courier, monospace;
  letter-spacing: 0.5px;
  white-space: nowrap;

  .card-mask {
    color: #909399;
    margin-right: 4px;
    user-select: none;
  }

  .card-last4 {
    color: #303133;
    font-weight: 600;
  }

  &.is-compact {
    display: inline-flex;
    align-items: baseline;
    letter-spacing: 0;
    font-size: 13px;

    .card-mask {
      color: #c0c4cc;
      font-size: 11px;
      margin-right: 4px;
      letter-spacing: 1.5px;
    }

    .card-last4 {
      color: #1d2129;
      font-weight: 700;
      font-size: 13px;
      letter-spacing: 0.8px;
    }
  }
}
</style>
