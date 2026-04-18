import request from '@/utils/request'

export const getMonthEventsApi = (month: string) =>
  request.get('/calendar/events/month', { params: { month } })

export const getDayEventsApi = (params: { eventDate: string; category?: number; status?: number }) =>
  request.get('/calendar/events/day', { params })

export const getEventDetailApi = (id: number) =>
  request.get(`/calendar/events/${id}`)

export const saveEventApi = (data: any) =>
  request.post('/calendar/events', data)

export const updateEventApi = (id: number, data: any) =>
  request.put(`/calendar/events/${id}`, data)

export const deleteEventApi = (id: number) =>
  request.delete(`/calendar/events/${id}`)

export const updateEventStatusApi = (id: number, status: number) =>
  request.put(`/calendar/events/${id}/status`, null, { params: { status } })

export const getCalendarStatsApi = (month: string) =>
  request.get('/calendar/events/stats/month', { params: { month } })

export const getYearStatsApi = (year: string) =>
  request.get('/calendar/events/stats/year', { params: { year } })
