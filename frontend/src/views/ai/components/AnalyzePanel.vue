<template>
  <div class="analyze-panel">
    <!-- 筛选区 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="form" inline size="default" label-width="auto">
        <el-form-item label="分析类型">
          <el-select v-model="form.type" placeholder="选择分析类型" style="width: 160px;">
            <el-option label="对账分析" value="reconcile" />
            <el-option label="异常检测" value="anomaly" />
            <el-option label="收支总结" value="summary" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="DataAnalysis" :loading="analyzing" @click="handleAnalyze">
            开始分析
          </el-button>
          <el-button :icon="RefreshRight" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分析结果 -->
    <el-card shadow="never" class="result-card" v-if="result">
      <template #header>
        <div class="result-header">
          <span>分析结果</span>
          <el-tag size="small" type="success" v-if="result.totalTokens">
            消耗 {{ result.totalTokens }} tokens
          </el-tag>
        </div>
      </template>
      <div class="result-content" v-html="formattedContent(result.content)"></div>
    </el-card>

    <!-- 加载状态 -->
    <div class="loading-state" v-if="analyzing && !result">
      <el-skeleton :rows="8" animated />
      <p style="color: #909399; margin-top: 16px;">AI 正在分析数据，请稍候...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { DataAnalysis, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { analyzeApi } from '@/api/ai'

const form = reactive({
  type: 'reconcile',
})

const dateRange = ref<string[]>([])
const analyzing = ref(false)
const result = ref<any>(null)

async function handleAnalyze() {
  analyzing.value = true
  result.value = null

  try {
    const [start, end] = dateRange.value || []
    const res = await analyzeApi({
      type: form.type,
      startDate: start,
      endDate: end,
    })

    const data = res.data as any
    if (data.success) {
      result.value = data
    } else {
      ElMessage.error(data.errorMessage || '分析失败')
    }
  } catch (e: any) {
    ElMessage.error('请求失败: ' + (e.message || '网络错误'))
  } finally {
    analyzing.value = false
  }
}

function resetForm() {
  form.type = 'reconcile'
  dateRange.value = []
  result.value = null
}

function formattedContent(text: string): string {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h3>$1</h3>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped lang="scss">
.analyze-panel {
  min-height: 400px;
}

.filter-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.result-card {
  margin-top: 16px;

  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .result-content {
    line-height: 1.8;
    font-size: 14px;
    color: var(--color-text-primary);

    :deep(h3) {
      font-size: 15px;
      font-weight: 700;
      color: var(--color-text-primary);
      margin: 16px 0 8px;
    }

    :deep(li) {
      margin-left: 18px;
      padding: 4px 0;
    }

    :deep(strong) {
      color: #1677ff;
    }
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}
</style>
