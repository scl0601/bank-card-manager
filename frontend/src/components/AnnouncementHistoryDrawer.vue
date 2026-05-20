<template>
  <el-drawer
    v-model="visible"
    title="更新记录"
    size="460px"
    class="announcement-history-drawer"
    @opened="loadHistory"
  >
    <div v-loading="loading" class="announcement-history">
      <el-empty v-if="!loading && records.length === 0" description="暂无公告" />

      <div
        v-for="item in records"
        :key="item.id"
        class="history-item"
        :class="{ unread: !item.read, active: activeId === item.id }"
        @click="openItem(item)"
      >
        <div class="history-item-header">
          <div class="history-item-title">
            <span class="unread-dot" v-if="!item.read"></span>
            <span>公告提示</span>
          </div>
          <el-tag v-if="item.pinned === 1" size="small" type="warning" effect="plain">置顶</el-tag>
        </div>
        <div class="history-item-meta">
          {{ item.publishTime ? formatTime(item.publishTime) : '未记录发布时间' }}
        </div>
        <ul v-if="activeId === item.id" class="history-content">
          <li v-for="line in splitContent(item.content)" :key="line">{{ line }}</li>
        </ul>
      </div>

      <div v-if="total > records.length" class="history-more">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Announcement } from '@/api/announcement'
import { getAnnouncementHistoryApi, markAnnouncementReadApi } from '@/api/announcement'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'changed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const loadingMore = ref(false)
const records = ref<Announcement[]>([])
const total = ref(0)
const page = ref(1)
const activeId = ref<number>()

async function loadHistory() {
  page.value = 1
  loading.value = true
  try {
    const res = await getAnnouncementHistoryApi({ current: page.value, size: 10 })
    records.value = res.data.records || []
    total.value = res.data.total || 0
    activeId.value = undefined
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  try {
    page.value += 1
    const res = await getAnnouncementHistoryApi({ current: page.value, size: 10 })
    records.value.push(...(res.data.records || []))
    total.value = res.data.total || 0
  } finally {
    loadingMore.value = false
  }
}

async function openItem(item: Announcement) {
  activeId.value = item.id
  if (item.read) return
  await markAnnouncementReadApi(item.id)
  item.read = true
  item.readTime = new Date().toISOString()
  emit('changed')
}

function splitContent(content: string) {
  return content.split(/\r?\n/).map(line => line.trim()).filter(Boolean)
}

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
.announcement-history {
  min-height: 180px;
}

.history-item {
  padding: 14px 14px 12px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
  margin-bottom: 12px;
}

.history-item:hover,
.history-item.active {
  border-color: #9ec5ff;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);
}

.history-item.unread {
  background: #f8fbff;
}

.history-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.history-item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.5;
}

.history-item-title span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-danger);
  flex-shrink: 0;
}

.history-item-meta {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.history-content {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--color-text-regular);
}

.history-content li {
  line-height: 1.6;
  margin-bottom: 6px;
}

.history-content li::marker {
  color: var(--color-primary);
}

.history-more {
  display: flex;
  justify-content: center;
  padding: 6px 0 12px;
}
</style>
