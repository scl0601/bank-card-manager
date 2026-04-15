<template>
  <div class="chat-panel">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-if="messages.length === 0" class="empty-state">
        <el-icon :size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
        <p>你好，我是银行卡管理 AI 助手，有什么可以帮你的？</p>
        <div class="quick-questions">
          <el-button size="small" plain @click="sendQuickQuestion(q)" v-for="q in quickQuestions" :key="q">
            {{ q }}
          </el-button>
        </div>
      </div>

      <transition-group name="msg-fade" tag="div">
        <div v-for="(msg, index) in messages" :key="index"
             class="message-item" :class="{ 'is-user': msg.role === 'user' }">
          <div class="avatar">
            <el-avatar v-if="msg.role === 'user'" :size="32" icon="UserFilled" style="background: #1677ff;" />
            <el-avatar v-else :size="32" icon="Cpu" style="background: #52c41a;" />
          </div>
          <div class="bubble">
            <div class="content" v-html="formatContent(msg.content)"></div>
          </div>
        </div>
      </transition-group>

      <!-- 加载中 -->
      <div v-if="loading" class="message-item">
        <div class="avatar">
          <el-avatar :size="32" icon="Cpu" style="background: #52c41a;" />
        </div>
        <div class="bubble">
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入你的问题..."
        @keydown.enter.exact.prevent="handleSend"
        :disabled="loading"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="loading"
        :disabled="!inputText.trim()"
        @click="handleSend"
        style="margin-left: 12px; height: 72px; flex-shrink: 0;"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
  tokens?: number
}

const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement>()

const quickQuestions = [
  '本月支出最多的前三类是什么？',
  '帮我看看有哪些异常的大额交易',
  '最近7天的收支趋势怎么样？'
]

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await chatApi({ message: text })
    const result = res.data as any
    if (result.success) {
      messages.value.push({
        role: 'assistant',
        content: result.content,
        tokens: result.totalTokens
      })
    } else {
      messages.value.push({
        role: 'assistant',
        content: result.errorMessage || '抱歉，处理失败，请稍后重试。'
      })
    }
  } catch (e: any) {
    messages.value.push({
      role: 'assistant',
      content: '网络错误或服务不可用，请检查连接后重试。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function sendQuickQuestion(q: string) {
  inputText.value = q
  handleSend()
}

/** 简单的 Markdown 转换（支持换行和加粗） */
function formatContent(text: string): string {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 220px);
  min-height: 400px;

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #fafafa;
    border-radius: 8px 8px 0 0;
    margin-bottom: 0;

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      gap: 12px;
      p {
        color: #909399;
        font-size: 14px;
      }
      .quick-questions {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        justify-content: center;
        margin-top: 8px;
      }
    }

    .message-item {
      display: flex;
      gap: 10px;
      margin-bottom: 16px;
      animation: fadeInUp 0.3s ease-out;

      &.is-user {
        flex-direction: row-reverse;
        .bubble {
          background: #1677ff;
          color: #fff;
          border-radius: 16px 4px 16px 16px;
        }
      }

      .avatar {
        flex-shrink: 0;
      }

      .bubble {
        max-width: 70%;
        padding: 10px 16px;
        background: #fff;
        border-radius: 4px 16px 16px 16px;
        box-shadow: 0 1px 4px rgba(0,0,0,0.08);

        .content {
          line-height: 1.6;
          word-break: break-word;
          font-size: 14px;
        }

        .token-info {
          margin-top: 6px;
          font-size: 11px;
          color: #909399;
          text-align: right;
        }
      }
    }
  }

  .input-area {
    display: flex;
    align-items: stretch;
    padding: 16px 20px;
    background: #fff;
    border-top: 1px solid #ebeef5;
    border-radius: 0 0 8px 8px;

    :deep(.el-textarea__inner) {
      resize: none;
      font-size: 14px;
    }
  }
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 0;

  span {
    width: 8px;
    height: 8px;
    background: #1677ff;
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
