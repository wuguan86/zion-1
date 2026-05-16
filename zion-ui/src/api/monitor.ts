import { request } from '@/utils/request'
import { PageResult } from './system'

// ==================== 操作日志 ====================
export interface SysOperLog {
  id?: number
  title: string
  businessType: number
  method: string
  requestMethod: string
  operName: string
  operUrl: string
  operIp: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string
  operTime: string
  costTime: number
}

export const operLogApi = {
  page(params: { page: number; pageSize: number; title?: string; operName?: string; status?: number }): Promise<PageResult<SysOperLog>> {
    return request({ url: '/monitor/operlog/page', method: 'get', params })
  },
  delete(id: number): Promise<void> {
    return request({ url: `/monitor/operlog/${id}`, method: 'delete' })
  },
  clean(): Promise<void> {
    return request({ url: '/monitor/operlog/clean', method: 'delete' })
  }
}

// ==================== 登录日志 ====================
export interface SysLoginLog {
  id?: number
  username: string
  ipaddr: string
  loginLocation: string
  browser: string
  os: string
  status: number
  msg: string
  loginTime: string
}

export const loginLogApi = {
  page(params: { page: number; pageSize: number; username?: string; status?: number }): Promise<PageResult<SysLoginLog>> {
    return request({ url: '/monitor/loginlog/page', method: 'get', params })
  },
  delete(id: number): Promise<void> {
    return request({ url: `/monitor/loginlog/${id}`, method: 'delete' })
  },
  clean(): Promise<void> {
    return request({ url: '/monitor/loginlog/clean', method: 'delete' })
  }
}
