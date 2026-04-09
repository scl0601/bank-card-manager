<template>
  <el-tag :type="tagType" :effect="effect" :size="size">
    {{ displayText }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** 状态值 */
  value: string | number | null | undefined
  /** 状态映射对象 { value: label } */
  labelMap?: Record<string, string>
  /** 标签类型映射对象 { value: type } */
  typeMap?: Record<string, string>
  /** 默认显示文本 */
  defaultText?: string
  /** 标签尺寸 */
  size?: 'large' | 'default' | 'small'
  /** 标签效果 */
  effect?: 'dark' | 'light' | 'plain'
  /** 默认标签类型 */
  defaultType?: string
}

const props = withDefaults(defineProps<Props>(), {
  labelMap: () => ({}),
  typeMap: () => ({}),
  defaultText: '-',
  size: 'default',
  effect: 'light',
  defaultType: 'info'
})

// 显示文本
const displayText = computed(() => {
  if (props.value === null || props.value === undefined) {
    return props.defaultText
  }
  return props.labelMap[String(props.value)] || props.defaultText
})

// 标签类型
const tagType = computed(() => {
  if (props.value === null || props.value === undefined) {
    return props.defaultType
  }
  return props.typeMap[String(props.value)] || props.defaultType
})
</script>
