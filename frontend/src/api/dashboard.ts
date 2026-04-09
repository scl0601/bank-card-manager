import request from '@/utils/request'

export const getDashboardStatsApi = () => request.get('/dashboard/stats')
