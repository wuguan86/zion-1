import { request } from '@/utils/request'

// 分页结果
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ==================== 首页统计 ====================
export interface DashboardStats {
  userCount: number
  roleCount: number
  menuCount: number
  permissionCount: number
}

export const dashboardApi = {
  getStats(): Promise<DashboardStats> {
    return request({ url: '/dashboard/stats', method: 'get' })
  }
}

// ==================== 用户管理 ====================
export interface SysUser {
  id?: number
  deptId?: number | null
  deptName?: string
  username: string
  password?: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  status: number
  remark?: string
  userType?: string   // 用户类型: admin-后台管理员 pc-PC前台用户 app-App/小程序用户
  openId?: string     // 微信openId
  isQuit?: number     // 是否离职(0-否 1-是)
  postNames?: string  // 岗位名称列表
  createTime?: string
}

export interface UserDetailResult {
  user: SysUser
  roleIds: number[]
  postIds: number[]
}

export const userApi = {
  page(params: { page: number; pageSize: number; username?: string; status?: number; userType?: string; deptId?: number; postId?: number }): Promise<PageResult<SysUser>> {
    return request({ url: '/sys/user/page', method: 'get', params })
  },
  
  detail(id: number): Promise<UserDetailResult> {
    return request({ url: `/sys/user/${id}`, method: 'get' })
  },
  
  create(data: { user: SysUser; roleIds: number[]; postIds: number[] }): Promise<void> {
    return request({ url: '/sys/user', method: 'post', data })
  },
  
  update(data: { user: SysUser; roleIds: number[]; postIds: number[] }): Promise<void> {
    return request({ url: '/sys/user', method: 'put', data })
  },
  
  delete(id: number): Promise<void> {
    return request({ url: `/sys/user/${id}`, method: 'delete' })
  },

  deleteBatch(ids: number[]): Promise<void> {
    return request({ url: '/sys/user/batch', method: 'delete', data: ids })
  },
  
  resetPassword(id: number): Promise<void> {
    return request({ url: `/sys/user/${id}/reset-password`, method: 'post' })
  },

  approve(id: number): Promise<void> {
    return request({ url: `/sys/user/${id}/approve`, method: 'post' })
  },

  reject(id: number): Promise<void> {
    return request({ url: `/sys/user/${id}/reject`, method: 'post' })
  },

  toggleQuit(id: number): Promise<void> {
    return request({ url: `/sys/user/${id}/quit`, method: 'post' })
  },

  // 导出用户
  exportUsers(params?: { username?: string; status?: number; userType?: string; deptId?: number; ids?: number[] }): Promise<Blob> {
    const searchParams: Record<string, any> = {}
    if (params?.username) searchParams.username = params.username
    if (params?.status !== undefined && params?.status !== null) searchParams.status = params.status
    if (params?.userType) searchParams.userType = params.userType
    if (params?.deptId) searchParams.deptId = params.deptId
    // 数组转逗号分隔字符串
    if (params?.ids && params.ids.length > 0) searchParams.ids = params.ids.join(',')
    return request({
      url: '/sys/user/export',
      method: 'get',
      params: searchParams,
      responseType: 'blob'
    })
  },

  // 导入用户
  importUsers(file: File): Promise<{ success: number; fail: number; errors: string[] }> {
    const formData = new FormData()
    formData.append('file', file)
    return request({
      url: '/sys/user/import',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 下载导入模板
  downloadTemplate(): Promise<Blob> {
    return request({
      url: '/sys/user/template',
      method: 'get',
      responseType: 'blob'
    })
  }
}

// ==================== 角色管理 ====================
export interface SysRole {
  id?: number
  name: string
  code: string
  sort: number
  status: number
  dataScope?: number  // 数据范围: 1-全部, 2-自定义, 3-本部门, 4-本部门及下级, 5-仅本人
  remark?: string
  createTime?: string
}

export interface RoleDetailResult {
  role: SysRole
  menuIds: number[]
  deptIds: number[]
}

// ==================== 岗位管理 ====================
export interface SysPost {
  id?: number
  parentId?: number
  postCode: string
  postName: string
  sort: number
  status: number
  remark?: string
  createTime?: string
  children?: SysPost[]
}

export const postApi = {
  page(params: { page: number; pageSize: number; postCode?: string; postName?: string; status?: number }): Promise<PageResult<SysPost>> {
    return request({ url: '/sys/post/page', method: 'get', params })
  },

  list(): Promise<SysPost[]> {
    return request({ url: '/sys/post/list', method: 'get' })
  },

  tree(): Promise<SysPost[]> {
    return request({ url: '/sys/post/tree', method: 'get' })
  },

  detail(id: number): Promise<SysPost> {
    return request({ url: `/sys/post/${id}`, method: 'get' })
  },

  create(data: SysPost): Promise<void> {
    return request({ url: '/sys/post', method: 'post', data })
  },

  update(data: SysPost): Promise<void> {
    return request({ url: '/sys/post', method: 'put', data })
  },

  delete(id: number): Promise<void> {
    return request({ url: `/sys/post/${id}`, method: 'delete' })
  },

  move(id: number, parentId: number): Promise<void> {
    return request({ url: `/sys/post/${id}/move`, method: 'post', params: { parentId } })
  }
}

// ==================== 角色管理 ====================
export const roleApi = {
  page(params: { page: number; pageSize: number; name?: string; status?: number }): Promise<PageResult<SysRole>> {
    return request({ url: '/sys/role/page', method: 'get', params })
  },
  
  list(): Promise<SysRole[]> {
    return request({ url: '/sys/role/list', method: 'get' })
  },
  
  detail(id: number): Promise<RoleDetailResult> {
    return request({ url: `/sys/role/${id}`, method: 'get' })
  },
  
  create(data: { role: SysRole; menuIds: number[]; deptIds?: number[] }): Promise<void> {
    return request({ url: '/sys/role', method: 'post', data })
  },
  
  update(data: { role: SysRole; menuIds: number[]; deptIds?: number[] }): Promise<void> {
    return request({ url: '/sys/role', method: 'put', data })
  },
  
  delete(id: number): Promise<void> {
    return request({ url: `/sys/role/${id}`, method: 'delete' })
  }
}

// ==================== 部门管理 ====================
export interface SysDept {
  id?: number
  parentId: number
  deptName: string
  sort: number
  leader?: string
  phone?: string
  email?: string
  status: number
  children?: SysDept[]
}

export const deptApi = {
  tree(): Promise<SysDept[]> {
    return request({ url: '/sys/dept/tree', method: 'get' })
  },
  
  list(): Promise<SysDept[]> {
    return request({ url: '/sys/dept/list', method: 'get' })
  }
}

// ==================== 菜单管理 ====================
export interface SysMenu {
  id?: number
  parentId: number
  name: string
  type: number
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort: number
  visible: number
  status: number
  isFrame?: number  // 是否外链(0-否 1-是)
  children?: SysMenu[]
}

export const menuApi = {
  tree(params?: { name?: string; status?: number }): Promise<SysMenu[]> {
    return request({ url: '/sys/menu/tree', method: 'get', params })
  },
  
  list(): Promise<SysMenu[]> {
    return request({ url: '/sys/menu/list', method: 'get' })
  },
  
  detail(id: number): Promise<SysMenu> {
    return request({ url: `/sys/menu/${id}`, method: 'get' })
  },
  
  create(data: SysMenu): Promise<void> {
    return request({ url: '/sys/menu', method: 'post', data })
  },
  
  update(data: SysMenu): Promise<void> {
    return request({ url: '/sys/menu', method: 'put', data })
  },
  
  delete(id: number): Promise<void> {
    return request({ url: `/sys/menu/${id}`, method: 'delete' })
  }
}

// ==================== 文件分组管理 ====================
export interface SysFileGroup {
  id?: number
  name: string
  sort?: number
  fileCount?: number
  createBy?: string
  createTime?: string
}

export interface FileGroupListResult {
  groups: SysFileGroup[]
  ungroupedCount: number
}

export const fileGroupApi = {
  list(): Promise<FileGroupListResult> {
    return request({ url: '/sys/file-group/list', method: 'get' })
  },

  detail(id: number): Promise<SysFileGroup> {
    return request({ url: `/sys/file-group/${id}`, method: 'get' })
  },

  create(data: SysFileGroup): Promise<void> {
    return request({ url: '/sys/file-group', method: 'post', data })
  },

  update(data: SysFileGroup): Promise<void> {
    return request({ url: '/sys/file-group', method: 'put', data })
  },

  delete(id: number): Promise<void> {
    return request({ url: `/sys/file-group/${id}`, method: 'delete' })
  }
}

// ==================== 文件管理 ====================
export interface SysFile {
  id?: number
  originalName: string
  fileName: string
  filePath: string
  url: string
  fileSize: number
  fileType: string
  fileSuffix: string
  storageType: string
  bucketName?: string
  groupId?: number
  createBy?: string
  createTime?: string
  remark?: string
}

export const fileApi = {
  page(params: { page: number; pageSize: number; originalName?: string; fileType?: string }): Promise<PageResult<SysFile>> {
    return request({ url: '/sys/file/page', method: 'get', params })
  },

  pageByGroup(params: { 
    page: number
    pageSize: number
    groupId?: number | null
    fileCategory?: string
    originalName?: string 
  }): Promise<PageResult<SysFile>> {
    return request({ url: '/sys/file/page-by-group', method: 'get', params })
  },
  
  detail(id: number): Promise<SysFile> {
    return request({ url: `/sys/file/${id}`, method: 'get' })
  },
  
  upload(file: File, path?: string, groupId?: number | null): Promise<SysFile> {
    const formData = new FormData()
    formData.append('file', file)
    if (path) {
      formData.append('path', path)
    }
    if (groupId !== undefined && groupId !== null && groupId > 0) {
      formData.append('groupId', groupId.toString())
    }
    return request({
      url: '/sys/file/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  uploadImage(file: File): Promise<SysFile> {
    const formData = new FormData()
    formData.append('file', file)
    return request({
      url: '/sys/file/upload/image',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  getDownloadUrl(id: number): string {
    return `/api/sys/file/download/${id}`
  },
  
  getPreviewUrl(id: number): string {
    return `/api/sys/file/preview/${id}`
  },

  getTextContent(id: number): Promise<string> {
    return request({ url: `/sys/file/text/${id}`, method: 'get' })
  },
  
  delete(id: number): Promise<void> {
    return request({ url: `/sys/file/${id}`, method: 'delete' })
  },
  
  deleteBatch(ids: number[]): Promise<void> {
    return request({ url: '/sys/file/batch', method: 'delete', data: ids })
  },

  moveToGroup(fileIds: number[], groupId: number | null): Promise<void> {
    return request({ url: '/sys/file/move', method: 'post', data: { fileIds, groupId } })
  },

  rename(id: number, newName: string): Promise<void> {
    return request({ url: `/sys/file/${id}/rename`, method: 'put', data: { newName } })
  }
}

// ==================== 文件配置管理 ====================
export interface SysFileConfig {
  id?: number
  name: string
  storageType: string
  master: number
  domain: string
  basePath?: string
  bucketName?: string
  accessKey?: string
  secretKey?: string
  endpoint?: string
  region?: string
  status: number
  remark?: string
  createTime?: string
}

export const fileConfigApi = {
  page(params: { page: number; pageSize: number; name?: string; storageType?: string }): Promise<PageResult<SysFileConfig>> {
    return request({ url: '/sys/file-config/page', method: 'get', params })
  },
  
  list(): Promise<SysFileConfig[]> {
    return request({ url: '/sys/file-config/list', method: 'get' })
  },
  
  detail(id: number): Promise<SysFileConfig> {
    return request({ url: `/sys/file-config/${id}`, method: 'get' })
  },
  
  getMaster(): Promise<SysFileConfig> {
    return request({ url: '/sys/file-config/master', method: 'get' })
  },
  
  create(data: SysFileConfig): Promise<void> {
    return request({ url: '/sys/file-config', method: 'post', data })
  },
  
  update(data: SysFileConfig): Promise<void> {
    return request({ url: '/sys/file-config', method: 'put', data })
  },
  
  delete(id: number): Promise<void> {
    return request({ url: `/sys/file-config/${id}`, method: 'delete' })
  },
  
  setMaster(id: number): Promise<void> {
    return request({ url: `/sys/file-config/master/${id}`, method: 'put' })
  },
  
  testConfig(data: SysFileConfig): Promise<boolean> {
    return request({ url: '/sys/file-config/test', method: 'post', data })
  }
}
