import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/modules/auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器 - 自动附加 Token
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // blob 类型直接返回（用于文件下载）
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const data = response.data
    if (data.code === 200) {
      return data
    }
    // 401 跳登录
    if (data.code === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    // 显示后端返回的具体错误信息
    ElMessage.error(data.message || '操作失败')
    return Promise.reject(new Error(data.message))
  },
  (error) => {
    // 从响应体中提取错误信息
    const errorMsg = error.response?.data?.message || error.message || '网络错误'
    
    if (error.response?.status === 401) {
      useAuthStore().logout()
      router.push('/login')
    } else {
      ElMessage.error(errorMsg)
    }
    return Promise.reject(error)
  }
)

export default request
