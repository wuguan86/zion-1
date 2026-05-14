<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="字典名称">
            <n-input v-model:value="searchForm.dictName" placeholder="请输入字典名称" clearable />
          </n-form-item>
          <n-form-item label="字典类型">
            <n-input v-model:value="searchForm.dictType" placeholder="请输入字典类型" clearable />
          </n-form-item>
          <n-form-item label="状态">
            <n-select v-model:value="searchForm.status" placeholder="请选择状态" :options="statusOptions" clearable style="width: 120px" />
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
        <n-button v-if="hasPermission('sys:dict:add')" type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增字典
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysDictType) => row.id"
        remote
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

    <!-- 字典类型弹窗 -->
    <n-modal v-model:show="modalVisible" :title="modalTitle" preset="card" style="width: 500px" :mask-closable="false">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="字典名称" path="dictName">
          <n-input v-model:value="formData.dictName" placeholder="请输入字典名称" />
        </n-form-item>
        <n-form-item label="字典类型" path="dictType">
          <n-input v-model:value="formData.dictType" placeholder="请输入字典类型" :disabled="!!formData.id" />
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
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 字典数据弹窗 -->
    <n-modal v-model:show="dataModalVisible" title="字典数据" preset="card" style="width: 900px" :mask-closable="false">
      <div class="table-toolbar">
        <n-button v-if="hasPermission('sys:dict:add')" type="primary" size="small" @click="handleAddData">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增数据
        </n-button>
      </div>
      <n-data-table :columns="dataColumns" :data="dictDataList" :loading="dataLoading" size="small" />
    </n-modal>

    <!-- 字典数据编辑弹窗 -->
    <n-modal v-model:show="dataFormVisible" :title="dataFormTitle" preset="card" style="width: 500px" :mask-closable="false">
      <n-form ref="dataFormRef" :model="dataFormData" :rules="dataRules" label-placement="left" label-width="80">
        <n-form-item label="字典标签" path="dictLabel">
          <n-input v-model:value="dataFormData.dictLabel" placeholder="请输入字典标签" />
        </n-form-item>
        <n-form-item label="字典键值" path="dictValue">
          <n-input v-model:value="dataFormData.dictValue" placeholder="请输入字典键值" />
        </n-form-item>
        <n-form-item label="显示排序" path="sort">
          <n-input-number v-model:value="dataFormData.sort" :min="0" placeholder="请输入排序" style="width: 100%" />
        </n-form-item>
        <n-form-item label="回显样式" path="listClass">
          <n-select v-model:value="dataFormData.listClass" placeholder="请选择回显样式" :options="listClassOptions" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="dataFormData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="dataFormData.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="dataFormVisible = false">取消</n-button>
          <n-button type="primary" :loading="dataSubmitLoading" @click="handleDataSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { NButton, NTag, NSpace, NPagination, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline } from '@vicons/ionicons5'
import { dictTypeApi, dictDataApi, type SysDictType, type SysDictData } from '@/api/org'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// 权限检查
const hasPermission = (permission: string) => userStore.hasPermission(permission)

const searchForm = reactive({ dictName: '', dictType: '', status: null as number | null })
const statusOptions = [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }]
const listClassOptions = [
  { label: '默认', value: 'default' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
  { label: '信息', value: 'info' }
]

const tableData = ref<SysDictType[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: true, pageSizes: [10, 20, 50] })

const columns: DataTableColumns<SysDictType> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '字典名称', key: 'dictName', width: 150 },
  { title: '字典类型', key: 'dictType', width: 150 },
  { title: '状态', key: 'status', width: 80, render(row) {
    return h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.status === 1 ? '启用' : '禁用' })
  }},
  { title: '备注', key: 'remark', width: 80, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right', render(row) {
    const buttons = [h(NButton, { size: 'small', onClick: () => handleViewData(row) }, { default: () => '字典数据' })]
    if (hasPermission('sys:dict:edit')) {
      buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }))
    }
    if (hasPermission('sys:dict:delete')) {
      buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
    }
    return h(NSpace, null, { default: () => buttons })
  }}
]

const modalVisible = ref(false)
const modalTitle = ref('新增字典类型')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const formData = reactive<SysDictType>({ id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
const rules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

// 字典数据相关
const dataModalVisible = ref(false)
const dictDataList = ref<SysDictData[]>([])
const dataLoading = ref(false)
const currentDictType = ref('')

const dataFormVisible = ref(false)
const dataFormTitle = ref('新增字典数据')
const dataFormRef = ref<FormInst | null>(null)
const dataSubmitLoading = ref(false)
const dataFormData = reactive<SysDictData>({ id: undefined, sort: 0, dictLabel: '', dictValue: '', dictType: '', listClass: 'default', isDefault: 0, status: 1, remark: '' })
const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
}

const dataColumns: DataTableColumns<SysDictData> = [
  { title: '排序', key: 'sort', width: 60 },
  { title: '字典标签', key: 'dictLabel', width: 120 },
  { title: '字典键值', key: 'dictValue', width: 120 },
  { title: '回显样式', key: 'listClass', width: 100, render(row) {
    const typeMap: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = { default: 'default', success: 'success', warning: 'warning', error: 'error', info: 'info' }
    return h(NTag, { type: typeMap[row.listClass || 'default'], size: 'small' }, { default: () => row.dictLabel })
  }},
  { title: '状态', key: 'status', width: 80, render(row) {
    return h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.status === 1 ? '启用' : '禁用' })
  }},
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  { title: '操作', key: 'actions', width: 150, render(row) {
    const buttons = []
    if (hasPermission('sys:dict:edit')) {
      buttons.push(h(NButton, { size: 'small', onClick: () => handleEditData(row) }, { default: () => '编辑' }))
    }
    if (hasPermission('sys:dict:delete')) {
      buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDeleteData(row) }, { default: () => '删除' }))
    }
    return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
  }}
]

async function loadData() {
  loading.value = true
  try {
    const res = await dictTypeApi.page({ page: pagination.page, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = res.list
    pagination.itemCount = res.total
  } finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { searchForm.dictName = ''; searchForm.dictType = ''; searchForm.status = null; handleSearch() }
function handlePageChange(page: number) { pagination.page = page; loadData() }
function handlePageSizeChange(pageSize: number) { pagination.pageSize = pageSize; pagination.page = 1; loadData() }

function handleAdd() {
  modalTitle.value = '新增字典类型'
  Object.assign(formData, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  modalVisible.value = true
}

async function handleEdit(row: SysDictType) {
  modalTitle.value = '编辑字典类型'
  Object.assign(formData, row)
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (formData.id) { await dictTypeApi.update(formData); message.success('更新成功') }
    else { await dictTypeApi.create(formData); message.success('创建成功') }
    modalVisible.value = false
    loadData()
  } finally { submitLoading.value = false }
}

function handleDelete(row: SysDictType) {
  dialog.warning({
    title: '提示', content: `确定要删除字典类型"${row.dictName}"吗？`, positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await dictTypeApi.delete(row.id!); message.success('删除成功'); loadData() }
  })
}

async function handleViewData(row: SysDictType) {
  currentDictType.value = row.dictType
  dataModalVisible.value = true
  loadDictData()
}

async function loadDictData() {
  dataLoading.value = true
  try { dictDataList.value = await dictDataApi.listByType(currentDictType.value) }
  finally { dataLoading.value = false }
}

function handleAddData() {
  dataFormTitle.value = '新增字典数据'
  Object.assign(dataFormData, { id: undefined, sort: 0, dictLabel: '', dictValue: '', dictType: currentDictType.value, listClass: 'default', isDefault: 0, status: 1, remark: '' })
  dataFormVisible.value = true
}

function handleEditData(row: SysDictData) {
  dataFormTitle.value = '编辑字典数据'
  Object.assign(dataFormData, row)
  dataFormVisible.value = true
}

async function handleDataSubmit() {
  try {
    await dataFormRef.value?.validate()
    dataSubmitLoading.value = true
    if (dataFormData.id) { await dictDataApi.update(dataFormData); message.success('更新成功') }
    else { await dictDataApi.create(dataFormData); message.success('创建成功') }
    dataFormVisible.value = false
    loadDictData()
  } finally { dataSubmitLoading.value = false }
}

function handleDeleteData(row: SysDictData) {
  dialog.warning({
    title: '提示', content: `确定要删除字典数据"${row.dictLabel}"吗？`, positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await dictDataApi.delete(row.id!); message.success('删除成功'); loadDictData() }
  })
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.page-layout{
  height: calc(100vh - 160px);
}
</style>
