<template>
  <div class="page-table">
    <!-- 表格 -->
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="data"
      :stripe="stripe"
      :border="border"
      :height="height"
      :max-height="maxHeight"
      :row-key="rowKey"
      :highlight-current-row="highlightCurrentRow"
      :empty-text="emptyText"
      v-bind="$attrs"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <slot />
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentSize"
        :page-sizes="pageSizes"
        :total="total"
        :layout="paginationLayout"
        :background="paginationBackground"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { PAGE_DEFAULTS } from '@/constants/dict'

interface Props {
  data: any[]
  loading?: boolean
  total?: number
  pageNum?: number
  pageSize?: number
  pageSizes?: number[]
  showPagination?: boolean
  stripe?: boolean
  border?: boolean
  height?: string | number
  maxHeight?: string | number
  rowKey?: string | ((row: any) => string)
  highlightCurrentRow?: boolean
  emptyText?: string
  paginationLayout?: string
  paginationBackground?: boolean
}

interface Emits {
  (e: 'update:pageNum', value: number): void
  (e: 'update:pageSize', value: number): void
  (e: 'size-change', size: number): void
  (e: 'current-change', page: number): void
  (e: 'selection-change', selection: any[]): void
  (e: 'sort-change', sort: any): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  total: 0,
  pageNum: PAGE_DEFAULTS.PAGE_NUM,
  pageSize: PAGE_DEFAULTS.PAGE_SIZE,
  pageSizes: () => [...PAGE_DEFAULTS.PAGE_SIZES],
  showPagination: true,
  stripe: true,
  border: false,
  highlightCurrentRow: true,
  emptyText: '暂无数据',
  paginationLayout: 'total, sizes, prev, pager, next, jumper',
  paginationBackground: true
})

const emit = defineEmits<Emits>()

const tableRef = ref()

// 当前页码
const currentPage = computed({
  get: () => props.pageNum,
  set: (val) => emit('update:pageNum', val)
})

// 每页条数
const currentSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val)
})

// 每页条数改变
const handleSizeChange = (size: number) => {
  emit('size-change', size)
}

// 页码改变
const handleCurrentChange = (page: number) => {
  emit('current-change', page)
}

// 选择改变
const handleSelectionChange = (selection: any[]) => {
  emit('selection-change', selection)
}

// 排序改变
const handleSortChange = (sort: any) => {
  emit('sort-change', sort)
}

// 暴露表格方法
defineExpose({
  tableRef,
  clearSelection: () => tableRef.value?.clearSelection(),
  toggleRowSelection: (row: any, selected?: boolean) => tableRef.value?.toggleRowSelection(row, selected),
  toggleAllSelection: () => tableRef.value?.toggleAllSelection(),
  setCurrentRow: (row: any) => tableRef.value?.setCurrentRow(row),
  clearSort: () => tableRef.value?.clearSort(),
  clearFilter: (columnKeys?: string[]) => tableRef.value?.clearFilter(columnKeys),
  doLayout: () => tableRef.value?.doLayout(),
  sort: (prop: string, order: string) => tableRef.value?.sort(prop, order)
})
</script>

<style scoped lang="scss">
.page-table {
  background: var(--color-card);
  padding: 16px 20px;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-card);

  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
