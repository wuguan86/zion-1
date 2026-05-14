<template>
  <div class="page-container">
    <n-grid :cols="2" :x-gap="16" :y-gap="16">
      <n-gi>
        <n-card title="CPU信息">
          <n-descriptions :column="1" label-placement="left">
            <n-descriptions-item label="操作系统">{{ serverInfo.cpu?.name }}</n-descriptions-item>
            <n-descriptions-item label="系统架构">{{ serverInfo.cpu?.arch }}</n-descriptions-item>
            <n-descriptions-item label="CPU核心数">{{ serverInfo.cpu?.availableProcessors }}</n-descriptions-item>
            <n-descriptions-item label="系统负载">{{ serverInfo.cpu?.systemLoadAverage?.toFixed(2) }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="内存信息">
          <n-descriptions :column="1" label-placement="left">
            <n-descriptions-item label="堆初始内存">{{ serverInfo.memory?.heapInit }}</n-descriptions-item>
            <n-descriptions-item label="堆已用内存">{{ serverInfo.memory?.heapUsed }}</n-descriptions-item>
            <n-descriptions-item label="堆最大内存">{{ serverInfo.memory?.heapMax }}</n-descriptions-item>
            <n-descriptions-item label="非堆已用内存">{{ serverInfo.memory?.nonHeapUsed }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="JVM信息">
          <n-descriptions :column="1" label-placement="left">
            <n-descriptions-item label="JVM名称">{{ serverInfo.jvm?.name }}</n-descriptions-item>
            <n-descriptions-item label="JVM版本">{{ serverInfo.jvm?.version }}</n-descriptions-item>
            <n-descriptions-item label="启动时间">{{ serverInfo.jvm?.startTime }}</n-descriptions-item>
            <n-descriptions-item label="运行时长">{{ serverInfo.jvm?.uptime }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="服务器信息">
          <n-descriptions :column="1" label-placement="left">
            <n-descriptions-item label="服务器名称">{{ serverInfo.sys?.hostName }}</n-descriptions-item>
            <n-descriptions-item label="服务器IP">{{ serverInfo.sys?.hostAddress }}</n-descriptions-item>
            <n-descriptions-item label="操作系统">{{ serverInfo.sys?.osName }}</n-descriptions-item>
            <n-descriptions-item label="系统版本">{{ serverInfo.sys?.osVersion }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="磁盘信息" style="margin-top: 16px">
      <n-data-table :columns="diskColumns" :data="serverInfo.disks || []" :row-key="(row: any) => row.path" />
    </n-card>

    <n-card title="实时监控图表" style="margin-top: 16px">
      <n-grid :cols="2" :x-gap="16">
        <n-gi>
          <div ref="cpuChartRef" style="height: 300px"></div>
        </n-gi>
        <n-gi>
          <div ref="memoryChartRef" style="height: 300px"></div>
        </n-gi>
      </n-grid>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, onUnmounted } from 'vue'
import { NProgress, type DataTableColumns } from 'naive-ui'
import { serverApi } from '@/api/monitor'

const serverInfo = ref<any>({})
const cpuChartRef = ref<HTMLElement | null>(null)
const memoryChartRef = ref<HTMLElement | null>(null)

let timer: ReturnType<typeof setInterval> | null = null
let cpuChart: any = null
let memoryChart: any = null
const cpuData: number[] = []
const memoryData: number[] = []
const timeLabels: string[] = []

const diskColumns: DataTableColumns<any> = [
  { title: '盘符路径', key: 'path', width: 100 },
  { title: '总大小', key: 'total', width: 120 },
  { title: '可用大小', key: 'free', width: 120 },
  { title: '已用大小', key: 'usable', width: 120 },
  { title: '使用率', key: 'usedPercent', width: 200, render(row) {
    const percent = parseFloat(row.usedPercent) || 0
    return h(NProgress, { type: 'line', percentage: percent, indicatorPlacement: 'inside', processing: percent > 80 })
  }}
]

async function loadServerInfo() {
  try {
    serverInfo.value = await serverApi.info()
    updateCharts()
  } catch { /* ignore */ }
}

function updateCharts() {
  if (typeof window === 'undefined') return
  
  const now = new Date().toLocaleTimeString()
  timeLabels.push(now)
  if (timeLabels.length > 10) timeLabels.shift()

  // CPU负载
  const cpuLoad = serverInfo.value.cpu?.systemLoadAverage || 0
  cpuData.push(Math.min(cpuLoad * 10, 100)) // 放大显示
  if (cpuData.length > 10) cpuData.shift()

  // 内存使用率
  const heapUsed = parseInt(serverInfo.value.memory?.heapUsed?.replace(/[^\d]/g, '') || '0')
  const heapMax = parseInt(serverInfo.value.memory?.heapMax?.replace(/[^\d]/g, '') || '1')
  const memPercent = heapMax > 0 ? (heapUsed / heapMax) * 100 : 0
  memoryData.push(memPercent)
  if (memoryData.length > 10) memoryData.shift()

  // 动态加载 ECharts
  if (!cpuChart && cpuChartRef.value) {
    import('echarts').then((echarts) => {
      cpuChart = echarts.init(cpuChartRef.value!)
      memoryChart = echarts.init(memoryChartRef.value!)
      renderCharts()
    }).catch(() => {})
  } else {
    renderCharts()
  }
}

function renderCharts() {
  if (!cpuChart || !memoryChart) return

  cpuChart.setOption({
    title: { text: 'CPU负载', left: 'center' },
    xAxis: { type: 'category', data: timeLabels },
    yAxis: { type: 'value', max: 100 },
    series: [{ data: cpuData, type: 'line', smooth: true, areaStyle: {} }]
  })

  memoryChart.setOption({
    title: { text: '内存使用率 (%)', left: 'center' },
    xAxis: { type: 'category', data: timeLabels },
    yAxis: { type: 'value', max: 100 },
    series: [{ data: memoryData, type: 'line', smooth: true, areaStyle: {} }]
  })
}

onMounted(() => {
  loadServerInfo()
  timer = setInterval(loadServerInfo, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  cpuChart?.dispose()
  memoryChart?.dispose()
})
</script>
