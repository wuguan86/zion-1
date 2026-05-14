import { request } from '@/utils/request'

// 学生表 类型定义
export interface Student {
  id?: number

  studentNo?:  string

  name?:  string

  gender?: number

  birthday?:  string

  phone?:  string

  email?:  string

  address?:  string

  classId?: number

  status?: number

  deleted?: number

  createTime?:  string

  updateTime?:  string

}

// 学生表 API
export const studentApi = {
  // 分页查询
  page(params: { page: number; pageSize: number; id?: number; name?:  string; status?: number }) {
    return request({ url: '/system/student/page', method: 'get', params })
  },

  // 获取详情
  detail(id: number) {
    return request({ url: `/system/student/${id}`, method: 'get' })
  },

  // 新增
  create(data: Student) {
    return request({ url: '/system/student', method: 'post', data })
  },

  // 修改
  update(data: Student) {
    return request({ url: '/system/student', method: 'put', data })
  },

  // 删除
  delete(ids: number[]) {
    return request({ url: `/system/student/${ids.join(',')}`, method: 'delete' })
  },

  // 导出
  export(params?: { ids?: number[]; id?: number; name?:  string; status?: number }) {
    const p: Record<string, any> = {}
    if (params?.ids?.length) p.ids = params.ids.join(',')
    if (params?.id !== undefined && params?.id !== null) p.id = params.id
    if (params?.name !== undefined && params?.name !== null) p.name = params.name
    if (params?.status !== undefined && params?.status !== null) p.status = params.status
    return request({ url: `/system/student/export`, method: 'get', params: p, responseType: 'blob' })
  },

  // 导入
  importData(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<{ success: number; fail: number; errors: string[] }>({
      url: `/system/student/import`,
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 下载导入模板
  downloadTemplate() {
    return request({ url: `/system/student/template`, method: 'get', responseType: 'blob' })
  }
}
