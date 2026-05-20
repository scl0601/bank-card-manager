import request from '@/utils/request'

export const getSpecialConfigApi = () => request.get('/special/config')
export const saveSpecialConfigApi = (data: any) => request.put('/special/config', data)

export const getSpecialCardsApi = () => request.get('/special/cards')
export const saveSpecialCardApi = (data: any) => request.post('/special/cards', data)
export const updateSpecialCardApi = (data: any) => request.put('/special/cards', data)
export const deleteSpecialCardApi = (id: number) => request.delete(`/special/cards/${id}`)

export const getSpecialBillPageApi = (params: any) => request.get('/special/bills/page', { params })
export const updateSpecialBillApi = (data: any) => request.put('/special/bills', data)
export const batchUpdateSpecialBillsApi = (data: any[]) => request.put('/special/bills/batch', data)
export const importSpecialBillsApi = (file: File, year?: number) => {
  const formData = new FormData()
  formData.append('file', file)
  if (year) {
    formData.append('year', String(year))
  }
  return request.post('/special/bills/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
export const deleteSpecialBillsBeforeYearApi = (data: any) => request.delete('/special/bills/before-year', { data })
export const deleteSpecialBillsAfterYearApi = (data: any) => request.delete('/special/bills/after-year', { data })
export const batchDeleteSpecialBillsApi = (ids: number[]) => request.delete('/special/bills/batch', { data: ids })

export const getSpecialProfitStatsApi = (params: any) => request.get('/special/profit/stats', { params })
export const updateSpecialProfitExtraFeesApi = (data: any) => request.put('/special/profit/extra-fees', data)
export const batchUpdateSpecialProfitExtraFeesApi = (data: any[]) => request.put('/special/profit/extra-fees/batch', data)
