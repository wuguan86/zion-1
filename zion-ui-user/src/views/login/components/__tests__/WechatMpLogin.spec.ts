import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import WechatMpLogin from '../WechatMpLogin.vue'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

vi.mock('@/api/auth', () => ({
  authApi: {
    createWechatQrLoginSession: vi.fn(),
    pollWechatQrLogin: vi.fn(),
    getInfo: vi.fn()
  }
}))

const replace = vi.fn()

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ replace })
  }
})

describe('WechatMpLogin', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(authApi.getInfo).mockResolvedValue({
      user: {
        id: 100,
        username: '',
        nickname: '微信用户',
        avatar: '',
        email: '',
        phone: '',
        gender: 0,
        status: 1
      },
      roles: [],
      permissions: []
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders a PC WeChat QR code and logs in when polling is confirmed', async () => {
    vi.mocked(authApi.createWechatQrLoginSession).mockResolvedValue({
      sessionId: 'session-1',
      qrCodeUrl: 'https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket-1',
      expiresIn: 300
    })
    vi.mocked(authApi.pollWechatQrLogin).mockResolvedValue({
      status: 'CONFIRMED',
      loginResult: {
        token: 'token-1',
        userId: 100,
        username: '',
        nickname: '微信用户',
        avatar: ''
      }
    })

    const wrapper = mount(WechatMpLogin)
    await flushPromises()

    expect(wrapper.find('.qr-image').attributes('src')).toBe('https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket-1')
    expect(authApi.createWechatQrLoginSession).toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(2000)
    await flushPromises()

    const userStore = useUserStore()
    expect(userStore.token).toBe('token-1')
    expect(replace).toHaveBeenCalledWith('/home')
  })
})
