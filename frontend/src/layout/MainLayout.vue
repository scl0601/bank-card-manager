<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon size="22"><CreditCard /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">银行卡管理</span>
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
      <main class="content-area">
        <router-view />
      </main>
    </div>

    <!-- 全局 AI 悬浮助手 -->
    <AiFloatWidget />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessageBox } from 'element-plus'
import AiFloatWidget from '@/components/AiFloatWidget.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const menuItems = [
  { path: '/dashboard',    title: '首页看板',   icon: 'House' },
  { path: '/owners',       title: '持卡人管理', icon: 'User' },
  { path: '/cards',        title: '银行卡管理', icon: 'CreditCard' },
  { path: '/transactions', title: '流水管理',   icon: 'List' },
  { path: '/books',        title: '个人记账',   icon: 'Wallet' },
  { path: '/bills',        title: '账单管理',   icon: 'Document' },
  { path: '/reminders',    title: '提醒中心',   icon: 'Bell' },
  { path: '/calendar',     title: '日历计划',   icon: 'Calendar' },
  { path: '/logs',         title: '系统日志',   icon: 'Tickets' }
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() =>
  menuItems.find(m => m.path === route.path)?.title || ''
)

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: var(--sidebar-width);
  min-height: 100vh;
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
  }
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--color-bg);
}
</style>
