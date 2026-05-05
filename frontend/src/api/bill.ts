import request from '@/utils/request'

export const getBillPageApi = (params: any) => request.get('/bills/page', { params })
export const getBillOverviewApi = (params: any) => request.get('/bills/overview', { params })
export const getBillDetailApi = (id: number) => request.get(`/bills/${id}`)
export const saveBillApi = (data: any) => request.post('/bills', data)
export const updateBillApi = (data: any) => request.put('/bills', data)
export const updateBillVerificationApi = (id: number, data: { verified?: boolean; expenseVerified?: boolean }) =>
  request.patch(`/bills/${id}/verification`, null, { params: data })
export const deleteBillApi = (id: number) => request.delete(`/bills/${id}`)
export const batchDeleteBillsApi = (ids: number[]) => request.delete('/bills/batch', { data: ids })
export const repayBillApi = (id: number, actualPayAmount: number, actualPayDate: string) =>
  request.patch(`/bills/${id}/repay`, null, { params: { actualPayAmount, actualPayDate } })
export const exportBillApi = (params: any) => request.get('/bills/export', { params, responseType: 'blob' })

// 自动生成年账单
export const generateAnnualBillsApi = (cardId: number, data: any) =>
  request.post(`/bills/generate-annual/${cardId}`, null, { params: data })

// 账单日 / 还款日联动更新
export const syncBillScheduleApi = (
  cardId: number,
  fromBillMonth: string,
  data: { newBillDay?: number; newRepayDay?: number }
) => request.patch(`/bills/${cardId}/sync-schedule`, null, { params: { fromBillMonth, ...data } })

// ========== 账单明细 ==========
export const getDetailListApi = (billId: number) => request.get(`/bills/${billId}/details`)
export const saveDetailApi = (data: any) => request.post('/bills/details', data)
export const updateDetailApi = (data: any) => request.put('/bills/details', data)
export const deleteDetailApi = (id: number) => request.delete(`/bills/details/${id}`)
export const batchDeleteDetailsApi = (ids: number[]) => request.delete('/bills/details/batch', { data: ids })
export const batchUpdateTypeApi = (data: { ids: number[]; detailType: number }) =>
  request.patch('/bills/details/batch-type', data)
