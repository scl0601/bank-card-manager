import request from '@/utils/request'

export const getCardPageApi = (params: any) => request.get('/cards/page', { params })
export const getCardListApi = () => request.get('/cards/list')
export const getCardDetailApi = (id: number) => request.get(`/cards/${id}`)
export const saveCardApi = (data: any) => request.post('/cards', data)
export const updateCardApi = (data: any) => request.put('/cards', data)
export const deleteCardApi = (id: number) => request.delete(`/cards/${id}`)
