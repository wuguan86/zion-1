<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧 Banner -->
      <div class="register-banner" :style="{ background: `linear-gradient(135deg, ${themeStore.primaryColor} 0%, ${adjustColor(themeStore.primaryColor, -30)} 100%)` }">
        <div class="banner-content">
          <div class="banner-logo">
            <img v-if="siteLogo" :src="siteLogo" class="logo-img" alt="Logo" />
            <div v-else class="logo-icon">{{ siteName.charAt(0) }}</div>
            <span class="logo-text">{{ siteName }}</span>
          </div>
          <h1 class="banner-title">加入我们</h1>
          <p class="banner-desc">创建账号，开启精彩之旅</p>
          <div class="banner-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>快速注册，即刻体验</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>安全可靠，隐私保护</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>功能丰富，持续更新</span>
            </div>
          </div>
        </div>
        <div class="banner-decoration">
          <div class="decoration-circle circle-1"></div>
          <div class="decoration-circle circle-2"></div>
          <div class="decoration-circle circle-3"></div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="register-form-wrapper">
        <div class="register-form">
          <h2 class="form-title">用户注册</h2>
          <p class="form-subtitle">请填写以下信息完成注册</p>

          <n-form ref="formRef" :model="formData" :rules="rules" size="large">
            <n-form-item path="username" label="用户名">
              <n-input v-model:value="formData.username" placeholder="4-20位字母数字下划线" :maxlength="20">
                <template #prefix><n-icon :component="PersonOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item path="password" label="密码">
              <n-input v-model:value="formData.password" type="password" placeholder="请输入密码" show-password-on="click" :maxlength="20">
                <template #prefix><n-icon :component="LockClosedOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item path="confirmPassword" label="确认密码">
              <n-input v-model:value="formData.confirmPassword" type="password" placeholder="请再次输入密码" show-password-on="click" :maxlength="20">
                <template #prefix><n-icon :component="LockClosedOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item path="nickname" label="昵称">
              <n-input v-model:value="formData.nickname" placeholder="请输入昵称（可选）" :maxlength="20">
                <template #prefix><n-icon :component="PersonCircleOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item v-if="verifyEmail" path="email" label="邮箱">
              <n-input v-model:value="formData.email" placeholder="请输入邮箱" :maxlength="50">
                <template #prefix><n-icon :component="MailOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item v-if="verifyPhone" path="phone" label="手机号">
              <n-input v-model:value="formData.phone" placeholder="请输入手机号" :maxlength="11">
                <template #prefix><n-icon :component="CallOutline" /></template>
              </n-input>
            </n-form-item>
            <n-form-item v-if="captchaEnabled" path="code" label="验证码">
              <div class="captcha-row">
                <n-input v-model:value="formData.code" placeholder="请输入验证码" :maxlength="6" />
                <img v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="loadCaptcha" title="点击刷新" />
                <n-spin v-else :size="20" />
              </div>
            </n-form-item>
            <n-form-item :show-label="false">
              <div class="form-actions">
                <n-checkbox v-model:checked="agreeTerms">
                  我已阅读并同意
                  <a class="link" :style="{ color: themeStore.primaryColor }">《用户协议》</a>
                  和
                  <a class="link" :style="{ color: themeStore.primaryColor }">《隐私政策》</a>
                </n-checkbox>
              </div>
            </n-form-item>
            <n-form-item :show-label="false">
              <n-button type="primary" block :loading="loading" :disabled="!agreeTerms" @click="handleRegister">注 册</n-button>
            </n-form-item>
          </n-form>

          <div class="register-footer">
            <span>已有账号？</span>
            <a class="login-link" :style="{ color: themeStore.primaryColor }" @click="goLogin">立即登录</a>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="page-footer">
      <span>{{ copyright }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, type FormInst, type FormRules } from 'naive-ui'
import { PersonOutline, LockClosedOutline, PersonCircleOutline, MailOutline, CallOutline } from '@vicons/ionicons5'
import { useSiteStore } from '@/stores/site'
import { useThemeStore } from '@/stores/theme'
import { authApi } from '@/api/auth'
import { configGroupApi } from '@/api/org'

const router = useRouter()
const message = useMessage()
const siteStore = useSiteStore()
const themeStore = useThemeStore()

// 站点配置
const siteName = computed(() => siteStore.siteName || 'Zion Admin')
const siteLogo = computed(() => siteStore.siteLogo)
const copyright = computed(() => siteStore.copyright || '版权所有 © 成都火星网络科技有限公司 2025-2030')

// 注册配置
const captchaEnabled = ref(false)
const verifyEmail = ref(false)
const verifyPhone = ref(false)
const agreeTerms = ref(false)

// 验证码
const captchaImg = ref('')
const captchaUuid = ref('')

// 颜色调整函数
function adjustColor(hex: string, percent: number): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.min(255, Math.max(0, (num >> 16) + percent))
  const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00FF) + percent))
  const b = Math.min(255, Math.max(0, (num & 0x0000FF) + percent))
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

// 加载配置
async function loadConfig() {
  try {
    const config = await configGroupApi.getPublicConfig()
    captchaEnabled.value = config.login?.captchaEnabled || false
    verifyEmail.value = config.register?.verifyEmail || false
    verifyPhone.value = config.register?.verifyPhone || false

    if (!config.register?.enabled) {
      message.warning('系统暂未开放注册')
      router.push('/login')
      return
    }

    if (captchaEnabled.value) {
      await loadCaptcha()
    }
  } catch (error) {
    console.error('加载配置失败', error)
  }
}

// 加载验证码
async function loadCaptcha() {
  try {
    const result = await authApi.getCaptcha()
    captchaImg.value = result.img
    captchaUuid.value = result.uuid
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

onMounted(() => {
  if (!siteStore.loaded) {
    siteStore.loadConfig()
  }
  loadConfig()
})

const formRef = ref<FormInst | null>(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: '',
  code: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名只能包含字母、数字、下划线，长度4-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_: any, value: string) => {
        if (value !== formData.password) {
          return new Error('两次输入的密码不一致')
        }
        return true
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!agreeTerms.value) {
    message.warning('请先同意用户协议和隐私政策')
    return
  }

  try {
    await formRef.value?.validate()
    loading.value = true

    const registerData: any = {
      username: formData.username,
      password: formData.password,
      nickname: formData.nickname || undefined,
      email: formData.email || undefined,
      phone: formData.phone || undefined
    }

    if (captchaEnabled.value) {
      registerData.uuid = captchaUuid.value
      registerData.code = formData.code
    }

    const result = await authApi.register(registerData)
    if (result === 'needAudit') {
      message.success('注册成功，请等待管理员审核通过后再登录')
    } else {
      message.success('注册成功，请登录')
    }
    router.push('/login')
  } catch (error: any) {
    if (captchaEnabled.value) {
      loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F9FAFB 0%, #E5E7EB 100%);
  padding: 20px;
  position: relative;
}

.register-container {
  display: flex;
  width: 100%;
  max-width: 1100px;
  min-height: 500px;
  background: #FFFFFF;
  border-radius: 20px;
  box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.register-banner {
  flex: 0.9;
  padding: 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.banner-content {
  position: relative;
  z-index: 1;
}

.banner-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;

  .logo-icon {
    width: 40px;
    height: 40px;
    background: #FFFFFF;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--primary-color, #111827);
    font-size: 20px;
    font-weight: 700;
  }

  .logo-img {
    width: 40px;
    height: 40px;
    object-fit: contain;
    border-radius: 10px;
  }

  .logo-text {
    font-size: 20px;
    font-weight: 700;
    color: #FFFFFF;
  }
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 12px;
}

.banner-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 28px;
}

.banner-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);

  .feature-dot {
    width: 6px;
    height: 6px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 50%;
    flex-shrink: 0;
  }
}

.banner-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.03);

  &.circle-1 { width: 400px; height: 400px; top: -100px; right: -100px; }
  &.circle-2 { width: 300px; height: 300px; bottom: -50px; left: -50px; }
  &.circle-3 { width: 200px; height: 200px; top: 50%; left: 50%; transform: translate(-50%, -50%); }
}

.register-form-wrapper {
  flex: 1.1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 32px;
  overflow-y: auto;
}

.register-form {
  width: 100%;
  max-width: 350px;
}

.form-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 4px;
}

.form-subtitle {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 20px;
}

:deep(.n-form-item) {
  margin-bottom: 12px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
}

:deep(.n-input) {
  --n-height: 36px;
  --n-border-radius: 6px;
}

:deep(.n-button) {
  --n-height: 36px;
  --n-border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;

  .n-input {
    flex: 1;
  }
}

.captcha-img {
  height: 36px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #E5E7EB;

  &:hover {
    opacity: 0.8;
  }
}

.form-actions {
  width: 100%;
  font-size: 13px;

  .link {
    cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #6B7280;
}

.login-link {
  cursor: pointer;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.page-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  color: #9CA3AF;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 768px) {
  .register-container {
    flex-direction: column;
    max-width: 450px;
  }

  .register-banner {
    padding: 32px;
    min-height: 200px;
  }

  .banner-title {
    font-size: 24px;
  }

  .banner-features {
    display: none;
  }

  .register-form-wrapper {
    padding: 24px;
  }
}
</style>
