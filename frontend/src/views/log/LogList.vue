<template>
  <div>
    <div class="page-title">系统日志</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="handleReset">
      <el-form-item label="操作人">
        <el-input v-model="query.operator" placeholder="操作人" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="操作模块">
        <el-input v-model="query.module" placeholder="模块名" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.result" placeholder="全部" clearable style="width:100px">
          <el-option v-for="item in LOG_RESULT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          value-format="YYYY-MM-DD" style="width:240px" @change="onDateChange" />
      </el-form-item>
      <template #extra-buttons>
        <ExportButton :loading="exporting" @click="handleExport" />
      </template>
    </SearchBar>

    <!-- 表格区 -->
    <PageTable
      :data="list"
      :loading="loading"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      border
      @update:page-num="val => query.pageNum = val"
      @update:page-size="val => query.pageSize = val"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <p v-if="row.requestMethod"><strong>请求方法：</strong>{{ row.requestMethod }}</p>
            <p v-if="row.requestParams"><strong>请求参数：</strong><code>{{ row.requestParams }}</code></p>
            <p v-if="row.errorMsg" class="error-msg"><strong>错误信息：</strong>{{ row.errorMsg }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="action" label="操作" width="100" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="requestPath" label="接口路径" width="220" show-overflow-tooltip />
      <el-table-column label="结果" width="80">
        <template #default="{ row }">
          <StatusTag :value="row.result" :label-map="LOG_RESULT_MAP" :type-map="LOG_RESULT_TAG_TYPE" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="clientIp" label="IP" width="130" />
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">
          <span :class="{ 'duration-slow': row.duration > 1000 }">
            {{ row.duration ? row.duration + 'ms' : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </PageTable>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Logs' })
import { ref } from 'vue'
import { formatTime } from '@/utils/formatters'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import { getLogPageApi, exportLogApi } from '@/api/log'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { LOG_RESULT_OPTIONS, LOG_RESULT_MAP, LOG_RESULT_TAG_TYPE } from '@/constants/dict'

const dateRange = ref<string[]>([])

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getLogPageApi,
  defaultQuery: { operator: '', module: '', result: undefined as any, startDate: '', endDate: '' },
  autoSearch: true,
  beforeFetch: (params) => {
    ;(params as any).current = params.pageNum; (params as any).size = params.pageSize
    delete (params as any).pageNum; delete (params as any).pageSize
  }
})

const { exporting, handleExport } = useExport({
  exportApi: exportLogApi,
  fileName: '操作日志'
})

function onDateChange(val: string[] | null) {
  if (val) { query.startDate = val[0]; query.endDate = val[1] }
  else { query.startDate = ''; query.endDate = '' }
}

// 重置时额外清空日期范围
const handleReset = () => {
  resetQuery()
  dateRange.value = []
}
</script>

<style scoped lang="scss">
.expand-content {
  padding: 12px 20px; background: #fafafa;
  p { margin: 6px 0; font-size: 13px; }
  code { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-size: 12px; word-break: break-all; }
  .error-msg { color: #ff4d4f; }
}
.duration-slow { color: #faad14; font-weight: 600; }
</style>
