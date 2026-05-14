import { ref } from 'vue'
import { authApi } from '@/api/auth'

export function useCaptcha() {
  const captchaUuid = ref('')
  const captchaImg = ref('')
  const loading = ref(false)

  async function refreshCaptcha() {
    loading.value = true
    try {
      const res = await authApi.getCaptcha()
      captchaUuid.value = res.uuid
      captchaImg.value = res.img
    } catch {
      // ignore
    } finally {
      loading.value = false
    }
  }

  return {
    captchaUuid,
    captchaImg,
    loading,
    refreshCaptcha
  }
}
