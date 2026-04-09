import request from '@/utils/request'

export const getBillPageApi = (params: any) => request.get('/bills/page', { params })
export const getBillDetailApi = (id: number) => request.get(`/bills/${id}`)
export const saveBillApi = (data: any) => request.post('/bills', data)
export const updateBillApi = (data: any) => request.put('/bills', data)
export const deleteBillApi = (id: number) => request.delete(`/bills/${id}`)
export const repayBillApi = (id: number, actualPayAmount: number, actualPayDate: string) =>
  request.patch(`/bills/${id}/repay`, null, { params: { actualPayAmount, actualPayDate } })
export const exportBillApi = (params: any) => request.get('/bills/export', { params, responseType: 'blob' })
