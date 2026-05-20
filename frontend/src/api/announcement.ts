import request from '@/utils/request'

export interface Announcement {
  id: number
  content: string
  status: number
  statusDesc?: string
  pinned: number
  sortOrder: number
  publishTime?: string
  createTime?: string
  updateTime?: string
  createBy?: string
  updateBy?: string
  readTime?: string
  popupDate?: string
  silentDate?: string
  read?: boolean
  shouldPopup?: boolean
}

export interface AnnouncementSavePayload {
  content: string
  pinned?: number
  sortOrder?: number
}

export const getLatestAnnouncementApi = () => request.get('/announcements/latest')
export const getAnnouncementHistoryApi = (params: any) => request.get('/announcements/history', { params })
export const getAnnouncementUnreadCountApi = () => request.get('/announcements/unread-count')
export const markAnnouncementReadApi = (id: number) => request.post(`/announcements/${id}/read`)
export const markAnnouncementPopupShownApi = (id: number) => request.post(`/announcements/${id}/popup-shown`)
export const silentAnnouncementTodayApi = (id: number) => request.post(`/announcements/${id}/silent-today`)

export const getAnnouncementAdminPageApi = (params: any) => request.get('/announcements/admin/page', { params })
export const createAnnouncementApi = (data: AnnouncementSavePayload) => request.post('/announcements/admin', data)
export const updateAnnouncementApi = (id: number, data: AnnouncementSavePayload) => request.put(`/announcements/admin/${id}`, data)
export const publishAnnouncementApi = (id: number) => request.patch(`/announcements/admin/${id}/publish`)
export const offlineAnnouncementApi = (id: number) => request.patch(`/announcements/admin/${id}/offline`)
export const deleteAnnouncementApi = (id: number) => request.delete(`/announcements/admin/${id}`)
