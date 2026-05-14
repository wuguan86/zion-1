<template>
  <div class="page-container">
    <n-card>
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="配置名称">
            <n-input v-model:value="searchForm.name" placeholder="请输入配置名称" clearable />
          </n-form-item>
          <n-form-item label="存储类型">
            <n-select
              v-model:value="searchForm.storageType"
              placeholder="请选择存储类型"
              :options="storageTypeOptions"
              clearable
              style="width: 150px"
            />
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
        <n-button type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增配置
        </n-button>
      </div>
      
      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SysFileConfig) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>
    
    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      style="width: 650px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="100"
        class="modal-form"
      >
        <n-form-item label="配置名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入配置名称" />
        </n-form-item>
        <n-form-item label="存储类型" path="storageType">
          <n-select
            v-model:value="formData.storageType"
            placeholder="请选择存储类型"
            :options="storageTypeOptions"
            :disabled="!!formData.id"
          />
        </n-form-item>
        <n-form-item label="访问域名" path="domain">
          <n-input v-model:value="formData.domain" placeholder="如：http://localhost:8080 或 https://cdn.example.com" />
        </n-form-item>
        
        <!-- 本地存储配置 -->
        <template v-if="formData.storageType === 'local'">
          <n-form-item label="存储路径" path="basePath">
            <n-input v-model:value="formData.basePath" placeholder="如：D:/uploads 或 /data/uploads" />
          </n-form-item>
        </template>
        
        <!-- MinIO 配置 -->
        <template v-if="formData.storageType === 'minio'">
          <n-form-item label="Endpoint" path="endpoint">
            <n-input v-model:value="formData.endpoint" placeholder="如：http://localhost:9000" />
          </n-form-item>
          <n-form-item label="存储桶" path="bucketName">
            <n-input v-model:value="formData.bucketName" placeholder="请输入存储桶名称" />
          </n-form-item>
          <n-form-item label="AccessKey" path="accessKey">
            <n-input v-model:value="formData.accessKey" placeholder="请输入AccessKey" />
          </n-form-item>
          <n-form-item label="SecretKey" path="secretKey">
            <n-input v-model:value="formData.secretKey" type="password" show-password-on="click" placeholder="请输入SecretKey" />
          </n-form-item>
        </template>
        
        <!-- 阿里云OSS配置 -->
        <template v-if="formData.storageType === 'aliyun'">
          <n-form-item label="Endpoint" path="endpoint">
            <n-input v-model:value="formData.endpoint" placeholder="如：oss-cn-hangzhou.aliyuncs.com" />
          </n-form-item>
          <n-form-item label="存储桶" path="bucketName">
            <n-input v-model:value="formData.bucketName" placeholder="请输入存储桶名称" />
          </n-form-item>
          <n-form-item label="AccessKeyId" path="accessKey">
            <n-input v-model:value="formData.accessKey" placeholder="请输入AccessKeyId" />
          </n-form-item>
          <n-form-item label="AccessKeySecret" path="secretKey">
            <n-input v-model:value="formData.secretKey" type="password" show-password-on="click" placeholder="请输入AccessKeySecret" />
          </n-form-item>
          <n-form-item label="地域" path="region">
            <n-input v-model:value="formData.region" placeholder="如：oss-cn-hangzhou" />
          </n-form-item>
        </template>
        
        <n-form-item label="主配置" path="master">
          <n-switch v-model:value="formData.master" :checked-value="1" :unchecked-value="0">
            <template #checked>是</template>
            <template #unchecked>否</template>
          </n-switch>
          <span style="margin-left: 8px; color: #999; font-size: 12px">设为主配置后，系统将使用该配置上传文件</span>
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleTestConfig" :loading="testLoading">测试配置</n-button>
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline } from '@vicons/ionicons5'
import { fileConfigApi, type SysFileConfig } from '@/api/system'

const message = useMessage()
const dialog = useDialog()

// 搜索表单
const searchForm = reactive({
  name: '',
  storageType: null as string | null
})

// 存储类型选项
const storageTypeOptions = [
  { label: '本地存储', value: 'local' },
  { label: 'MinIO', value: 'minio' },
  { label: '阿里云OSS', value: 'aliyun' }
]

// 表格数据
const tableData = ref<SysFileConfig[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 表格列
const columns: DataTableColumns<SysFileConfig> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '配置名称', key: 'name', width: 150 },
  {
    title: '存储类型',
    key: 'storageType',
    width: 120,
    render(row) {
      const typeMap: Record<string, { text: string; type: 'default' | 'success' | 'info' }> = {
        local: { text: '本地存储', type: 'default' },
        minio: { text: 'MinIO', type: 'success' },
        aliyun: { text: '阿里云OSS', type: 'info' }
      }
      const config = typeMap[row.storageType] || { text: row.storageType, type: 'default' as const }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    title: '主配置',
    key: 'master',
    width: 80,
    render(row) {
      return h(NTag, {
        type: row.master === 1 ? 'success' : 'default',
        size: 'small'
      }, { default: () => row.master === 1 ? '是' : '否' })
    }
  },
  { title: '访问域名', key: 'domain', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
        size: 'small'
      }, { default: () => row.status === 1 ? '启用' : '禁用' })
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    fixed: 'right',
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          row.master !== 1 ? h(NButton, { size: 'small', type: 'info', onClick: () => handleSetMaster(row) }, { default: () => '设为主配置' }) : null,
          h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row), disabled: row.master === 1 }, { default: () => '删除' })
        ].filter(Boolean)
      })
    }
  }
]

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增配置')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const testLoading = ref(false)

const formData = reactive<SysFileConfig>({
  id: undefined,
  name: '',
  storageType: 'local',
  master: 0,
  domain: '',
  basePath: '',
  bucketName: '',
  accessKey: '',
  secretKey: '',
  endpoint: '',
  region: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  storageType: [{ required: true, message: '请选择存储类型', trigger: 'change' }],
  domain: [{ required: true, message: '请输入访问域名', trigger: 'blur' }]
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await fileConfigApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      storageType: searchForm.storageType || undefined
    })
    tableData.value = res.list
    pagination.itemCount = res.total
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.name = ''
  searchForm.storageType = null
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

// 新增
function handleAdd() {
  modalTitle.value = '新增配置'
  Object.assign(formData, {
    id: undefined,
    name: '',
    storageType: 'local',
    master: 0,
    domain: '',
    basePath: '',
    bucketName: '',
    accessKey: '',
    secretKey: '',
    endpoint: '',
    region: '',
    status: 1,
    remark: ''
  })
  modalVisible.value = true
}

// 编辑
async function handleEdit(row: SysFileConfig) {
  modalTitle.value = '编辑配置'
  try {
    const res = await fileConfigApi.detail(row.id!)
    Object.assign(formData, res)
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 测试配置
async function handleTestConfig() {
  testLoading.value = true
  try {
    const result = await fileConfigApi.testConfig(formData)
    if (result) {
      message.success('配置测试成功')
    } else {
      message.error('配置测试失败，请检查配置信息')
    }
  } catch (error) {
    message.error('配置测试失败')
  } finally {
    testLoading.value = false
  }
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    if (formData.id) {
      await fileConfigApi.update(formData)
      message.success('更新成功')
    } else {
      await fileConfigApi.create(formData)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

// 设为主配置
function handleSetMaster(row: SysFileConfig) {
  dialog.warning({
    title: '提示',
    content: `确定要将"${row.name}"设为主配置吗？设置后系统将使用该配置上传文件。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fileConfigApi.setMaster(row.id!)
        message.success('设置成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 删除
function handleDelete(row: SysFileConfig) {
  dialog.warning({
    title: '提示',
    content: `确定要删除配置"${row.name}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fileConfigApi.delete(row.id!)
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
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

.modal-form {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
