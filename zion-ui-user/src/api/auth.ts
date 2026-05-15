import { request } from '@/utils/request'
import type { LoginResult, CaptchaResult, WechatQrLoginSession, WechatQrLoginStatus } from '@/types/login'

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

  /** 获取微信公众号授权URL */
  getWechatAuthorizeUrl(): Promise<{ authorizeUrl: string }> {
    return request({ url: '/web/auth/wechat/authorize', method: 'get' })
  },

  /** 创建电脑端微信公众号扫码登录会话 */
  createWechatQrLoginSession(): Promise<WechatQrLoginSession> {
    return request({ url: '/web/auth/wechat/qr/session', method: 'post' })
  },

  /** 查询电脑端微信公众号扫码登录状态 */
  pollWechatQrLogin(sessionId: string): Promise<WechatQrLoginStatus> {
    return request({ url: '/web/auth/wechat/qr/poll', method: 'get', params: { sessionId } })
  }
}
