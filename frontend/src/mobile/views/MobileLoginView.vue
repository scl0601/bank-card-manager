<template>
  <div class="mobile-login-page">
    <div class="login-body">
      <div class="login-logo-area">
        <el-icon size="44" color="#1677ff"><CreditCard /></el-icon>
        <h1>银行卡管理</h1>
        <p>日历计划 · 移动端</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { CreditCard } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!await formRef.value?.validate().catch(() => false)) return
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.replace('/m/calendar')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.mobile-login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f0fe 0%, #f0f2f5 100%);
}

.login-body {
  padding: 80px 24px 40px;
}

.login-logo-area {
  text-align: center;
  margin-bottom: 36px;

  h1 {
    font-size: var(--mobile-font-xl);
    font-weight: 700;
    color: #1a1a1a;
    margin-top: 16px;
  }

  p {
    color: #8c8c8c;
    font-size: var(--mobile-font-sm);
    margin-top: 4px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 10px;
}
</style>
