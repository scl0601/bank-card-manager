<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon size="22"><CreditCard /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">卡务管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#1677ff"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧内容区 -->
    <div class="main-content">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
            <Fold v-if="!isCollapsed" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" :max="99" :hidden="unreadCount <= 0" class="announcement-badge">
            <el-tooltip content="更新记录" placement="bottom">
              <el-button class="announcement-btn" circle @click="openAnnouncementHistory">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-tooltip>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="28" icon="UserFilled" />
              <span>{{ authStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容 -->
      <main class="content-area" :class="{ 'dense-content-area': isDenseRoute }">
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="['Monitor','Dashboard','Cards','CardUsers','Transactions','Books','Bills','ProfitStats','SpecialChannel','Reminders','Feedbacks','Calendar','Logs','Announcements']">
            <component :is="Component" :key="route.name || route.path" />
          </keep-alive>
        </router-view>
      </main>
    </div>

    <!-- 全局 AI 悬浮助手 -->
    <AnnouncementDialog
      v-if="latestAnnouncement"
      v-model="announcementDialogVisible"
      :announcement="latestAnnouncement"
      @read="handleAnnouncementRead"
      @silent-today="handleAnnouncementSilentToday"
    />
    <AnnouncementHistoryDrawer
      v-model="announcementHistoryVisible"
      @changed="refreshAnnouncementUnread"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineAsyncComponent, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessageBox } from '@/plugins/element-feedback'
import { getProfitUserMonthListApi } from '@/api/profit'
import type { Announcement } from '@/api/announcement'
import {
  getLatestAnnouncementApi,
  getAnnouncementUnreadCountApi,
  markAnnouncementReadApi,
  silentAnnouncementTodayApi
} from '@/api/announcement'

const AnnouncementDialog = defineAsyncComponent(() => import('@/components/AnnouncementDialog.vue'))
const AnnouncementHistoryDrawer = defineAsyncComponent(() => import('@/components/AnnouncementHistoryDrawer.vue'))

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)
const unreadCount = ref(0)
const latestAnnouncement = ref<Announcement | null>(null)
const announcementDialogVisible = ref(false)
const announcementHistoryVisible = ref(false)

const allMenuItems = [
  { path: '/dashboard',    title: '首页看板',   icon: 'House' },
  { path: '/users',        title: '用户信息',   icon: 'UserFilled' },
  { path: '/cards',        title: '卡务管理',   icon: 'CreditCard' },
  { path: '/bills',        title: '账单信息',   icon: 'Document' },
  { path: '/profits',      title: '收益统计',   icon: 'TrendCharts' },
  { path: '/special',      title: '特殊通道',   icon: 'Connection' },
  { path: '/calendar',     title: '日历计划',   icon: 'Calendar' },
  { path: '/books',        title: '个人记账',   icon: 'Wallet' },
  { path: '/monitor',      title: '监控列表',   icon: 'Monitor', roles: ['MONITOR'] },
  { path: '/announcements', title: '公告管理',   icon: 'Bell', roles: ['MONITOR'] },
  { path: '/logs',         title: '系统日志',   icon: 'Tickets', roles: ['MONITOR'] }
]

const menuItems = computed(() => {
  if (authStore.role === 'MONITOR') {
    return allMenuItems.filter(item => item.roles?.includes('MONITOR'))
  }
  return allMenuItems.filter(item => !item.roles || item.roles.includes(authStore.role))
})

const activeMenu = computed(() => route.path)
const denseRouteNames = new Set(['Cards', 'CardUsers', 'Bills', 'ProfitStats', 'SpecialChannel', 'Books'])
const isDenseRoute = computed(() => denseRouteNames.has(String(route.name || '')))
const currentTitle = computed(() =>
  menuItems.value.find(m => m.path === route.path)?.title || ''
)
let routePrefetchTimers: number[] = []
const currentYear = new Date().getFullYear()

function requestIdle(task: () => void, timeout = 1200) {
  const ric = (window as any).requestIdleCallback
  if (typeof ric === 'function') {
    routePrefetchTimers.push(ric(task, { timeout }))
    return
  }
  routePrefetchTimers.push(window.setTimeout(task, timeout))
}

function cancelIdle() {
  const cic = (window as any).cancelIdleCallback
  for (const timer of routePrefetchTimers) {
    if (typeof cic === 'function') cic(timer)
    else window.clearTimeout(timer)
  }
  routePrefetchTimers = []
}

function prefetchRouteChunks() {
  requestIdle(() => {
    void Promise.allSettled([
      import('@/views/card/CardList.vue'),
      import('@/views/bill/BillList.vue'),
      import('@/views/book/BookList.vue'),
      import('@/views/profit/ProfitStatsView.vue'),
      import('@/views/special/SpecialChannelView.vue'),
      import('@/views/calendar/CalendarView.vue')
    ])
  })
}

function prefetchProfitStats() {
  if (authStore.role === 'MONITOR') return
  requestIdle(() => {
    const params = { year: currentYear }
    const key = `userMonths|${JSON.stringify(params)}`
    const cache = ((window as any).__profitStatsPrefetchCache ||= {
      key: '',
      rows: [],
      fetchedAt: 0,
      promise: null
    })
    if (cache.key === key && (cache.promise || Date.now() - cache.fetchedAt < 30 * 1000)) return
    cache.key = key
    cache.promise = getProfitUserMonthListApi(params)
      .then((res: any) => {
        cache.rows = res.data || []
        cache.fetchedAt = Date.now()
        return cache.rows
      })
      .finally(() => {
        cache.promise = null
      })
  }, 1800)
}

async function loadAnnouncementEntry() {
  try {
    await Promise.all([loadLatestAnnouncement(), refreshAnnouncementUnread()])
  } catch {
    // 公告加载失败不影响主流程
  }
}

async function loadLatestAnnouncement() {
  const res = await getLatestAnnouncementApi()
  latestAnnouncement.value = res.data || null
  if (authStore.role !== 'MONITOR' && latestAnnouncement.value?.shouldPopup) {
    announcementDialogVisible.value = true
  }
}

async function refreshAnnouncementUnread() {
  const res = await getAnnouncementUnreadCountApi()
  unreadCount.value = Number(res.data || 0)
}

function openAnnouncementHistory() {
  announcementHistoryVisible.value = true
}

async function handleAnnouncementRead(id: number) {
  await markAnnouncementReadApi(id)
  announcementDialogVisible.value = false
  if (latestAnnouncement.value?.id === id) {
    latestAnnouncement.value.read = true
  }
  await refreshAnnouncementUnread()
}

async function handleAnnouncementSilentToday(id: number) {
  await silentAnnouncementTodayApi(id)
  announcementDialogVisible.value = false
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    authStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  prefetchRouteChunks()
  prefetchProfitStats()
  loadAnnouncementEntry()
})
onUnmounted(cancelIdle)
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  min-width: 1024px;
  width: 100%;
}

@supports (height: 100dvh) {
  .main-layout {
    height: 100dvh;
    min-height: 100dvh;
  }
}

.sidebar {
  width: var(--sidebar-width);
  min-height: 100%;
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
  flex-shrink: 0;

  &.collapsed {
    width: 64px;
  }

  .logo {
    height: var(--header-height);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    border-bottom: 1px solid #002140;
    padding: 0 16px;
    white-space: nowrap;
  }

  .el-menu {
    border-right: none;
    width: 100%;
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  min-height: 0;
}

.header {
  height: var(--header-height);
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    min-width: 0;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }

  .collapse-btn {
    font-size: 18px;
    cursor: pointer;
    color: #595959;
    &:hover { color: var(--color-primary); }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    color: #333;
    font-size: 14px;
    min-width: 0;
    white-space: nowrap;
  }
}

.announcement-badge {
  display: inline-flex;
}

.announcement-btn.el-button {
  width: 32px;
  height: 32px;
  color: #526074;
  border-color: #d7dee8;
  background: #fff;

  &:hover {
    color: var(--color-primary);
    border-color: #9ec5ff;
    background: #f3f8ff;
  }
}

.content-area {
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  background: var(--color-bg);
  scrollbar-gutter: stable;
}

.content-area.dense-content-area {
  display: flex;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  padding: 8px;
}

.content-area.dense-content-area :deep(.cards-page),
.content-area.dense-content-area :deep(.card-user-page),
.content-area.dense-content-area :deep(.bill-page),
.content-area.dense-content-area :deep(.book-workbench),
.content-area.dense-content-area :deep(.profit-page),
.content-area.dense-content-area :deep(.special-page) {
  flex: 1;
  min-height: 0;
  min-width: 0;
}
</style>
