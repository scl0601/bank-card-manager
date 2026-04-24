import request from '@/utils/request'

export const getCardPageApi = (params: any) => request.get('/cards/page', { params })
export const getCardListApi = () => request.get('/cards/list')
export const getCardDetailApi = (id: number) => request.get(`/cards/${id}`)
export const saveCardApi = (data: any) => request.post('/cards', data)
export const updateCardApi = (data: any) => request.put('/cards', data)
export const deleteCardApi = (id: number) => request.delete(`/cards/${id}`)

/** 按用户分组查询银行卡（两级层级模型） */
export const getCardsGroupedByUserApi = (params?: any) => request.get('/cards/grouped-by-user', { params })

// ========== 用户 CRUD（含两级层级） ==========
const USER_BASE = '/card/user'
export const getUserPageTreeApi = (params: any) => request.get(`${USER_BASE}/page`, { params })
export const getUserTreeApi = () => request.get(`${USER_BASE}/tree`)
export const getUserListActiveApi = () => request.get(`${USER_BASE}/list-active`)
export const getUserDetailApi = (id: number) => request.get(`${USER_BASE}/detail/${id}`)
export const saveUserApi = (data: any) => request.post(USER_BASE, data)
export const updateUserApi = (data: any) => request.put(USER_BASE, data)
export const deleteUserApi = (id: number) => request.delete(`${USER_BASE}/${id}`)

/** 修改手续费率（级联更新子用户） */
export const updateUserFeeRateApi = (data: { userId: number; feeRate: number }) =>
  request.put(`${USER_BASE}/update-fee-rate`, data)
