<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { LockClosed, Person } from '@vicons/ionicons5'
import { useCaptcha } from '@/composables/useCaptcha'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { captchaUuid, captchaImg, refreshCaptcha } = useCaptcha()
const { loading, errorMsg, login } = useLogin()

const form = ref({
  username: '',
  password: '',
  captcha: ''
})

onMounted(() => {
  refreshCaptcha()
})

async function handleSubmit() {
  await login({
    loginType: 'password',
    username: form.value.username,
    password: form.value.password,
    uuid: captchaUuid.value,
    code: form.value.captcha
  })
  if (!errorMsg.value) {
    refreshCaptcha()
  }
}
</script>

<template>
  <div class="password-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #667eea">
        <n-icon size="28" color="#fff"><Person /></n-icon>
      </div>
      <h2>账号密码登录</h2>
    </div>

    <n-form @submit.prevent="handleSubmit">
      <n-form-item>
        <n-input
          v-model:value="form.username"
          placeholder="请输入用户名"
          size="large"
          clearable
        >
          <template #prefix>
            <n-icon><Person /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <n-input
          v-model:value="form.password"
          type="password"
          placeholder="请输入密码"
          size="large"
          show-password-on="click"
        >
          <template #prefix>
            <n-icon><LockClosed /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <div class="captcha-row">
          <n-input
            v-model:value="form.captcha"
            placeholder="验证码"
            size="large"
          />
          <div class="captcha-img" @click="refreshCaptcha">
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
            <n-spin v-else size="small" />
          </div>
        </div>
      </n-form-item>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <n-button
        type="primary"
        size="large"
        block
        attr-type="submit"
        :loading="loading"
        :disabled="!form.username || !form.password"
      >
        登录
      </n-button>
    </n-form>
  </div>
</template>

<style lang="scss" scoped>
.password-login {
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

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .n-spin {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}
</style>
