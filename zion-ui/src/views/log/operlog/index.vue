<template>
  <div class="page-container">
    <n-card class="page-layout">
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="模块名称">
            <n-input v-model:value="searchForm.title" placeholder="请输入模块名称" clearable />
          </n-form-item>
          <n-form-item label="操作人员">
            <n-input v-model:value="searchForm.operName" placeholder="请输入操作人员" clearable />
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

      <div class="table-toolbar">
        <n-button v-if="hasPermission('sys:operlog:delete')" type="error" @click="handleClean">
          <template #icon><n-icon><TrashOutline /></n-icon></template>
          清空日志
        </n-button>
      </div>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysOperLog) => row.id"
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

    <n-modal v-model:show="detailVisible" title="日志详情" preset="card" style="width: 700px">
      <n-descriptions :column="2" label-placement="left">
        <n-descriptions-item label="模块名称">{{ detailData.title }}</n-descriptions-item>
        <n-descriptions-item label="请求方式">{{ detailData.requestMethod }}</n-descriptions-item>
        <n-descriptions-item label="操作人员">{{ detailData.operName }}</n-descriptions-item>
        <n-descriptions-item label="操作地址">{{ detailData.operIp }}</n-descriptions-item>
        <n-descriptions-item label="请求地址" :span="2">{{ detailData.operUrl }}</n-descriptions-item>
        <n-descriptions-item label="方法名称" :span="2">{{ detailData.method }}</n-descriptions-item>
        <n-descriptions-item label="请求参数" :span="2">
          <div class="code-container">
            <n-code :code="formatJson(detailData.operParam)" language="json" word-wrap />
          </div>
        </n-descriptions-item>
        <n-descriptions-item label="返回参数" :span="2">
          <div class="code-container">
            <n-code :code="formatJson(detailData.jsonResult)" language="json" word-wrap />
          </div>
        </n-descriptions-item>
        <n-descriptions-item label="操作状态">
          <n-tag :type="detailData.status === 0 ? 'success' : 'error'" size="small">{{ detailData.status === 0 ? '正常' : '异常' }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="耗时">{{ detailData.costTime }}ms</n-descriptions-item>
        <n-descriptions-item label="操作时间" :span="2">{{ detailData.operTime }}</n-descriptions-item>
        <n-descriptions-item v-if="detailData.errorMsg" label="错误信息" :span="2">
          <n-text type="error">{{ detailData.errorMsg }}</n-text>
        </n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { NButton, NTag, NSpace, NPagination, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { SearchOutline, RefreshOutline, TrashOutline } from '@vicons/ionicons5'
import { operLogApi, type SysOperLog } from '@/api/monitor'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

const searchForm = reactive({ title: '', operName: '', status: null as number | null })
const statusOptions = [{ label: '正常', value: 0 }, { label: '异常', value: 1 }]
const tableData = ref<SysOperLog[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const pageCount = computed(() => Math.ceil(pagination.itemCount / pagination.pageSize))

const detailVisible = ref(false)
const detailData = ref<SysOperLog>({} as SysOperLog)

const businessTypeMap: Record<number, string> = { 0: '其他', 1: '新增', 2: '修改', 3: '删除', 4: '查询', 5: '导出' }

const columns: DataTableColumns<SysOperLog> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '模块名称', key: 'title', width: 120 },
  { title: '业务类型', key: 'businessType', width: 100, render(row) {
    return h('span', {}, businessTypeMap[row.businessType] || '其他')
  }},
  { title: '请求方式', key: 'requestMethod', width: 100 },
  { title: '操作人员', key: 'operName', width: 100 },
  { title: 'IP地址', key: 'operIp', width: 140 },
  { title: '状态', key: 'status', width: 80, render(row) {
    return h(NTag, { type: row.status === 0 ? 'success' : 'error', size: 'small' }, { default: () => row.status === 0 ? '正常' : '异常' })
  }},
  { title: '耗时', key: 'costTime', width: 80, render(row) { return h('span', {}, `${row.costTime}ms`) }},
  { title: '操作时间', key: 'operTime', width: 180 },
  { title: '操作', key: 'actions', width: 120, fixed: 'right', render(row) {
    const buttons = [h(NButton, { size: 'small', onClick: () => handleDetail(row) }, { default: () => '详情' })]
    if (hasPermission('sys:operlog:delete')) {
      buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
    }
    return h(NSpace, null, { default: () => buttons })
  }}
]

async function loadData() {
  loading.value = true
  try {
    const res = await operLogApi.page({ page: pagination.page, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
  } finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { searchForm.title = ''; searchForm.operName = ''; searchForm.status = null; handleSearch() }
function handlePageChange(page: number) { pagination.page = page; loadData() }
function handlePageSizeChange(pageSize: number) { pagination.pageSize = pageSize; pagination.page = 1; loadData() }

function handleDetail(row: SysOperLog) {
  detailData.value = row
  detailVisible.value = true
}

// 格式化 JSON 字符串
function formatJson(jsonStr: string | undefined | null): string {
  if (!jsonStr) return ''
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch {
    // 如果不是有效的 JSON，直接返回原字符串
    return jsonStr
  }
}

function handleDelete(row: SysOperLog) {
  dialog.warning({
    title: '提示', content: '确定要删除该日志吗？', positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await operLogApi.delete(row.id!); message.success('删除成功'); loadData() }
  })
}

function handleClean() {
  dialog.warning({
    title: '提示', content: '确定要清空所有操作日志吗？', positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await operLogApi.clean(); message.success('清空成功'); loadData() }
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.code-container {
  max-height: 200px;
  overflow: auto;
  background-color: #f5f5f5;
  border-radius: 4px;
  padding: 8px;
  width: 100%;
}

.code-container :deep(.n-code) {
  white-space: pre-wrap;
  word-break: break-all;
}

</style>
