import request from '@/utils/request'
import type { ApiResult } from './types'

export interface LoginResponse {
  token: string
  username: string
  nickname: string
  role: string
  dataScope?: string
}

export const loginApi = (data: { username: string; password: string }) =>
  request.post<any, ApiResult<LoginResponse>>('/auth/login', data)

export const logoutApi = () => request.post<any, ApiResult<void>>('/auth/logout')
