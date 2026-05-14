import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PasswordLogin from '../PasswordLogin.vue'

vi.mock('@/api/auth', () => ({
  authApi: {
    getCaptcha: vi.fn().mockResolvedValue({ uuid: 'test-uuid', img: 'data:image/png;base64,xxx' }),
    login: vi.fn().mockResolvedValue({ token: 'token123', userId: 1, username: 'test', nickname: 'Test', avatar: '' })
  }
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ replace: vi.fn() })
  }
})

describe('PasswordLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders back button and form', () => {
    const wrapper = mount(PasswordLogin)
    expect(wrapper.find('.back-btn').exists()).toBe(true)
    expect(wrapper.find('h2').text()).toBe('账号密码登录')
  })

  it('emits back when clicking back button', async () => {
    const wrapper = mount(PasswordLogin)
    await wrapper.find('.back-btn').trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('disables login button when fields are empty', () => {
    const wrapper = mount(PasswordLogin)
    const btn = wrapper.find('button[type="submit"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })
})
