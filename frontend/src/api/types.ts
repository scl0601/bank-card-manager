export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  current: number
  size: number
}

export type ApiPageResult<T = unknown> = ApiResult<PageResult<T>>
