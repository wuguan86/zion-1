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

// ==================== 在线用户 ====================
export interface OnlineUser {
  tokenId: string
  loginName?: string
  deptName?: string
  ipaddr?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: number
  loginTime: string
  lastAccessTime: string
  tokenValue?: string
}

export const onlineApi = {
  list(): Promise<OnlineUser[]> {
    return request({ url: '/monitor/online/list', method: 'get' })
  },
  forceLogout(tokenId: string): Promise<void> {
    return request({ url: `/monitor/online/${tokenId}`, method: 'delete' })
  }
}

// ==================== 缓存监控 ====================
export interface CacheStats {
  usedMemory: number
  maxMemory: number
  ops: number
  hitRate: number
  connectedClients: number
}

export const cacheApi = {
  info(): Promise<any> {
    return request({ url: '/monitor/cache/info', method: 'get' })
  },
  stats(): Promise<CacheStats> {
    return request({ url: '/monitor/cache/stats', method: 'get' })
  },
  keys(pattern?: string): Promise<string[]> {
    return request({ url: '/monitor/cache/keys', method: 'get', params: { pattern } })
  },
  delete(key: string): Promise<void> {
    return request({ url: '/monitor/cache', method: 'delete', params: { key } })
  },
  getValue(key: string): Promise<any> {
    return request({ url: '/monitor/cache/value', method: 'get', params: { key } })
  }
}

// ==================== 服务监控 ====================
export const serverApi = {
  info(): Promise<any> {
    return request({ url: '/monitor/server/info', method: 'get' })
  }
}

// ==================== API 访问统计 ====================
export interface ApiAccessLog {
  id?: number
  startTime?: string
  endTime?: string
  apiPath?: string
  method?: string
  statusCode?: number
  success?: number
  costTime?: number
  ip?: string
  userId?: number
}

export interface ApiAccessStatistics {
  totalCount: number
  successCount: number
  failCount: number
  dailyStats: Record<string, { total: number; success: number; fail: number }>
  topPaths: Array<{ apiPath: string; count: number }>
  methodCount: Record<string, number>
}

export const apiAccessApi = {
  page(params: {
    page: number
    pageSize: number
    apiPath?: string
    method?: string
    success?: number
    startTime?: string
    endTime?: string
  }): Promise<PageResult<ApiAccessLog>> {
    return request({ url: '/monitor/api-access/page', method: 'get', params })
  },
  statistics(params?: { startDate?: string; endDate?: string }): Promise<ApiAccessStatistics> {
    return request({ url: '/monitor/api-access/statistics', method: 'get', params })
  }
}
