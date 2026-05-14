export type LoginType = 'password' | 'sms' | 'wechat'

export interface LoginRequest {
  loginType: LoginType
  username?: string
  password?: string
  phone?: string
  smsCode?: string
  platform?: string
  authCode?: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  gender: number
  status: number
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
  avatar: string
}

export interface CaptchaResult {
  uuid: string
  img: string
}

export interface QrcodeResult {
  ticket: string
  qrUrl: string
  expireSeconds: number
}

export interface ScanStatusResult {
  status: 'pending' | 'scanned' | 'confirmed' | 'cancelled' | 'expired'
}
