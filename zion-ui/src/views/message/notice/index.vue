<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="标题">
            <n-input v-model:value="searchForm.title" placeholder="请输入标题" clearable />
          </n-form-item>
          <n-form-item label="类型">
            <n-select
              v-model:value="searchForm.noticeType"
              placeholder="请选择类型"
              :options="noticeTypeOptions"
              clearable
              style="width: 120px"
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
        <n-button v-if="hasPermission('sys:notice:add')" type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增通知
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysNotice) => row.id"
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
      style="width: 700px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="80"
      >
        <n-form-item label="通知标题" path="title">
          <n-input v-model:value="formData.title" placeholder="请输入通知标题" />
        </n-form-item>
        <n-form-item label="通知类型" path="noticeType">
          <n-select
            v-model:value="formData.noticeType"
            placeholder="请选择通知类型"
            :options="noticeTypeOptions"
          />
        </n-form-item>
        <n-form-item label="推送渠道" path="channels">
          <div class="channel-options">
            <n-space vertical>
              <n-space>
                <n-checkbox-group v-model:value="formData.channels">
                  <n-space>
                    <n-checkbox
                      v-for="ch in baseChannelOptions"
                      :key="ch.code"
                      :value="ch.code"
                      :disabled="!ch.enabled"
                    >
                      {{ ch.name }}{{ !ch.enabled ? '(未配置)' : '' }}
                    </n-checkbox>
                  </n-space>
                </n-checkbox-group>
                <n-checkbox
                  :checked="webhookExpanded"
                  @update:checked="onWebhookToggle"
                >
                  Webhook(飞书/钉钉/企业微信)
                </n-checkbox>
              </n-space>
              <div v-if="webhookExpanded" class="webhook-sub-options">
                <n-checkbox-group v-model:value="formData.channels">
                  <n-space>
                    <n-checkbox
                      v-for="ch in webhookChannelOptions"
                      :key="ch.code"
                      :value="ch.code"
                      :disabled="!ch.enabled"
                    >
                      {{ ch.name }}{{ !ch.enabled ? '(未配置)' : '' }}
                    </n-checkbox>
                  </n-space>
                </n-checkbox-group>
              </div>
            </n-space>
          </div>
        </n-form-item>
        <n-form-item label="推送对象" path="targetType">
          <n-radio-group v-model:value="formData.targetType">
            <n-space>
              <n-radio :value="1">指定用户</n-radio>
              <n-radio :value="2">按部门</n-radio>
              <n-radio :value="3">全部推送</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="formData.targetType === 1" label="选择用户">
          <n-select
            v-model:value="formData.targetIds"
            multiple
            filterable
            placeholder="请选择用户"
            :options="userOptions"
            :loading="usersLoading"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item v-if="formData.targetType === 2" label="选择部门">
          <n-select
            v-model:value="formData.targetIds"
            multiple
            filterable
            placeholder="请选择部门"
            :options="deptOptions"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="通知内容" path="content">
          <n-input
            v-model:value="formData.content"
            type="textarea"
            placeholder="请输入通知内容"
            :rows="6"
          />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="formData.status">
            <n-radio :value="0">草稿</n-radio>
            <n-radio :value="1">发布</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 查看详情弹窗 -->
    <n-modal v-model:show="detailVisible" preset="card" title="通知详情" style="width: 600px">
      <div v-if="detailData" class="notice-detail">
        <h3>{{ detailData.title }}</h3>
        <div class="notice-meta">
          <span>发布者：{{ detailData.createName }}</span>
          <span>发布时间：{{ detailData.createTime }}</span>
        </div>
        <n-divider />
        <div class="notice-content">{{ detailData.content }}</div>
      </div>
    </n-modal>

    <!-- 通知记录弹窗 -->
    <n-modal
      v-model:show="sendLogsVisible"
      preset="card"
      :title="`通知记录：${sendLogsNoticeTitle}`"
      style="width: 680px"
    >
      <n-data-table
        :columns="sendLogColumns"
        :data="sendLogsData"
        :loading="sendLogsLoading"
        :row-key="(row: NoticeSendLog) => row.id || row.channel + row.sendTime"
        size="small"
      />
      <n-empty v-if="!sendLogsLoading && sendLogsData.length === 0" description="暂无推送记录" style="margin: 24px 0" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { NButton, NTag, NSpace, NPagination, NCheckboxGroup, NCheckbox, NRadioGroup, NRadio, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline } from '@vicons/ionicons5'
import { noticeApi, type SysNotice, type NoticeChannelOption, type NoticeSendLog } from '@/api/message'
import { deptApi, type SysDept } from '@/api/org'
import { userApi } from '@/api/system'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// 搜索表单
const searchForm = reactive({
  title: '',
  noticeType: null as number | null,
  status: null as number | null
})

// 选项
const noticeTypeOptions = [
  { label: '通知', value: 1 },
  { label: '公告', value: 2 }
]

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const channelOptions = ref<NoticeChannelOption[]>([])
const webhookExpanded = ref(false)

const baseChannelOptions = computed(() =>
  channelOptions.value.filter((ch) => ['station', 'email'].includes(ch.code))
)
const webhookChannelOptions = computed(() =>
  channelOptions.value.filter((ch) => ['dingtalk', 'feishu', 'wechat_work'].includes(ch.code))
)

function onWebhookToggle(checked: boolean) {
  webhookExpanded.value = checked
  if (!checked && formData.channels) {
    formData.channels = formData.channels.filter(
      (c) => !['dingtalk', 'feishu', 'wechat_work'].includes(c)
    )
  }
}
const userOptions = ref<{ label: string; value: number }[]>([])
const deptOptions = ref<{ label: string; value: number }[]>([])
const usersLoading = ref(false)

// 表格数据
const tableData = ref<SysNotice[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 表格列
const columns: DataTableColumns<SysNotice> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '类型',
    key: 'noticeType',
    width: 100,
    render(row) {
      const typeMap: Record<number, { text: string; type: 'info' | 'warning' }> = {
        1: { text: '通知', type: 'info' },
        2: { text: '公告', type: 'warning' }
      }
      const config = typeMap[row.noticeType] || { text: '未知', type: 'info' as const }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'default',
        size: 'small'
      }, { default: () => row.status === 1 ? '已发布' : '草稿' })
    }
  },
  { title: '创建者', key: 'createName', width: 100 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    fixed: 'right',
    render(row) {
      const buttons = [h(NButton, { size: 'small', onClick: () => handleView(row) }, { default: () => '查看' })]
      if (row.status === 1 && hasPermission('sys:notice:list')) {
        buttons.push(h(NButton, { size: 'small',  onClick: () => handleShowSendLogs(row) }, { default: () => '通知记录' }))
      }
      if (hasPermission('sys:notice:edit')) {
        buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }))
        if (row.status === 0) {
          buttons.push(h(NButton, { size: 'small',  onClick: () => handlePublish(row) }, { default: () => '发布' }))
        }
      }
      if (hasPermission('sys:notice:delete')) {
        buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
      }
      return h(NSpace, null, { default: () => buttons })
    }
  }
]

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增通知')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)

const formData = reactive<SysNotice & { channels?: string[]; targetIds?: number[] }>({
  id: undefined,
  title: '',
  content: '',
  noticeType: 1,
  channels: ['station'],
  targetType: 3,
  targetIds: [],
  status: 0
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  noticeType: [{ required: true, type: 'number', message: '请选择通知类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }]
}

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<SysNotice | null>(null)

// 通知记录弹窗
const sendLogsVisible = ref(false)
const sendLogsNoticeId = ref<number>(0)
const sendLogsNoticeTitle = ref('')
const sendLogsData = ref<NoticeSendLog[]>([])
const sendLogsLoading = ref(false)
const retryingChannel = ref<string | null>(null)

const CHANNEL_NAMES: Record<string, string> = {
  station: '站内信',
  email: '邮件',
  dingtalk: '钉钉',
  feishu: '飞书',
  wechat_work: '企业微信'
}

const sendLogColumns: DataTableColumns<NoticeSendLog> = [
  {
    title: '推送渠道',
    key: 'channel',
    width: 100,
    render: (row) => CHANNEL_NAMES[row.channel] || row.channel
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
        size: 'small'
      }, { default: () => (row.status === 1 ? '成功' : '失败') })
  },
  {
    title: '目标/成功',
    key: 'count',
    width: 100,
    render: (row) => `${row.targetCount ?? 0} / ${row.successCount ?? 0}`
  },
  { title: '失败原因', key: 'errorMsg', ellipsis: { tooltip: true } },
  { title: '推送时间', key: 'sendTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) =>
      row.status === 2 && hasPermission('sys:notice:edit')
        ? h(NButton, {
            size: 'small',
            type: 'primary',
            loading: retryingChannel.value === row.channel,
            onClick: () => handleRetry(row.channel)
          }, { default: () => '重试' })
        : null
  }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await noticeApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      noticeType: searchForm.noticeType ?? undefined,
      status: searchForm.status ?? undefined
    })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
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
  searchForm.title = ''
  searchForm.noticeType = null
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

async function loadChannels() {
  try {
    channelOptions.value = await noticeApi.getChannels()
  } catch { channelOptions.value = [] }
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await userApi.page({ page: 1, pageSize: 500 })
    userOptions.value = (res.list || []).map((u: any) => ({ label: `${u.nickname || u.username} (${u.username})`, value: u.id }))
  } finally { usersLoading.value = false }
}

async function loadDepts() {
  try {
    const list = await deptApi.tree()
    const flatten = (nodes: any[]): { label: string; value: number }[] => {
      const result: { label: string; value: number }[] = []
      const walk = (items: any[], prefix = '') => {
        for (const n of items || []) {
          result.push({ label: prefix + (n.deptName || ''), value: n.id })
          if (n.children?.length) walk(n.children, prefix + '　')
        }
      }
      walk(list)
      return result
    }
    deptOptions.value = flatten(list || [])
  } catch { deptOptions.value = [] }
}

// 新增
async function handleAdd() {
  modalTitle.value = '新增通知'
  Object.assign(formData, {
    id: undefined,
    title: '',
    content: '',
    noticeType: 1,
    channels: ['station'],
    targetType: 3,
    targetIds: [],
    status: 0
  })
  await loadChannels()
  await loadUsers()
  await loadDepts()
  modalVisible.value = true
}

// 编辑
async function handleEdit(row: SysNotice) {
  modalTitle.value = '编辑通知'
  await loadChannels()
  await loadUsers()
  await loadDepts()
  try {
    const detail = await noticeApi.detail(row.id!)
    Object.assign(formData, detail)
    if (!formData.channels?.length) formData.channels = ['station']
    if (formData.targetType == null) formData.targetType = 3
    if (!formData.targetIds) formData.targetIds = []
    // 兼容旧数据：webhook 展开为 dingtalk、feishu、wechat_work
    if (formData.channels?.includes('webhook')) {
      formData.channels = formData.channels.filter((c: string) => c !== 'webhook')
      formData.channels.push(...['dingtalk', 'feishu', 'wechat_work'])
    }
    const webhookCodes = ['dingtalk', 'feishu', 'wechat_work']
    webhookExpanded.value = formData.channels?.some((c: string) => webhookCodes.includes(c)) ?? false
  } catch {
    Object.assign(formData, row)
  }
  modalVisible.value = true
}

// 查看
function handleView(row: SysNotice) {
  detailData.value = row
  detailVisible.value = true
}

// 通知记录
async function handleShowSendLogs(row: SysNotice) {
  sendLogsNoticeId.value = row.id!
  sendLogsNoticeTitle.value = row.title
  sendLogsVisible.value = true
  sendLogsLoading.value = true
  sendLogsData.value = []
  try {
    sendLogsData.value = await noticeApi.getSendLogs(row.id!)
  } catch {
    message.error('加载推送记录失败')
  } finally {
    sendLogsLoading.value = false
  }
}

async function loadSendLogs() {
  if (!sendLogsNoticeId.value) return
  try {
    sendLogsData.value = await noticeApi.getSendLogs(sendLogsNoticeId.value)
  } catch {
    message.error('加载推送记录失败')
  }
}

// 重试
async function handleRetry(channel: string) {
  if (!sendLogsNoticeId.value) return
  retryingChannel.value = channel
  try {
    await noticeApi.retry(sendLogsNoticeId.value, channel)
    message.success('重试成功')
    await loadSendLogs()
  } catch (e: any) {
    message.error(e?.message || '重试失败')
  } finally {
    retryingChannel.value = null
  }
}

// 发布
function handlePublish(row: SysNotice) {
  dialog.warning({
    title: '提示',
    content: '确定要发布该通知吗？将按配置的渠道和推送对象发送。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await noticeApi.publish(row.id!)
        message.success('发布成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const payload = {
      ...formData,
      channels: formData.channels?.length ? formData.channels : ['station'],
      targetIds: formData.targetType === 3 ? [] : (formData.targetIds || [])
    }
    if (formData.id) {
      await noticeApi.update(payload)
      message.success('更新成功')
    } else {
      await noticeApi.create(payload)
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
function handleDelete(row: SysNotice) {
  dialog.warning({
    title: '提示',
    content: `确定要删除通知"${row.title}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await noticeApi.delete(row.id!)
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
  loadChannels()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.notice-detail h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
}

.notice-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #999;
}

.notice-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

.webhook-sub-options {
  padding-left: 24px;
  margin-top: 8px;
}

.page-layout{
  height: calc(100vh - 160px);
}
</style>
