import request from '@/utils/request'

export const getProfitOverviewApi = (params: any) => request.get('/profits/overview', { params })
export const getProfitUserPageApi = (params: any) => request.get('/profits/users', { params })
export const getProfitCardPageApi = (params: any) => request.get('/profits/cards', { params })
export const getProfitMonthListApi = (params: any) => request.get('/profits/months', { params })
