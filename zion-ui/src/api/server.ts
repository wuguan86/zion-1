import request from '@/utils/request'

export interface Server {
  id?: number
  name: string
  host: string
  port: number
  username: string
  authType: number // 1-密码 2-密钥
  password?: string
  privateKey?: string
  passphrase?: string
  description?: string
  status: number
  sort: number
  lastConnectTime?: string
  createTime?: string
}

export interface ServerPageParams {
  page: number
  pageSize: number
  name?: string
  status?: number
}

export const serverApi = {
  // 分页查询
  list(params: ServerPageParams) {
    return request({
      url: '/monitor/server-manager/list',
      method: 'get',
      params
    })
  },

  // 获取所有启用的服务器
  all() {
    return request<Server[]>({
      url: '/monitor/server-manager/all',
      method: 'get'
    })
  },

  // 获取详情
  getById(id: number) {
    return request<Server>({
      url: `/monitor/server-manager/${id}`,
      method: 'get'
    })
  },

  // 新增
  add(data: Server) {
    return request({
      url: '/monitor/server-manager',
      method: 'post',
      data
    })
  },

  // 修改
  update(data: Server) {
    return request({
      url: '/monitor/server-manager',
      method: 'put',
      data
    })
  },

  // 删除
  remove(id: number) {
    return request({
      url: `/monitor/server-manager/${id}`,
      method: 'delete'
    })
  },

  // 批量删除
  batchRemove(ids: number[]) {
    return request({
      url: '/monitor/server-manager/batch',
      method: 'delete',
      data: ids
    })
  },

  // 测试连接
  testConnection(id: number) {
    return request<boolean>({
      url: `/monitor/server-manager/test/${id}`,
      method: 'post'
    })
  },

  // 测试连接（通过参数）
  testConnectionByParams(data: {
    host: string
    port: number
    username: string
    authType: number
    password?: string
    privateKey?: string
    passphrase?: string
  }) {
    return request<boolean>({
      url: '/monitor/server-manager/test',
      method: 'post',
      data
    })
  }
}
