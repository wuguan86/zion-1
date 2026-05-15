<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { LoginType } from '@/types/login'
import { Key, PhonePortrait, QrCode } from '@vicons/ionicons5'

defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  select: [type: LoginType]
}>()

const route = useRoute()
const wechatError = computed(() => (route?.query.wechatError as string) || '')

const cards = [
  {
    type: 'password' as LoginType,
    title: '账号密码',
    desc: '使用账号密码登录',
    icon: Key,
    color: '#667eea'
  },
  {
    type: 'sms' as LoginType,
    title: '短信验证码',
    desc: '手机号快速验证登录',
    icon: PhonePortrait,
    color: '#f093fb'
  },
  {
    type: 'wechat' as LoginType,
    title: '微信公众号',
    desc: '使用微信公众号授权登录',
    icon: QrCode,
    color: '#4facfe'
  }
]
</script>

<template>
  <div class="login-selector">
    <div class="selector-header">
      <h1 class="selector-title">欢迎回来</h1>
      <p class="selector-subtitle">请选择登录方式</p>
    </div>

    <div class="cards-grid">
      <div
        v-for="card in cards"
        :key="card.type"
        class="login-card"
        :class="{ disabled }"
        @click="!disabled && emit('select', card.type)"
      >
        <div class="card-icon" :style="{ background: card.color }">
          <n-icon size="32">
            <component :is="card.icon" />
          </n-icon>
        </div>
        <div class="card-content">
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </div>
        <div class="card-arrow">
          <n-icon size="20">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </n-icon>
        </div>
      </div>
    </div>

    <p v-if="wechatError" class="error-msg">{{ wechatError }}</p>
  </div>
</template>

<style lang="scss" scoped>
.login-selector {
  max-width: 480px;
  margin: 0 auto;
  padding: 40px 24px;
}

.selector-header {
  text-align: center;
  margin-bottom: 40px;
}

.selector-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.selector-subtitle {
  font-size: 15px;
  color: #6b7280;
}

.cards-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.login-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: #667eea;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.12);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }

  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.card-content {
  flex: 1;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
    margin-bottom: 4px;
  }

  p {
    font-size: 13px;
    color: #9ca3af;
  }
}

.card-arrow {
  color: #d1d5db;
  flex-shrink: 0;
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 16px;
  text-align: center;
}
</style>
