import request from '@/utils/request'

// ========== 记账 CRUD ==========
export const getBookPageApi = (params: any) => request.get('/books/page', { params })
export const saveBookApi = (data: any) => request.post('/books', data)
export const updateBookApi = (data: any) => request.put('/books', data)
export const deleteBookApi = (id: number) => request.delete(`/books/${id}`)
export const batchDeleteBookApi = (ids: number[]) => request.delete('/books/batch', { data: ids })
export const exportBookApi = (params: any) => request.get('/books/export', { params, responseType: 'blob' })
export const previewWechatBookImportApi = (data: FormData) => request.post('/books/wechat/import/preview', data)
export const importWechatBookApi = (data: any) => request.post('/books/wechat/import', data)
export const previewAlipayBookImportApi = (data: FormData) => request.post('/books/alipay/import/preview', data)
export const importAlipayBookApi = (data: any) => request.post('/books/alipay/import', data)

// ========== 统计 ==========
export const getBookSummaryApi = (yearMonth?: string) => request.get('/books/summary', { params: { yearMonth } })
export const getBookOverviewApi = (yearMonth?: string) => request.get('/books/overview', { params: { yearMonth } })
export const getBookTrendApi = (yearMonth?: string) => request.get('/books/trend', { params: { yearMonth } })
export const getBookCalendarApi = (yearMonth?: string) => request.get('/books/calendar', { params: { yearMonth } })

// ========== 分类管理 ==========
export const getCategoryListApi = (type?: number, enabledOnly?: boolean) => request.get('/books/categories', { params: { type, enabledOnly } })
export const saveCategoryApi = (data: any) => request.post('/books/categories', data)
export const updateCategoryApi = (data: any) => request.put('/books/categories', data)
export const deleteCategoryApi = (id: number) => request.delete(`/books/categories/${id}`)

// ========== 账户管理 ==========
export const getBookAccountListApi = (enabledOnly?: boolean) => request.get('/books/accounts', { params: { enabledOnly } })
export const saveBookAccountApi = (data: any) => request.post('/books/accounts', data)
export const updateBookAccountApi = (data: any) => request.put('/books/accounts', data)
export const deleteBookAccountApi = (id: number) => request.delete(`/books/accounts/${id}`)

// ========== 预算管理 ==========
export const getBookBudgetListApi = (yearMonth?: string) => request.get('/books/budgets', { params: { yearMonth } })
export const saveBookBudgetApi = (data: any) => request.post('/books/budgets', data)
export const updateBookBudgetApi = (data: any) => request.put('/books/budgets', data)
export const deleteBookBudgetApi = (id: number) => request.delete(`/books/budgets/${id}`)
