<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="角色名称">
            <n-input v-model:value="searchForm.name" placeholder="请输入角色名称" clearable />
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
        <n-button v-if="hasPermission('sys:role:add')" type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增角色
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SysRole) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      style="width: 700px"
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
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="角色名称" path="name">
              <n-input v-model:value="formData.name" placeholder="请输入角色名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="角色编码" path="code">
              <n-input v-model:value="formData.code" placeholder="请输入角色编码" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="排序" path="sort">
              <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
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
        <n-form-item label="菜单权限" path="menuIds">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="menuTreeData"
              :checked-keys="menuIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleMenuCheck"
              @update:indeterminate-keys="handleIndeterminateChange"
            />
          </div>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
        <n-form-item label="数据权限" path="dataScope">
          <n-select
            v-model:value="formData.dataScope"
            placeholder="请选择数据范围"
            :options="dataScopeOptions"
          />
        </n-form-item>
        <n-form-item v-if="formData.dataScope === 2" label="部门权限">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="deptTreeData"
              :checked-keys="deptIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleDeptCheck"
            />
          </div>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline } from '@vicons/ionicons5'
import { roleApi, menuApi, deptApi, type SysRole, type SysMenu, type SysDept } from '@/api/system'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// 权限检查
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// 搜索表单
const searchForm = reactive({
  name: '',
  status: null as number | null
})

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 数据范围选项
const dataScopeOptions = [
  { label: '全部数据权限', value: 1 },
  { label: '自定数据权限', value: 2 },
  { label: '本部门数据权限', value: 3 },
  { label: '本部门及以下数据权限', value: 4 },
  { label: '仅本人数据权限', value: 5 }
]

// 表格数据
const tableData = ref<SysRole[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 菜单树
const menuTreeData = ref<TreeOption[]>([])
const menuIds = ref<number[]>([])

// 部门树
const deptTreeData = ref<TreeOption[]>([])
const deptIds = ref<number[]>([])

// 表格列
const columns: DataTableColumns<SysRole> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'name', width: 150 },
  { title: '角色编码', key: 'code', width: 150 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small' },
        { default: () => (row.status === 1 ? '启用' : '禁用') }
      )
    }
  },
  { title: '备注', key: 'remark',width: 180, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render(row) {
      const buttons = []
      if (hasPermission('sys:role:edit')) {
        buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPermission('sys:role:delete')) {
        buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
      }
      return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
    }
  }
]

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增角色')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)

const formData = reactive<SysRole>({
  id: undefined,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 转换菜单为树结构
function convertMenuToTree(menus: SysMenu[]): TreeOption[] {
  return menus.map(menu => ({
    key: menu.id,
    label: menu.name,
    children: menu.children ? convertMenuToTree(menu.children) : undefined
  }))
}

// 转换部门为树结构
function convertDeptToTree(depts: SysDept[]): TreeOption[] {
  return depts.map(dept => ({
    key: dept.id,
    label: dept.deptName,
    children: dept.children ? convertDeptToTree(dept.children) : undefined
  }))
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await roleApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      status: searchForm.status ?? undefined
    })
    tableData.value = res.list
    pagination.itemCount = res.total
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

// 加载菜单树
async function loadMenuTree() {
  try {
    const menus = await menuApi.tree()
    menuTreeData.value = convertMenuToTree(menus)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 加载部门树
async function loadDeptTree() {
  try {
    const depts = await deptApi.tree()
    deptTreeData.value = convertDeptToTree(depts)
  } catch (error) {
    // 错误已在拦截器处理
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
  searchForm.status = null
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

// 菜单选择 - 同时记录半选的父节点
const halfCheckedKeys = ref<number[]>([])

function handleMenuCheck(keys: number[]) {
  menuIds.value = keys
}

function handleIndeterminateChange(keys: number[]) {
  halfCheckedKeys.value = keys
}

// 部门选择
function handleDeptCheck(keys: number[]) {
  deptIds.value = keys
}

// 新增
function handleAdd() {
  modalTitle.value = '新增角色'
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    sort: 0,
    status: 1,
    dataScope: 1,
    remark: ''
  })
  menuIds.value = []
  deptIds.value = []
  modalVisible.value = true
}

// 获取所有叶子节点ID
function getLeafNodeIds(nodes: TreeOption[]): number[] {
  const leafIds: number[] = []
  function traverse(items: TreeOption[]) {
    for (const item of items) {
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      } else {
        leafIds.push(item.key as number)
      }
    }
  }
  traverse(nodes)
  return leafIds
}

// 编辑
async function handleEdit(row: SysRole) {
  modalTitle.value = '编辑角色'
  try {
    const res = await roleApi.detail(row.id!)
    Object.assign(formData, res.role)
    // 只选中叶子节点，父节点会自动计算半选状态
    const leafIds = getLeafNodeIds(menuTreeData.value)
    menuIds.value = res.menuIds.filter((id: number) => leafIds.includes(id))
    // 部门回显
    deptIds.value = res.deptIds || []
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    // 合并选中的节点和半选的父节点
    const allMenuIds = [...new Set([...menuIds.value, ...halfCheckedKeys.value])]

    const data = {
      role: { ...formData },
      menuIds: allMenuIds,
      deptIds: formData.dataScope === 2 ? deptIds.value : []
    }

    if (formData.id) {
      await roleApi.update(data)
      message.success('更新成功')
    } else {
      await roleApi.create(data)
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

// 删除
function handleDelete(row: SysRole) {
  dialog.warning({
    title: '提示',
    content: `确定要删除角色"${row.name}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await roleApi.delete(row.id!)
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
  loadMenuTree()
  loadDeptTree()
})
</script>

<style lang="scss" scoped>
.menu-tree-wrapper {
  width: 100%;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 12px;
}

.page-layout{
  height: calc(100vh - 160px);
}

</style>
