<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { QrCode } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { loading, errorMsg, login } = useLogin()

const qrUrl = ref('')
const ticket = ref('')
const statusText = ref('正在加载二维码...')
const isExpired = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await loadQrcode()
})

onUnmounted(() => {
  stopPolling()
})

async function loadQrcode() {
  isExpired.value = false
  statusText.value = '正在加载二维码...'
  try {
    const res = await authApi.getWechatQrcode()
    qrUrl.value = res.qrUrl
    ticket.value = res.ticket
    statusText.value = '请使用微信扫一扫'
    startPolling()
  } catch (e: any) {
    statusText.value = '加载二维码失败：' + (e.message || '未知错误')
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await authApi.getWechatStatus(ticket.value)
      switch (res.status) {
        case 'scanned':
          statusText.value = '扫码成功，请在手机上确认登录'
          break
        case 'confirmed':
          statusText.value = '登录中...'
          stopPolling()
          await login({
            loginType: 'social',
            platform: 'wechat_open',
            authCode: ticket.value
          })
          break
        case 'cancelled':
          statusText.value = '已取消，可重新扫码'
          stopPolling()
          break
        case 'expired':
          statusText.value = '二维码已过期'
          isExpired.value = true
          stopPolling()
          break
      }
    } catch {
      // ignore poll errors
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
</script>

<template>
  <div class="wechat-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #4facfe">
        <n-icon size="28" color="#fff"><QrCode /></n-icon>
      </div>
      <h2>微信扫码登录</h2>
    </div>

    <div class="qrcode-area">
      <div class="qrcode-wrapper" :class="{ expired: isExpired }">
        <img v-if="qrUrl" :src="qrUrl" alt="微信扫码" class="qrcode-img" />
        <n-spin v-else size="large" />
        <div v-if="isExpired" class="qrcode-overlay" @click="loadQrcode">
          <n-icon size="32">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </n-icon>
          <span>点击刷新</span>
        </div>
      </div>
      <p class="status-text" :class="{ error: isExpired }">{{ statusText }}</p>
    </div>

    <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
  </div>
</template>

<style lang="scss" scoped>
.wechat-login {
  max-width: 400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s;

  &:hover {
    color: #374151;
  }
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.qrcode-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-wrapper {
  width: 220px;
  height: 220px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #fff;

  &.expired {
    .qrcode-img {
      filter: blur(4px) opacity(0.3);
    }
  }
}

.qrcode-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qrcode-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  color: #4facfe;
  font-size: 14px;
  font-weight: 500;
}

.status-text {
  margin-top: 16px;
  font-size: 14px;
  color: #6b7280;
  text-align: center;

  &.error {
    color: #ef4444;
  }
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  text-align: center;
}
</style>
