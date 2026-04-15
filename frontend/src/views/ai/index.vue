<template>
  <div class="ai-page">
    <div class="page-title">AI 智能助手</div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="智能对话" name="chat">
        <ChatPanel />
      </el-tab-pane>
      <el-tab-pane label="流水分析" name="analyze">
        <AnalyzePanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ChatPanel from './components/ChatPanel.vue'
import AnalyzePanel from './components/AnalyzePanel.vue'

const activeTab = ref('chat')
</script>

<style scoped lang="scss">
.ai-page {
  .page-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: 16px;
  }
}
</style>
