<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="表名">
            <n-input v-model:value="searchForm.tableName" placeholder="请输入表名" clearable />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                搜索
              </n-button>
              <n-button @click="handleReset">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                重置
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <n-space>
          <n-button type="primary" @click="handleOpenImport">
            <template #icon><n-icon><CloudDownloadOutline /></n-icon></template>
            导入表
          </n-button>
          <n-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchGenerate">
            <template #icon><n-icon><CodeSlashOutline /></n-icon></template>
            生成代码
          </n-button>
          <n-button type="error" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            <template #icon><n-icon><TrashOutline /></n-icon></template>
            删除
          </n-button>
        </n-space>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: GenTable) => row.id"
        remote
        @update:checked-row-keys="handleCheck"
      />

      <div class="pagination-container" style="display: flex; justify-content: flex-end; margin-top: 12px">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        >
          <template #prefix>
            共 {{ pagination.itemCount }} 条
          </template>
        </n-pagination>
      </div>
    </n-card>

    <!-- 导入表弹窗 -->
    <n-modal v-model:show="showImportModal" preset="card" title="导入表" style="width: 900px">
      <!-- 搜索 -->
      <div class="import-search">
        <n-form inline>
          <n-form-item label="表名">
            <n-input v-model:value="importSearchForm.tableName" placeholder="请输入表名" clearable @keyup.enter="handleImportSearch" />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleImportSearch">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                搜索
              </n-button>
              <n-button @click="handleImportReset">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                重置
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>
      <n-spin :show="importLoading">
        <n-data-table
          :columns="importColumns"
          :data="dbTables"
          :row-key="(row: DatabaseTable) => row.tableName"
          max-height="400px"
          remote
          @update:checked-row-keys="handleImportCheck"
        />
        <div class="pagination-container" style="display: flex; justify-content: flex-end; margin-top: 12px">
          <n-pagination
            v-model:page="importPagination.page"
            v-model:page-size="importPagination.pageSize"
            :item-count="importPagination.itemCount"
            :page-sizes="[10, 20, 50, 100]"
            show-size-picker
            show-quick-jumper
            @update:page="handleImportPageChange"
            @update:page-size="handleImportPageSizeChange"
          >
            <template #prefix>
              共 {{ importPagination.itemCount }} 条
            </template>
          </n-pagination>
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showImportModal = false">取消</n-button>
          <n-button type="primary" :disabled="selectedTableNames.length === 0" @click="handleImport">
            导入 ({{ selectedTableNames.length }})
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 编辑配置弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" title="编辑生成配置" style="width: 1100px">
      <n-tabs type="line">
        <n-tab-pane name="basic" tab="基本信息">
          <n-form ref="editFormRef" :model="editForm" label-placement="left" label-width="100px">
            <n-grid :cols="2" :x-gap="24">
              <n-form-item-gi label="表名">
                <n-input v-model:value="editForm.tableName" disabled />
              </n-form-item-gi>
              <n-form-item-gi label="表描述">
                <n-input v-model:value="editForm.tableComment" />
              </n-form-item-gi>
              <n-form-item-gi label="实体类名">
                <n-input v-model:value="editForm.className" />
              </n-form-item-gi>
              <n-form-item-gi label="包路径">
                <n-input v-model:value="editForm.packageName" />
              </n-form-item-gi>
              <n-form-item-gi label="模块名">
                <n-input v-model:value="editForm.moduleName" />
              </n-form-item-gi>
              <n-form-item-gi label="业务名">
                <n-input v-model:value="editForm.businessName" />
              </n-form-item-gi>
              <n-form-item-gi label="功能名称">
                <n-input v-model:value="editForm.functionName" />
              </n-form-item-gi>
              <n-form-item-gi label="作者">
                <n-input v-model:value="editForm.author" />
              </n-form-item-gi>
            </n-grid>
          </n-form>
        </n-tab-pane>
        <n-tab-pane name="columns" tab="字段配置">
          <n-data-table :columns="columnEditColumns" :data="editForm.columns" :max-height="400" />
        </n-tab-pane>
        <n-tab-pane name="layout" tab="布局配置">
          <n-form ref="editFormRef" :model="editForm" label-placement="left" label-width="120px">
            <n-form-item label="表单布局">
              <n-radio-group v-model:value="editForm.formLayout">
                <n-space>
                  <n-radio value="vertical">从上到下（单列）</n-radio>
                  <n-radio value="grid">一行两列</n-radio>
                </n-space>
              </n-radio-group>
            </n-form-item>
            <n-alert type="info" style="margin-top: 12px">
              表单布局将影响新增/编辑弹窗中表单项的排列方式。选择「一行两列」时，表单项将并排显示；选择「从上到下」时，表单项将垂直排列。
            </n-alert>
          </n-form>
        </n-tab-pane>
      </n-tabs>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveEdit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 预览代码弹窗 -->
    <n-modal v-model:show="showPreviewModal" preset="card" title="预览代码" style="width: 90vw; height: 90vh">
      <div class="preview-container">
        <n-tabs type="card" v-model:value="previewTab">
          <n-tab-pane v-for="(code, name) in previewCodes" :key="name" :name="name" :tab="name">
            <n-code :code="code" :language="getLanguage(name)" show-line-numbers class="preview-code" />
          </n-tab-pane>
        </n-tabs>
      </div>
      <template #footer>
        <n-space justify="end">

          <n-button @click="showPreviewModal = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 预览效果弹窗 - 渲染 Vue 页面查看样式布局 -->
    <n-modal
      v-model:show="showPreviewEffectModal"
      preset="card"
      title="预览效果"
      style="width: 90vw; height: 90vh"
      :mask-closable="false"
    >
      <div class="preview-effect-container">
        <iframe
          :src="previewEffectPath"
          class="preview-effect-iframe"
          frameborder="0"
        />
      </div>
      <template #footer>
        <n-button @click="showPreviewEffectModal = false">关闭</n-button>
      </template>
    </n-modal>

    <!-- 生成方式选择弹窗 -->
    <n-modal v-model:show="showGenerateModal" preset="card" title="生成代码" style="width: 500px">
      <div class="generate-options">
        <n-alert type="info" style="margin-bottom: 16px">
          请选择生成方式
        </n-alert>
        <n-space vertical size="large">
          <n-card
            hoverable
            :class="['generate-option', { 'generate-option-selected': generateType === 'project' }]"
            @click="generateType = 'project'"
          >
            <template #header>
              <n-space align="center">
                <n-icon size="24" color="#18a058"><CodeSlashOutline /></n-icon>
                <span>生成到项目</span>
              </n-space>
            </template>
            <n-text depth="3">
              直接将代码生成到当前项目的对应目录中，包括：
              <ul style="margin: 8px 0 0 16px; padding: 0;">
                <li>Entity、Mapper、Service、Controller (后端)</li>
                <li>API、页面组件 (前端)</li>
                <li>自动创建菜单数据</li>
              </ul>
            </n-text>
          </n-card>
          <n-card
            hoverable
            :class="['generate-option', { 'generate-option-selected': generateType === 'download' }]"
            @click="generateType = 'download'"
          >
            <template #header>
              <n-space align="center">
                <n-icon size="24" color="#2080f0"><CloudDownloadOutline /></n-icon>
                <span>下载代码包</span>
              </n-space>
            </template>
            <n-text depth="3">
              将生成的代码打包成 ZIP 文件下载，您可以手动复制到项目中
            </n-text>
          </n-card>
        </n-space>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showGenerateModal = false">取消</n-button>
          <n-button type="primary" :loading="generateLoading" :disabled="!generateType" @click="handleConfirmGenerate">下一步</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 预览文件弹窗 -->
    <n-modal v-model:show="showPreviewFilesModal" preset="card" :title="previewAction === 'generate' ? '确认生成' : '确认移除'" style="width: 600px">
      <n-alert :type="previewAction === 'generate' ? 'info' : 'warning'" style="margin-bottom: 16px">
        {{ previewAction === 'generate' ? '以下文件将被生成到项目中：' : '以下文件将被删除：' }}
      </n-alert>
      <n-card title="文件列表" size="small">
        <n-scrollbar style="max-height: 300px">
          <n-spin :show="previewLoading">
            <n-list v-if="previewFiles.length > 0">
              <n-list-item v-for="file in previewFiles" :key="file">
                <n-text :code="!file.startsWith('[')">{{ file }}</n-text>
              </n-list-item>
            </n-list>
            <n-empty v-else description="没有找到文件" />
          </n-spin>
        </n-scrollbar>
      </n-card>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPreviewFilesModal = false">取消</n-button>
          <n-button
            :type="previewAction === 'generate' ? 'primary' : 'error'"
            :loading="generateLoading"
            @click="handleExecuteAction"
          >
            {{ previewAction === 'generate' ? '确认生成' : '确认删除' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 生成/移除结果弹窗 -->
    <n-modal v-model:show="showResultModal" preset="card" :title="resultType === 'generate' ? '生成完成' : '移除完成'" style="width: 600px">
      <n-alert :type="resultType === 'generate' ? 'success' : 'warning'" style="margin-bottom: 16px">
        {{ resultType === 'generate' ? '代码已成功生成到项目中' : '代码已从项目中移除' }}，共 {{ generatedFiles.length }} 个文件
      </n-alert>
      <n-card :title="resultType === 'generate' ? '生成的文件列表' : '已移除的文件列表'" size="small">
        <n-scrollbar style="max-height: 300px">
          <n-list v-if="generatedFiles.length > 0">
            <n-list-item v-for="file in generatedFiles" :key="file">
              <n-text code>{{ file }}</n-text>
            </n-list-item>
          </n-list>
          <n-empty v-else description="没有找到相关文件" />
        </n-scrollbar>
      </n-card>
      <n-alert v-if="resultType === 'generate'" type="info" style="margin-top: 16px">
        <div>提示：</div>
        <ul style="margin: 4px 0 0 16px; padding: 0;">
          <li>请重启后端服务以加载新生成的 Java 代码</li>
          <li>菜单数据已自动创建，无需手动执行 SQL</li>
          <li>路由会自动根据菜单配置动态加载，无需手动配置</li>
        </ul>
      </n-alert>
      <n-alert v-else type="info" style="margin-top: 16px">
        提示：请重启后端服务以使更改生效，菜单数据已自动删除
      </n-alert>
      <template #footer>
        <n-space justify="end">
          <n-button type="primary" @click="showResultModal = false">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NSpace, NIcon, NTag, NSwitch, NSelect, NInput, NText, NList, NListItem, NScrollbar, NAlert, NEmpty, NSpin, NPagination, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { SearchOutline, RefreshOutline, CloudDownloadOutline, CodeSlashOutline, TrashOutline, SettingsOutline, EyeOutline, SyncOutline, CloseCircleOutline, ExpandOutline } from '@vicons/ionicons5'
import { genApi, type GenTable, type GenTableColumn, type DatabaseTable } from '@/api/gen'
import { dictTypeApi } from '@/api/org'
import { formatDateTime } from '@/utils/datetime'

const message = useMessage()
const dialog = useDialog()

// 搜索表单
const searchForm = reactive({
  tableName: ''
})

// 表格数据
const tableData = ref<GenTable[]>([])
const loading = ref(false)
const selectedIds = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 导入表
const showImportModal = ref(false)
const importLoading = ref(false)
const dbTables = ref<DatabaseTable[]>([])
const selectedTableNames = ref<string[]>([])
const importSearchForm = reactive({
  tableName: ''
})
const importPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 编辑配置
const showEditModal = ref(false)
const editFormRef = ref()
const dictTypeOptions = ref<{ label: string; value: string }[]>([])
const editForm = reactive<GenTable>({
  id: undefined,
  tableName: '',
  tableComment: '',
  className: '',
  packageName: '',
  moduleName: '',
  businessName: '',
  functionName: '',
  author: '',
  genType: 'crud',
  frontType: 'naive-ui',
  formLayout: 'vertical',
  columns: []
})

// 预览代码
const showPreviewModal = ref(false)
const previewTab = ref('')
const previewCodes = ref<Record<string, string>>({})
const currentPreviewTable = ref<GenTable | null>(null)

// 预览效果弹窗（渲染 Vue 页面查看样式布局）
const showPreviewEffectModal = ref(false)
const previewEffectPath = ref('')

// 生成代码
const showGenerateModal = ref(false)
const currentGenerateId = ref<number | null>(null)
const showResultModal = ref(false)
const generatedFiles = ref<string[]>([])
const generateLoading = ref(false)
const resultType = ref<'generate' | 'remove'>('generate')
const generateType = ref<'project' | 'download' | ''>('')

// 预览文件
const showPreviewFilesModal = ref(false)
const previewFiles = ref<string[]>([])
const previewLoading = ref(false)
const previewAction = ref<'generate' | 'remove'>('generate')

// 表格列
const columns: DataTableColumns<GenTable> = [
  { type: 'selection' },
  { title: '表名', key: 'tableName', width: 200 },
  { title: '表描述', key: 'tableComment', ellipsis: { tooltip: true } },
  { title: '实体类', key: 'className', width: 200 },
  { title: '创建时间', key: 'createTime', width: 180, render(row) {
    return formatDateTime(row.createTime)
  }},
  {
    title: '操作',
    key: 'actions',
    width: 500,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', quaternary: true, onClick: () => handlePreview(row) }, {
            default: () => [h(NIcon, null, { default: () => h(EyeOutline) }), ' 预览']
          }),
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) }, {
            default: () => [h(NIcon, null, { default: () => h(SettingsOutline) }), ' 配置']
          }),
          h(NButton, { size: 'small', quaternary: true, type: 'success', onClick: () => handleGenerate(row) }, {
            default: () => [h(NIcon, null, { default: () => h(CodeSlashOutline) }), ' 生成']
          }),
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleSync(row) }, {
            default: () => [h(NIcon, null, { default: () => h(SyncOutline) }), ' 同步']
          }),
          h(NButton, { size: 'small', quaternary: true, type: 'warning', onClick: () => handleRemoveCodeDirect(row) }, {
            default: () => [h(NIcon, null, { default: () => h(CloseCircleOutline) }), ' 移除']
          }),
          h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, {
            default: () => [h(NIcon, null, { default: () => h(TrashOutline) }),'删除']
          })
        ]
      })
    }
  }
]

// 导入表列
const importColumns: DataTableColumns<DatabaseTable> = [
  { type: 'selection' },
  { title: '表名', key: 'tableName' },
  { title: '表描述', key: 'tableComment' },
  { title: '创建时间', key: 'createTime', render(row) {
    return formatDateTime(row.createTime)
  }}
]

// 字段编辑列
const columnEditColumns: DataTableColumns<GenTableColumn> = [
  { title: '字段名', key: 'columnName', width: 120 },
  { title: '字段描述', key: 'columnComment', width: 120,
    render(row) {
      return h(NInput, { value: row.columnComment, size: 'small', onUpdateValue: (v: string) => { row.columnComment = v } })
    }
  },
  { title: 'Java类型', key: 'javaType', width: 100,
    render(row) {
      return h(NSelect, {
        value: row.javaType, size: 'small', style: { width: '100px' },
        options: [
          { label: 'Long', value: 'Long' },
          { label: 'Integer', value: 'Integer' },
          { label: 'String', value: 'String' },
          { label: 'Double', value: 'Double' },
          { label: 'BigDecimal', value: 'BigDecimal' },
          { label: 'LocalDateTime', value: 'LocalDateTime' },
          { label: 'LocalDate', value: 'LocalDate' },
          { label: 'Boolean', value: 'Boolean' }
        ],
        onUpdateValue: (v: string) => { row.javaType = v }
      })
    }
  },
  { title: 'Java字段', key: 'javaField', width: 120 },
  { title: '插入', key: 'isInsert', width: 60,
    render(row) {
      return h(NSwitch, { value: row.isInsert === 1, size: 'small', onUpdateValue: (v: boolean) => { row.isInsert = v ? 1 : 0 } })
    }
  },
  { title: '编辑', key: 'isEdit', width: 60,
    render(row) {
      return h(NSwitch, { value: row.isEdit === 1, size: 'small', onUpdateValue: (v: boolean) => { row.isEdit = v ? 1 : 0 } })
    }
  },
  { title: '列表', key: 'isList', width: 60,
    render(row) {
      return h(NSwitch, { value: row.isList === 1, size: 'small', onUpdateValue: (v: boolean) => { row.isList = v ? 1 : 0 } })
    }
  },
  { title: '查询', key: 'isQuery', width: 60,
    render(row) {
      return h(NSwitch, { value: row.isQuery === 1, size: 'small', onUpdateValue: (v: boolean) => { row.isQuery = v ? 1 : 0 } })
    }
  },
  { title: '查询方式', key: 'queryType', width: 100,
    render(row) {
      return h(NSelect, {
        value: row.queryType, size: 'small', style: { width: '90px' },
        options: [
          { label: '=', value: 'EQ' },
          { label: '!=', value: 'NE' },
          { label: '>', value: 'GT' },
          { label: '<', value: 'LT' },
          { label: 'LIKE', value: 'LIKE' },
          { label: 'BETWEEN', value: 'BETWEEN' }
        ],
        onUpdateValue: (v: string) => { row.queryType = v }
      })
    }
  },
  { title: '显示类型', key: 'htmlType', width: 120,
    render(row) {
      return h(NSelect, {
        value: row.htmlType, size: 'small', style: { width: '110px' },
        options: [
          { label: '文本框', value: 'input' },
          { label: '文本域', value: 'textarea' },
          { label: '下拉框', value: 'select' },
          { label: '单选框', value: 'radio' },
          { label: '复选框', value: 'checkbox' },
          { label: '日期控件', value: 'datetime' },
          { label: '图片上传', value: 'imageUpload' },
          { label: '文件上传', value: 'fileUpload' },
          { label: '富文本', value: 'editor' }
        ],
        onUpdateValue: (v: string) => {
          row.htmlType = v
          if (!['select', 'radio', 'checkbox'].includes(v)) row.dictType = ''
        }
      })
    }
  },
  { title: '字典类型', key: 'dictType', width: 150,
    render(row) {
      const isDictField = ['select', 'radio', 'checkbox'].includes(row.htmlType || '')
      if (!isDictField) return '-'
      return h(NSelect, {
        value: row.dictType || null,
        size: 'small',
        style: { width: '140px' },
        options: dictTypeOptions.value,
        clearable: true,
        placeholder: '选择字典',
        onUpdateValue: (v: string | null) => { row.dictType = v || '' }
      })
    }
  }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await genApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      tableName: searchForm.tableName || undefined
    })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
  } finally {
    loading.value = false
  }
}

// 加载数据库表
async function loadDbTables() {
  importLoading.value = true
  try {
    const res = await genApi.dbTableList({
      page: importPagination.page,
      pageSize: importPagination.pageSize,
      tableName: importSearchForm.tableName || undefined
    })
    dbTables.value = res.list
    importPagination.itemCount = Number(res.total)
  } finally {
    importLoading.value = false
  }
}

// 导入表搜索
function handleImportSearch() {
  importPagination.page = 1
  loadDbTables()
}

// 导入表重置
function handleImportReset() {
  importSearchForm.tableName = ''
  importPagination.page = 1
  loadDbTables()
}

// 导入表分页
function handleImportPageChange(page: number) {
  importPagination.page = page
  loadDbTables()
}

function handleImportPageSizeChange(pageSize: number) {
  importPagination.pageSize = pageSize
  importPagination.page = 1
  loadDbTables()
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.tableName = ''
  handleSearch()
}

// 分页
function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

// 选择
function handleCheck(keys: Array<string | number>) {
  selectedIds.value = keys as number[]
}

function handleImportCheck(keys: Array<string | number>) {
  selectedTableNames.value = keys as string[]
}

// 打开导入弹窗
function handleOpenImport() {
  importSearchForm.tableName = ''
  importPagination.page = 1
  selectedTableNames.value = []
  showImportModal.value = true
  loadDbTables()
}

// 导入表
async function handleImport() {
  try {
    await genApi.importTable(selectedTableNames.value)
    message.success('导入成功')
    showImportModal.value = false
    selectedTableNames.value = []
    loadData()
    loadDbTables() // 重新加载可导入的表列表
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 编辑配置
async function handleEdit(row: GenTable) {
  try {
    const [table, dictTypes] = await Promise.all([
      genApi.getTable(row.id!),
      dictTypeApi.list()
    ])
    Object.assign(editForm, table)
    dictTypeOptions.value = dictTypes.map(d => ({ label: `${d.dictName} (${d.dictType})`, value: d.dictType }))
    showEditModal.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 保存配置
async function handleSaveEdit() {
  try {
    await genApi.updateTable(editForm)
    message.success('保存成功')
    showEditModal.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 预览代码
async function handlePreview(row: GenTable) {
  try {
    currentPreviewTable.value = row
    previewCodes.value = await genApi.previewCode(row.id!)
    const keys = Object.keys(previewCodes.value)
    previewTab.value = keys.length > 0 ? keys[0] : ''
    showPreviewModal.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 查看预览效果 - 在弹窗中通过 iframe 渲染页面查看样式布局
function handleViewPreview() {
  const table = currentPreviewTable.value
  if (!table?.moduleName || !table?.businessName) return
  previewEffectPath.value = `/${table.moduleName}/${table.businessName}`
  showPreviewEffectModal.value = true
}

// 生成代码 - 打开选择弹窗
function handleGenerate(row: GenTable) {
  currentGenerateId.value = row.id!
  generateType.value = ''  // 重置选择
  showGenerateModal.value = true
}

// 批量生成 - 直接下载
function handleBatchGenerate() {
  window.open(genApi.downloadCode(selectedIds.value), '_blank')
}

// 确认生成 - 下一步（预览文件）
async function handleConfirmGenerate() {
  if (!currentGenerateId.value || !generateType.value) return

  if (generateType.value === 'project') {
    // 先预览将要生成的文件
    previewLoading.value = true
    previewAction.value = 'generate'
    showGenerateModal.value = false
    showPreviewFilesModal.value = true
    try {
      previewFiles.value = await genApi.previewGenerateFiles(currentGenerateId.value)
    } catch (error) {
      // 错误已在拦截器处理
    } finally {
      previewLoading.value = false
    }
  } else {
    handleDownloadCode()
  }
}

// 执行操作（生成或移除）
async function handleExecuteAction() {
  if (!currentGenerateId.value) return

  generateLoading.value = true
  try {
    if (previewAction.value === 'generate') {
      const files = await genApi.generateToProject(currentGenerateId.value)
      generatedFiles.value = files
      resultType.value = 'generate'
    } else {
      const files = await genApi.removeGeneratedCode(currentGenerateId.value)
      generatedFiles.value = files
      resultType.value = 'remove'
    }
    showPreviewFilesModal.value = false
    showResultModal.value = true
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    generateLoading.value = false
  }
}

// 下载代码包
function handleDownloadCode() {
  if (!currentGenerateId.value) return
  window.open(genApi.downloadCode([currentGenerateId.value]), '_blank')
  showGenerateModal.value = false
}

// 移除已生成代码（从列表操作）- 先预览
async function handleRemoveCodeDirect(row: GenTable) {
  currentGenerateId.value = row.id!
  previewLoading.value = true
  previewAction.value = 'remove'
  previewFiles.value = []
  showPreviewFilesModal.value = true

  try {
    previewFiles.value = await genApi.previewRemoveFiles(row.id!)
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    previewLoading.value = false
  }
}

// 同步表结构
async function handleSync(row: GenTable) {
  dialog.warning({
    title: '提示',
    content: `确定要同步表"${row.tableName}"的结构吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await genApi.syncTable(row.id!)
        message.success('同步成功')
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 删除
function handleDelete(row: GenTable) {
  dialog.warning({
    title: '提示',
    content: `确定要删除表"${row.tableName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await genApi.deleteTable([row.id!])
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 批量删除
function handleBatchDelete() {
  dialog.warning({
    title: '提示',
    content: `确定要删除选中的 ${selectedIds.value.length} 个表吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await genApi.deleteTable(selectedIds.value)
        message.success('删除成功')
        selectedIds.value = []
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 获取代码语言
function getLanguage(filename: string): string {
  if (filename.endsWith('.java')) return 'java'
  if (filename.endsWith('.vue')) return 'vue'
  if (filename.endsWith('.ts')) return 'typescript'
  if (filename.endsWith('.sql')) return 'sql'
  return 'text'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.import-search {
  margin-bottom: 16px;
}

.preview-container {
  height: calc(90vh - 150px);
  overflow: auto;
}

.preview-code {
  font-size: 13px;
  max-height: calc(90vh - 200px);
  overflow: auto;
}

.preview-effect-container {
  height: calc(90vh - 120px);
  min-height: 400px;
}

.preview-effect-iframe {
  width: 100%;
  height: 100%;
  min-height: 400px;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
}

.generate-options {
  padding: 8px 0;
}

.generate-option {
  cursor: pointer;
  transition: all 0.3s;
}

.generate-option:hover {
  border-color: #18a058;
  box-shadow: 0 2px 12px rgba(24, 160, 88, 0.15);
}

.generate-option-selected {
  border-color: #18a058;
  background-color: rgba(24, 160, 88, 0.08);
  box-shadow: 0 2px 12px rgba(24, 160, 88, 0.2);
}
.page-layout{
  height: calc(100vh - 160px);
}
</style>
