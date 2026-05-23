<template>
  <div class="detail-skeleton">
    <span class="detail-skeleton-dot"></span>
    <span>明细加载中...</span>
    <el-skeleton animated class="detail-skeleton-line">
      <template #template>
        <el-skeleton-item variant="text" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
// Compact placeholder for bill detail loading.
</script>

<style scoped>
.detail-skeleton {
  display: grid;
  grid-template-columns: auto auto minmax(120px, 1fr);
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 6px 12px;
  background: #f8fafc;
  border-top: 1px solid #edf1f6;
  color: #667085;
  font-size: 12px;
}

.detail-skeleton-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #409eff;
  box-shadow: 0 0 0 4px rgb(64 158 255 / 10%);
}

.detail-skeleton-line {
  width: min(320px, 100%);
}

.detail-skeleton-line :deep(.el-skeleton__item) {
  height: 10px;
  margin: 0;
  border-radius: 999px;
}
</style>
