<template>
  <div class="login-page">
    <!-- 动态背景粒子 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <!-- 渐变动画背景 -->
    <div class="gradient-bg"></div>

    <div class="login-card">
      <div class="card-glow"></div>
      <div class="login-header">
        <div class="icon-wrapper">
          <el-icon size="48"><CreditCard /></el-icon>
        </div>
        <h1>银行卡管理后台</h1>
        <p>内部管理系统，请使用账号登录</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" class="tech-input" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            :prefix-icon="Lock" show-password @keyup.enter="handleLogin" class="tech-input" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            <span class="btn-text">登 录</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/modules/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const canvasRef = ref<HTMLCanvasElement>()

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
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 粒子系统
interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number
}

let particles: Particle[] = []
let animationId: number

function initParticles() {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  // 创建粒子
  particles = []
  const particleCount = 80
  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      size: Math.random() * 2 + 1,
      opacity: Math.random() * 0.5 + 0.2
    })
  }

  function animate() {
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // 更新和绘制粒子
    particles.forEach(particle => {
      particle.x += particle.vx
      particle.y += particle.vy

      if (particle.x < 0 || particle.x > canvas.width) particle.vx *= -1
      if (particle.y < 0 || particle.y > canvas.height) particle.vy *= -1

      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(22, 119, 255, ${particle.opacity})`
      ctx.fill()
    })

    // 绘制连线
    particles.forEach((p1, i) => {
      particles.slice(i + 1).forEach(p2 => {
        const dx = p1.x - p2.x
        const dy = p1.y - p2.y
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < 120) {
          ctx.beginPath()
          ctx.moveTo(p1.x, p1.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `rgba(22, 119, 255, ${0.15 * (1 - distance / 120)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      })
    })

    animationId = requestAnimationFrame(animate)
  }

  animate()
}

function handleResize() {
  const canvas = canvasRef.value
  if (canvas) {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
}

onMounted(() => {
  initParticles()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: #0a0e27;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

// 粒子画布
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

// 渐变动画背景
.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 50%, rgba(22, 119, 255, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(120, 40, 200, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 40% 20%, rgba(0, 200, 255, 0.1) 0%, transparent 50%);
  animation: gradientShift 15s ease infinite;
  z-index: 0;
}

@keyframes gradientShift {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

// 登录卡片
.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3),
              inset 0 1px 1px rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 2;
  animation: cardFloat 3s ease-in-out infinite;

  &::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    background: linear-gradient(45deg,
      rgba(22, 119, 255, 0.5),
      rgba(120, 40, 200, 0.5),
      rgba(0, 200, 255, 0.5),
      rgba(22, 119, 255, 0.5)
    );
    border-radius: 20px;
    z-index: -1;
    opacity: 0;
    transition: opacity 0.3s;
    background-size: 300% 300%;
    animation: gradientRotate 4s linear infinite;
  }

  &:hover::before {
    opacity: 1;
  }
}

@keyframes cardFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes gradientRotate {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

// 卡片光晕
.card-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(22, 119, 255, 0.1) 0%, transparent 70%);
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: glowPulse 3s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% {
    opacity: 0.5;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 0.8;
    transform: translate(-50%, -50%) scale(1.1);
  }
}

// 登录头部
.login-header {
  text-align: center;
  margin-bottom: 32px;

  .icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, rgba(22, 119, 255, 0.2), rgba(120, 40, 200, 0.2));
    border-radius: 50%;
    margin-bottom: 16px;
    position: relative;
    animation: iconRotate 10s linear infinite;

    &::before {
      content: '';
      position: absolute;
      inset: -3px;
      border-radius: 50%;
      background: linear-gradient(45deg, #1677ff, #7828c8, #00c8ff, #1677ff);
      background-size: 300% 300%;
      animation: gradientRotate 3s linear infinite;
      z-index: -1;
      filter: blur(8px);
    }

    :deep(.el-icon) {
      color: #fff;
      filter: drop-shadow(0 0 10px rgba(22, 119, 255, 0.8));
    }
  }

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: #fff;
    margin: 12px 0 8px;
    text-shadow: 0 0 20px rgba(22, 119, 255, 0.5);
    letter-spacing: 2px;
  }

  p {
    color: rgba(255, 255, 255, 0.6);
    font-size: 13px;
  }
}

@keyframes iconRotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

// 科技感输入框
:deep(.tech-input) {
  .el-input__wrapper {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: none;
    transition: all 0.3s;

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(22, 119, 255, 0.5);
    }

    &.is-focus {
      background: rgba(255, 255, 255, 0.1);
      border-color: #1677ff;
      box-shadow: 0 0 20px rgba(22, 119, 255, 0.3),
                  inset 0 0 10px rgba(22, 119, 255, 0.1);
    }
  }

  .el-input__inner {
    color: #fff;

    &::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }
  }

  .el-input__prefix,
  .el-input__suffix {
    color: rgba(255, 255, 255, 0.6);
  }
}

// 登录按钮
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #1677ff, #7828c8);
  border: none;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
  }

  &:hover::before {
    width: 300px;
    height: 300px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(22, 119, 255, 0.5);
  }

  .btn-text {
    position: relative;
    z-index: 1;
  }
}
</style>
