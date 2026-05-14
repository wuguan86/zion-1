<template>
  <div class="page-container">
    <n-card>
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="客户ID">
            <n-input v-model:value="searchForm.id" placeholder="请输入客户ID" clearable />
          </n-form-item>
          <n-form-item label="客户姓名">
            <n-input v-model:value="searchForm.name" placeholder="请输入客户姓名" clearable />
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
          <n-button type="primary" @click="handleAdd">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增
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
        :pagination="pagination"
        :row-key="(row) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
        @update:checked-row-keys="handleCheck"
      />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" style="width: 600px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item label="客户姓名" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入客户姓名" />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </n-form-item>
        <n-form-item label="身份证号" path="idCard">
          <n-input v-model:value="formData.idCard" placeholder="请输入身份证号" />
        </n-form-item>
        <n-form-item label="地址" path="address">
          <n-input v-model:value="formData.address" placeholder="请输入地址" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NSpace, NIcon, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, TrashOutline, CreateOutline } from '@vicons/ionicons5'
import { customerApi, type Customer } from '@/api/customer'

const message = useMessage()
const dialog = useDialog()

// 搜索表单
const searchForm = reactive({
  id:  null as number | null,
  name: '',
})

// 表格数据
const tableData = ref<Customer[]>([])
const loading = ref(false)
const selectedIds = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const formData = reactive<Customer>({
  name: '',
  phone: '',
  idCard: '',
  address: '',
})

// 表单校验规则
const formRules = {
  name: { required: true, message: '请输入客户姓名', trigger: 'blur' },
  phone: { required: true, message: '请输入手机号', trigger: 'blur' },
}

// 表格列
const columns: DataTableColumns<Customer> = [
  { type: 'selection' },
  { title: '客户ID', key: 'id' },
  { title: '客户姓名', key: 'name' },
  { title: '手机号', key: 'phone' },
  { title: '身份证号', key: 'idCard' },
  { title: '地址', key: 'address' },
  { title: '备注', key: 'remark' },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '更新时间', key: 'updateTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) }, {
            default: () => [h(NIcon, null, { default: () => h(CreateOutline) }), ' 编辑']
          }),
          h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, {
            default: () => [h(NIcon, null, { default: () => h(TrashOutline) }), ' 删除']
          })
        ]
      })
    }
  }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await customerApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      id: searchForm.id || undefined,
      name: searchForm.name || undefined,
    })
    tableData.value = res.list
    pagination.itemCount = res.total
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
  searchForm.id =  null

  searchForm.name = ''

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

// 新增
function handleAdd() {
  modalTitle.value = '新增客户表'
  Object.keys(formData).forEach(key => {
    (formData as any)[key] = undefined
  })
  modalVisible.value = true
}

// 编辑
function handleEdit(row: Customer) {
  modalTitle.value = '编辑客户表'
  Object.assign(formData, row)
  modalVisible.value = true
}

// 提交
async function handleSubmit() {
  await formRef.value?.validate()
  try {
    if (formData.id) {
      await customerApi.update(formData)
      message.success('修改成功')
    } else {
      await customerApi.create(formData)
      message.success('新增成功')
    }
    modalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 删除
function handleDelete(row: Customer) {
  dialog.warning({
    title: '提示',
    content: '确定要删除该记录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await customerApi.delete([row.id!])
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
    content: `确定要删除选中的 ${selectedIds.value.length} 条记录吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await customerApi.delete(selectedIds.value)
        message.success('删除成功')
        selectedIds.value = []
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
</style>
