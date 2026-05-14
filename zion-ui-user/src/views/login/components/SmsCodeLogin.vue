<script setup lang="ts">
import { ref } from 'vue'
import { PhonePortrait } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { loading, errorMsg, login } = useLogin()

const form = ref({
  phone: '',
  smsCode: ''
})

const smsLoading = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

async function sendSmsCode() {
  if (countdown.value > 0 || !form.value.phone) return
  smsLoading.value = true
  try {
    await authApi.sendSmsCode(form.value.phone)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e: any) {
    // error handled by interceptor
  } finally {
    smsLoading.value = false
  }
}

async function handleSubmit() {
  await login({
    loginType: 'sms',
    phone: form.value.phone,
    smsCode: form.value.smsCode
  })
}
</script>

<template>
  <div class="sms-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #f093fb">
        <n-icon size="28" color="#fff"><PhonePortrait /></n-icon>
      </div>
      <h2>短信验证码登录</h2>
    </div>

    <n-form @submit.prevent="handleSubmit">
      <n-form-item>
        <n-input
          v-model:value="form.phone"
          placeholder="请输入手机号"
          size="large"
          maxlength="11"
        >
          <template #prefix>
            <n-icon><PhonePortrait /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <div class="sms-row">
          <n-input
            v-model:value="form.smsCode"
            placeholder="验证码"
            size="large"
            maxlength="6"
          />
          <n-button
            size="large"
            :loading="smsLoading"
            :disabled="countdown > 0 || !form.phone"
            @click="sendSmsCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </n-button>
        </div>
      </n-form-item>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <n-button
        type="primary"
        size="large"
        block
        attr-type="submit"
        :loading="loading"
        :disabled="!form.phone || !form.smsCode"
      >
        登录
      </n-button>
    </n-form>
  </div>
</template>

<style lang="scss" scoped>
.sms-login {
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

.sms-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}
</style>
