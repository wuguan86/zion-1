<script setup lang="ts">
import { ref, type Component } from 'vue'
import type { LoginType } from '@/types/login'
import LoginSelector from './components/LoginSelector.vue'
import PasswordLogin from './components/PasswordLogin.vue'
import SmsCodeLogin from './components/SmsCodeLogin.vue'
import WechatQrcodeLogin from './components/WechatQrcodeLogin.vue'

type Step = 'select' | 'form'

const currentStep = ref<Step>('select')
const activeType = ref<LoginType>('password')
const loginLoading = ref(false)

const formComponents: Record<LoginType, Component> = {
  password: PasswordLogin,
  sms: SmsCodeLogin,
  wechat: WechatQrcodeLogin
}

function handleSelect(type: LoginType) {
  activeType.value = type
  currentStep.value = 'form'
}

function handleBack() {
  currentStep.value = 'select'
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-shape shape-1" />
      <div class="bg-shape shape-2" />
      <div class="bg-shape shape-3" />
    </div>

    <div class="login-container">
      <Transition name="slide-fade" mode="out-in">
        <LoginSelector
          v-if="currentStep === 'select'"
          key="select"
          :disabled="loginLoading"
          @select="handleSelect"
        />
        <component
          v-else
          :is="formComponents[activeType]"
          key="form"
          @back="handleBack"
        />
      </Transition>
    </div>

    <footer class="login-footer">
      <span>&copy; 2026 Zion. All rights reserved.</span>
    </footer>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;

  &.shape-1 {
    width: 600px;
    height: 600px;
    background: #667eea;
    top: -200px;
    right: -100px;
  }

  &.shape-2 {
    width: 400px;
    height: 400px;
    background: #f093fb;
    bottom: -100px;
    left: -80px;
  }

  &.shape-3 {
    width: 300px;
    height: 300px;
    background: #4facfe;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.06),
    0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.login-footer {
  position: absolute;
  bottom: 24px;
  font-size: 13px;
  color: #9ca3af;

  span {
    opacity: 0.7;
  }
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
