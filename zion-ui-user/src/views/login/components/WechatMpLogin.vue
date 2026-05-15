<script setup lang="ts">
import { computed, onMounted, onUnmounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { QrCode, Refresh } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { WechatQrLoginSession } from '@/types/login'

const emit = defineEmits<{
  back: []
}>()

const router = useRouter()
const userStore = useUserStore()

const loading = shallowRef(false)
const polling = shallowRef(false)
const errorMsg = shallowRef('')
const session = shallowRef<WechatQrLoginSession | null>(null)

let pollTimer: number | null = null

const qrCodeUrl = computed(() => session.value?.qrCodeUrl || '')
const statusText = computed(() => {
  if (loading.value) return '正在生成二维码'
  if (polling.value) return '请使用手机微信扫码登录'
  if (session.value) return '二维码已失效，请刷新'
  return '点击刷新生成二维码'
})

function stopPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
  polling.value = false
}

async function loadQrCode() {
  stopPolling()
  loading.value = true
  errorMsg.value = ''
  try {
    session.value = await authApi.createWechatQrLoginSession()
    polling.value = true
    pollTimer = window.setInterval(pollLoginStatus, 2000)
  } catch (error: any) {
    session.value = null
    errorMsg.value = error?.message || '二维码生成失败'
  } finally {
    loading.value = false
  }
}

async function pollLoginStatus() {
  if (!session.value || !polling.value) return
  try {
    const result = await authApi.pollWechatQrLogin(session.value.sessionId)
    if (result.status === 'CONFIRMED' && result.loginResult) {
      stopPolling()
      await userStore.applyLoginResult(result.loginResult)
      router.replace('/home')
      return
    }
    if (result.status === 'EXPIRED') {
      stopPolling()
      errorMsg.value = '二维码已失效，请刷新后重试'
    }
  } catch (error: any) {
    stopPolling()
    errorMsg.value = error?.message || '扫码状态查询失败'
  }
}

onMounted(() => {
  loadQrCode()
})

onUnmounted(() => {
  stopPolling()
})
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

    <div class="wechat-area">
      <div class="qr-frame">
        <n-spin v-if="loading" size="medium" />
        <img v-else-if="qrCodeUrl" class="qr-image" :src="qrCodeUrl" alt="微信扫码登录二维码">
        <n-icon v-else size="56" color="#9ca3af"><QrCode /></n-icon>
      </div>

      <p class="desc">{{ statusText }}</p>

      <n-button
        secondary
        size="large"
        block
        :loading="loading"
        @click="loadQrCode"
      >
        <template #icon>
          <n-icon><Refresh /></n-icon>
        </template>
        刷新二维码
      </n-button>
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
  margin-bottom: 28px;
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

.wechat-area {
  text-align: center;
}

.qr-frame {
  width: 216px;
  height: 216px;
  margin: 0 auto 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qr-image {
  width: 196px;
  height: 196px;
  object-fit: contain;
}

.desc {
  min-height: 20px;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  text-align: center;
}
</style>
