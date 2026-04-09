import request from '@/utils/request'

export const getLogPageApi = (params: any) => request.get('/logs/page', { params })
export const exportLogApi = (params: any) => request.get('/logs/export', { params, responseType: 'blob' })
