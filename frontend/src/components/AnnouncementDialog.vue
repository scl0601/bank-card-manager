<template>
  <el-dialog
    v-model="visible"
    width="520px"
    align-center
    class="announcement-dialog"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <template #header>
      <div class="announcement-header">
        <span class="announcement-icon">
          <el-icon><Bell /></el-icon>
        </span>
        <div>
          <h2 class="announcement-title">公告提示</h2>
          <p class="announcement-subtitle">
            <span v-if="announcement.publishTime">{{ formatTime(announcement.publishTime) }}</span>
            <span v-else>更新内容</span>
          </p>
        </div>
      </div>
    </template>

    <div class="announcement-body">
      <p class="announcement-intro">本次更新带来以下优化：</p>
      <ul class="announcement-list">
        <li v-for="item in contentLines" :key="item">{{ item }}</li>
      </ul>
    </div>

    <template #footer>
      <div class="announcement-footer">
        <el-button @click="emit('silent-today', announcement.id)">今日不弹</el-button>
        <el-button type="primary" @click="emit('read', announcement.id)">我知道啦</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Announcement } from '@/api/announcement'

const props = defineProps<{
  modelValue: boolean
  announcement: Announcement
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'read', id: number): void
  (e: 'silent-today', id: number): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const contentLines = computed(() =>
  props.announcement.content
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean)
)

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
:deep(.announcement-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.announcement-dialog .el-dialog__header) {
  padding: 22px 24px 14px;
  margin-right: 0;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.announcement-dialog .el-dialog__body) {
  padding: 20px 24px 8px;
}

:deep(.announcement-dialog .el-dialog__footer) {
  padding: 14px 24px 22px;
}

.announcement-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.announcement-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: var(--color-primary);
  background: #eaf2ff;
  font-size: 20px;
  flex-shrink: 0;
}

.announcement-title {
  color: var(--color-text-primary);
  font-size: 18px;
  line-height: 1.4;
  font-weight: var(--font-weight-semibold);
}

.announcement-subtitle {
  margin-top: 2px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.4;
}

.announcement-body {
  color: var(--color-text-regular);
}

.announcement-intro {
  color: var(--color-text-primary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 18px;
  margin: 0;
}

.announcement-list li {
  line-height: 1.6;
  padding-left: 2px;
}

.announcement-list li::marker {
  color: var(--color-primary);
}

.announcement-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.announcement-footer :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
