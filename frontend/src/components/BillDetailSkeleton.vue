<template>
  <div class="detail-skeleton" style="padding: 20px;">
    <el-skeleton :rows="5" animated>
      <template #template>
        <div style="display: flex; align-items: center; margin-bottom: 16px;">
          <el-skeleton-item variant="text" style="width: 20%; margin-right: 16px;" />
          <el-skeleton-item variant="text" style="width: 30%; margin-right: 16px;" />
          <el-skeleton-item variant="text" style="width: 15%; margin-right: 16px;" />
          <el-skeleton-item variant="text" style="width: 20%;" />
        </div>
        <el-skeleton-item variant="rect" style="height: 40px; margin-bottom: 10px;" />
        <el-skeleton-item variant="rect" style="height: 40px; margin-bottom: 10px;" />
        <el-skeleton-item variant="rect" style="height: 40px; margin-bottom: 10px;" />
        <el-skeleton-item variant="rect" style="height: 40px; margin-bottom: 10px;" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
// 骨架屏组件，用于明细加载时的占位显示
</script>

<style scoped>
.detail-skeleton {
  background: #fafbfc;
  border-radius: 4px;
}
</style>
