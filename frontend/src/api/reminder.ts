import request from '@/utils/request'

export const getReminderPageApi = (params: any) => request.get('/reminders/page', { params })
export const markHandledApi = (id: number) => request.patch(`/reminders/${id}/handle`)
export const markIgnoredApi = (id: number) => request.patch(`/reminders/${id}/ignore`)
export const scanBillRemindersApi = () => request.post('/reminders/scan/bill')
export const scanCardRemindersApi = () => request.post('/reminders/scan/card')
export const batchMarkHandledApi = (ids: number[]) => request.patch('/reminders/batch/handle', ids)
export const batchMarkIgnoredApi = (ids: number[]) => request.patch('/reminders/batch/ignore', ids)
