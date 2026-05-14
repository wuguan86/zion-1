import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useLogin() {
  const router = useRouter()
  const userStore = useUserStore()
  const loading = ref(false)
  const errorMsg = ref('')

  async function login(data: { loginType: string } & Record<string, any>) {
    loading.value = true
    errorMsg.value = ''
    try {
      await userStore.login(data)
      router.replace('/home')
    } catch (e: any) {
      errorMsg.value = e.message || '登录失败'
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    errorMsg,
    login
  }
}
