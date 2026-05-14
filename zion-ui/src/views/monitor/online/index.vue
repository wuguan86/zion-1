<template>
  <div class="page-container">
    <n-card class="page-layout">
      <div class="table-toolbar">
        <n-button @click="loadData">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          刷新
        </n-button>
      </div>

      <n-data-table :columns="columns" :data="tableData" :loading="loading" :row-key="(row: OnlineUser) => row.tokenId" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import { NButton, NSpace, NTag, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { onlineApi, type OnlineUser } from '@/api/monitor'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

const tableData = ref<OnlineUser[]>([])
const loading = ref(false)

const columns: DataTableColumns<OnlineUser> = [
  { title: '序号', key: 'index', width: 60, render: (_row, index) => index + 1 },
  { title: '会话编号', key: 'tokenId', ellipsis: { tooltip: true }, minWidth: 180 },
  { title: '登录名称', key: 'loginName', width: 100 },
  { title: '主机', key: 'ipaddr', width: 130 },
  { title: '登录地点', key: 'loginLocation', width: 140 },
  { title: '浏览器', key: 'browser', width: 120 },
  { title: '操作系统', key: 'os', width: 120 },
  { title: '会话状态', key: 'status', width: 100, render(row) {
    return h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small' }, { default: () => row.status === 1 ? '在线' : '离线' })
  }},
  { title: '登录时间', key: 'loginTime', width: 180 },
  { title: '最后访问时间', key: 'lastAccessTime', width: 180 },
  { title: '操作', key: 'actions', width: 100, fixed: 'right', render(row) {
    return hasPermission('monitor:online:forceLogout')
      ? h(NButton, { size: 'small', type: 'error', onClick: () => handleForceLogout(row) }, { default: () => '强退' })
      : '-'
  }}
]

async function loadData() {
  loading.value = true
  try { tableData.value = await onlineApi.list() }
  finally { loading.value = false }
}

function handleForceLogout(row: OnlineUser) {
  dialog.warning({
    title: '提示', content: '确定要强制下线该用户吗？', positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      const tokenValue = row.tokenValue || row.tokenId
      const isSelf = !!userStore.token && tokenValue === userStore.token
      await onlineApi.forceLogout(tokenValue)
      message.success('操作成功')
      if (isSelf) {
        await userStore.logout()
        return
      }
      loadData()
    }
  })
}

onMounted(() => loadData())
</script>
<style lang="scss" scoped>
.page-layout{
  height: calc(100vh - 160px);
}
</style>
