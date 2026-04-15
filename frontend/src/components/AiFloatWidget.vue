<template>
  <div class="ai-float-widget" ref="widgetRef">
    <!-- 遮罩层 -->
    <transition name="fade">
      <div v-if="visible" class="overlay" @click="toggle"></div>
    </transition>

    <!-- 悬浮按钮（奶龙） -->
    <div
      class="float-btn"
      :class="{ expanded: isExpanded, dragging: isDragging }"
      :style="btnStyle"
      @mouseenter="onEnter"
      @mouseleave="onLeave"
      @mousedown.stop.prevent="startDrag($event, 'btn')"
      @click.stop="toggle"
    >
      <svg class="nilong-icon" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
        <!-- 身体 - 圆滚滚的黄色小胖子 -->
        <ellipse cx="32" cy="38" rx="22" ry="20" fill="url(#nl-body)"/>
        <!-- 肚子 - 浅色 -->
        <ellipse cx="32" cy="42" rx="14" ry="12" fill="#fff5e6"/>
        <!-- 头部 - 大圆 -->
        <circle cx="32" cy="18" r="15" fill="url(#nl-body)"/>
        <!-- 粉色帽子 -->
        <path d="M17 14 C17 6, 47 6, 47 14 Q47 18, 44 19 L20 19 Q17 18, 17 14Z" fill="#e8a0bf"/>
        <path d="M16 13 Q32 4, 48 13 L46 16 Q32 8, 18 16 Z" fill="#f5d5e0"/>
        <!-- 小耳朵/角 -->
        <ellipse cx="18" cy="10" rx="4" ry="6" fill="url(#nl-body)" transform="rotate(-20 18 10)"/>
        <ellipse cx="46" cy="10" rx="4" ry="6" fill="url(#nl-body)" transform="rotate(20 46 10)"/>
        <!-- 眼睛 - 大大的黑眼珠 -->
        <ellipse cx="25" cy="17" rx="4" ry="4.5" fill="#fff"/>
        <ellipse cx="39" cy="17" rx="4" ry="4.5" fill="#fff"/>
        <circle cx="25.8" cy="17.8" r="2.5" fill="#333"/>
        <circle cx="39.8" cy="17.8" r="2.5" fill="#333"/>
        <!-- 眼睛高光 -->
        <circle cx="26.5" cy="16.5" r="1" fill="#fff"/>
        <circle cx="40.5" cy="16.5" r="1" fill="#fff"/>
        <!-- 腮红 -->
        <ellipse cx="19" cy="22" rx="3.5" ry="2.5" fill="#ffb6c1" opacity="0.55"/>
        <ellipse cx="45" cy="22" rx="3.5" ry="2.5" fill="#ffb6c1" opacity="0.55"/>
        <!-- 嘴巴 - 小小的 -->
        <path d="M29 25 Q32 27 35 24" stroke="#c9956b" stroke-width="1.5" stroke-linecap="round" fill="none"/>
        <!-- 小手 -->
        <ellipse cx="9" cy="34" rx="5" ry="6" fill="url(#nl-body)"/>
        <ellipse cx="55" cy="34" rx="5" ry="6" fill="url(#nl-body)"/>
        <!-- 小脚 - 粉色裤子 -->
        <ellipse cx="22" cy="57" rx="7" ry="4" fill="#e8a0bf"/>
        <ellipse cx="42" cy="57" rx="7" ry="4" fill="#e8a0bf"/>

        <defs>
          <linearGradient id="nl-body" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#ffd666"/>
            <stop offset="60%" stop-color="#ffa940"/>
            <stop offset="100%" stop-color="#fa8c16"/>
          </linearGradient>
        </defs>
      </svg>
      <!-- 名字标签 -->
      <span class="btn-name">奶龙</span>
      <!-- 脉冲 -->
      <span class="pulse-ring"></span>
    </div>

    <!-- 悬浮窗 -->
    <transition name="window-in">
      <div v-if="visible" class="float-window" :style="windowStyle" @click.stop>
        <!-- 头部（可拖拽） -->
        <div
          class="window-header"
          @mousedown.stop.prevent="startDrag($event, 'window')"
        >
          <div class="header-left">
            <div class="header-nilong">
              <svg width="28" height="28" viewBox="0 0 64 64" fill="none">
                <defs><linearGradient id="nl-hd" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#ffd666"/><stop offset="100%" stop-color="#fa8c16"/></linearGradient></defs>
                <ellipse cx="32" cy="38" rx="21" ry="19" fill="rgba(255,255,255,0.92)"/>
                <circle cx="32" cy="18" r="14" fill="rgba(255,255,255,0.92)"/>
                <path d="M19 14 C19 7, 45 7, 45 14 L43 17 Q32 10, 21 17 Z" fill="#e8a0bf" opacity="0.8"/>
                <ellipse cx="25" cy="17" rx="3.5" ry="4" fill="#fa8c16"/>
                <ellipse cx="39" cy="17" rx="3.5" ry="4" fill="#fa8c16"/>
                <circle cx="26" cy="16.5" r="1.2" fill="#fff"/>
                <circle cx="40" cy="16.5" r="1.2" fill="#fff"/>
              </svg>
            </div>
            <span class="header-title">奶龙助手</span>
            <span class="status-dot"></span>
            <span v-if="quotaLoaded && displayTotal > 0" class="total-token" :class="{ low: displayPercent > 70 }">
              剩余 <strong>{{ fmt(displayRemaining) }}</strong> / {{ fmt(displayTotal) }}
            </span>
            <span v-if="isDraggingWindow" class="drag-hint">移动中...</span>
          </div>
          <div class="header-actions">
            <span class="new-chat-btn" @click.stop="resetChat" title="新对话" v-if="messages.length > 0">
              <el-icon :size="13"><RefreshRight /></el-icon> 新对话
            </span>
            <el-icon class="close-btn" @click.stop="toggle"><Close /></el-icon>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="message-list" ref="messageListRef">
          <!-- 空状态 -->
          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-nilong">
              <svg width="72" height="72" viewBox="0 0 64 64" fill="none">
                <defs><linearGradient id="nl-emp" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#ffd666"/><stop offset="100%" stop-color="#fa8c16"/></linearGradient></defs>
                <ellipse cx="32" cy="38" rx="22" ry="20" fill="url(#nl-emp)"/>
                <ellipse cx="32" cy="42" rx="14" ry="12" fill="#fff5e6"/>
                <circle cx="32" cy="18" r="15" fill="url(#nl-emp)"/>
                <path d="M17 14 C17 6, 47 6, 47 14 Q47 18, 44 19 L20 19 Q17 18, 17 14Z" fill="#e8a0bf"/>
                <path d="M16 13 Q32 4, 48 13 L46 16 Q32 8, 18 16 Z" fill="#f5d5e0"/>
                <ellipse cx="18" cy="10" rx="4" ry="6" fill="url(#nl-emp)" transform="rotate(-20 18 10)"/>
                <ellipse cx="46" cy="10" rx="4" ry="6" fill="url(#nl-emp)" transform="rotate(20 46 10)"/>
                <ellipse cx="25" cy="17" rx="4" ry="4.5" fill="#fff"/>
                <ellipse cx="39" cy="17" rx="4" ry="4.5" fill="#fff"/>
                <circle cx="25.8" cy="17.8" r="2.5" fill="#333"/>
                <circle cx="39.8" cy="17.8" r="2.5" fill="#333"/>
                <circle cx="26.5" cy="16.5" r="1" fill="#fff"/>
                <circle cx="40.5" cy="16.5" r="1" fill="#fff"/>
                <ellipse cx="19" cy="22" rx="3.5" ry="2.5" fill="#ffb6c1" opacity="0.55"/>
                <ellipse cx="45" cy="22" rx="3.5" ry="2.5" fill="#ffb6c1" opacity="0.55"/>
                <path d="M29 25 Q32 27 35 24" stroke="#c9956b" stroke-width="1.5" stroke-linecap="round" fill="none"/>
                <ellipse cx="9" cy="34" rx="5" ry="6" fill="url(#nl-emp)"/>
                <ellipse cx="55" cy="34" rx="5" ry="6" fill="url(#nl-emp)"/>
                <ellipse cx="22" cy="57" rx="7" ry="4" fill="#e8a0bf"/>
                <ellipse cx="42" cy="57" rx="7" ry="4" fill="#e8a0bf"/>
              </svg>
            </div>
            <p class="empty-greeting">嗨！我是奶龙 🐲<br/>你的全能AI小助手~</p>
            <p class="empty-desc">查待办、看流水、聊天气、闲聊解闷<br/>什么都可以问我哦！</p>
            <div class="quick-questions">
              <el-button round size="small" type="primary" plain @click="sendQuickQuestion('今天有什么待办？')">今天待办</el-button>
              <el-button round size="small" type="primary" plain @click="sendQuickQuestion('最近流水怎么样？')">最近流水</el-button>
              <el-button round size="small" type="primary" plain @click="sendQuickQuestion('今天天气怎么样？')">聊聊天气</el-button>
            </div>
          </div>

          <!-- 消息列表 -->
          <div v-for="(msg, idx) in messages" :key="idx" class="message-item" :class="{ 'is-user': msg.role === 'user' }">
            <div class="avatar-wrap">
              <el-avatar v-if="msg.role === 'user'" :size="30" icon="UserFilled" />
              <div v-else class="nilong-avatar-sm">
                <svg width="30" height="30" viewBox="0 0 64 64" fill="none">
                  <defs><linearGradient id="nls" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#ffd666"/><stop offset="100%" stop-color="#fa8c16"/></linearGradient></defs>
                  <ellipse cx="32" cy="37" rx="20" ry="18" fill="url(#nls)"/>
                  <circle cx="32" cy="18" r="13" fill="url(#nls)"/>
                  <path d="M20 14 C20 8, 44 8, 44 14 L42 16 Q32 10, 22 16 Z" fill="#e8a0bf" opacity="0.7"/>
                  <ellipse cx="25" cy="17" rx="3.2" ry="3.8" fill="#fff"/>
                  <ellipse cx="39" cy="17" rx="3.2" ry="3.8" fill="#fff"/>
                  <circle cx="25.6" cy="17.6" r="2" fill="#333"/>
                  <circle cx="39.6" cy="17.6" r="2" fill="#333"/>
                  <path d="M29 23 Q32 25 35 22" stroke="#c9956b" stroke-width="1.2" stroke-linecap="round" fill="none"/>
                </svg>
              </div>
            </div>
            <div class="bubble" :class="'bubble-' + msg.role">
              <div class="bubble-content" v-html="formatContent(msg.content)"></div>
              <span class="token-tag" v-if="msg.tokens">{{ msg.tokens }} tokens</span>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="message-item">
            <div class="avatar-wrap">
              <div class="nilong-avatar-sm">
                <svg width="30" height="30" viewBox="0 0 64 64" fill="none">
                  <defs><linearGradient id="nll" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#ffd666"/><stop offset="100%" stop-color="#fa8c16"/></linearGradient></defs>
                  <ellipse cx="32" cy="37" rx="20" ry="18" fill="url(#nll)"/>
                  <circle cx="32" cy="18" r="13" fill="url(#nll)"/>
                  <ellipse cx="25" cy="17" rx="3.2" ry="3.8" fill="#fff"/>
                  <ellipse cx="39" cy="17" rx="3.2" ry="3.8" fill="#fff"/>
                </svg>
              </div>
            </div>
            <div class="bubble bubble-assistant">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="1"
              placeholder="问问奶龙..."
              @keydown.enter.exact.prevent="handleSend"
              :disabled="loading"
              resize="none"
            />
            <el-button type="primary" circle :icon="Promotion" :loading="loading" :disabled="!inputText.trim()" @click="handleSend" class="send-btn" />
          </div>
          <div class="input-footer">
            <span class="ai-disclaimer">内容由 AI 生成，仅供参考</span>
            <span class="token-info" :class="{ 'token-low': displayPercent > 70 }" v-if="quotaLoaded && displayTotal > 0">
              已用 {{ fmt(displayUsed) }} / 剩余 <strong>{{ fmt(displayRemaining) }}</strong> ({{ displayPercent }}%)
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { Close, Promotion, RefreshRight } from '@element-plus/icons-vue'
import { chatApi, quotaApi } from '@/api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
  tokens?: number
  inputTokens?: number
  outputTokens?: number
}

interface QuotaInfo {
  totalQuota: number
  usedQuota: number
  remainingQuota: number
  usagePercent: number
}

const visible = ref(false)
const isExpanded = ref(false)
const isDragging = ref(false)
const isDraggingWindow = ref(false)
const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement>()
const widgetRef = ref<HTMLElement>()

// 配额信息（来自后端）
const quota = ref<QuotaInfo>({ totalQuota: 0, usedQuota: 0, remainingQuota: 0, usagePercent: 0 })
const quotaLoaded = ref(false)

let shrinkTimer: ReturnType<typeof setTimeout> | null = null

// ---- 拖拽状态 ----
const pos = ref({ x: 0, y: 0 })
const dragOffset = ref({ x: 0, y: 0 })

// 配额显示：有数据时用真实配额，没有时 fallback 到本次对话累计
const displayTotal = computed(() => quota.value.totalQuota || 0)
const displayUsed = computed(() => quota.value.usedQuota || 0)
const displayRemaining = computed(() => quota.value.remainingQuota || 0)
const displayPercent = computed(() => quota.value.usagePercent || 0)

// 默认右下角位置
function initPosition() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  pos.value.x = vw - 24 - 56
  pos.value.y = vh - 24 - 56 - 60
}

const btnStyle = computed(() => ({
  left: `${pos.value.x}px`,
  top: `${pos.value.y}px`,
}))

const windowStyle = computed(() => {
  if (!visible.value) return {}
  const winW = 400
  const winH = 570
  const btnSize = 56
  const margin = 12
  let left = pos.value.x - winW / 2 + btnSize / 2 // 居中对齐按钮
  let top = pos.value.y - winH - 8 // 按钮上方弹出

  // 右边界检测：窗口不能超出右边缘
  if (left + winW > window.innerWidth - margin) {
    left = window.innerWidth - winW - margin
  }
  // 左边界检测：窗口不能超出左边缘
  if (left < margin) {
    left = margin
  }
  // 上边界检测：如果上方空间不够，改到下方
  if (top < margin) {
    top = pos.value.y + btnSize + 8
  }

  return { left: `${left}px`, top: `${top}px` }
})

// ---- 拖拽逻辑 ----
function startDrag(e: MouseEvent, target: 'btn' | 'window') {
  if (target === 'btn') isDragging.value = true
  else isDraggingWindow.value = true
  dragOffset.value.x = e.clientX - pos.value.x
  dragOffset.value.y = e.clientY - pos.value.y
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e: MouseEvent) {
  let newX = e.clientX - dragOffset.value.x
  let newY = e.clientY - dragOffset.value.y
  const btnSize = 56
  const margin = 8
  newX = Math.max(margin, Math.min(newX, window.innerWidth - btnSize - margin))
  newY = Math.max(margin, Math.min(newY, window.innerHeight - btnSize - margin))
  pos.value.x = newX
  pos.value.y = newY
}

function onDragEnd() {
  isDragging.value = false
  isDraggingWindow.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// ---- 打开/关闭/切换 ----
/** 点击奶龙：打开 <-> 关闭 切换 */
function toggle() {
  if (isDragging.value) return // 拖拽不触发
  if (visible.value) close()
  else open()
}
function open() {
  messages.value = []
  inputText.value = ''
  visible.value = true
  isExpanded.value = true
  cancelShrink()
  fetchQuota()
}
function close() {
  visible.value = false
}
function resetChat() {
  messages.value = []
  inputText.value = ''
  fetchQuota()
}

// ---- 悬浮按钮交互 ----
function onEnter() {
  cancelShrink()
  isExpanded.value = true
}
function onLeave() {
  if (!visible.value && !isDragging.value) {
    shrinkTimer = setTimeout(() => { isExpanded.value = false }, 2000)
  }
}

function cancelShrink() {
  if (shrinkTimer) { clearTimeout(shrinkTimer); shrinkTimer = null }
}

onMounted(() => {
  initPosition()
  window.addEventListener('resize', initPosition)
  // 页面加载时就查一次配额
  fetchQuota()
})
onUnmounted(() => {
  window.removeEventListener('resize', initPosition)
})

// ---- 查询配额 ----
async function fetchQuota() {
  try {
    const res = await quotaApi()
    const d = res.data as any
    if (d.success) {
      quota.value = {
        totalQuota: d.totalQuota || 0,
        usedQuota: d.usedQuota || 0,
        remainingQuota: d.remainingQuota || 0,
        usagePercent: d.usagePercent || 0,
      }
      quotaLoaded.value = true
    }
  } catch {
    // 静默失败，不影响使用
  }
}

// ---- 格式化数字 ----
function fmt(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString()
}

// ---- 发送消息 ----
function scrollToBottom() {
  nextTick(() => { messageListRef.value?.scrollTo(0, messageListRef.value.scrollHeight) })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // 发送时带上历史消息（最多最近20条），让AI有记忆
    const history = messages.value.slice(0, -1).map(m => ({
      role: m.role,
      content: m.content,
    }))
    const res = await chatApi({ message: text, history })
    const result = res.data as any
    const reply = result.success ? result.content : (result.errorMessage || '抱歉，处理失败，请稍后再试哦~')
    messages.value.push({ role: 'assistant', content: reply, tokens: result.totalTokens, inputTokens: result.inputTokens, outputTokens: result.outputTokens })
    // 如果后端返回了配额信息，同步更新
    if (result.totalQuota != null) {
      quota.value = {
        totalQuota: result.totalQuota || 0,
        usedQuota: result.usedQuota || 0,
        remainingQuota: result.remainingQuota || 0,
        usagePercent: result.usagePercent || 0,
      }
    } else {
      // 否则重新拉取一次配额
      fetchQuota()
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络好像有点问题，稍后再找奶龙聊天吧~' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function sendQuickQuestion(q: string) { inputText.value = q; handleSend() }

function formatContent(text: string): string {
  if (!text) return ''
  return text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>').replace(/\n/g,'<br/>')
}

defineExpose({ open, close })
</script>

<style scoped lang="scss">
/* ========== 容器 ========== */
.ai-float-widget {
  position: fixed;
  z-index: 10000;
}

/* ========== 遮罩 ========== */
.overlay {
  position: fixed;
  inset: 0;
  z-index: -1;
}

/* ========== 悬浮按钮（奶龙 - 黄胖版） ========== */
.float-btn {
  position: absolute;
  cursor: grab;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translateY(24px);
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.3s;
  user-select: none;

  &.expanded {
    transform: translateY(0);
  }

  &.dragging {
    cursor: grabbing;
    transform: translateY(0) scale(1.08);
    transition: transform 0.15s ease;
    filter: brightness(1.08) drop-shadow(0 8px 24px rgba(250, 140, 22, 0.4));
  }

  &:not(.dragging):hover { filter: brightness(1.06); }

  .nilong-icon {
    width: 56px;
    height: 56px;
    filter: drop-shadow(0 4px 14px rgba(250, 140, 22, 0.3));
    transition: transform 0.35s ease;
    pointer-events: none;

    .float-btn:hover &,
    .float-btn.expanded &:not(.float-btn.dragging &) {
      animation: nilongBounce 1.5s ease-in-out infinite;
    }
  }

  .btn-name {
    margin-top: -4px;
    font-size: 10px;
    font-weight: 700;
    color: #fa8c16;
    background: #fff7e6;
    padding: 1px 8px;
    border-radius: 8px;
    opacity: 0;
    transform: translateY(-4px);
    transition: all 0.3s ease;
    white-space: nowrap;
    pointer-events: none;

    .expanded:not(.dragging) & { opacity: 1; transform: translateY(0); }
  }

  .pulse-ring {
    position: absolute;
    top: -6px; left: -6px; right: -6px; bottom: -6px;
    border-radius: 50%;
    border: 2px solid rgba(250, 140, 22, 0.25);
    animation: pulse 2.5s infinite;
    pointer-events: none;
  }
}

@keyframes nilongBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}
@keyframes pulse {
  0%   { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(1.35); opacity: 0; }
}

/* ========== 悬浮窗口 ========== */
.float-window {
  position: absolute;
  width: 400px;
  height: 570px;
  border-radius: 20px;
  background: #fff;
  box-shadow:
    0 20px 60px rgba(250, 140, 22, 0.12),
    0 8px 24px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(250, 140, 22, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 130px;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(255,214,102,0.07) 0%, transparent 100%);
    border-radius: 20px 20px 0 0;
  }
}

/* ========== 头部（可拖拽，暖橙主题） ========== */
.window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: linear-gradient(135deg, #ffc53d 0%, #fa8c16 50%, #d46b08 100%);
  color: #fff;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  cursor: grab;

  &:active { cursor: grabbing; }

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-nilong { line-height: 0; }

    .header-title {
      font-size: 14.5px;
      font-weight: 700;
      letter-spacing: 0.5px;
    }

    .status-dot {
      width: 7px; height: 7px;
      border-radius: 50%;
      background: #52c41a;
      box-shadow: 0 0 6px #52c41a;
      margin-left: 4px;
    }

    .total-token {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-size: 11px;
      font-weight: 600;
      background: rgba(255,255,255,0.18);
      padding: 2px 8px;
      border-radius: 10px;
      opacity: 0.9;

      &.low { // 剩余不足30%时变红
        background: rgba(255,77,79,0.3);
        animation: tokenWarn 1s ease-in-out infinite alternate;
      }
    }

    .drag-hint {
      font-size: 11px;
      opacity: 0.75;
      background: rgba(255,255,255,0.18);
      padding: 1px 8px;
      border-radius: 8px;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .new-chat-btn {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 11.5px;
    cursor: pointer;
    opacity: 0.85;
    transition: all 0.2s;
    padding: 3px 8px;
    border-radius: 10px;
    &:hover { opacity: 1; background: rgba(255,255,255,0.18); }
  }

  .close-btn {
    width: 28px; height: 28px;
    display: flex; align-items: center; justify-content: center;
    border-radius: 50%;
    cursor: pointer;
    opacity: 0.85;
    transition: all 0.2s;
    &:hover { opacity: 1; background: rgba(255,255,255,0.2); }
  }
}

/* ========== 消息列表 ========== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px 8px;
  background: linear-gradient(180deg, #fffbe6 0%, #fafafa 100%);

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 3px; }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 8px;
    padding: 20px 0;

    .empty-nilong {
      animation: nilongWave 2s ease-in-out infinite;
      margin-bottom: 4px;
    }

    .empty-greeting {
      color: #d46b08;
      font-size: 15px;
      font-weight: 600;
      line-height: 1.6;
      text-align: center;
    }

    .empty-desc {
      color: #999;
      font-size: 12.5px;
      line-height: 1.7;
      text-align: center;
    }

    .quick-questions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 10px;
    }
  }
}

@keyframes nilongWave {
  0%, 100% { transform: rotate(0deg) translateY(0); }
  25% { transform: rotate(-5deg) translateY(-3px); }
  75% { transform: rotate(5deg) translateY(-3px); }
}

/* ========== 消息气泡 ========== */
.message-item {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: flex-start;

  &.is-user { flex-direction: row-reverse; }

  .avatar-wrap { flex-shrink: 0; padding-top: 2px; }

  .nilong-avatar-sm {
    width: 30px; height: 30px;
    border-radius: 10px;
    overflow: hidden;
  }

  .bubble {
    max-width: 75%;
    padding: 10px 14px;
    border-radius: 16px;
    font-size: 13.5px;
    line-height: 1.65;
    word-break: break-word;
    position: relative;
  }

  .bubble-user {
    background: linear-gradient(135deg, #ffc53d, #fa8c16);
    color: #fff;
    border-radius: 16px 4px 16px 16px;
    box-shadow: 0 2px 8px rgba(250, 140, 22, 0.25);
    .token-tag { color: rgba(255,255,255,0.55); }
  }

  .bubble-assistant {
    background: #fff;
    color: #333;
    border-radius: 4px 16px 16px 16px;
    box-shadow: 0 1px 6px rgba(250, 140, 22, 0.05);
    .token-tag { color: #bbb; }
  }

  .bubble-content { white-space: pre-wrap; }
  .token-tag {
    display: block;
    margin-top: 5px;
    font-size: 10.5px;
    text-align: right;
    letter-spacing: 0.3px;
  }
}

/* ========== 打字动画 ========== */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
  span {
    width: 7px; height: 7px;
    background: linear-gradient(135deg, #ffc53d, #fa8c16);
    border-radius: 50%;
    animation: bounce 1.4s infinite both;
    &:nth-child(1){animation-delay:-0.32s} &:nth-child(2){animation-delay:-0.16s}
  }
}
@keyframes bounce {
  0%,80%,100%{transform:scale(0.6);opacity:0.4} 40%{transform:scale(1);opacity:1}
}

/* ========== 输入区 ========== */
.input-area {
  padding: 10px 14px 12px;
  background: #fff;
  border-top: 1px solid #ffe7ba;
  flex-shrink: 0;

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    background: #fffbe6;
    border-radius: 14px;
    padding: 6px 8px 6px 14px;
    border: 1.5px solid #ffd591;
    transition: border-color 0.2s, box-shadow 0.2s;

    &:focus-within {
      border-color: #fa8c16;
      box-shadow: 0 0 0 3px rgba(250, 140, 22, 0.1);
    }

    :deep(.el-textarea__inner) {
      resize: none !important;
      font-size: 13px;
      border: none !important;
      background: transparent !important;
      box-shadow: none !important;
      padding: 4px 0 !important;
    }

    .send-btn {
      width: 32px; height: 32px;
      flex-shrink: 0;
      background: linear-gradient(135deg, #ffc53d, #fa8c16) !important;
      border-color: transparent !important;
    }
  }

  .input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 6px;
    padding: 0 2px;
  }

  .input-hint {
    font-size: 11px;
    color: #bbb;
  }

  .ai-disclaimer {
    font-size: 11px;
    color: #bbb8b0;
    letter-spacing: 0.3px;
  }

  .token-info {
    font-size: 11px;
    color: #999;

    strong {
      color: #fa8c16;
      font-weight: 600;
    }

    &.token-low {
      strong { color: #ff4d4f; }
    }
  }
}

/* ========== 过渡 ========== */
.fade-enter-active,.fade-leave-active{transition:opacity 0.2s}
.fade-enter-from,.fade-leave-to{opacity:0}

.window-in-enter-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1)}
.window-in-leave-active{transition:all 0.2s ease-in}
.window-in-enter-from{opacity:0;transform:translateY(20px) scale(0.92)}
.window-in-leave-to{opacity:0;transform:translateY(10px) scale(0.96)}

@keyframes tokenWarn {
  from { opacity: 0.9; }
  to { opacity: 0.5; background: rgba(255,77,79,0.45); }
}
</style>
