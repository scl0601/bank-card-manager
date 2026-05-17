<template>
  <el-config-provider :locale="zhCn">
    <router-view v-slot="{ Component, route }">
      <component :is="Component" v-if="!route.meta?.isMobile" />
      <component :is="Component" v-else />
    </router-view>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import '@/utils/device'
</script>
