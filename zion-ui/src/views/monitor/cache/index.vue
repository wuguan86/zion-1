<template>
  <div class="page-container">
    <!-- 统计图表 -->
    <div class="stats-section">
      <n-grid :cols="2" :x-gap="16" :y-gap="16">
        <n-gi>
          <n-card title="内存使用" size="small" :bordered="false" content-style="background: transparent">
            <div ref="memoryChartRef" class="chart-box"></div>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="QPS 折线图" size="small" :bordered="false" content-style="background: transparent">
            <div ref="qpsChartRef" class="chart-box"></div>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="命中率趋势" size="small" :bordered="false" content-style="background: transparent">
            <div ref="hitRateChartRef" class="chart-box"></div>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="连接数趋势" size="small" :bordered="false" content-style="background: transparent">
            <div ref="clientsChartRef" class="chart-box"></div>
          </n-card>
        </n-gi>
      </n-grid>
    </div>

    <n-card title="缓存键列表" style="margin-top: 16px">
      <div class="search-form">
        <n-form inline label-placement="left">
          <n-form-item label="键名模式">
            <n-input v-model:value="searchPattern" placeholder="请输入键名模式" clearable style="width: 300px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="loadKeys">
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              搜索
            </n-button>
          </n-form-item>
        </n-form>
      </div>

      <n-data-table :columns="columns" :data="keys" :loading="keysLoading" :row-key="(row: string) => row" />
    </n-card>

    <n-modal v-model:show="detailVisible" preset="card" title="缓存详情" style="width: 800px">
      <n-descriptions :column="1" label-placement="left" bordered>
        <n-descriptions-item label="键名">
          {{ cacheDetail.key }}
        </n-descriptions-item>
        <n-descriptions-item label="类型">
          <n-tag type="info">{{ cacheDetail.type }}</n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="有效期">
          {{ formatTTL(cacheDetail.ttl) }}
        </n-descriptions-item>
        <n-descriptions-item label="值">
          <n-scrollbar style="max-height: 400px">
            <n-code :code="formatValue(cacheDetail.value)" language="json" word-wrap />
          </n-scrollbar>
        </n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, onUnmounted } from 'vue'
import { NButton, NSpace, NTag, NInput, NForm, NFormItem, NIcon, NCard, NGrid, NGi, NDescriptions, NDescriptionsItem, NDataTable, NModal, NScrollbar, NCode, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { SearchOutline, EyeOutline, TrashOutline } from '@vicons/ionicons5'
import { cacheApi } from '@/api/monitor'

const message = useMessage()
const dialog = useDialog()

const keys = ref<string[]>([])
const keysLoading = ref(false)
const searchPattern = ref('*')

const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })

const detailVisible = ref(false)
const cacheDetail = ref<{ key: string; type: string; value: string; ttl: number }>({
  key: '', type: '', value: '', ttl: -1
})

const columns: DataTableColumns<string> = [
  { title: '键名', key: 'key', render(row) { return h('span', { style: 'word-break: break-all;' }, row) }},
  { title: '操作', key: 'actions', width: 180, render(row) {
    return h(NSpace, null, {
      default: () => [
        h(NButton, { size: 'small', quaternary: true, type: 'primary', onClick: () => handleView(row) }, {
          default: () => [h(NIcon, null, { default: () => h(EyeOutline) }), ' 查看']
        }),
        h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, {
          default: () => [h(NIcon, null, { default: () => h(TrashOutline) }), ' 删除']
        })
      ]
    })
  }}
]

// 图表
const memoryChartRef = ref<HTMLElement | null>(null)
const qpsChartRef = ref<HTMLElement | null>(null)
const hitRateChartRef = ref<HTMLElement | null>(null)
const clientsChartRef = ref<HTMLElement | null>(null)
let memoryChart: any = null
let qpsChart: any = null
let hitRateChart: any = null
let clientsChart: any = null

const timeLabels: string[] = []
const qpsData: number[] = []
const hitRateData: number[] = []
const clientsData: number[] = []
const MAX_POINTS = 20

let timer: ReturnType<typeof setInterval> | null = null

async function loadStats() {
  try {
    const stats = await cacheApi.stats()
    updateCharts(stats)
  } catch { /* ignore */ }
}

function updateCharts(stats: { usedMemory: number; maxMemory: number; ops: number; hitRate: number; connectedClients: number }) {
  const now = new Date().toLocaleTimeString()
  timeLabels.push(now)
  if (timeLabels.length > MAX_POINTS) timeLabels.shift()

  qpsData.push(stats.ops)
  if (qpsData.length > MAX_POINTS) qpsData.shift()

  hitRateData.push(Math.round(stats.hitRate * 100))
  if (hitRateData.length > MAX_POINTS) hitRateData.shift()

  clientsData.push(stats.connectedClients)
  if (clientsData.length > MAX_POINTS) clientsData.shift()

  import('echarts').then((echarts) => {
    if (!memoryChartRef.value) return
    if (!memoryChart) memoryChart = echarts.init(memoryChartRef.value)
    const used = stats.usedMemory
    const max = stats.maxMemory || used || 1
    const usedPercent = max > 0 ? Math.min((used / max) * 100, 100) : 0
    memoryChart.setOption({
      tooltip: { formatter: '{b}: {c}%' },
      series: [{
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: () => `已用 ${usedPercent.toFixed(1)}%` },
        data: [
          { value: usedPercent, name: '已用', itemStyle: { color: '#18a058' } },
          { value: 100 - usedPercent, name: '可用', itemStyle: { color: '#e5e7eb' } }
        ]
      }]
    })

    if (!qpsChart && qpsChartRef.value) qpsChart = echarts.init(qpsChartRef.value)
    if (qpsChart) {
      qpsChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: timeLabels },
        yAxis: { type: 'value', name: 'QPS' },
        series: [{ data: qpsData, type: 'line', smooth: true, areaStyle: {}, itemStyle: { color: '#18a058' } }]
      })
    }

    if (!hitRateChart && hitRateChartRef.value) hitRateChart = echarts.init(hitRateChartRef.value)
    if (hitRateChart) {
      hitRateChart.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
        xAxis: { type: 'category', data: timeLabels },
        yAxis: { type: 'value', name: '命中率%', min: 0, max: 100 },
        series: [{ data: hitRateData, type: 'line', smooth: true, areaStyle: {}, itemStyle: { color: '#2080f0' } }]
      })
    }

    if (!clientsChart && clientsChartRef.value) clientsChart = echarts.init(clientsChartRef.value)
    if (clientsChart) {
      clientsChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: timeLabels },
        yAxis: { type: 'value', name: '连接数' },
        series: [{ data: clientsData, type: 'line', smooth: true, areaStyle: {}, itemStyle: { color: '#f0a020' } }]
      })
    }
  }).catch(() => {})
}

async function loadKeys() {
  keysLoading.value = true
  try {
    const res = await cacheApi.keys(searchPattern.value) || []
    keys.value = res
    pagination.itemCount = res.length
    pagination.page = 1
  } finally { keysLoading.value = false }
}

async function handleView(key: string) {
  detailVisible.value = true
  try {
    const res = await cacheApi.getValue(key)
    cacheDetail.value = { key, type: res.type || 'unknown', value: res.value, ttl: res.ttl ?? -1 }
  } catch {
    message.error('获取详情失败')
    detailVisible.value = false
  }
}

function handleDelete(key: string) {
  dialog.warning({
    title: '提示', content: `确定要删除缓存"${key}"吗？`, positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      await cacheApi.delete(key)
      message.success('删除成功')
      keys.value = keys.value.filter(k => k !== key)
      pagination.itemCount = keys.value.length
    }
  })
}

function formatTTL(ttl: number) {
  if (ttl === -1) return '永久有效'
  if (ttl === -2) return '已过期'
  return `${ttl} 秒`
}

function formatValue(value: any) {
  if (!value) return ''
  if (typeof value === 'string') {
    try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value }
  }
  return JSON.stringify(value, null, 2)
}

onMounted(() => {
  loadKeys()
  loadStats()
  timer = setInterval(loadStats, 3000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  memoryChart?.dispose()
  qpsChart?.dispose()
  hitRateChart?.dispose()
  clientsChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-section { margin-bottom: 16px; }
.chart-box { height: 260px; }
</style>
