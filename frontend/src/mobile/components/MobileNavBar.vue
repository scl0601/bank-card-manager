<template>
  <header class="mobile-nav-bar" :class="{ scrolled }">
    <div class="nav-left">
      <button v-if="showBack" class="nav-back btn-bounce" @click="handleBack">
        <el-icon size="18"><ArrowLeft /></el-icon>
      </button>
      <span v-if="title" class="nav-title">{{ title }}</span>
    </div>
    <div class="nav-right">
      <slot name="right" />
      <el-dropdown v-if="showUserMenu" trigger="click" @command="handleCommand">
        <span class="nav-user">
          <el-avatar :size="28" icon="UserFilled" />
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ authStore.nickname || authStore.username }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  title?: string
  showBack?: boolean
  showUserMenu?: boolean
  scrolled?: boolean
}>(), {
  showBack: false,
  showUserMenu: true,
  scrolled: false,
})

const router = useRouter()
const authStore = useAuthStore()

function handleBack() {
  router.back()
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    authStore.logout()
    router.replace('/m/login')
  }
}
</script>

<style scoped lang="scss">
.mobile-nav-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--mobile-nav-height);
  padding: 0 12px;
  background: #fff;
  border-bottom: 1px solid transparent; // 默认透明边框，滚动后通过父级添加渐变阴影
  flex-shrink: 0;
  transition: border-color 0.25s ease;

  // 滚动后显示实线边框作为视觉分隔
  &.scrolled {
    border-bottom-color: #eef0f2;
  }
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--mobile-touch-min);
  height: var(--mobile-touch-min);
  border: none;
  background: none;
  cursor: pointer;
  color: var(--color-text-primary);
  border-radius: 50%;
  &:active { background: #f0f0f0; }
}

.nav-title {
  font-size: var(--mobile-font-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.nav-user {
  display: flex;
  cursor: pointer;
}
</style>
