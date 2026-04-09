import request from '@/utils/request'

export const loginApi = (data: { username: string; password: string }) =>
  request.post('/auth/login', data)

export const logoutApi = () => request.post('/auth/logout')
