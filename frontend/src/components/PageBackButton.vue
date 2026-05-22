<template>
  <button class="page-back-button" type="button" title="返回上一页" @click="goBack">
    <el-icon :size="15"><ArrowLeft /></el-icon>
    <span>返回</span>
  </button>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  fallback?: string
}>(), {
  fallback: '/dashboard'
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push(props.fallback)
}
</script>

<style scoped lang="scss">
.page-back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #dbe2ea;
  border-radius: 10px;
  background: #fff;
  color: #526074;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.04);
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

  &:hover {
    color: #0958d9;
    border-color: rgba(9, 88, 217, 0.24);
    background: #eaf2ff;
    box-shadow: 0 6px 14px rgba(9, 88, 217, 0.08);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}
</style>
