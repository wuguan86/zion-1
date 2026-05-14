<template>
  <Transition name="slide-up">
    <div v-if="messageStore.showNotification && messageStore.currentNotification" class="notification-popup">
      <div class="notification-header">
        <div class="notification-icon">
          <n-icon v-if="messageStore.currentNotification.type === 'notice'" size="20">
            <NotificationsOutline />
          </n-icon>
          <n-icon v-else size="20">
            <ChatbubbleOutline />
          </n-icon>
        </div>
        <span class="notification-title">{{ messageStore.currentNotification.title }}</span>
        <n-button quaternary circle size="tiny" @click="messageStore.closeNotification()">
          <template #icon>
            <n-icon><CloseOutline /></n-icon>
          </template>
        </n-button>
      </div>
      <div class="notification-content">
        {{ messageStore.currentNotification.content }}
      </div>
      <div class="notification-footer">
        <span class="notification-time">{{ formatTime(messageStore.currentNotification.time) }}</span>
        <n-button text type="primary" size="small" @click="handleView">
          查看详情
        </n-button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NotificationsOutline, ChatbubbleOutline, CloseOutline } from '@vicons/ionicons5'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const messageStore = useMessageStore()

function formatTime(timestamp: number | string | undefined): string {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return ''
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

function handleView() {
  const notification = messageStore.currentNotification
  messageStore.closeNotification()
  if (notification?.type === 'notice') {
    router.push('/message/notice')
  } else {
    // 如果有群ID，跳转到群聊
    if (notification?.groupId) {
      router.push({ path: '/message/chat', query: { groupId: notification.groupId.toString() } })
    }
    // 如果有发送者ID，跳转到私聊
    else if (notification?.senderId) {
      router.push({ path: '/message/chat', query: { userId: notification.senderId.toString() } })
    } else {
      router.push('/message/chat')
    }
  }
}
</script>

<style scoped>
.notification-popup {
  position: fixed;
  left: 20px;
  bottom: 20px;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  overflow: hidden;
}

.notification-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  gap: 8px;
}

.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #e8f5e9;
  border-radius: 50%;
  color: #18a058;
}

.notification-title {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-content {
  padding: 12px 16px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 12px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

/* 动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
