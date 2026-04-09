import request from '@/utils/request'

// ========== 记账 CRUD ==========
export const getBookPageApi = (params: any) => request.get('/books/page', { params })
export const saveBookApi = (data: any) => request.post('/books', data)
export const updateBookApi = (data: any) => request.put('/books', data)
export const deleteBookApi = (id: number) => request.delete(`/books/${id}`)
export const batchDeleteBookApi = (ids: number[]) => request.delete('/books/batch', { data: ids })
export const exportBookApi = (params: any) => request.get('/books/export', { params, responseType: 'blob' })

// ========== 统计 ==========
export const getBookSummaryApi = (yearMonth?: string) => request.get('/books/summary', { params: { yearMonth } })

// ========== 分类管理 ==========
export const getCategoryListApi = (type?: number, enabledOnly?: boolean) => request.get('/books/categories', { params: { type, enabledOnly } })
export const saveCategoryApi = (data: any) => request.post('/books/categories', data)
export const updateCategoryApi = (data: any) => request.put('/books/categories', data)
export const deleteCategoryApi = (id: number) => request.delete(`/books/categories/${id}`)
