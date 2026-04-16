<template>
  <router-view v-slot="{ Component, route }">
    <component :is="Component" v-if="!route.meta?.isMobile" />
    <component :is="Component" v-else />
  </router-view>
</template>

<script setup lang="ts">
import '@/utils/device'
</script>
