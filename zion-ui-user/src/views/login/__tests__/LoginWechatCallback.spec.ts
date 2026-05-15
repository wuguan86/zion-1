import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import LoginView from '../index.vue'

describe('LoginView WeChat OAuth callback', () => {
  let originalLocation: Location
  let router: Router

  beforeEach(async () => {
    originalLocation = window.location
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { href: '' }
    })

    router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', component: LoginView }]
    })

    router.push('/login?code=wechat-code&state=wechat-state')
    await router.isReady()
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation
    })
    vi.restoreAllMocks()
  })

  it('redirects WeChat code callbacks to backend callback endpoint', () => {
    mount(LoginView, {
      global: {
        plugins: [router]
      }
    })

    expect(window.location.href).toBe('/api/web/auth/wechat/callback?code=wechat-code&state=wechat-state')
  })
})
