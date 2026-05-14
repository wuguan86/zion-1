<template>
  <div class="page-container">
    <!-- 用户列表 -->
    <n-card class="user-list-card" size="small">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="用户名">
            <n-input v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
          </n-form-item>
          <n-form-item label="用户类型">
            <n-select
              v-model:value="searchForm.userType"
              placeholder="请选择用户类型"
              :options="userTypeOptions"
              clearable
              style="width: 140px"
            />
          </n-form-item>
          <n-form-item label="状态">
            <n-select
              v-model:value="searchForm.status"
              placeholder="请选择状态"
              :options="statusOptions"
              clearable
              style="width: 120px"
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
        <n-space>
          <n-button v-if="hasPermission('sys:user:add')" type="primary" @click="handleAdd">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增用户
          </n-button>
          <n-button v-if="hasPermission('sys:user:import')" @click="importModalVisible = true">
            <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
            导入
          </n-button>
          <n-button v-if="hasPermission('sys:user:export')" @click="handleExport">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            导出{{ checkedRowKeys.length > 0 ? `(${checkedRowKeys.length})` : '' }}
          </n-button>
          <n-button 
            v-if="hasPermission('sys:user:delete') && checkedRowKeys.length > 0" 
            type="error" 
            @click="handleBatchDelete"
          >
            <template #icon><n-icon><TrashOutline /></n-icon></template>
            批量删除({{ checkedRowKeys.length }})
          </n-button>
        </n-space>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysUser) => row.id"
        v-model:checked-row-keys="checkedRowKeys"
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

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="80"
        class="modal-form"
      >
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="用户名" path="username">
              <n-input v-model:value="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="!formData.id">
            <n-form-item label="密码" path="password">
              <n-input v-model:value="formData.password" type="password" placeholder="留空默认123456" show-password-on="click" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="昵称" path="nickname">
              <n-input v-model:value="formData.nickname" placeholder="请输入昵称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="归属部门" path="deptId">
              <n-tree-select
                v-model:value="formData.deptId"
                :options="deptOptions"
                key-field="id"
                label-field="deptName"
                children-field="children"
                placeholder="请选择归属部门"
                clearable
                default-expand-all
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="邮箱" path="email">
              <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="手机号" path="phone">
              <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="性别" path="gender">
              <n-radio-group v-model:value="formData.gender">
                <n-radio :value="1">男</n-radio>
                <n-radio :value="2">女</n-radio>
                <n-radio :value="0">未知</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="用户类型" path="userType">
              <n-select
                v-model:value="formData.userType"
                :options="userTypeOptions"
                placeholder="请选择用户类型"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="角色" path="roleIds">
              <n-select
                v-model:value="roleIds"
                multiple
                :options="roleOptions"
                placeholder="请选择角色"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="岗位" path="postIds">
              <n-select
                v-model:value="postIds"
                multiple
                :options="postOptions"
                placeholder="请选择岗位"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="状态" path="status">
              <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
                <template #checked>启用</template>
                <template #unchecked>禁用</template>
              </n-switch>
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
    <!-- 导入弹窗 -->
    <n-modal
      v-model:show="importModalVisible"
      title="导入用户"
      preset="card"
      style="width: 500px"
      :mask-closable="false"
    >
      <n-space vertical>
        <n-alert type="info" :show-icon="true">
          <template #header>导入说明</template>
          <ul style="margin: 0; padding-left: 16px; line-height: 1.8">
            <li>请先下载导入模板，按模板格式填写数据</li>
            <li>用户名不能重复，否则导入失败</li>
            <li>密码默认为 123456</li>
            <li>性别填写：男/女/未知</li>
            <li>用户类型填写：后台管理员/PC前台用户/App小程序用户</li>
            <li>状态填写：启用/禁用</li>
            <li>角色和岗位填写对应名称，多个用逗号分隔</li>
          </ul>
        </n-alert>
        <n-space>
          <n-button type="primary" @click="handleDownloadTemplate">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            下载模板
          </n-button>
        </n-space>
        <n-upload
          :max="1"
          accept=".xlsx,.xls"
          :show-file-list="true"
          :custom-request="handleImportUpload"
        >
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon size="48" :depth="3">
                <CloudUploadOutline />
              </n-icon>
            </div>
            <n-text style="font-size: 16px">点击或拖拽文件到此处上传</n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">支持 .xlsx 或 .xls 格式</n-p>
          </n-upload-dragger>
        </n-upload>
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button @click="importModalVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed, type HTMLAttributes } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, NTag, NSpace, NDropdown, NPagination, NGrid, NGi, NUploadDragger, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption, type UploadCustomRequestOptions } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, ChevronDownOutline, CloudUploadOutline, DownloadOutline, TrashOutline } from '@vicons/ionicons5'
import { userApi, roleApi, postApi, type SysUser, type SysRole } from '@/api/system'
import { deptApi, type SysDept } from '@/api/org'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const route = useRoute()

// 权限检查
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// ==================== 部门选择 ====================
const deptOptions = ref<SysDept[]>([])
const selectedDeptId = ref<number | undefined>(undefined)

// 加载部门树（用于下拉选择）
async function loadDeptOptions() {
  try {
    const tree = await deptApi.tree()
    deptOptions.value = tree
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// ==================== 搜索表单 ====================
const searchForm = reactive({
  username: '',
  status: null as number | null,
  userType: null as string | null
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
  { label: '待审核', value: 2 },
  { label: '审核拒绝', value: 3 }
]

// ==================== 表格 ====================
const tableData = ref<SysUser[]>([])
const loading = ref(false)
const checkedRowKeys = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const pageCount = computed(() => Math.ceil(pagination.itemCount / pagination.pageSize))

const roleOptions = ref<Array<{ label: string; value: number }>>([])

const userTypeOptions = [
  { label: '后台管理员', value: 'admin' },
  { label: 'PC前台用户', value: 'pc' },
  { label: 'App/小程序用户', value: 'app' }
]

const columns: DataTableColumns<SysUser> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 60 },
  { title: '用户名', key: 'username', width: 100 },
  { title: '昵称', key: 'nickname', width: 100 },
  { title: '部门', key: 'deptName', width: 100, render(row) {
    return row.deptName || '-'
  }},
  { title: '岗位', key: 'postNames', width: 150, render(row) {
    return row.postNames || '-'
  }},
  {
    title: '用户类型',
    key: 'userType',
    width: 110,
    render(row) {
      const typeMap: Record<string, { type: 'info' | 'success' | 'warning'; label: string }> = {
        admin: { type: 'info', label: '后台管理员' },
        pc: { type: 'success', label: 'PC前台' },
        app: { type: 'warning', label: 'App/小程序' }
      }
      const t = typeMap[row.userType || 'admin'] || { type: 'info', label: row.userType || '未知' }
      return h(NTag, { type: t.type, size: 'small' }, { default: () => t.label })
    }
  },
  { title: '手机号', key: 'phone', width: 120 , render(row) {
      return row.phone || '-'
    }},
  {
    title: '离职',
    key: 'isQuit',
    width: 80,
    render(row) {
      const quit = row.isQuit === 1
      return h(NTag, { type: quit ? 'error' : 'success', size: 'small' }, { default: () => (quit ? '是' : '否') })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      const statusMap: Record<number, { type: 'success' | 'error' | 'warning' | 'info'; label: string }> = {
        0: { type: 'error', label: '禁用' },
        1: { type: 'success', label: '启用' },
        2: { type: 'warning', label: '待审核' },
        3: { type: 'error', label: '审核拒绝' }
      }
      const status = statusMap[row.status] || { type: 'info', label: '未知' }
      return h(NTag, { type: status.type, size: 'small' }, { default: () => status.label })
    }
  },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    fixed: 'right',
    render(row) {
      const buttons = []
      // 待审核状态显示审核按钮
      if (row.status === 2 && hasPermission('sys:user:edit')) {
        buttons.push(
          h(NButton, { size: 'small', type: 'success', onClick: () => handleApprove(row) }, { default: () => '通过' })
        )
        buttons.push(
          h(NButton, { size: 'small', type: 'error', onClick: () => handleReject(row) }, { default: () => '拒绝' })
        )
      }
      if (hasPermission('sys:user:edit')) {
        buttons.push(
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' })
        )
      }
      if (hasPermission('sys:user:delete')) {
        buttons.push(
          h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' })
        )
      }

      // 更多操作
      if (hasPermission('sys:user:edit')) {
        const moreOptions = []

        // 重置密码移入更多
        if (row.status !== 2) {
          moreOptions.push({
            label: '重置密码',
            key: 'resetPassword'
          })
        }

        moreOptions.push({
          label: row.isQuit === 1 ? '取消离职' : '离职',
          key: 'toggleQuit'
        })

        buttons.push(
          h(
            NDropdown,
            {
              trigger: 'click',
              options: moreOptions,
              onSelect: (key) => {
                if (key === 'toggleQuit') {
                  handleToggleQuit(row)
                } else if (key === 'resetPassword') {
                  handleResetPassword(row)
                }
              }
            },
            {
              default: () =>
                h(
                  NButton,
                  { size: 'small' },
                  {
                    default: () => '更多',
                    icon: () => h(ChevronDownOutline)
                  }
                )
            }
          )
        )
      }

      return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
    }
  }
]

// ==================== 弹窗 ====================
const modalVisible = ref(false)
const modalTitle = ref('新增用户')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const roleIds = ref<number[]>([])
const postIds = ref<number[]>([])
const postOptions = ref<Array<{ label: string; value: number }>>([])

const formData = reactive<SysUser>({
  id: undefined,
  deptId: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  status: 1,
  userType: 'admin',
  remark: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

// ==================== 数据加载 ====================
async function loadData() {
  loading.value = true
  try {
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      username: searchForm.username || undefined,
      status: searchForm.status ?? undefined,
      userType: searchForm.userType || undefined,
      deptId: selectedDeptId.value
    })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
    console.log('Pagination updated:', pagination.itemCount)
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const roles = await roleApi.list()
    roleOptions.value = roles.map((role: SysRole) => ({
      label: role.name,
      value: role.id!
    }))
  } catch (error) {
    // 错误已在拦截器处理
  }
}

async function loadPostOptions() {
  try {
    const posts = await postApi.list()
    postOptions.value = posts.map(p => ({
      label: p.postName,
      value: p.id!
    }))
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// ==================== 操作方法 ====================
function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.status = null
  searchForm.userType = null
  selectedDeptId.value = undefined
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

function handleAdd() {
  modalTitle.value = '新增用户'
  Object.assign(formData, {
    id: undefined,
    deptId: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    gender: 0,
    status: 1,
    userType: 'admin',
    remark: ''
  })
  roleIds.value = []
  postIds.value = []
  modalVisible.value = true
}

async function handleEdit(row: SysUser) {
  modalTitle.value = '编辑用户'
  try {
    const res = await userApi.detail(row.id!)
    Object.assign(formData, res.user)
    roleIds.value = res.roleIds
    postIds.value = res.postIds
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    const data = {
      user: { ...formData },
      roleIds: roleIds.value,
      postIds: postIds.value
    }

    if (formData.id) {
      await userApi.update(data)
      message.success('更新成功')
    } else {
      await userApi.create(data)
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

function handleDelete(row: SysUser) {
  dialog.warning({
    title: '提示',
    content: `确定要删除用户"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.delete(row.id!)
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 审核通过
function handleApprove(row: SysUser) {
  dialog.success({
    title: '审核通过',
    content: `确定通过用户"${row.username}"的注册申请吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.approve(row.id!)
        message.success('审核通过')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 审核拒绝
function handleReject(row: SysUser) {
  dialog.warning({
    title: '审核拒绝',
    content: `确定拒绝用户"${row.username}"的注册申请吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.reject(row.id!)
        message.success('已拒绝')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

function handleResetPassword(row: SysUser) {
  dialog.warning({
    title: '提示',
    content: `确定要重置用户"${row.username}"的密码吗？重置后密码为123456`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.resetPassword(row.id!)
        message.success('密码重置成功')
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

function handleToggleQuit(row: SysUser) {
  const nextQuit = row.isQuit === 1 ? 0 : 1
  dialog.warning({
    title: '提示',
    content: nextQuit === 1 ? `确定将用户"${row.username}"设置为离职吗？` : `确定将用户"${row.username}"取消离职吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.toggleQuit(row.id!)
        message.success(nextQuit === 1 ? '已设置离职' : '已取消离职')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// ==================== 导入弹窗 ====================
const importModalVisible = ref(false)

// 导出用户
async function handleExport() {
  // 必须先勾选数据
  if (checkedRowKeys.value.length === 0) {
    message.warning('请先勾选要导出的用户')
    return
  }
  try {
    const blob = await userApi.exportUsers({
      ids: checkedRowKeys.value
    })
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户数据.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 批量删除
function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请选择要删除的用户')
    return
  }
  dialog.warning({
    title: '提示',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 个用户吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.deleteBatch(checkedRowKeys.value)
        message.success('删除成功')
        checkedRowKeys.value = []
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 下载导入模板
async function handleDownloadTemplate() {
  try {
    const blob = await userApi.downloadTemplate()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 导入上传
async function handleImportUpload({ file }: UploadCustomRequestOptions) {
  if (!file.file) return
  try {
    const result = await userApi.importUsers(file.file)
    if (result.fail > 0) {
      dialog.warning({
        title: '导入结果',
        content: `成功: ${result.success} 条，失败: ${result.fail} 条\n错误信息: ${result.errors?.join('\n') || '无'}`,
        positiveText: '确定'
      })
    } else {
      message.success(`导入成功，共 ${result.success} 条数据`)
      importModalVisible.value = false
    }
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

onMounted(() => {
  const deptId = route.query.deptId
  if (deptId) {
    selectedDeptId.value = Number(deptId)
  }
  loadDeptOptions()
  loadData()
  loadRoles()
  loadPostOptions()
})
</script>

<style scoped>

</style>
