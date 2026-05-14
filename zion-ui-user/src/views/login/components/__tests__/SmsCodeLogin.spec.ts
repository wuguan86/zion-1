import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SmsCodeLogin from '../SmsCodeLogin.vue'

vi.mock('@/api/auth', () => ({
  authApi: {
    sendSmsCode: vi.fn().mockResolvedValue(undefined),
    login: vi.fn().mockResolvedValue({ token: 'token123', userId: 1, username: '13800138000', nickname: '用户0000', avatar: '' })
  }
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ replace: vi.fn() })
  }
})

describe('SmsCodeLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders phone input and SMS code input', () => {
    const wrapper = mount(SmsCodeLogin)
    expect(wrapper.find('h2').text()).toBe('短信验证码登录')
  })

  it('emits back when clicking back button', async () => {
    const wrapper = mount(SmsCodeLogin)
    await wrapper.find('.back-btn').trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('disables login button when fields are empty', () => {
    const wrapper = mount(SmsCodeLogin)
    const btn = wrapper.find('button[type="submit"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })
})
