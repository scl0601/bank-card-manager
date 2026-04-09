<template>
  <span class="amount-display" :class="amountClass" :style="amountStyle">
    {{ displayText }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatAmount, formatAmountWithSign } from '@/utils/formatters'

interface Props {
  value: number | string | null | undefined
  /** 是否显示正负号 */
  showSign?: boolean
  /** 是否显示千分位 */
  showThousands?: boolean
  /** 小数位数 */
  decimals?: number
  /** 前缀符号（如 ¥、$） */
  prefix?: string
  /** 正数颜色 */
  positiveColor?: string
  /** 负数颜色 */
  negativeColor?: string
  /** 零值颜色 */
  zeroColor?: string
  /** 是否加粗 */
  bold?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showSign: false,
  showThousands: true,
  decimals: 2,
  prefix: '',
  positiveColor: '#67C23A',
  negativeColor: '#F56C6C',
  zeroColor: '#606266',
  bold: true
})

// 数值
const numValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') {
    return 0
  }
  const num = Number(props.value)
  return isNaN(num) ? 0 : num
})

// 格式化后的文本
const displayText = computed(() => {
  let text = props.showSign 
    ? formatAmountWithSign(numValue.value)
    : formatAmount(numValue.value)
  
  if (props.prefix) {
    text = props.prefix + text
  }
  
  return text
})

// 样式类
const amountClass = computed(() => ({
  'amount-positive': numValue.value > 0,
  'amount-negative': numValue.value < 0,
  'amount-zero': numValue.value === 0,
  'amount-bold': props.bold
}))

// 内联样式
const amountStyle = computed(() => {
  let color = props.zeroColor
  if (numValue.value > 0) {
    color = props.positiveColor
  } else if (numValue.value < 0) {
    color = props.negativeColor
  }
  
  return {
    color
  }
})
</script>

<style scoped lang="scss">
.amount-display {
  font-family: 'Courier New', Courier, monospace;
  white-space: nowrap;

  &.amount-bold {
    font-weight: 600;
  }
}
</style>
