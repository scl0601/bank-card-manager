import request from '@/utils/request'

export const getMonitorTodayApi = (params: { date?: string }) =>
  request.get('/monitor/today', { params })
