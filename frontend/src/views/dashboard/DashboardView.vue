<template>
  <div>
    <div class="page-title">首页看板</div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <div class="stat-card clickable" @click="item.route && router.push(item.route)">
          <div class="stat-icon" :style="{ background: item.color + '1a' }">
            <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="page-card quick-actions">
      <div class="card-title">快捷操作</div>
      <div class="action-list">
        <el-button type="primary" plain @click="router.push('/owners')">
          <el-icon><Plus /></el-icon>添加持卡人
        </el-button>
        <el-button type="success" plain @click="router.push('/cards')">
          <el-icon><Plus /></el-icon>添加银行卡
        </el-button>
        <el-button type="warning" plain @click="router.push('/transactions')">
          <el-icon><EditPen /></el-icon>记一笔流水
        </el-button>
        <el-button type="danger" plain @click="router.push('/bills')">
          <el-icon><Document /></el-icon>查看账单
        </el-button>
        <el-button plain @click="router.push('/reminders')">
          <el-icon><Bell /></el-icon>提醒中心
        </el-button>
      </div>
    </div>

    <!-- 主内容区：左侧 + 右侧 -->
    <el-row :gutter="16">
      <!-- 左栏 -->
      <el-col :span="8">
        <!-- 本月收支 -->
        <div class="page-card">
          <div class="card-title">本月收支</div>
          <div class="finance-summary">
            <div class="finance-item income">
              <span class="finance-label">本月收入</span>
              <span class="amount-positive">{{ formatAmount(stats.monthlyIncome) }}</span>
            </div>
            <div class="finance-item expense">
              <span class="finance-label">本月支出</span>
              <span class="amount-negative">{{ formatAmount(stats.monthlyExpense) }}</span>
            </div>
            <div class="finance-item net" v-if="stats.monthlyIncome || stats.monthlyExpense">
              <span class="finance-label">本月净额</span>
              <span :class="(stats.monthlyIncome || 0) - (stats.monthlyExpense || 0) >= 0 ? 'amount-positive' : 'amount-negative'">
                {{ formatAmount(((stats.monthlyIncome || 0) - (stats.monthlyExpense || 0))) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 卡片类型分布 -->
        <div class="page-card no-top-margin">
          <div class="card-title">卡片类型分布</div>
          <v-chart :option="cardTypeOption" style="height: 200px" autoresize />
        </div>

        <!-- 提醒概况 -->
        <div class="page-card no-top-margin">
          <div class="card-title">提醒概况</div>
          <div class="reminder-list">
            <div class="reminder-item clickable" @click="router.push('/reminders')">
              <el-tag type="warning">即将到期</el-tag>
              <span class="reminder-count">{{ stats.upcomingCount || 0 }} 条</span>
            </div>
            <div class="reminder-item clickable" @click="router.push('/reminders')">
              <el-tag type="danger">今日到期</el-tag>
              <span class="reminder-count">{{ stats.todayDueCount || 0 }} 条</span>
            </div>
            <div class="reminder-item clickable" @click="router.push('/bills?status=OVERDUE')">
              <el-tag type="danger">逾期账单</el-tag>
              <span class="reminder-count">{{ stats.overdueBillCount || 0 }} 条</span>
            </div>
          </div>
        </div>

        <!-- 最近操作日志 -->
        <div class="page-card no-top-margin">
          <div class="card-title" style="display:flex;justify-content:space-between;align-items:center">
            <span>最近操作日志</span>
            <el-button type="primary" link @click="router.push('/logs')">查看全部 →</el-button>
          </div>
          <div v-if="stats.recentLogs?.length" class="recent-log-list">
            <div v-for="log in stats.recentLogs" :key="log.id" class="recent-log-item">
              <div class="log-main">
                <span class="log-operator">{{ log.operator }}</span>
                <el-tag size="small" :type="log.result === 0 ? 'success' : 'danger'">
                  {{ log.result === 0 ? '成功' : '失败' }}
                </el-tag>
              </div>
              <div class="log-desc">{{ log.module }} - {{ log.action }}</div>
              <div class="log-time">{{ log.createTime }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无操作日志" :image-size="60" />
        </div>
      </el-col>

      <!-- 右栏 -->
      <el-col :span="16">
        <!-- 近7日收支趋势 -->
        <div class="page-card chart-card">
          <div class="card-title">近7日收支趋势</div>
          <v-chart :option="trendOption" style="height: 300px" autoresize />
        </div>

        <!-- 银行分布 + 即将到期账单 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <div class="page-card no-top-margin">
              <div class="card-title">银行分布</div>
              <v-chart :option="bankDistOption" style="height: 280px" autoresize />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="page-card no-top-margin">
              <div class="card-title" style="display:flex;justify-content:space-between;align-items:center">
                <span>即将到期账单 Top 5</span>
                <el-button type="primary" link @click="router.push('/bills')">查看全部 →</el-button>
              </div>
              <el-table :data="stats.upcomingBills || []" size="small" v-if="stats.upcomingBills?.length" :show-header="true">
                <el-table-column prop="ownerName" label="持卡人" width="80" />
                <el-table-column label="银行卡" min-width="130">
                  <template #default="{ row }">{{ row.bankName }} *{{ row.cardNoLast4 }}</template>
                </el-table-column>
                <el-table-column label="金额" width="100" align="right">
                  <template #default="{ row }">{{ formatAmount(row.billAmount) }}</template>
                </el-table-column>
                <el-table-column prop="repayDate" label="还款日" width="100" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="billStatusTag(row.status)" size="small">{{ billStatusDesc(row.status) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无即将到期账单" :image-size="60" />
            </div>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboardStatsApi } from '@/api/dashboard'
import { formatAmount } from '@/utils/formatters'
import { BILL_STATUS_MAP, BILL_STATUS_TAG_TYPE } from '@/constants/dict'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])
const router = useRouter()

interface DailyTrendItem { date: string; income: number; expense: number }
interface BankDistItem { bankName: string; cardCount: number }

const stats = ref<any>({
  totalOwners: 0, totalCards: 0, creditCardCount: 0, debitCardCount: 0,
  pendingReminderCount: 0, overdueBillCount: 0, monthlyIncome: 0,
  monthlyExpense: 0, upcomingCount: 0, todayDueCount: 0, dailyTrend: [],
  recentLogs: [], upcomingBills: [], bankDistribution: []
})

const statCards = computed(() => [
  { label: '持卡人总数', value: stats.value.totalOwners, icon: 'User', color: '#1677ff', route: '/owners' },
  { label: '银行卡总数', value: stats.value.totalCards, icon: 'CreditCard', color: '#52c41a', route: '/cards' },
  { label: '待处理提醒', value: stats.value.pendingReminderCount, icon: 'Bell', color: '#faad14', route: '/reminders' },
  { label: '逾期账单数', value: stats.value.overdueBillCount, icon: 'Warning', color: '#ff4d4f', route: '/bills?status=OVERDUE' }
])

// 近7日收支趋势
const trendOption = computed(() => {
  const trend = stats.value.dailyTrend || []
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['收入', '支出'], top: 0 },
    grid: { left: 16, right: 16, bottom: 16, top: 36, containLabel: true },
    xAxis: { type: 'category', data: trend.map((d: DailyTrendItem) => d.date) },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v } },
    series: [
      {
        name: '收入', type: 'bar', data: trend.map((d: DailyTrendItem) => d.income),
        itemStyle: { color: '#52c41a', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32
      },
      {
        name: '支出', type: 'bar', data: trend.map((d: DailyTrendItem) => d.expense),
        itemStyle: { color: '#ff4d4f', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32
      }
    ]
  }
})

// 卡片类型分布饼图
const cardTypeOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 张 ({d}%)' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie', radius: ['42%', '65%'], center: ['50%', '42%'],
    label: { show: false },
    data: [
      { value: stats.value.creditCardCount || 0, name: '信用卡', itemStyle: { color: '#1677ff' } },
      { value: stats.value.debitCardCount || 0, name: '借记卡', itemStyle: { color: '#52c41a' } }
    ]
  }]
}))

// 银行分布横向柱状图
const bankDistOption = computed(() => {
  const dist = stats.value.bankDistribution || []
  if (!dist.length) return { title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#ccc', fontSize: 14 } } }
  const names = dist.map((d: BankDistItem) => d.bankName)
  const counts = dist.map((d: BankDistItem) => d.cardCount)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 16, right: 40, bottom: 16, top: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: names, axisLabel: { width: 60, overflow: 'truncate' } },
    series: [{
      type: 'bar', data: counts, barMaxWidth: 20,
      itemStyle: { color: '#1677ff', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', formatter: '{c} 张', fontSize: 12 }
    }]
  }
})

// 账单状态映射
function billStatusDesc(status: number) {
  const map: Record<number, string> = { 0: '待还款', 1: '已还清', 2: '部分还款', 3: '逾期' }
  return map[status] || '未知'
}
function billStatusTag(status: number) {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

onMounted(async () => {
  const res = await getDashboardStatsApi()
  stats.value = res.data
})
</script>

<style scoped lang="scss">
.stat-row { margin-bottom: 16px; }

.stat-card {
  background: var(--color-card); border-radius: var(--border-radius); box-shadow: var(--shadow-card);
  padding: 20px; display: flex; align-items: center; gap: 16px;
  &.clickable { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
    &:hover { transform: translateY(-2px); box-shadow: var(--shadow-hover); }
  }
}
.stat-icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; line-height: 1.2; }
.stat-label { font-size: 13px; color: #8c8c8c; margin-top: 4px; }

.quick-actions {
  margin-bottom: 16px;
  .action-list { display: flex; gap: 12px; flex-wrap: wrap; }
}

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; color: #1a1a1a; }
.no-top-margin { margin-top: 0 !important; }

.finance-summary {
  .finance-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 12px 0; border-bottom: 1px solid var(--color-border);
    &:last-child { border-bottom: none; }
    .finance-label { font-size: 14px; color: #666; }
  }
  .amount-positive { font-size: 16px; font-weight: 600; color: #52c41a; }
  .amount-negative { font-size: 16px; font-weight: 600; color: #ff4d4f; }
}

.reminder-list {
  .reminder-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 0; border-bottom: 1px solid var(--color-border);
    &:last-child { border-bottom: none; }
    &.clickable { cursor: pointer; transition: background 0.2s;
      &:hover { background: #f5f7fa; margin: 0 -20px; padding: 10px 20px; }
    }
    .reminder-count { font-size: 14px; font-weight: 500; color: #333; }
  }
}

.recent-log-list { }
.recent-log-item {
  padding: 8px 0; border-bottom: 1px solid var(--color-border);
  &:last-child { border-bottom: none; }
  .log-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
  .log-operator { font-weight: 500; font-size: 13px; }
  .log-desc { font-size: 12px; color: #666; }
  .log-time { font-size: 11px; color: #999; margin-top: 2px; }
}
</style>
