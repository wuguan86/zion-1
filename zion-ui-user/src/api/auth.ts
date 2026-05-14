import { request } from '@/utils/request'
import type { LoginResult, CaptchaResult, QrcodeResult, ScanStatusResult } from '@/types/login'

export const authApi = {
  /** 获取图形验证码 */
  getCaptcha(): Promise<CaptchaResult> {
    return request({ url: '/auth/captcha', method: 'get' })
  },

  /** 发送短信验证码 */
  sendSmsCode(phone: string): Promise<void> {
    return request({ url: '/web/auth/sms-code', method: 'post', data: { phone } })
  },

  /** 登录 */
  login(data: { loginType: string } & Record<string, any>): Promise<LoginResult> {
    return request({ url: '/web/auth/login', method: 'post', data })
  },

  /** 退出登录 */
  logout(): Promise<void> {
    return request({ url: '/web/auth/logout', method: 'post' })
  },

  /** 获取当前用户信息 */
  getInfo(): Promise<{ user: import('@/types/login').UserInfo; roles: string[]; permissions: string[] }> {
    return request({ url: '/web/auth/info', method: 'get' })
  },

  /** 获取微信扫码二维码 */
  getWechatQrcode(): Promise<QrcodeResult> {
    return request({ url: '/web/auth/wechat/qrcode', method: 'get' })
  },

  /** 查询微信扫码状态 */
  getWechatStatus(ticket: string): Promise<ScanStatusResult> {
    return request({ url: '/web/auth/wechat/status', method: 'get', params: { ticket } })
  }
}
