import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = userStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

let isLoggingOut = false

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (res.code !== 200) {
      const isLogoutRequest = response.config.url?.includes('/auth/logout')

      if (res.code === 401 && !isLoggingOut && !isLogoutRequest) {
        isLoggingOut = true
        window.$message?.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        isLoggingOut = false
        return Promise.reject(new Error('登录已过期'))
      }

      if (!isLogoutRequest) {
        window.$message?.error(res.message || '请求失败')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    window.$message?.error(message)
    return Promise.reject(error)
  }
)

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as Promise<T>
}

export default service
