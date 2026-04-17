import request from '@/utils/request'

// ==================== 类型定义 ====================
export interface FeedbackQuery {
  pageNum?: number
  pageSize?: number
  titleKeyword?: string
  status?: number | null
  priority?: number | null
  feedbackType?: number | null
  submitter?: string
  startTime?: string
  endTime?: string
}

export interface FeedbackCreateForm {
  title: string
  content: string
  feedbackType: number
  priority: number
}

export interface FeedbackUpdateForm extends FeedbackCreateForm {
  status: number
  submitter: string
  assignee?: string
  latestRemark?: string
  resolvedTime?: string | null
  closedTime?: string | null
  deleteAttachmentIds?: number[]
}

export interface FeedbackStatusUpdate {
  status: number
  remark: string
}

export interface FeedbackRemark {
  remark: string
}

export interface FeedbackAttachment {
  id: number
  fileName: string
  fileUrl: string
  fileSize: number
  fileSuffix: string
  contentType: string
  sortNo: number
}

export interface FeedbackProcessLog {
  id: number
  actionType: string
  actionDesc: string
  fromStatus: number | null
  fromStatusDesc: string
  toStatus: number | null
  toStatusDesc: string
  remark: string
  operator: string
  operateTime: string
}

export interface FeedbackVO {
  id: number
  feedbackNo: string
  title: string
  content: string
  feedbackType: number
  feedbackTypeDesc: string
  priority: number
  priorityDesc: string
  status: number
  statusDesc: string
  submitter: string
  assignee: string
  latestRemark: string
  attachmentCount: number
  resolvedTime: string | null
  closedTime: string | null
  createTime: string
  updateTime: string
  attachments?: FeedbackAttachment[]
  processLogs?: FeedbackProcessLog[]
}

export interface FeedbackStats {
  pendingCount: number
  inProgressCount: number
  resolvedCount: number
  weekNewCount: number
  totalCount: number
}

// ==================== API 方法 ====================

/** 分页查询 */
export const getFeedbackPageApi = (params: FeedbackQuery) =>
  request.get('/feedbacks/page', { params })

/** 详情 */
export const getFeedbackDetailApi = (id: number) =>
  request.get(`/feedbacks/${id}`)

/** 提交反馈（multipart/form-data） */
export const createFeedbackApi = (dto: FeedbackCreateForm, files?: File[]) => {
  const formData = new FormData()
  const blob = new Blob([JSON.stringify(dto)], { type: 'application/json' })
  formData.append('data', blob)
  if (files && files.length > 0) {
    files.forEach(f => formData.append('files', f))
  }
  return request.post('/feedbacks', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 编辑反馈（multipart/form-data） */
export const updateFeedbackApi = (id: number, dto: FeedbackUpdateForm, files?: File[]) => {
  const formData = new FormData()
  const blob = new Blob([JSON.stringify(dto)], { type: 'application/json' })
  formData.append('data', blob)
  if (files && files.length > 0) {
    files.forEach(f => formData.append('files', f))
  }
  return request.put(`/feedbacks/${id}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 修改状态 */
export const updateFeedbackStatusApi = (id: number, dto: FeedbackStatusUpdate) =>
  request.patch(`/feedbacks/${id}/status`, dto)

/** 追加备注 */
export const addFeedbackRemarkApi = (id: number, dto: FeedbackRemark) =>
  request.post(`/feedbacks/${id}/remarks`, dto)

/** 统计数据 */
export const getFeedbackStatsApi = () =>
  request.get('/feedbacks/stats')

/** 删除附件 */
export const deleteAttachmentApi = (attachmentId: number) =>
  request.delete(`/feedbacks/attachments/${attachmentId}`)

/** 下载附件（强制下载） */
export const downloadFeedbackAttachmentApi = (attachmentId: number, fileName?: string): Promise<Blob> =>
  request.get<Blob, Blob>(`/feedbacks/attachments/${attachmentId}/download`, {
    params: fileName ? { fileName } : undefined,
    responseType: 'blob'
  })

/** 预览附件（inline 模式，返回正确 MIME 类型） */
export const previewFeedbackAttachmentApi = (attachmentId: number): Promise<Blob> =>
  request.get<Blob, Blob>(`/feedbacks/attachments/${attachmentId}/download`, {
    params: { inline: true },
    responseType: 'blob'
  })

/** 逻辑删除 */
export const deleteFeedbackApi = (id: number) =>
  request.delete(`/feedbacks/${id}`)
