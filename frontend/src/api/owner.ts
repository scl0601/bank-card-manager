import request from '@/utils/request'

export const getOwnerPageApi = (params: any) => request.get('/owners/page', { params })
export const getOwnerDetailApi = (id: number) => request.get(`/owners/${id}`)
export const saveOwnerApi = (data: any) => request.post('/owners', data)
export const updateOwnerApi = (data: any) => request.put('/owners', data)
export const deleteOwnerApi = (id: number) => request.delete(`/owners/${id}`)
