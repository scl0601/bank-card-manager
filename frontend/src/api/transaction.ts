import request from '@/utils/request'

export const getTransactionPageApi = (params: any) => request.get('/transactions/page', { params })
export const saveTransactionApi = (data: any) => request.post('/transactions', data)
export const updateTransactionApi = (data: any) => request.put('/transactions', data)
export const deleteTransactionApi = (id: number) => request.delete(`/transactions/${id}`)
export const exportTransactionApi = (params: any) => request.get('/transactions/export', { params, responseType: 'blob' })
export const batchDeleteTransactionApi = (ids: number[]) => request.delete('/transactions/batch', { data: ids })
