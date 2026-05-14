import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wsManager } from '@/utils/websocket'

export interface MessageNotification {
  id: number
  type: 'notice' | 'chat'
  title: string
  content: string
  time: number
  read: boolean
  senderId?: number  // 私聊消息的发送者ID
  groupId?: number   // 群聊消息的群ID
}

export const useMessageStore = defineStore('message', () => {
  // 未读通知数量
  const noticeCount = ref(0)
  // 未读聊天数量
  const chatCount = ref(0)
  // 通知列表（最近的）
  const notifications = ref<MessageNotification[]>([])
  // 是否显示通知弹窗
  const showNotification = ref(false)
  // 当前通知
  const currentNotification = ref<MessageNotification | null>(null)

  // 初始化WebSocket监听
  function initWebSocket() {
    // 监听通知消息
    wsManager.on('notice', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'notice',
        title: data.title || '系统通知',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false
      }
      addNotification(notification)
      noticeCount.value++
    })

    // 监听聊天消息（私聊）
    wsManager.on('chat', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'chat',
        title: data.senderName || '新消息',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false,
        senderId: data.senderId
      }
      addNotification(notification)
      chatCount.value++
    })
    
    // 监听群聊消息
    wsManager.on('groupChat', (data) => {
      const notification: MessageNotification = {
        id: Date.now(),
        type: 'chat',
        title: data.senderName ? `${data.senderName}(群消息)` : '群消息',
        content: data.content || '',
        time: data.time || Date.now(),
        read: false,
        groupId: data.groupId
      }
      addNotification(notification)
      chatCount.value++
    })

    // 监听未读数量更新
    wsManager.on('unread', (data) => {
      noticeCount.value = data.noticeCount || 0
      chatCount.value = data.chatCount || 0
    })

    // 连接WebSocket
    wsManager.connect()
  }

  // 添加通知
  function addNotification(notification: MessageNotification) {
    notifications.value.unshift(notification)
    // 最多保留20条
    if (notifications.value.length > 20) {
      notifications.value.pop()
    }
    // 显示通知弹窗
    currentNotification.value = notification
    showNotification.value = true
    // 3秒后自动关闭
    setTimeout(() => {
      if (currentNotification.value?.id === notification.id) {
        showNotification.value = false
      }
    }, 5000)
  }

  // 设置未读数量
  function setUnreadCount(notice: number, chat: number) {
    noticeCount.value = notice
    chatCount.value = chat
  }

  // 清除通知未读
  function clearNoticeCount() {
    noticeCount.value = 0
  }

  // 清除聊天未读
  function clearChatCount() {
    chatCount.value = 0
  }

  // 关闭通知弹窗
  function closeNotification() {
    showNotification.value = false
    currentNotification.value = null
  }

  // 断开WebSocket
  function disconnectWebSocket() {
    wsManager.disconnect()
  }

  // 总未读数
  const totalUnread = () => noticeCount.value + chatCount.value

  return {
    noticeCount,
    chatCount,
    notifications,
    showNotification,
    currentNotification,
    initWebSocket,
    addNotification,
    setUnreadCount,
    clearNoticeCount,
    clearChatCount,
    closeNotification,
    disconnectWebSocket,
    totalUnread
  }
})
