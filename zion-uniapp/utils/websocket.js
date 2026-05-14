/**
 * WebSocket 客户端
 * 管理 WebSocket 连接，处理实时消息
 */
import { BASE_URL } from './request.js'
import { getToken } from './auth.js'

class WebSocketClient {
  constructor() {
    this.socketTask = null
    this.isConnected = false
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.listeners = {}
    this.reconnectCount = 0
    this.maxReconnect = 10
  }

  /**
   * 建立连接
   */
  connect() {
    if (this.isConnected || this.socketTask) return

    const token = getToken()
    if (!token) return

    // 将http替换为ws
    const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://')
    const url = `${wsUrl}/ws/message?token=${token}`

    this.socketTask = uni.connectSocket({
      url,
      success: () => {
        console.log('[WS] 正在连接...')
      },
      fail: (err) => {
        console.error('[WS] 连接失败:', err)
        this.reconnect()
      }
    })

    this.socketTask.onOpen(() => {
      console.log('[WS] 连接成功')
      this.isConnected = true
      this.reconnectCount = 0
      this.startHeartbeat()
      this.emit('open')
    })

    this.socketTask.onMessage((res) => {
      try {
        const data = JSON.parse(res.data)
        console.log('[WS] 收到消息:', data)

        // 分发消息
        if (data.type === 'chat') {
          this.emit('chat', data)
        } else if (data.type === 'groupChat') {
          this.emit('groupChat', data)
        } else if (data.type === 'notice') {
          this.emit('notice', data)
        } else if (data.type === 'pong') {
          // 心跳响应
        } else {
          this.emit('message', data)
        }
      } catch (e) {
        console.log('[WS] 消息解析失败:', res.data)
      }
    })

    this.socketTask.onClose(() => {
      console.log('[WS] 连接关闭')
      this.isConnected = false
      this.socketTask = null
      this.stopHeartbeat()
      this.emit('close')
      this.reconnect()
    })

    this.socketTask.onError((err) => {
      console.error('[WS] 错误:', err)
      this.isConnected = false
      this.emit('error', err)
    })
  }

  /**
   * 发送消息
   */
  send(data) {
    if (!this.isConnected || !this.socketTask) {
      console.warn('[WS] 未连接，无法发送')
      return false
    }
    const msg = typeof data === 'string' ? data : JSON.stringify(data)
    this.socketTask.send({ data: msg })
    return true
  }

  /**
   * 断线重连
   */
  reconnect() {
    if (this.reconnectCount >= this.maxReconnect) {
      console.log('[WS] 超过最大重连次数')
      return
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    const delay = Math.min(3000 * Math.pow(1.5, this.reconnectCount), 30000)
    this.reconnectCount++

    console.log(`[WS] ${delay / 1000}s 后第 ${this.reconnectCount} 次重连`)
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  /**
   * 心跳检测
   */
  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.send({ type: 'ping' })
      }
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 关闭连接
   */
  close() {
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.maxReconnect = 0 // 阻止重连
    if (this.socketTask) {
      this.socketTask.close()
      this.socketTask = null
    }
    this.isConnected = false
  }

  /**
   * 注册事件监听
   */
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  }

  /**
   * 移除事件监听
   */
  off(event, callback) {
    if (!this.listeners[event]) return
    if (!callback) {
      this.listeners[event] = []
    } else {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(cb => cb(data))
    }
  }
}

// 单例
const wsClient = new WebSocketClient()
export default wsClient
