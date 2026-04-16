<template>
  <div class="mobile-layout">
    <!-- 导航栏 -->
    <MobileNavBar
      :title="pageTitle"
      :show-back="showBack"
      :show-user-menu="!isLoginPage"
      :scrolled="contentScrolled"
      :class="{ 'nav-shadow': contentScrolled }"
    >
      <template #right>
        <slot name="nav-right" />
      </template>
    </MobileNavBar>

    <!-- 内容区 -->
    <main ref="contentRef" class="mobile-content" @scroll="handleScroll">
      <router-view v-slot="{ Component, route }">
        <transition :name="transitionName(route)" mode="out-in" appear>
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <!-- 底部安全区占位 -->
    <div class="mobile-safe-bottom" style="height: 8px;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, type RouteLocationNormalized } from 'vue-router'
import MobileNavBar from '../components/MobileNavBar.vue'

const route = useRoute()
const contentRef = ref<HTMLElement | null>(null)
const contentScrolled = ref(false)

const pageTitle = computed(() => {
  return (route.meta?.mobileTitle as string) || '日历计划'
})

const showBack = computed(() => {
  return route.meta?.showBack === true || route.name !== 'MobileCalendar'
})

const isLoginPage = computed(() => {
  return route.path === '/m/login'
})

// 根据路由深度决定过渡方向
function transitionName(r: RouteLocationNormalized): string {
  // 日历主页是根页面，其他页从右侧进入
  if (r.name === 'MobileCalendar') return 'mobile-slide-right'
  if (r.name === 'MobileEventDetail' || r.name === 'MobileLoginPage') return 'mobile-slide-left'
  return 'mobile-slide-left'
}

// 滚动监听 —— 为导航栏添加阴影
function handleScroll() {
  const el = contentRef.value
  if (!el) return
  contentScrolled.value = el.scrollTop > 4
}

onMounted(() => {
  contentRef.value?.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  contentRef.value?.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
@import '../../styles/index.scss';
@import '../styles/mobile.scss';

.mobile-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  background: var(--mobile-page-bg);
}

.mobile-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth; // 平滑滚动
}

// 导航栏阴影类
.nav-shadow {
  &::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(to bottom, rgba(0, 0, 0, 0.08), transparent);
  }
}
</style>
