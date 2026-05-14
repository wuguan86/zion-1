<template>
  <div class="page-container">
    <div class="dept-layout">
      <!-- 左侧部门树 -->
      <n-card class="dept-tree-card" size="small">
        <template #header>
          <div class="dept-tree-header">
            <span>部门体系</span>
          </div>
        </template>
        <div class="dept-search">
          <n-input v-model:value="deptTreeSearch" placeholder="搜索部门" clearable size="small">
            <template #prefix>
              <n-icon>
                <SearchOutline/>
              </n-icon>
            </template>
          </n-input>
        </div>
        <div class="dept-tree-wrapper">
          <n-tree
              :data="tableData"
              :pattern="deptTreeSearch"
              :default-expand-all="true"
              :selected-keys="selectedDeptKeys"
              :node-props="deptNodeProps"
              key-field="id"
              label-field="deptName"
              children-field="children"
              selectable
              block-line
              draggable
              @drop="handleDrop"
          />
        </div>
        <template #footer>
          <n-button block dashed size="small" v-if="hasPermission('sys:dept:add')" @click="handleAdd(0)">
            <template #icon>
              <n-icon>
                <AddOutline/>
              </n-icon>
            </template>
            新增顶级部门
          </n-button>
        </template>
      </n-card>

      <!-- 右侧用户列表 -->
      <n-card class="user-list-card" size="small">
        <template #header>
          <div class="card-header">
            <span>{{ selectedDeptName ? `【${selectedDeptName}】部门成员` : '所有用户' }}</span>
            <n-space>
              <n-button v-if="selectedDeptId && hasPermission('sys:dept:edit')" size="small" @click="handleEditDept">
                编辑部门
              </n-button>
              <n-button v-if="selectedDeptId && hasPermission('sys:dept:add')" type="primary" size="small"
                        @click="handleAdd(selectedDeptId)">
                新增子部门
              </n-button>
              <n-button v-if="selectedDeptId && hasPermission('sys:dept:delete')" type="error" size="small"
                        @click="handleDeleteDept">
                删除部门
              </n-button>
            </n-space>
          </div>
        </template>

        <!-- 用户表格 -->
        <n-data-table
            :columns="userColumns"
            :data="userData"
            :loading="userLoading"
            :row-key="(row: SysUser) => row.id"
            remote
        />

        <div style="display: flex; justify-content: flex-end; margin-top: 12px">
          <n-pagination
              v-model:page="pagination.page"
              v-model:page-size="pagination.pageSize"
              :item-count="pagination.itemCount"
              :page-sizes="pagination.pageSizes"
              show-size-picker
              @update:page="handlePageChange"
              @update:page-size="handlePageSizeChange"
          />
        </div>
      </n-card>
    </div>

    <!-- 部门编辑弹窗 -->
    <n-modal v-model:show="modalVisible" :title="modalTitle" preset="card" style="width: 550px" :mask-closable="false">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="90">
        <n-form-item label="上级部门" path="parentId">
          <n-tree-select
              v-model:value="formData.parentId"
              :options="deptOptions"
              key-field="id"
              label-field="deptName"
              children-field="children"
              placeholder="请选择上级部门"
              clearable
              default-expand-all
          />
        </n-form-item>
        <n-form-item label="部门名称" path="deptName">
          <n-input v-model:value="formData.deptName" placeholder="请输入部门名称"/>
        </n-form-item>
        <n-form-item label="负责人" path="leader">
          <n-input v-model:value="formData.leader" placeholder="请输入负责人"/>
        </n-form-item>
        <n-form-item label="联系电话" path="phone">
          <n-input v-model:value="formData.phone" placeholder="请输入联系电话"/>
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="formData.email" placeholder="请输入邮箱"/>
        </n-form-item>
        <n-form-item label="显示排序" path="sort">
          <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%"/>
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>正常</template>
            <template #unchecked>停用</template>
          </n-switch>
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
import {ref, reactive, h, onMounted, computed, type HTMLAttributes} from 'vue'
import {
  NButton,
  NTag,
  NSpace,
  NPagination,
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type TreeOption,
  type TreeDropInfo
} from 'naive-ui'
import {SearchOutline, AddOutline} from '@vicons/ionicons5'
import {deptApi, type SysDept} from '@/api/org'
import {userApi, type SysUser, postApi} from '@/api/system'
import {useUserStore} from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// ==================== 部门树逻辑 ====================
const deptTreeSearch = ref('')
const tableData = ref<SysDept[]>([]) // 树形数据

const selectedDeptKeys = ref<number[]>([])
const selectedDeptId = ref<number | undefined>(undefined)
const selectedDeptName = ref('')

const deptOptions = computed(() => [
  {id: 0, deptName: '主目录', children: tableData.value}
])

const deptNodeProps = ({option}: { option: TreeOption }): HTMLAttributes => {
  return {
    onClick() {
      const id = option.id as number
      if (selectedDeptId.value === id) {
        selectedDeptId.value = undefined
        selectedDeptKeys.value = []
        selectedDeptName.value = ''
      } else {
        selectedDeptId.value = id
        selectedDeptKeys.value = [id]
        selectedDeptName.value = option.deptName as string
      }
      handlePageChange(1)
    }
  }
}

async function loadData() {
  try {
    tableData.value = await deptApi.tree()
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}


async function handleDrop({node, dragNode, dropPosition}: TreeDropInfo) {
  const dragId = dragNode.id as number
  const targetId = node.id as number

  let parentId: number
  if (dropPosition === 'after') {
    // 拖拽到目标节点内部，目标节点成为新的父节点
    parentId = targetId
  } else {
    // 拖拽到目标节点的前后，目标节点的父节点成为新的父节点
    parentId = (node.parentId as number) || 0
  }

  try {
    await deptApi.move(dragId, parentId)
    message.success('移动成功')
    loadData() // 重新加载树形结构
  } catch (error) {
    // 错误已在请求拦截器处理
  }
}

// ==================== 用户列表逻辑 ====================
const userData = ref<SysUser[]>([])
const userLoading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const userColumns: DataTableColumns<SysUser> = [
  {title: 'ID', key: 'id', width: 60},
  {title: '用户名', key: 'username', width: 100},
  {title: '昵称', key: 'nickname', width: 100},
  {
    title: '部门', key: 'deptName', width: 100, render(row) {
      return row.deptName || '-'
    }
  },
  {
    title: '岗位', key: 'postNames', width: 150, render(row) {
      return row.postNames || '-'
    }
  },
  {
    title: '用户类型',
    key: 'userType',
    width: 110,
    render(row) {
      const typeMap: Record<string, { type: 'info' | 'success' | 'warning'; label: string }> = {
        admin: {type: 'info', label: '后台管理员'},
        pc: {type: 'success', label: 'PC前台'},
        app: {type: 'warning', label: 'App/小程序'}
      }
      const t = typeMap[row.userType || 'admin'] || {type: 'info', label: row.userType || '未知'}
      return h(NTag, {type: t.type, size: 'small'}, {default: () => t.label})
    }
  },
  {
    title: '手机号', key: 'phone', width: 120, render(row) {
      return row.phone || '-'
    }
  },
  {
    title: '离职',
    key: 'isQuit',
    width: 80,
    render(row) {
      const quit = row.isQuit === 1
      return h(NTag, {type: quit ? 'error' : 'success', size: 'small'}, {default: () => (quit ? '是' : '否')})
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      const statusMap: Record<number, { type: 'success' | 'error' | 'warning' | 'info'; label: string }> = {
        0: {type: 'error', label: '禁用'},
        1: {type: 'success', label: '启用'},
        2: {type: 'warning', label: '待审核'},
        3: {type: 'error', label: '审核拒绝'}
      }
      const status = statusMap[row.status] || {type: 'info', label: '未知'}
      return h(NTag, {type: status.type, size: 'small'}, {default: () => status.label})
    }
  },
  {title: '创建时间', key: 'createTime', width: 170}
]

async function loadUserData() {
  userLoading.value = true
  try {
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      deptId: selectedDeptId.value
    })
    userData.value = res.list
    pagination.itemCount = Number(res.total)
  } catch (error) {
    console.error('加载用户数据失败:', error)
  } finally {
    userLoading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.page = page
  loadUserData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  handlePageChange(1)
}

// ==================== 部门维护逻辑 ====================
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const formData = reactive<SysDept>({
  id: undefined,
  parentId: 0,
  deptName: '',
  sort: 0,
  leader: '',
  phone: '',
  email: '',
  status: 1
})
const rules: FormRules = {
  deptName: [{required: true, message: '请输入部门名称', trigger: 'blur'}]
}

function handleAdd(parentId: number = 0) {
  modalTitle.value = '新增部门'
  Object.assign(formData, {id: undefined, parentId, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1})
  modalVisible.value = true
}

async function handleEditDept() {
  if (!selectedDeptId.value) return
  try {
    const dept = await deptApi.detail(selectedDeptId.value)
    modalTitle.value = '编辑部门'
    Object.assign(formData, dept)
    modalVisible.value = true
  } catch (error) {
    console.error('获取部门详情失败:', error)
  }
}

function handleDeleteDept() {
  if (!selectedDeptId.value) return
  dialog.warning({
    title: '提示',
    content: `确定要删除部门"${selectedDeptName.value}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deptApi.delete(selectedDeptId.value!)
        message.success('删除成功')
        selectedDeptId.value = undefined
        selectedDeptKeys.value = []
        selectedDeptName.value = ''
        loadData()
        loadUserData()
      } catch (error) {
        console.error('删除部门失败:', error)
      }
    }
  })
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (formData.id) {
      await deptApi.update(formData)
      message.success('更新成功')
    } else {
      await deptApi.create(formData)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadUserData()
})
</script>

<style scoped>
.dept-layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.dept-tree-card {
  width: 260px;
  flex-shrink: 0;
}

.dept-tree-header {
  font-weight: bold;
}

.dept-search {
  margin-bottom: 10px;
}

.dept-tree-wrapper {
  height: calc(100vh - 280px);
  overflow-y: auto;
}

.user-list-card {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
