import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/login'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const infoLoading = ref(false)

  const isLogin = computed(() => !!token.value)
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')

  async function login(loginData: { loginType: string } & Record<string, any>) {
    const res = await authApi.login(loginData)
    await applyLoginResult(res)
    return res
  }

  async function applyLoginResult(res: {
    token: string
    userId: number
    username?: string
    nickname?: string
    avatar?: string
  }) {
    token.value = res.token
    user.value = {
      id: res.userId,
      username: res.username || '',
      nickname: res.nickname || '',
      avatar: res.avatar || '',
      email: '',
      phone: '',
      gender: 0,
      status: 1
    }
    await getInfo()
    return res
  }

  async function getInfo() {
    if (infoLoading.value) return
    infoLoading.value = true
    try {
      const res = await authApi.getInfo()
      user.value = res.user
      roles.value = res.roles
      permissions.value = res.permissions
      return res
    } finally {
      infoLoading.value = false
    }
  }

  async function logout() {
    const hadToken = !!token.value
    token.value = null
    user.value = null
    roles.value = []
    permissions.value = []

    if (hadToken) {
      try {
        await authApi.logout()
      } catch {
        // ignore
      }
    }

    router.push('/login')
  }

  return {
    token,
    user,
    roles,
    permissions,
    isLogin,
    nickname,
    avatar,
    login,
    applyLoginResult,
    getInfo,
    logout
  }
}, {
  persist: {
    key: 'Zion-user',
    paths: ['token']
  }
})
