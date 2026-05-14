<template>
  <div class="page-container">
    <div class="post-layout">
      <!-- 左侧岗位树 -->
      <n-card class="post-tree-card" size="small">
        <template #header>
          <div class="post-tree-header">
            <span>岗位体系</span>
          </div>
        </template>
        <div class="post-search">
          <n-input v-model:value="postTreeSearch" placeholder="搜索岗位" clearable size="small">
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
        </div>
        <div class="post-tree-wrapper">
          <n-tree
            :data="postTreeData"
            :pattern="postTreeSearch"
            :default-expand-all="true"
            :selected-keys="selectedPostKeys"
            :node-props="postNodeProps"
            key-field="id"
            label-field="postName"
            children-field="children"
            selectable
            block-line
            draggable
            @drop="handleDrop"
          />
        </div>
        <template #footer>
          <n-button block dashed size="small" v-if="hasPermission('sys:post:add')" @click="handleAdd(0)">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增顶级岗位
          </n-button>
        </template>
      </n-card>

      <!-- 右侧用户列表 -->
      <n-card class="user-list-card" size="small">
        <template #header>
          <div class="card-header">
            <span>{{ selectedPostName ? `【${selectedPostName}】岗位成员` : '所有用户' }}</span>
            <n-space>
               <n-button v-if="selectedPostId && hasPermission('sys:post:edit')" size="small" @click="handleEditPost">
                编辑岗位
              </n-button>
              <n-button v-if="selectedPostId && hasPermission('sys:post:add')" type="primary" size="small" @click="handleAdd(selectedPostId)">
                新增子岗位
              </n-button>
              <n-button v-if="selectedPostId && hasPermission('sys:post:delete')" type="error" size="small" @click="handleDeletePost">
                删除岗位
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

    <!-- 岗位编辑弹窗 -->
    <n-modal v-model:show="modalVisible" :title="modalTitle" preset="card" style="width: 500px" :mask-closable="false">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="上级岗位" path="parentId">
          <n-tree-select
            v-model:value="formData.parentId"
            :options="postTreeOptions"
            key-field="id"
            label-field="postName"
            children-field="children"
            placeholder="请选择上级岗位"
            clearable
          />
        </n-form-item>
        <n-form-item label="岗位编码" path="postCode">
          <n-input v-model:value="formData.postCode" placeholder="请输入岗位编码" />
        </n-form-item>
        <n-form-item label="岗位名称" path="postName">
          <n-input v-model:value="formData.postName" placeholder="请输入岗位名称" />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>正常</template>
            <template #unchecked>停用</template>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed, type HTMLAttributes } from 'vue'
import { NButton, NTag, NSpace, NPagination, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption, type TreeDropInfo } from 'naive-ui'
import { SearchOutline, AddOutline } from '@vicons/ionicons5'
import { postApi, userApi, type SysUser, type SysPost } from '@/api/system'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// ==================== 岗位树逻辑 ====================
const postTreeSearch = ref('')
const postTreeData = ref<SysPost[]>([])
const selectedPostKeys = ref<number[]>([])
const selectedPostId = ref<number | undefined>(undefined)
const selectedPostName = ref('')

const postTreeOptions = computed(() => [
  { id: 0, postName: '顶级岗位', children: postTreeData.value }
])

const postNodeProps = ({ option }: { option: TreeOption }): HTMLAttributes => {
  return {
    onClick() {
      const id = option.id as number
      if (selectedPostId.value === id) {
        selectedPostId.value = undefined
        selectedPostKeys.value = []
        selectedPostName.value = ''
      } else {
        selectedPostId.value = id
        selectedPostKeys.value = [id]
        selectedPostName.value = option.postName as string
      }
      handlePageChange(1)
    }
  }
}

async function loadPostTree() {
  try {
    postTreeData.value = await postApi.tree()
  } catch (error) {
    console.error('加载岗位树失败:', error)
  }
}

// ==================== 拖拽处理逻辑 ====================
async function handleDrop({ node, dragNode, dropPosition }: TreeDropInfo) {
  const dragId = dragNode.id as number
  const targetId = node.id as number

  let parentId: number

  if (dropPosition === 'inside') {
    // 拖拽到目标节点内部，目标节点成为新的父节点
    parentId = targetId
  } else {
    // 拖拽到目标节点的前后，目标节点的父节点成为新的父节点
    parentId = (node.parentId as number) || 0
  }

  try {
    await postApi.move(dragId, parentId)
    message.success('移动成功')
    loadPostTree() // 重新加载树形结构
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
  { title: '创建时间', key: 'createTime', width: 170 }
]

async function loadUserData() {
  userLoading.value = true
  try {
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      postId: selectedPostId.value
    })
    userData.value = res.list
    pagination.itemCount = res.total
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

// ==================== 岗位维护逻辑 ====================
const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const formData = reactive<SysPost>({ id: undefined, parentId: 0, postCode: '', postName: '', sort: 0, status: 1, remark: '' })
const rules: FormRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

function handleAdd(parentId: number = 0) {
  modalTitle.value = '新增岗位'
  Object.assign(formData, { id: undefined, parentId, postCode: '', postName: '', sort: 0, status: 1, remark: '' })
  modalVisible.value = true
}

async function handleEditPost() {
  if (!selectedPostId.value) return
  try {
    const post = await postApi.detail(selectedPostId.value)
    modalTitle.value = '编辑岗位'
    Object.assign(formData, post)
    modalVisible.value = true
  } catch (error) {
    console.error('获取岗位详情失败:', error)
  }
}

function handleDeletePost() {
  if (!selectedPostId.value) return
  dialog.warning({
    title: '提示',
    content: `确定要删除岗位"${selectedPostName.value}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await postApi.delete(selectedPostId.value!)
        message.success('删除成功')
        selectedPostId.value = undefined
        selectedPostKeys.value = []
        selectedPostName.value = ''
        loadPostTree()
        loadUserData()
      } catch (error) {
        console.error('删除岗位失败:', error)
      }
    }
  })
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (formData.id) {
      await postApi.update(formData)
      message.success('更新成功')
    } else {
      await postApi.create(formData)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadPostTree()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadPostTree()
  loadUserData()
})
</script>

<style scoped>
.post-layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.post-tree-card {
  width: 260px;
  flex-shrink: 0;
}

.post-tree-header {
  font-weight: bold;
}

.post-search {
  margin-bottom: 10px;
}

.post-tree-wrapper {
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
