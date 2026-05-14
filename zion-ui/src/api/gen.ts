import { request } from '@/utils/request'

// 数据库表信息
export interface DatabaseTable {
  tableName: string
  tableComment: string
  createTime: string
  updateTime: string
}

// 代码生成表
export interface GenTable {
  id?: number
  tableName: string
  tableComment: string
  className: string
  packageName: string
  moduleName: string
  businessName: string
  functionName: string
  author: string
  genType: string
  genPath?: string
  frontType: string
  formLayout?: string
  parentMenuId?: number
  remark?: string
  createTime?: string
  updateTime?: string
  columns?: GenTableColumn[]
}

// 表字段信息
export interface GenTableColumn {
  id?: number
  tableId?: number
  columnName: string
  columnComment: string
  columnType: string
  javaType: string
  javaField: string
  isPk: number
  isIncrement: number
  isRequired: number
  isInsert: number
  isEdit: number
  isList: number
  isQuery: number
  queryType: string
  htmlType: string
  dictType?: string
  sort: number
}

// 代码生成 API
export const genApi = {
  // 分页查询数据库表列表
  dbTableList(params: { page: number; pageSize: number; tableName?: string }) {
    return request({ url: '/tool/gen/db/list', method: 'get', params })
  },

  // 导入表
  importTable(tableNames: string[]): Promise<void> {
    return request({ url: '/tool/gen/import', method: 'post', data: tableNames })
  },

  // 分页查询已导入的表
  page(params: { page: number; pageSize: number; tableName?: string }) {
    return request({ url: '/tool/gen/page', method: 'get', params })
  },

  // 获取表详情
  getTable(id: number): Promise<GenTable> {
    return request({ url: `/tool/gen/${id}`, method: 'get' })
  },

  // 修改表配置
  updateTable(data: GenTable): Promise<void> {
    return request({ url: '/tool/gen', method: 'put', data })
  },

  // 删除表
  deleteTable(ids: number[]): Promise<void> {
    return request({ url: `/tool/gen/${ids.join(',')}`, method: 'delete' })
  },

  // 预览代码
  previewCode(id: number): Promise<Record<string, string>> {
    return request({ url: `/tool/gen/preview/${id}`, method: 'get' })
  },

  // 下载代码
  downloadCode(ids: number[]): string {
    return `/api/tool/gen/download?ids=${ids.join(',')}`
  },

  // 预览将要生成的文件
  previewGenerateFiles(id: number): Promise<string[]> {
    return request({ url: `/tool/gen/preview-generate/${id}`, method: 'get' })
  },

  // 生成代码到项目
  generateToProject(id: number): Promise<string[]> {
    return request({ url: `/tool/gen/generate/${id}`, method: 'post' })
  },

  // 预览将要移除的文件
  previewRemoveFiles(id: number): Promise<string[]> {
    return request({ url: `/tool/gen/preview-remove/${id}`, method: 'get' })
  },

  // 移除已生成的代码
  removeGeneratedCode(id: number): Promise<string[]> {
    return request({ url: `/tool/gen/remove-code/${id}`, method: 'delete' })
  },

  // 同步表结构
  syncTable(id: number): Promise<void> {
    return request({ url: `/tool/gen/sync/${id}`, method: 'post' })
  }
}
