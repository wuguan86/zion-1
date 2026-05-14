import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginSelector from '../LoginSelector.vue'

describe('LoginSelector', () => {
  it('renders 3 login cards', () => {
    const wrapper = mount(LoginSelector)
    const cards = wrapper.findAll('.login-card')
    expect(cards).toHaveLength(3)
  })

  it('emits select with "password" when clicking password card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[0].trigger('click')
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')![0]).toEqual(['password'])
  })

  it('emits select with "sms" when clicking SMS card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[1].trigger('click')
    expect(wrapper.emitted('select')![0]).toEqual(['sms'])
  })

  it('emits select with "wechat" when clicking WeChat card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[2].trigger('click')
    expect(wrapper.emitted('select')![0]).toEqual(['wechat'])
  })

  it('does not emit when disabled', async () => {
    const wrapper = mount(LoginSelector, {
      props: { disabled: true }
    })
    await wrapper.findAll('.login-card')[0].trigger('click')
    expect(wrapper.emitted('select')).toBeFalsy()
  })
})
