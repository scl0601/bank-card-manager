<template>
  <div class="book-workbench">
    <section class="book-overview-screen">
      <section class="book-hero">
        <div class="hero-main">
          <div class="hero-topline">
            <div>
              <div class="page-title">个人记账</div>
            </div>
            <el-date-picker v-model="currentMonth" class="hero-month" type="month" value-format="YYYY-MM" :clearable="false" />
          </div>

          <div class="hero-balance">
            <span>本月支出</span>
            <strong>-{{ money(overview.totalExpense) }}</strong>
          </div>

          <div class="hero-summary">
            <div v-for="card in secondaryMetrics" :key="card.key" class="summary-chip" :class="card.tone">
              <span>{{ card.label }}</span>
              <strong :class="card.amountClass">{{ card.value }}</strong>
            </div>
          </div>
        </div>

        <div class="hero-side">
          <div class="hero-actions">
            <el-button type="primary" :icon="Plus" @click="openAdd()">记一笔</el-button>
            <el-button v-for="action in quickActions" :key="action.key" :icon="action.icon" @click="action.handler">
              {{ action.label }}
            </el-button>
            <ExportButton :loading="exporting" @click="exportCurrent" />
          </div>

          <div class="budget-brief" :class="budgetStatus.tone">
            <div class="budget-brief-head">
              <span>{{ budgetStatus.title }}</span>
              <strong>{{ budgetStatus.percentText }}</strong>
            </div>
            <el-progress :percentage="budgetProgressPercent" :status="budgetStatus.progressStatus" :show-text="false" />
            <div class="budget-brief-foot">
              <span>预算 {{ money(overview.totalBudget) }}</span>
              <span>剩余 {{ money(overview.budgetRemaining) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="insight-grid">
        <div class="panel trend-panel">
          <div class="panel-head">
            <div>
              <h3>本月趋势</h3>
              <span>按天汇总收入与支出</span>
            </div>
            <div class="panel-actions">
              <span class="panel-total">净额 {{ signedMoney(overview.netAmount) }}</span>
              <el-button link type="primary" @click="openInsightDialog('trend')">展开全部</el-button>
            </div>
          </div>
          <VChart class="trend-chart" :option="trendOption" autoresize />
        </div>

        <div class="panel insight-panel">
          <div class="panel-head">
            <div>
              <h3>预算进度</h3>
              <span>{{ budgetSummaryText }}</span>
            </div>
            <div class="panel-actions">
              <el-button link type="primary" @click="openBudgetAdd">{{ budgets.length ? '调整' : '新增预算' }}</el-button>
              <el-button link type="primary" @click="openInsightDialog('budget')">展开全部</el-button>
            </div>
          </div>
          <div v-if="budgets.length" class="budget-list">
            <div v-for="item in previewBudgets" :key="item.id" class="budget-item">
              <div>
                <span>{{ item.categoryName || '总预算' }}</span>
                <strong>{{ money(item.usedAmount) }} / {{ money(item.amount) }}</strong>
              </div>
              <el-progress :percentage="Math.min(100, Number(item.usagePercent || 0))" :status="Number(item.usagePercent) > 100 ? 'exception' : undefined" :show-text="false" />
            </div>
          </div>
          <el-empty v-else :image-size="42" description="本月还没有预算" />
        </div>

        <div class="panel insight-panel">
          <div class="panel-head">
            <div>
              <h3>账户余额</h3>
              <span>点击账户筛选对应流水</span>
            </div>
            <div class="panel-actions">
              <el-button link type="primary" @click="openAccountAdd">{{ accounts.length ? '新增' : '新增账户' }}</el-button>
              <el-button link type="primary" @click="openInsightDialog('account')">展开全部</el-button>
            </div>
          </div>
          <div class="account-list" v-if="visibleAccounts.length">
            <button
              v-for="account in visibleAccounts"
              :key="account.id"
              class="account-row"
              :class="{ active: query.accountId === account.id }"
              @click="selectAccount(account.id)"
            >
              <span>
                <b>{{ account.name }}</b>
                <small>{{ account.accountTypeDesc || accountTypeText(account.accountType) }}</small>
              </span>
              <strong>{{ money(account.currentBalance) }}</strong>
            </button>
          </div>
          <el-empty v-else :image-size="42" description="暂无账户，先新增一个账户" />
        </div>

        <div class="panel insight-panel">
          <div class="panel-head">
            <div>
              <h3>支出排行</h3>
              <span>本月主要消费分类</span>
            </div>
            <div class="panel-actions">
              <el-button link type="primary" @click="openInsightDialog('rank')">展开全部</el-button>
            </div>
          </div>
          <div v-if="topCategoryRanks.length" class="rank-list">
            <div v-for="item in topCategoryRanks" :key="item.categoryId || item.categoryName" class="rank-row">
              <div class="rank-meta">
                <span>{{ item.categoryName || '未分类' }}</span>
                <strong>{{ money(item.amount) }}</strong>
              </div>
              <div class="rank-bar"><i :style="{ width: `${item.percent}%` }" /></div>
            </div>
          </div>
          <el-empty v-else :image-size="42" description="暂无支出记录" />
        </div>
      </section>
    </section>

    <el-dialog v-model="insightDialogVisible" :title="insightDialogTitle" width="760px" class="insight-dialog" destroy-on-close>
      <div v-if="insightDialogType === 'trend'" class="insight-dialog-body">
        <VChart class="dialog-trend-chart" :option="trendDialogOption" autoresize />
        <el-table v-if="trendRows.length" :data="trendRows" border size="small" max-height="320">
          <el-table-column prop="date" label="日期" min-width="120" />
          <el-table-column label="收入" min-width="120" align="right">
            <template #default="{ row }"><span class="amount-positive">+{{ money(row.income) }}</span></template>
          </el-table-column>
          <el-table-column label="支出" min-width="120" align="right">
            <template #default="{ row }"><span class="amount-negative">-{{ money(row.expense) }}</span></template>
          </el-table-column>
          <el-table-column label="净额" min-width="120" align="right">
            <template #default="{ row }">{{ signedMoney(Number(row.income || 0) - Number(row.expense || 0)) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-else :image-size="60" description="暂无趋势数据" />
      </div>

      <el-table v-else-if="insightDialogType === 'budget'" :data="budgets" border size="small" max-height="520" empty-text="本月还没有预算">
        <el-table-column label="预算项" min-width="160">
          <template #default="{ row }">{{ row.categoryName || '总预算' }}</template>
        </el-table-column>
        <el-table-column label="已用/预算" min-width="180" align="right">
          <template #default="{ row }">{{ money(row.usedAmount) }} / {{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="进度" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="Math.min(100, Number(row.usagePercent || 0))" :status="Number(row.usagePercent) > 100 ? 'exception' : undefined" />
          </template>
        </el-table-column>
      </el-table>

      <el-table
        v-else-if="insightDialogType === 'account'"
        :data="accounts"
        border
        size="small"
        max-height="520"
        empty-text="暂无账户，先新增一个账户"
        @row-click="handleDialogAccountFilter"
      >
        <el-table-column prop="name" label="账户" min-width="160" />
        <el-table-column label="类型" min-width="120">
          <template #default="{ row }">{{ row.accountTypeDesc || accountTypeText(row.accountType) }}</template>
        </el-table-column>
        <el-table-column label="余额" min-width="130" align="right">
          <template #default="{ row }">{{ money(row.currentBalance) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="allCategoryRanks" border size="small" max-height="520" empty-text="暂无支出记录">
        <el-table-column label="分类" min-width="160">
          <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="金额" min-width="130" align="right">
          <template #default="{ row }">{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="占比" min-width="240">
          <template #default="{ row }">
            <div class="dialog-rank-cell">
              <div class="rank-bar"><i :style="{ width: `${row.percent}%` }" /></div>
              <span>{{ row.percent }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <section class="panel ledger-panel">
      <div class="ledger-head">
        <div>
          <h3>收支流水</h3>
          <span v-if="selectedDate">已筛选 {{ selectedDate }}</span>
          <span v-else>查看、筛选或补记本月流水</span>
        </div>
        <div class="ledger-tabs">
          <el-segmented v-model="viewMode" :options="[{ label: '流水', value: 'list' }, { label: '日历', value: 'calendar' }]" />
          <el-button v-if="selectedDate" link type="primary" @click="openAdd(selectedDate)">按当天记一笔</el-button>
        </div>
      </div>

      <div class="ledger-toolbar">
        <div class="ledger-filters">
          <el-select v-model="query.bookType" placeholder="全部类型" clearable style="width: 108px" @change="runSearch">
            <el-option v-for="item in BOOK_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="query.accountId" placeholder="全部账户" clearable filterable style="width: 132px" @change="runSearch">
            <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-cascader
            v-model="query.categoryIds"
            :options="categoryOptions"
            :props="{ multiple: true, checkStrictly: true, value: 'id', label: 'name', children: 'children', emitPath: false }"
            placeholder="全部分类"
            clearable
            collapse-tags
            collapse-tags-tooltip
            style="width: 180px"
            @change="runSearch"
          />
          <el-input v-model="query.keyword" placeholder="搜索备注、商家、账户" clearable style="width: 180px" @keyup.enter="runSearch" @clear="runSearch" />
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <el-button v-if="selectedDate" link type="primary" @click="clearDateFilter">清除日期</el-button>
      </div>

      <PageTable
        v-if="viewMode === 'list'"
        :data="list"
        :loading="loading"
        :total="total"
        :page-num="query.pageNum"
        :page-size="query.pageSize"
        border
        size="small"
        @update:page-num="(val) => { query.pageNum = val }"
        @update:page-size="(val) => { query.pageSize = val }"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      >
        <el-table-column label="日期" width="126">
          <template #default="{ row }">
            <div class="date-cell">
              <strong>{{ row.bookDate }}</strong>
              <span>{{ row.bookTime || '--:--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="76" align="center">
          <template #default="{ row }">
            <StatusTag :value="row.bookType" :label-map="BOOK_TYPE_MAP" :type-map="BOOK_TYPE_TAG_TYPE" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="分类/对象" min-width="150">
          <template #default="{ row }">
            <div class="main-cell">
              <strong>{{ row.bookType === BOOK_TYPE_VALUE.TRANSFER ? '账户转账' : (row.categoryName || '未分类') }}</strong>
              <span>{{ row.merchant || row.description || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="账户" min-width="160">
          <template #default="{ row }">
            <span v-if="row.bookType === BOOK_TYPE_VALUE.TRANSFER">{{ row.accountName || '未关联账户' }} → {{ row.targetAccountName || '未关联账户' }}</span>
            <span v-else>{{ row.accountName || '未关联账户' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="118" align="right">
          <template #default="{ row }">
            <span :class="amountClass(row.bookType)">
              {{ amountPrefix(row.bookType) }}{{ money(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作" width="112" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除这条流水？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </PageTable>

      <div v-else class="calendar-wrap">
        <div class="calendar-weekdays">
          <span v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day">{{ day }}</span>
        </div>
        <div class="calendar-grid">
          <button
            v-for="day in calendarDays"
            :key="day.date"
            class="calendar-day"
            :class="{ active: selectedDate === day.date, muted: !day.inMonth, filled: day.income > 0 || day.expense > 0 }"
            @click="selectCalendarDate(day.date)"
          >
            <span class="day-num">{{ day.day }}</span>
            <span class="day-amounts">
              <small v-if="day.income > 0" class="amount-positive">+{{ shortMoney(day.income) }}</small>
              <small v-if="day.expense > 0" class="amount-negative">-{{ shortMoney(day.expense) }}</small>
            </span>
          </button>
        </div>
      </div>
    </section>

    <el-drawer v-model="recordDrawerVisible" :title="recordDialogTitle" size="500px" destroy-on-close>
      <el-alert v-if="!activeAccounts.length" title="暂无可用账户，请先新增一个账户再记账。" type="warning" :closable="false" class="drawer-alert" />
      <el-alert v-if="formData.bookType !== BOOK_TYPE_VALUE.TRANSFER && !formCategoryOptions.length" title="暂无可用分类，请先在分类管理中新增分类。" type="warning" :closable="false" class="drawer-alert" />
      <el-form ref="recordFormRef" :model="formData" :rules="rules" label-width="78px" class="compact-form record-form">
        <div class="record-entry-card">
          <el-form-item class="record-type-item" prop="bookType">
            <el-segmented v-model="formData.bookType" :options="BOOK_TYPE_OPTIONS" @change="onTypeChange" />
          </el-form-item>
          <el-form-item class="record-amount-item" prop="amount">
            <span class="record-currency">¥</span>
            <el-input-number v-model="formData.amount" :min="0.01" :precision="2" :controls="false" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="日期" prop="bookDate">
          <el-date-picker v-model="formData.bookDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker v-model="formData.bookTime" value-format="HH:mm:ss" format="HH:mm" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="formData.bookType !== BOOK_TYPE_VALUE.TRANSFER" label="分类" prop="categoryId">
          <el-cascader v-model="formData.categoryId" :options="formCategoryOptions" :props="{ checkStrictly: true, value: 'id', label: 'name', children: 'children', emitPath: false }" filterable style="width:100%" />
        </el-form-item>
        <el-form-item :label="formData.bookType === BOOK_TYPE_VALUE.TRANSFER ? '转出账户' : '账户'" prop="accountId">
          <el-select v-model="formData.accountId" placeholder="请选择账户" filterable style="width:100%">
            <el-option v-for="item in activeAccounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.bookType === BOOK_TYPE_VALUE.TRANSFER" label="转入账户" prop="targetAccountId">
          <el-select v-model="formData.targetAccountId" placeholder="请选择转入账户" filterable style="width:100%">
            <el-option v-for="item in activeAccounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商家/对象">
          <el-input v-model="formData.merchant" placeholder="如：便利店、房东、工资" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="accountDrawerVisible" title="账户管理" size="600px" destroy-on-close>
      <div class="drawer-actions"><el-button type="primary" :icon="Plus" @click="openAccountAdd">新增账户</el-button></div>
      <el-table :data="accounts" border size="small">
        <el-table-column prop="name" label="账户" />
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ accountTypeText(row.accountType) }}</template></el-table-column>
        <el-table-column label="余额" width="130" align="right"><template #default="{ row }">{{ money(row.currentBalance) }}</template></el-table-column>
        <el-table-column label="状态" width="86"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="openAccountEdit(row)">编辑</el-button><el-button link type="danger" @click="handleAccountDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="accountDialogVisible" :title="accountForm.id ? '编辑账户' : '新增账户'" width="420px">
      <el-form :model="accountForm" label-width="78px" class="compact-form">
        <el-form-item label="名称"><el-input v-model="accountForm.name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="accountForm.accountType" style="width:100%"><el-option v-for="item in BOOK_ACCOUNT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="初始余额"><el-input-number v-model="accountForm.initialBalance" :precision="2" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="当前余额"><el-input-number v-model="accountForm.currentBalance" :precision="2" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="accountEnabled" active-text="启用" inactive-text="停用" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="accountForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="accountDialogVisible = false">取消</el-button><el-button type="primary" @click="submitAccount">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="budgetDrawerVisible" title="预算管理" size="620px" destroy-on-close>
      <div class="drawer-actions"><el-button type="primary" :icon="Plus" @click="openBudgetAdd">新增预算</el-button></div>
      <el-table :data="budgets" border size="small">
        <el-table-column prop="categoryName" label="预算项" />
        <el-table-column label="预算" width="120" align="right"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="已用" width="120" align="right"><template #default="{ row }">{{ money(row.usedAmount) }}</template></el-table-column>
        <el-table-column label="进度" min-width="160"><template #default="{ row }"><el-progress :percentage="Math.min(100, Number(row.usagePercent || 0))" :status="Number(row.usagePercent) > 100 ? 'exception' : undefined" /></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="openBudgetEdit(row)">编辑</el-button><el-button link type="danger" @click="handleBudgetDelete(row.id)">删除</el-button></template></el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="budgetDialogVisible" :title="budgetForm.id ? '编辑预算' : '新增预算'" width="440px">
      <el-form :model="budgetForm" label-width="78px" class="compact-form">
        <el-form-item label="月份"><el-date-picker v-model="budgetForm.budgetMonth" type="month" value-format="YYYY-MM" style="width:100%" /></el-form-item>
        <el-form-item label="分类"><el-cascader v-model="budgetForm.categoryId" :options="expenseCategoryOptions" :props="{ checkStrictly: true, value: 'id', label: 'name', children: 'children', emitPath: false }" clearable placeholder="总预算" style="width:100%" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="budgetForm.amount" :min="0.01" :precision="2" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="budgetForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="budgetDialogVisible = false">取消</el-button><el-button type="primary" @click="submitBudget">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="categoryDrawerVisible" title="分类管理" size="580px" destroy-on-close>
      <CategoryDrawer @refresh="loadCategories" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Books' })
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Aim, Plus, Setting, Wallet } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage, ElMessageBox } from '@/plugins/element-feedback'
import PageTable from '@/components/PageTable/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import CategoryDrawer from './CategoryDrawer.vue'
import {
  deleteBookAccountApi,
  deleteBookApi,
  deleteBookBudgetApi,
  exportBookApi,
  getBookAccountListApi,
  getBookBudgetListApi,
  getBookCalendarApi,
  getBookOverviewApi,
  getBookPageApi,
  getBookTrendApi,
  getCategoryListApi,
  saveBookAccountApi,
  saveBookApi,
  saveBookBudgetApi,
  updateBookAccountApi,
  updateBookApi,
  updateBookBudgetApi
} from '@/api/book'
import { useExport } from '@/composables/useExport'
import { usePageTable } from '@/composables/usePageTable'
import { BOOK_ACCOUNT_TYPE_MAP, BOOK_ACCOUNT_TYPE_OPTIONS, BOOK_TYPE_MAP, BOOK_TYPE_OPTIONS, BOOK_TYPE_TAG_TYPE, BOOK_TYPE_VALUE } from '@/constants/dict'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const now = new Date()
const currentMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const viewMode = ref<'list' | 'calendar'>('list')
const selectedDate = ref('')
const overview = ref<any>({ totalIncome: 0, totalExpense: 0, netAmount: 0, totalAssets: 0, totalBudget: 0, budgetRemaining: 0, budgetUsagePercent: 0, budgets: [], categoryRanks: [] })
const accounts = ref<any[]>([])
const budgets = ref<any[]>([])
const trendRows = ref<any[]>([])
const calendarRows = ref<any[]>([])
const allCategoryTree = ref<any[]>([])
const insightPreviewCount = 5

const { loading, list, total, query, handleSearch, resetQuery, handleCurrentChange, handleSizeChange } = usePageTable({
  fetchApi: getBookPageApi,
  defaultQuery: { bookType: undefined as any, categoryIds: [] as number[], accountId: undefined as any, keyword: '', yearMonth: currentMonth.value, pageSize: 12 },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).yearMonth = currentMonth.value
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  }
})

const { exporting, handleExport } = useExport({
  exportApi: exportBookApi,
  fileName: '个人记账记录'
})

const activeAccounts = computed(() => accounts.value.filter(item => Number(item.status) === 0))
const previewBudgets = computed(() => budgets.value.slice(0, insightPreviewCount))
const visibleAccounts = computed(() => accounts.value.slice(0, insightPreviewCount))
const categoryOptions = computed(() => buildCascaderOptions(allCategoryTree.value))
const incomeCategoryOptions = computed(() => buildCascaderOptions(allCategoryTree.value.filter(item => Number(item.type) === BOOK_TYPE_VALUE.INCOME)))
const expenseCategoryOptions = computed(() => buildCascaderOptions(allCategoryTree.value.filter(item => Number(item.type) === BOOK_TYPE_VALUE.EXPENSE)))
const formCategoryOptions = computed(() => formData.bookType === BOOK_TYPE_VALUE.INCOME ? incomeCategoryOptions.value : expenseCategoryOptions.value)
const budgetPercent = computed(() => Math.min(999, Number(overview.value.budgetUsagePercent || 0)))
const budgetProgressPercent = computed(() => Math.min(100, Number(overview.value.budgetUsagePercent || 0)))
const budgetSummaryText = computed(() => overview.value.totalBudget > 0 ? `已用 ${money(overview.value.budgetUsed)}，${budgetPercent.value}%` : '设置总预算或分类预算')
const accountEnabled = computed({
  get: () => Number(accountForm.status) === 0,
  set: (val: boolean) => { accountForm.status = val ? 0 : 1 }
})
const secondaryMetrics = computed(() => [
  {
    key: 'income',
    label: '本月收入',
    value: `+${money(overview.value.totalIncome)}`,
    tone: 'income',
    amountClass: 'amount-positive'
  },
  {
    key: 'net',
    label: '本月结余',
    value: signedMoney(overview.value.netAmount),
    tone: Number(overview.value.netAmount) >= 0 ? 'income' : 'expense',
    amountClass: Number(overview.value.netAmount) >= 0 ? 'amount-positive' : 'amount-negative'
  },
  {
    key: 'asset',
    label: '账户资产',
    value: money(overview.value.totalAssets),
    tone: 'asset',
    amountClass: 'amount-transfer'
  }
])
const budgetStatus = computed(() => {
  const totalBudget = Number(overview.value.totalBudget || 0)
  if (totalBudget <= 0) {
    return { title: '本月预算', percentText: '未设置', tone: 'empty', progressStatus: undefined as undefined }
  }
  const percent = Number(overview.value.budgetUsagePercent || 0)
  if (percent > 100) {
    return { title: '预算已超支', percentText: `${percent.toFixed(0)}%`, tone: 'danger', progressStatus: 'exception' as const }
  }
  if (percent >= 80) {
    return { title: '预算接近上限', percentText: `${percent.toFixed(0)}%`, tone: 'warning', progressStatus: 'warning' as const }
  }
  return { title: '预算控制良好', percentText: `${percent.toFixed(0)}%`, tone: 'safe', progressStatus: 'success' as const }
})
const topCategoryRanks = computed(() => {
  return buildRankRows(overview.value.categoryRanks || []).slice(0, insightPreviewCount)
})
const allCategoryRanks = computed(() => buildRankRows(overview.value.categoryRanks || []))
const insightDialogTitle = computed(() => {
  const titleMap: Record<InsightDialogType, string> = {
    trend: '本月趋势',
    budget: '预算进度',
    account: '账户余额',
    rank: '支出排行'
  }
  return titleMap[insightDialogType.value]
})
const quickActions = computed(() => [
  { key: 'account', label: '账户', icon: Wallet, handler: () => { accountDrawerVisible.value = true } },
  { key: 'budget', label: '预算', icon: Aim, handler: () => { budgetDrawerVisible.value = true } },
  { key: 'category', label: '分类', icon: Setting, handler: () => { categoryDrawerVisible.value = true } }
])

function buildRankRows(rows: any[]) {
  const max = Math.max(...rows.map((item: any) => Number(item.amount || 0)), 1)
  return rows.map((item: any) => ({
    ...item,
    percent: Math.max(6, Math.round((Number(item.amount || 0) / max) * 100))
  }))
}

const trendOption = computed(() => createTrendOption(false))
const trendDialogOption = computed(() => createTrendOption(true))

function createTrendOption(isDialog = false) {
  return {
  color: ['#2f9e44', '#cf1322'],
  tooltip: { trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: '#9aa6b2' } } },
  legend: { top: 0, right: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 11, color: '#667085' }, data: ['收入', '支出'] },
  grid: { left: isDialog ? 44 : 10, right: isDialog ? 28 : 16, top: 36, bottom: isDialog ? 34 : 22, containLabel: true },
  xAxis: {
    type: 'category',
    data: trendRows.value.map(item => String(item.date).slice(5)),
    boundaryGap: true,
    axisLabel: { fontSize: isDialog ? 10 : 9, color: '#7c8799', margin: isDialog ? 8 : 4 },
    axisLine: { lineStyle: { color: '#e5eaf0' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLabel: { fontSize: isDialog ? 10 : 9, color: '#7c8799', margin: isDialog ? 8 : 4, formatter: (value: number) => isDialog ? money(value) : axisMoney(value) },
    splitLine: { lineStyle: { color: '#edf1f5' } }
  },
  series: [
    {
      name: '收入',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: trendRows.value.map(item => Number(item.income || 0)),
      areaStyle: { color: 'rgba(47, 158, 68, 0.12)' },
      lineStyle: { width: 2 },
      itemStyle: { color: '#2f9e44' }
    },
    {
      name: '支出',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: trendRows.value.map(item => Number(item.expense || 0)),
      areaStyle: { color: 'rgba(207, 19, 34, 0.10)' },
      lineStyle: { width: 2 },
      itemStyle: { color: '#cf1322' }
    }
  ]
  }
}

const calendarDays = computed(() => {
  const [year, month] = currentMonth.value.split('-').map(Number)
  const first = new Date(year, month - 1, 1)
  const start = new Date(first)
  start.setDate(first.getDate() - first.getDay())
  const map = new Map(calendarRows.value.map((item: any) => [item.date, item]))
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start)
    date.setDate(start.getDate() + index)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    const row: any = map.get(key) || {}
    return { date: key, day: date.getDate(), inMonth: date.getMonth() === month - 1, income: Number(row.income || 0), expense: Number(row.expense || 0) }
  })
})

const recordDrawerVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const recordFormRef = ref<any>(null)
const recordDialogTitle = computed(() => `${isEdit.value ? '编辑' : '新增'}${BOOK_TYPE_MAP[formData.bookType] || '流水'}`)
const formData = reactive<any>({ id: undefined, bookType: BOOK_TYPE_VALUE.EXPENSE, amount: 0, bookDate: today(), bookTime: currentTime(), categoryId: undefined, accountId: undefined, targetAccountId: undefined, merchant: '', description: '' })
const rules = computed(() => ({
  bookType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  bookDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  categoryId: formData.bookType === BOOK_TYPE_VALUE.TRANSFER ? [] : [{ required: true, message: '请选择分类', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
  targetAccountId: formData.bookType === BOOK_TYPE_VALUE.TRANSFER ? [{ required: true, message: '请选择转入账户', trigger: 'change' }] : []
}))

const accountDrawerVisible = ref(false)
const accountDialogVisible = ref(false)
const accountForm = reactive<any>({ id: undefined, name: '', accountType: 3, initialBalance: 0, currentBalance: 0, status: 0, sortOrder: 0, remark: '' })
const budgetDrawerVisible = ref(false)
const budgetDialogVisible = ref(false)
const budgetForm = reactive<any>({ id: undefined, budgetMonth: currentMonth.value, categoryId: undefined, amount: 0, remark: '' })
const categoryDrawerVisible = ref(false)
type InsightDialogType = 'trend' | 'budget' | 'account' | 'rank'
const insightDialogVisible = ref(false)
const insightDialogType = ref<InsightDialogType>('trend')

watch(currentMonth, () => {
  query.yearMonth = currentMonth.value
  selectedDate.value = ''
  refreshAll()
})

async function refreshAll() {
  await Promise.all([loadAccounts(), loadCategories()])
  await Promise.all([loadOverview(), loadBudgets(), loadTrend(), loadCalendar()])
  handleSearch()
}

async function loadOverview() {
  const res: any = await getBookOverviewApi(currentMonth.value)
  overview.value = res.data || overview.value
}

async function loadAccounts() {
  const res: any = await getBookAccountListApi(false)
  accounts.value = res.data || []
}

async function loadBudgets() {
  const res: any = await getBookBudgetListApi(currentMonth.value)
  budgets.value = res.data || []
}

async function loadTrend() {
  const res: any = await getBookTrendApi(currentMonth.value)
  trendRows.value = res.data || []
}

async function loadCalendar() {
  const res: any = await getBookCalendarApi(currentMonth.value)
  calendarRows.value = res.data || []
}

async function loadCategories() {
  const res: any = await getCategoryListApi(undefined, true)
  allCategoryTree.value = res.data || []
}

function runSearch() {
  query.pageNum = 1
  handleSearch()
}

function resetFilters() {
  resetQuery()
  query.yearMonth = currentMonth.value
  query.keyword = ''
  query.accountId = undefined
  query.categoryIds = []
  query.bookDateStart = undefined
  query.bookDateEnd = undefined
  selectedDate.value = ''
  handleSearch()
}

function exportCurrent() {
  handleExport({ ...query, yearMonth: currentMonth.value })
}

function openAdd(date?: string) {
  isEdit.value = false
  const defaultType = expenseCategoryOptions.value.length ? BOOK_TYPE_VALUE.EXPENSE : BOOK_TYPE_VALUE.INCOME
  Object.assign(formData, { id: undefined, bookType: BOOK_TYPE_VALUE.EXPENSE, amount: 0, bookDate: date || selectedDate.value || today(), bookTime: currentTime(), categoryId: undefined, accountId: activeAccounts.value[0]?.id, targetAccountId: undefined, merchant: '', description: '' })
  formData.bookType = defaultType
  recordDrawerVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(formData, { id: row.id, bookType: row.bookType, amount: row.amount, bookDate: row.bookDate, bookTime: row.bookTime || currentTime(), categoryId: row.categoryId || undefined, accountId: row.accountId || undefined, targetAccountId: row.targetAccountId || undefined, merchant: row.merchant || '', description: row.description || '' })
  recordDrawerVisible.value = true
}

function onTypeChange() {
  formData.categoryId = undefined
  formData.targetAccountId = undefined
}

async function handleSubmit() {
  await recordFormRef.value?.validate?.()
  submitting.value = true
  try {
    const payload = { ...formData, categoryId: formData.bookType === BOOK_TYPE_VALUE.TRANSFER ? undefined : formData.categoryId }
    if (isEdit.value) await updateBookApi(payload)
    else await saveBookApi(payload)
    ElMessage.success('保存成功')
    recordDrawerVisible.value = false
    refreshAll()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deleteBookApi(id)
  ElMessage.success('删除成功')
  refreshAll()
}

function selectCalendarDate(date: string) {
  selectedDate.value = date
  query.bookDateStart = date
  query.bookDateEnd = date
  viewMode.value = 'list'
  runSearch()
}

function clearDateFilter() {
  selectedDate.value = ''
  query.bookDateStart = undefined
  query.bookDateEnd = undefined
  runSearch()
}

function selectAccount(accountId: number) {
  query.accountId = query.accountId === accountId ? undefined : accountId
  runSearch()
}

function openInsightDialog(type: InsightDialogType) {
  insightDialogType.value = type
  insightDialogVisible.value = true
}

function handleDialogAccountFilter(row: any) {
  if (!row?.id) return
  selectAccount(row.id)
  insightDialogVisible.value = false
}

function openAccountAdd() {
  Object.assign(accountForm, { id: undefined, name: '', accountType: 3, initialBalance: 0, currentBalance: 0, status: 0, sortOrder: accounts.value.length + 1, remark: '' })
  accountDialogVisible.value = true
}

function openAccountEdit(row: any) {
  Object.assign(accountForm, { ...row })
  accountDialogVisible.value = true
}

async function submitAccount() {
  if (!accountForm.name) return ElMessage.warning('请输入账户名称')
  if (accountForm.id) await updateBookAccountApi({ ...accountForm })
  else await saveBookAccountApi({ ...accountForm })
  ElMessage.success('账户已保存')
  accountDialogVisible.value = false
  refreshAll()
}

async function handleAccountDelete(id: number) {
  await ElMessageBox.confirm('删除账户前请确认没有历史流水。已有流水的账户建议停用。', '删除账户')
  await deleteBookAccountApi(id)
  ElMessage.success('账户已删除')
  refreshAll()
}

function openBudgetAdd() {
  Object.assign(budgetForm, { id: undefined, budgetMonth: currentMonth.value, categoryId: undefined, amount: 0, remark: '' })
  budgetDialogVisible.value = true
}

function openBudgetEdit(row: any) {
  Object.assign(budgetForm, { id: row.id, budgetMonth: row.budgetMonth, categoryId: row.categoryId || undefined, amount: row.amount, remark: row.remark || '' })
  budgetDialogVisible.value = true
}

async function submitBudget() {
  if (!budgetForm.budgetMonth || !budgetForm.amount) return ElMessage.warning('请填写预算月份和金额')
  if (budgetForm.id) await updateBookBudgetApi({ ...budgetForm })
  else await saveBookBudgetApi({ ...budgetForm })
  ElMessage.success('预算已保存')
  budgetDialogVisible.value = false
  refreshAll()
}

async function handleBudgetDelete(id: number) {
  await deleteBookBudgetApi(id)
  ElMessage.success('预算已删除')
  refreshAll()
}

function buildCascaderOptions(tree: any[]): any[] {
  return tree.map(item => ({
    id: item.id,
    name: item.name,
    children: item.children?.length ? item.children.map((child: any) => ({ id: child.id, name: child.name })) : undefined
  }))
}

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function signedMoney(value: any) {
  const num = Number(value || 0)
  return `${num >= 0 ? '+' : '-'}${money(Math.abs(num))}`
}

function shortMoney(value: any) {
  const num = Number(value || 0)
  return num >= 10000 ? `${(num / 10000).toFixed(1)}万` : num.toFixed(0)
}

function axisMoney(value: any) {
  const num = Number(value || 0)
  const abs = Math.abs(num)
  if (abs >= 10000) return `${(num / 10000).toFixed(abs >= 100000 ? 0 : 1)}万`
  if (abs >= 1000) return `${Math.round(num / 100) / 10}k`
  return String(Math.round(num))
}

function amountClass(type: number) {
  return type === BOOK_TYPE_VALUE.INCOME ? 'amount-positive' : type === BOOK_TYPE_VALUE.EXPENSE ? 'amount-negative' : 'amount-transfer'
}

function amountPrefix(type: number) {
  return type === BOOK_TYPE_VALUE.INCOME ? '+' : type === BOOK_TYPE_VALUE.EXPENSE ? '-' : ''
}

function accountTypeText(type: number) {
  return BOOK_ACCOUNT_TYPE_MAP[type] || '其他'
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

function currentTime() {
  return new Date().toTimeString().slice(0, 8)
}

onMounted(refreshAll)
</script>

<style scoped lang="scss">
.book-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #1f2a37;
  --book-ink: #101828;
  --book-muted: #667085;
  --book-line: rgba(13, 79, 130, 0.16);
  --book-glow: rgba(20, 184, 166, 0.2);
  --book-deep: #09233f;
  --book-cyan: #14b8a6;
  scroll-snap-type: none;
}

.book-overview-screen {
  display: grid;
  gap: 10px;
}

.book-hero {
  display: grid;
  grid-template-columns: minmax(420px, 1.35fr) minmax(320px, 0.65fr);
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.hero-main,
.hero-side,
.panel {
  position: relative;
  border: 1px solid var(--book-line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
  min-width: 0;
  transition: border-color 0.22s ease, box-shadow 0.22s ease;
}

.hero-side:hover,
.panel:hover {
  border-color: rgba(13, 79, 130, 0.24);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.hero-main {
  position: relative;
  overflow: hidden;
  padding: 14px 16px;
  color: #fff;
  background:
    linear-gradient(118deg, rgba(8, 26, 51, 0.98) 0%, rgba(10, 57, 92, 0.96) 58%, rgba(13, 124, 112, 0.9) 100%),
    radial-gradient(circle at 10% 22%, rgba(64, 196, 255, 0.18), transparent 30%);
  border-color: rgba(125, 226, 255, 0.22);
  box-shadow: 0 18px 42px rgba(9, 35, 63, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.hero-main::before {
  content: none;
}

.hero-main::after {
  content: '';
  position: absolute;
  right: -96px;
  bottom: -118px;
  width: 310px;
  height: 310px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.22), rgba(59, 130, 246, 0.08) 46%, transparent 70%);
}

.hero-topline,
.hero-summary,
.budget-brief-head,
.budget-brief-foot,
.ledger-head,
.ledger-toolbar,
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.hero-topline {
  position: relative;
  z-index: 1;
  align-items: flex-start;
}

.page-title {
  font-size: 23px;
  line-height: 1.15;
  font-weight: 800;
  color: inherit;
  letter-spacing: 0;
  text-shadow: none;
}

.hero-month {
  width: 132px;
  flex-shrink: 0;
}

.hero-month :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.22) inset, 0 10px 26px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(12px);
}

.hero-month :deep(.el-input__inner),
.hero-month :deep(.el-input__prefix),
.hero-month :deep(.el-input__suffix) {
  color: #fff;
}

.hero-balance {
  position: relative;
  z-index: 1;
  margin-top: 16px;
}

.hero-balance span,
.summary-chip span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-balance strong {
  display: block;
  margin-top: 6px;
  font-family: var(--font-mono);
  font-size: clamp(30px, 3.8vh, 40px);
  line-height: 1;
  letter-spacing: 0;
  overflow-wrap: anywhere;
  text-shadow: none;
}

.hero-summary {
  position: relative;
  z-index: 1;
  margin-top: 16px;
  align-items: stretch;
}

.summary-chip {
  flex: 1;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.11);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.summary-chip.income {
  box-shadow: inset 0 2px 0 rgba(47, 158, 68, 0.78), 0 12px 26px rgba(0, 0, 0, 0.12);
}

.summary-chip.expense {
  box-shadow: inset 0 2px 0 rgba(255, 120, 117, 0.8), 0 12px 26px rgba(0, 0, 0, 0.12);
}

.summary-chip.asset {
  box-shadow: inset 0 2px 0 rgba(125, 226, 255, 0.68);
}

.summary-chip strong {
  display: block;
  margin-top: 5px;
  font-family: var(--font-mono);
  font-size: 16px;
  line-height: 1.1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 253, 0.98));
}

.hero-side::before,
.panel::before {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(20, 184, 166, 0.48), rgba(22, 119, 255, 0.26), transparent);
}

.hero-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.hero-actions :deep(.el-button) {
  width: 100%;
  height: 32px;
  margin-left: 0;
  border-radius: 8px;
  border-color: rgba(13, 79, 130, 0.14);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  color: #1f2a37;
}

.hero-actions :deep(.el-button .el-icon) {
  color: inherit;
}

.hero-actions :deep(.el-button--primary) {
  border-color: rgba(20, 184, 166, 0.72);
  background: linear-gradient(135deg, #0f8b8d, #1677ff);
  box-shadow: 0 10px 20px rgba(22, 119, 255, 0.18);
  color: #fff;
}

.budget-brief {
  padding: 10px;
  border: 1px solid rgba(13, 79, 130, 0.14);
  border-radius: 8px;
  background: #f8fafc;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.budget-brief.safe { background: #f2fbf5; border-color: #cfeeda; }
.budget-brief.warning { background: #fff8e6; border-color: #ffe2a7; }
.budget-brief.danger { background: #fff1f0; border-color: #ffc8c4; }

.budget-brief-head {
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #526074;
}

.budget-brief-head strong {
  font-family: var(--font-mono);
  font-size: 18px;
  color: #1f2a37;
}

.budget-brief-foot {
  margin-top: 8px;
  color: #667085;
  font-size: 13px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  align-items: stretch;
  min-height: 0;
}

.panel {
  padding: 10px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 251, 253, 0.99));
}

.trend-panel {
  grid-column: span 2;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  border-color: rgba(13, 79, 130, 0.14);
  color: #1f2a37;
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
}

.panel-head {
  align-items: flex-start;
  min-height: 42px;
  margin-bottom: 8px;
}

.panel-head > div:first-child {
  min-width: 0;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.panel-actions :deep(.el-button) {
  margin-left: 0;
}

.panel-head h3,
.ledger-head h3 {
  margin: 0;
  font-size: 14px;
  line-height: 1.2;
  color: var(--book-ink);
}

.panel-head span,
.ledger-head span {
  display: block;
  margin-top: 3px;
  color: #7c8799;
  font-size: 12px;
  line-height: 1.25;
}

.panel-total {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-weight: 700;
  color: #0f766e;
}

.trend-chart {
  height: 214px;
  width: 100%;
}

.insight-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.budget-list,
.account-list,
.rank-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 5px;
  min-height: 0;
}

.budget-item,
.account-row,
.rank-row {
  min-height: 48px;
  padding: 7px 9px;
  border: 1px solid rgba(13, 79, 130, 0.12);
  border-radius: 8px;
  background: #fff;
  box-shadow: none;
}

.budget-item div,
.rank-meta,
.account-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.budget-item span,
.rank-meta span {
  color: #526074;
  font-size: 12px;
}

.budget-item strong,
.rank-meta strong,
.account-row strong {
  font-family: var(--font-mono);
  font-size: 12px;
  color: #1f2a37;
  white-space: nowrap;
}

.budget-item :deep(.el-progress) {
  margin-top: 5px;
}

.account-row {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.rank-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
}

.account-row:hover,
.account-row.active {
  border-color: rgba(22, 119, 255, 0.42);
  background: #f3f8ff;
  box-shadow: inset 3px 0 0 #1677ff;
}

.account-row span {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.account-row b {
  font-size: 13px;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-row small {
  color: #8a94a6;
  font-size: 11px;
  line-height: 1.1;
}

.rank-bar {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: #e7eef5;
}

.rank-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1677ff, #14b8a6);
  box-shadow: none;
}

.insight-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}

.insight-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dialog-trend-chart {
  width: 100%;
  height: 340px;
}

.dialog-rank-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: center;
  gap: 10px;
}

.dialog-rank-cell span {
  color: #667085;
  font-family: var(--font-mono);
  font-size: 12px;
  text-align: right;
}

.ledger-panel {
  padding: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, #ffffff, #f8fbff),
    radial-gradient(circle at 0% 0%, rgba(22, 119, 255, 0.1), transparent 32%);
}

.ledger-head {
  align-items: center;
  padding: 14px 18px 10px;
  margin-bottom: 0;
}

.ledger-tabs {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.ledger-toolbar {
  align-items: center;
  padding: 0 18px 10px;
  border-bottom: 1px solid rgba(13, 79, 130, 0.12);
}

.ledger-filters {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.ledger-filters :deep(.el-input__wrapper),
.ledger-filters :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 0 0 1px rgba(13, 79, 130, 0.11) inset;
}

.ledger-panel :deep(.page-table) {
  padding: 10px 16px 14px;
  border-radius: 0;
  box-shadow: none;
}

.ledger-panel :deep(.el-table .cell) {
  line-height: 18px;
}

.ledger-panel :deep(.el-table--small .el-table__cell) {
  padding: 7px 0;
}

.date-cell,
.main-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.date-cell span,
.main-cell span {
  color: #8a94a6;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount-positive {
  color: #2f9e44;
  font-weight: 700;
  font-family: var(--font-mono);
}

.amount-negative {
  color: #cf1322;
  font-weight: 700;
  font-family: var(--font-mono);
}

.amount-transfer {
  color: #d97706;
  font-weight: 700;
  font-family: var(--font-mono);
}

.calendar-wrap {
  padding: 14px 16px 16px;
}

.calendar-weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.calendar-weekdays {
  margin-bottom: 8px;
  color: #7c8799;
  font-size: 12px;
  text-align: center;
}

.calendar-grid {
  gap: 8px;
}

.calendar-day {
  min-height: 76px;
  border: 1px solid rgba(13, 79, 130, 0.12);
  border-radius: 8px;
  background: #fff;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.calendar-day:hover,
.calendar-day.active {
  border-color: rgba(22, 119, 255, 0.5);
  background: #f3f8ff;
  box-shadow: inset 0 0 0 1px rgba(22, 119, 255, 0.08);
}

.calendar-day.muted {
  background: #f8fafc;
  color: #a3adbd;
}

.calendar-day.filled {
  background: #f8fbff;
  box-shadow: inset 0 2px 0 rgba(20, 184, 166, 0.28);
}

.day-num {
  font-weight: 800;
}

.day-amounts {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 0;
}

.day-amounts small {
  max-width: 100%;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.drawer-alert {
  margin-bottom: 8px;
}

.compact-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.compact-form :deep(.el-input__wrapper),
.compact-form :deep(.el-select__wrapper) {
  min-height: 32px;
}

.record-entry-card {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid rgba(13, 79, 130, 0.16);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(8, 26, 51, 0.96), rgba(10, 119, 111, 0.86)),
    radial-gradient(circle at 100% 0%, rgba(125, 226, 255, 0.24), transparent 40%);
  box-shadow: 0 16px 34px rgba(9, 35, 63, 0.18);
}

.record-entry-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.record-type-item :deep(.el-form-item__content),
.record-amount-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.record-type-item :deep(.el-segmented) {
  width: 100%;
}

.record-amount-item {
  position: relative;
  margin-bottom: 0 !important;
}

.record-currency {
  position: absolute;
  left: 12px;
  top: 50%;
  z-index: 1;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.72);
  font-weight: 700;
}

.record-amount-item :deep(.el-input-number .el-input__inner) {
  height: 46px;
  padding-left: 26px;
  font-family: var(--font-mono);
  font-size: 24px;
  font-weight: 800;
  text-align: left;
  color: #fff;
}

.record-entry-card :deep(.el-input__wrapper),
.record-entry-card :deep(.el-segmented) {
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.18) inset;
}

.record-entry-card :deep(.el-segmented__item) {
  color: rgba(255, 255, 255, 0.78);
}

.record-entry-card :deep(.el-segmented__item.is-selected) {
  color: #08233f;
  font-weight: 800;
}

.panel :deep(.el-empty) {
  padding: 10px 0;
}

.panel :deep(.el-empty__description) {
  margin-top: 4px;
}

@media (max-width: 1180px) {
  .book-hero {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trend-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .book-hero,
  .insight-grid {
    grid-template-columns: 1fr;
  }

  .trend-panel {
    grid-column: auto;
  }

  .hero-topline,
  .hero-summary,
  .ledger-head,
  .ledger-toolbar {
    flex-direction: column;
  }

  .hero-month {
    width: 100%;
  }

  .hero-balance strong {
    font-size: 30px;
  }

  .hero-actions {
    grid-template-columns: 1fr;
  }

  .ledger-tabs,
  .ledger-filters {
    justify-content: flex-start;
    width: 100%;
  }

  .calendar-grid {
    gap: 5px;
  }

  .calendar-day {
    min-height: 68px;
    padding: 6px;
  }
}
</style>
