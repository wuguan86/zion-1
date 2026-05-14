<template>
  <div class="page-container">
    <!-- 操作栏 -->
    <n-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <n-input v-model:value="searchName" placeholder="搜索服务器名称" clearable style="width: 200px">
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          <n-select v-model:value="searchStatus" placeholder="状态" clearable style="width: 120px" :options="statusOptions" />
          <n-button type="primary" @click="loadServers">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            搜索
          </n-button>
        </div>
        <div class="toolbar-right">
          <n-button type="primary" @click="handleAdd">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增服务器
          </n-button>
        </div>
      </div>
    </n-card>

    <!-- 服务器卡片列表 -->
    <div class="server-grid">
      <n-card v-for="server in servers" :key="server.id" class="server-card" hoverable>
        <div class="server-header">
          <div class="server-icon" :class="{ online: server.status === 1 }">
            <n-icon size="28"><ServerOutline /></n-icon>
          </div>
          <div class="server-info">
            <div class="server-name">{{ server.name }}</div>
            <div class="server-host">{{ server.host }}:{{ server.port }}</div>
          </div>
          <n-tag :type="server.status === 1 ? 'success' : 'default'" size="small">
            {{ server.status === 1 ? '启用' : '禁用' }}
          </n-tag>
        </div>
        
        <div class="server-details">
          <div class="detail-item">
            <span class="label">用户名:</span>
            <span class="value">{{ server.username }}</span>
          </div>
          <div class="detail-item">
            <span class="label">认证方式:</span>
            <span class="value">{{ server.authType === 1 ? '密码' : '密钥' }}</span>
          </div>
          <div class="detail-item" v-if="server.lastConnectTime">
            <span class="label">最后连接:</span>
            <span class="value">{{ formatTime(server.lastConnectTime) }}</span>
          </div>
          <div class="detail-item" v-if="server.description">
            <span class="label">描述:</span>
            <span class="value">{{ server.description }}</span>
          </div>
        </div>

        <div class="server-actions">
          <n-button type="primary" size="small" @click="handleConnect(server)" :disabled="server.status !== 1">
            <template #icon><n-icon><TerminalOutline /></n-icon></template>
            连接
          </n-button>
          <n-button size="small" @click="handleTest(server)" :loading="testingId === server.id">
            <template #icon><n-icon><FlashOutline /></n-icon></template>
            测试
          </n-button>
          <n-button size="small" @click="handleEdit(server)">
            <template #icon><n-icon><CreateOutline /></n-icon></template>
            编辑
          </n-button>
          <n-popconfirm @positive-click="handleDelete(server.id!)">
            <template #trigger>
              <n-button size="small" type="error">
                <template #icon><n-icon><TrashOutline /></n-icon></template>
                删除
              </n-button>
            </template>
            确定删除该服务器吗？
          </n-popconfirm>
        </div>
      </n-card>

      <!-- 空状态 -->
      <div v-if="servers.length === 0 && !loading" class="empty-state">
        <n-empty description="暂无服务器">
          <template #extra>
            <n-button type="primary" @click="handleAdd">添加服务器</n-button>
          </template>
        </n-empty>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showModal" preset="card" :title="editingServer ? '编辑服务器' : '新增服务器'" style="width: 600px">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="100">
        <n-form-item label="服务器名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入服务器名称" />
        </n-form-item>
        <n-form-item label="服务器地址" path="host">
          <n-input v-model:value="formData.host" placeholder="请输入IP地址或域名" />
        </n-form-item>
        <n-form-item label="SSH端口" path="port">
          <n-input-number v-model:value="formData.port" :min="1" :max="65535" style="width: 100%" />
        </n-form-item>
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="formData.username" placeholder="请输入SSH用户名" />
        </n-form-item>
        <n-form-item label="认证方式" path="authType">
          <n-radio-group v-model:value="formData.authType">
            <n-radio :value="1">密码</n-radio>
            <n-radio :value="2">密钥</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="formData.authType === 1" label="密码" path="password">
          <n-input v-model:value="formData.password" type="password" show-password-on="click" :placeholder="editingServer ? '不修改请留空' : '请输入密码'" />
        </n-form-item>
        <n-form-item v-if="formData.authType === 2" label="私钥" path="privateKey">
          <n-input v-model:value="formData.privateKey" type="textarea" :rows="4" placeholder="请粘贴私钥内容" />
        </n-form-item>
        <n-form-item v-if="formData.authType === 2" label="私钥密码">
          <n-input v-model:value="formData.passphrase" type="password" show-password-on="click" placeholder="如有请输入" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="formData.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </n-form-item>
        <n-form-item label="排序">
          <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleTestForm" :loading="testingForm">测试连接</n-button>
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit" :loading="submitting">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- SSH 终端弹窗 -->
    <div v-if="showTerminal" class="terminal-modal" :class="{ fullscreen: isFullscreen }">
      <div class="terminal-modal-mask" @click="!isFullscreen && disconnectTerminal()"></div>
      <div class="terminal-modal-content">
        <div class="terminal-modal-header">
          <span class="terminal-title">SSH终端 - {{ connectingServer?.name }}</span>
          <div class="terminal-header-actions">
            <n-button quaternary circle size="small" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
              <template #icon>
                <n-icon size="18">
                  <ContractOutline v-if="isFullscreen" />
                  <ExpandOutline v-else />
                </n-icon>
              </template>
            </n-button>
            <n-button quaternary circle size="small" @click="disconnectTerminal" title="关闭">
              <template #icon>
                <n-icon size="18"><CloseOutline /></n-icon>
              </template>
            </n-button>
          </div>
        </div>
        <div class="terminal-modal-body">
          <div class="terminal-container" ref="terminalRef"></div>
        </div>
        <div class="terminal-modal-footer">
          <n-button @click="disconnectTerminal">断开连接</n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useMessage, type FormInst, type FormRules } from 'naive-ui'
import { SearchOutline, AddOutline, ServerOutline, TerminalOutline, FlashOutline, CreateOutline, TrashOutline, ExpandOutline, ContractOutline, CloseOutline } from '@vicons/ionicons5'
import { serverApi, type Server } from '@/api/server'
import { useUserStore } from '@/stores/user'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import '@xterm/xterm/css/xterm.css'

const message = useMessage()

// 列表数据
const servers = ref<Server[]>([])
const loading = ref(false)
const searchName = ref('')
const searchStatus = ref<number | null>(null)
const testingId = ref<number | null>(null)

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 表单数据
const showModal = ref(false)
const formRef = ref<FormInst | null>(null)
const editingServer = ref<Server | null>(null)
const submitting = ref(false)
const testingForm = ref(false)

const formData = ref<Server>({
  name: '',
  host: '',
  port: 22,
  username: 'root',
  authType: 1,
  password: '',
  privateKey: '',
  passphrase: '',
  description: '',
  status: 1,
  sort: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入服务器地址', trigger: 'blur' }],
  port: [{ required: true, type: 'number', message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  authType: [{ required: true, type: 'number', message: '请选择认证方式', trigger: 'change' }]
}

// 终端相关
const showTerminal = ref(false)
const terminalRef = ref<HTMLElement | null>(null)
const connectingServer = ref<Server | null>(null)
const isFullscreen = ref(false)
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null
let ws: WebSocket | null = null

// 切换全屏
function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  // 等待 DOM 更新后调整终端大小
  nextTick(() => {
    setTimeout(() => {
      if (fitAddon && terminal) {
        fitAddon.fit()
        // 发送新的终端尺寸到服务器
        ws?.send(JSON.stringify({
          type: 'resize',
          cols: terminal.cols,
          rows: terminal.rows
        }))
      }
    }, 100)
  })
}

// 加载服务器列表
async function loadServers() {
  loading.value = true
  try {
    const res = await serverApi.list({
      page: 1,
      pageSize: 100,
      name: searchName.value || undefined,
      status: searchStatus.value ?? undefined
    })
    servers.value = res.records || []
  } catch (error) {
    console.error('加载服务器列表失败', error)
  } finally {
    loading.value = false
  }
}

// 新增
function handleAdd() {
  editingServer.value = null
  formData.value = {
    name: '',
    host: '',
    port: 22,
    username: 'root',
    authType: 1,
    password: '',
    privateKey: '',
    passphrase: '',
    description: '',
    status: 1,
    sort: 0
  }
  showModal.value = true
}

// 编辑
function handleEdit(server: Server) {
  editingServer.value = server
  formData.value = { ...server, password: '', privateKey: '', passphrase: '' }
  showModal.value = true
}

// 删除
async function handleDelete(id: number) {
  try {
    await serverApi.remove(id)
    message.success('删除成功')
    loadServers()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 测试连接
async function handleTest(server: Server) {
  testingId.value = server.id!
  try {
    const success = await serverApi.testConnection(server.id!)
    if (success) {
      message.success('连接成功')
      loadServers()
    } else {
      message.error('连接失败')
    }
  } catch (error) {
    message.error('连接测试失败')
  } finally {
    testingId.value = null
  }
}

// 测试表单中的连接
async function handleTestForm() {
  testingForm.value = true
  try {
    const success = await serverApi.testConnectionByParams({
      host: formData.value.host,
      port: formData.value.port,
      username: formData.value.username,
      authType: formData.value.authType,
      password: formData.value.password,
      privateKey: formData.value.privateKey,
      passphrase: formData.value.passphrase
    })
    if (success) {
      message.success('连接成功')
    } else {
      message.error('连接失败，请检查配置')
    }
  } catch (error) {
    message.error('连接测试失败')
  } finally {
    testingForm.value = false
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true
    
    if (editingServer.value) {
      await serverApi.update({ ...formData.value, id: editingServer.value.id })
      message.success('修改成功')
    } else {
      await serverApi.add(formData.value)
      message.success('添加成功')
    }
    
    showModal.value = false
    loadServers()
  } catch (error) {
    // 表单验证失败或接口错误
  } finally {
    submitting.value = false
  }
}

// 连接终端
async function handleConnect(server: Server) {
  connectingServer.value = server
  showTerminal.value = true
  
  await nextTick()
  initTerminal(server)
}

// 初始化终端
function initTerminal(server: Server) {
  if (!terminalRef.value) return
  
  // 创建终端实例
  terminal = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Menlo, Monaco, "Courier New", monospace',
    theme: {
      background: '#1e1e1e',
      foreground: '#d4d4d4',
      cursor: '#ffffff',
      cursorAccent: '#1e1e1e',
      black: '#000000',
      red: '#cd3131',
      green: '#0dbc79',
      yellow: '#e5e510',
      blue: '#2472c8',
      magenta: '#bc3fbc',
      cyan: '#11a8cd',
      white: '#e5e5e5'
    }
  })
  
  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)
  terminal.open(terminalRef.value)
  fitAddon.fit()
  
  // 建立 WebSocket 连接
  // 开发环境直接连后端，生产环境使用相对路径
  const userStore = useUserStore()
  const token = userStore.token
  const isDev = import.meta.env.DEV
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = isDev ? 'localhost:8080' : location.host
  const wsUrl = `${wsProtocol}//${wsHost}/ws/ssh?token=${token}`
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    // 发送连接请求
    ws?.send(JSON.stringify({
      type: 'connect',
      serverId: server.id,
      cols: terminal?.cols || 80,
      rows: terminal?.rows || 24
    }))
  }
  
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)
    if (msg.type === 'data') {
      terminal?.write(msg.data)
    } else if (msg.type === 'connected') {
      terminal?.write('\r\n\x1b[32m连接成功！\x1b[0m\r\n\r\n')
    } else if (msg.type === 'error') {
      terminal?.write(`\r\n\x1b[31m错误: ${msg.message}\x1b[0m\r\n`)
    }
  }
  
  ws.onclose = () => {
    terminal?.write('\r\n\x1b[33m连接已断开\x1b[0m\r\n')
  }
  
  ws.onerror = () => {
    terminal?.write('\r\n\x1b[31mWebSocket连接错误\x1b[0m\r\n')
  }
  
  // 终端输入
  terminal.onData((data) => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'data', data }))
    }
  })
  
  // 终端大小变化
  terminal.onResize(({ cols, rows }) => {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'resize', cols, rows }))
    }
  })
  
  // 窗口大小变化时调整终端
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
function handleResize() {
  fitAddon?.fit()
}

// 断开终端连接
function disconnectTerminal() {
  if (ws) {
    ws.close()
    ws = null
  }
  if (terminal) {
    terminal.dispose()
    terminal = null
  }
  fitAddon = null
  showTerminal.value = false
  connectingServer.value = null
  isFullscreen.value = false
  window.removeEventListener('resize', handleResize)
}

// 格式化时间
function formatTime(time: string) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadServers()
})

onUnmounted(() => {
  disconnectTerminal()
})
</script>

<style lang="scss" scoped>
.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.server-card {
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }
}

.server-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.server-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;

  &.online {
    background: #d1fae5;
    color: #059669;
  }
}

.server-info {
  flex: 1;
}

.server-name {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.server-host {
  font-size: 13px;
  color: #6b7280;
  font-family: monospace;
}

.server-details {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  font-size: 13px;
  margin-bottom: 6px;

  &:last-child {
    margin-bottom: 0;
  }

  .label {
    color: #6b7280;
    width: 80px;
    flex-shrink: 0;
  }

  .value {
    color: #374151;
  }
}

.server-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

// 终端弹窗
.terminal-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.terminal-modal-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
}

.terminal-modal-content {
  position: relative;
  width: 900px;
  height: 600px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.terminal-modal.fullscreen .terminal-modal-content {
  width: 100vw;
  height: 100vh;
  border-radius: 0;
}

.terminal-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.terminal-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.terminal-header-actions {
  display: flex;
  gap: 4px;
}

.terminal-modal-body {
  flex: 1;
  padding: 12px;
  overflow: hidden;
  min-height: 0;
}

.terminal-container {
  width: 100%;
  height: 100%;
  background: #1e1e1e;
  border-radius: 6px;
  overflow: hidden;
}

.terminal-modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

// 暗黑主题
:global(body.dark-theme) {
  .server-icon {
    background: #27272a;
    color: #a1a1aa;

    &.online {
      background: #064e3b;
      color: #34d399;
    }
  }

  .server-name {
    color: #e4e4e7;
  }

  .server-host {
    color: #71717a;
  }

  .server-details {
    background: #27272a;
  }

  .detail-item {
    .label {
      color: #71717a;
    }

    .value {
      color: #a1a1aa;
    }
  }

  .terminal-modal-content {
    background: #18181c;
  }

  .terminal-modal-header {
    border-bottom-color: #3f3f46;
  }

  .terminal-title {
    color: #e4e4e7;
  }

  .terminal-modal-footer {
    border-top-color: #3f3f46;
  }
}
</style>

<style lang="scss">
body.dark-theme .server-icon {
  background: #27272a;
  color: #a1a1aa;
}

body.dark-theme .server-icon.online {
  background: #064e3b;
  color: #34d399;
}

body.dark-theme .server-name {
  color: #e4e4e7;
}

body.dark-theme .server-host {
  color: #71717a;
}

body.dark-theme .server-details {
  background: #27272a;
}

body.dark-theme .detail-item .label {
  color: #71717a;
}

body.dark-theme .detail-item .value {
  color: #a1a1aa;
}

body.dark-theme .terminal-modal-content {
  background: #18181c;
}

body.dark-theme .terminal-modal-header {
  border-bottom-color: #3f3f46;
}

body.dark-theme .terminal-title {
  color: #e4e4e7;
}

body.dark-theme .terminal-modal-footer {
  border-top-color: #3f3f46;
}
</style>
