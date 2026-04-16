<template>
  <router-view v-slot="{ Component, route }">
    <transition name="fade-slide" mode="out-in">
      <!-- 移动端路由：使用移动端布局（由 MobileLayout 内部处理） -->
      <component
        v-if="route.meta?.isMobile"
        :is="Component"
        :key="route.path"
      />
      <!-- 桌面端路由 -->
      <component
        v-else
        :is="Component"
        :key="route.path"
      />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import '@/utils/device'
</script>
