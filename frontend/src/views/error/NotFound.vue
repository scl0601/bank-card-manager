<template>
  <div class="not-found">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-desc">抱歉，您访问的页面不存在</div>
      <el-button type="primary" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.content {
  text-align: center;
  color: #fff;
}
.error-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 2px 2px 4px rgba(0,0,0,.2);
}
.error-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
}
.error-desc {
  font-size: 16px;
  color: rgba(255,255,255,.8);
  margin-bottom: 32px;
}
</style>
