/**
 * 分页表格组合式函数
 * 封装列表查询、分页、重置等通用逻辑
 * 支持 autoSearch 自动搜索模式：筛选条件变化时自动触发查询（带防抖）
 */
import { ref, reactive, computed, watch, nextTick } from 'vue'
import type { FormInstance } from 'element-plus'
import { PAGE_DEFAULTS } from '@/constants/dict'

/** 防抖函数 */
function debounce<T extends (...args: any[]) => any>(fn: T, ms: number): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), ms)
  }
}

export interface PageQuery {
  pageNum: number
  pageSize: number
  [key: string]: any
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface PageTableOptions<T> {
  /** 查询接口函数 */
  fetchApi: (params: any) => Promise<{ data: PageResult<T> }>
  /** 默认查询参数 */
  defaultQuery?: Record<string, any>
  /** 是否立即加载 */
  immediate?: boolean
  /** 加载前回调，返回false阻止加载 */
  beforeFetch?: (params: any) => boolean | void
  /** 加载后回调 */
  afterFetch?: (data: T[]) => void
  /** 是否开启自动搜索（筛选条件变化时自动触发查询），默认 false */
  autoSearch?: boolean
  /** 自动搜索防抖时间（毫秒），默认 400 */
  debounceMs?: number
}

export function usePageTable<T = any>(options: PageTableOptions<T>) {
  const {
    fetchApi,
    defaultQuery = {},
    immediate = true,
    beforeFetch,
    afterFetch,
    autoSearch = false,
    debounceMs = 400,
  } = options

  // 加载状态
  const loading = ref(false)

  // 列表数据
  const list = ref<T[]>([]) as any

  // 分页总数
  const total = ref(0)

  // 查询表单引用
  const queryFormRef = ref<FormInstance>()

  // 查询参数
  const query = reactive<PageQuery>({
    pageNum: PAGE_DEFAULTS.PAGE_NUM,
    pageSize: PAGE_DEFAULTS.PAGE_SIZE,
    ...defaultQuery
  })

  // 分页配置
  const pageSizes = PAGE_DEFAULTS.PAGE_SIZES

  // 内部变更标记（防止 reset/handleSearch 触发 autoSearch watch）
  let _isInternalChange = false

  // 渲染锁：loadData 完成后保持到 nextTick（Vue DOM 更新）之后才解除
  // 原因：loading.value=false 在 finally 中先执行，但 Vue 的组件重渲染在微任务队列中稍后发生，
  //       重渲染时 Element Plus 表单控件会归一化 v-model 值（undefined→null 等），触发 deep watcher
  let _renderLocked = false
  let _loadRequestId = 0

  // 加载数据
  const loadData = async () => {
    const fetchParams = { ...query }
    // 加载前回调
    if (beforeFetch) {
      const result = beforeFetch(fetchParams)
      if (result === false) return
    }

    const requestId = ++_loadRequestId
    loading.value = true
    _renderLocked = true
    try {
      const { data } = await fetchApi(fetchParams)
      if (requestId !== _loadRequestId) return
      list.value = data.records || []
      total.value = data.total || 0
      query.pageNum = data.current || query.pageNum
      query.pageSize = data.size || query.pageSize

      // 加载后回调
      if (afterFetch) {
        afterFetch(list.value)
      }
    } catch (error) {
      if (requestId !== _loadRequestId) return
      console.error('加载数据失败:', error)
      list.value = []
      total.value = 0
    } finally {
      if (requestId === _loadRequestId) {
        loading.value = false
        // 延迟到 Vue DOM 更新完成后解除锁，防止组件重渲染时表单控件归一化值触发死循环
        nextTick(() => { _renderLocked = false })
      }
    }
  }

  // 重置查询
  const resetQuery = async () => {
    _isInternalChange = true
    try {
      // 重置页码
      query.pageNum = PAGE_DEFAULTS.PAGE_NUM
      query.pageSize = PAGE_DEFAULTS.PAGE_SIZE

      // 重置查询条件
      Object.keys(query).forEach(key => {
        if (key !== 'pageNum' && key !== 'pageSize') {
          delete query[key]
        }
      })

      // 恢复默认查询条件
      Object.assign(query, defaultQuery)

      // 清空表单验证
      queryFormRef.value?.resetFields()

      // 重新加载
      await loadData()
    } finally {
      _isInternalChange = false
    }
  }

  // 页码改变
  const handleCurrentChange = (pageNum: number) => {
    query.pageNum = pageNum
    loadData()
  }

  // 每页条数改变
  const handleSizeChange = (pageSize: number) => {
    query.pageNum = PAGE_DEFAULTS.PAGE_NUM
    query.pageSize = pageSize
    loadData()
  }

  // 搜索（手动）
  const handleSearch = () => {
    _isInternalChange = true
    query.pageNum = PAGE_DEFAULTS.PAGE_NUM
    loadData()
    setTimeout(() => { _isInternalChange = false }, 50)
  }

  // 刷新当前页
  const refresh = () => {
    loadData()
  }

  // 刷新到第一页
  const refreshFirstPage = () => {
    query.pageNum = PAGE_DEFAULTS.PAGE_NUM
    loadData()
  }

  // 计算属性：是否为空
  const isEmpty = computed(() => !loading.value && list.value.length === 0)

  // 立即加载
  if (immediate) {
    loadData()
  }

  // 自动搜索：watch query 中非分页字段的变化（带防抖）
  if (autoSearch) {
    const debouncedSearch = debounce(() => {
      query.pageNum = PAGE_DEFAULTS.PAGE_NUM
      loadData()
    }, debounceMs)

    watch(
      () => {
        const pick: Record<string, any> = {}
        for (const key in query) {
          if (key !== 'pageNum' && key !== 'pageSize') {
            pick[key] = (query as any)[key]
          }
        }
        return pick
      },
      () => {
        // 正在加载、内部变更、或渲染锁期间跳过，防止组件重渲染导致表单控件归一化值触发死循环
        if (_isInternalChange || loading.value || _renderLocked) return
        debouncedSearch()
      },
      { deep: true }
    )
  }

  return {
    // 状态
    loading,
    list,
    total,
    query,
    pageSizes,
    queryFormRef,
    isEmpty,

    // 方法
    loadData,
    resetQuery,
    handleCurrentChange,
    handleSizeChange,
    handleSearch,
    refresh,
    refreshFirstPage,
    autoSearch,
  }
}
