import { request } from '@/utils/request'
import { encryptPasswordFields } from '@/utils/crypto'

// 类型定义
export interface LoginParams {
  username: string
  password: string
  uuid?: string
  code?: string
  phone?: string  // 短信验证码时使用
  rememberMe?: boolean
}

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  uuid?: string
  code?: string
}

export interface CaptchaResult {
  uuid: string
  img: string
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

export interface MenuInfo {
  id: number
  parentId: number
  name: string
  type: number
  path: string
  component: string
  permission: string
  icon: string
  sort: number
  visible: number
  status: number
  isFrame: number  // 是否外链(0-否 1-是)
  children?: MenuInfo[]
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export interface UserInfoResult {
  user: UserInfo
  roles: string[]
  permissions: string[]
  menus: MenuInfo[]
}

// 个人信息
export interface ProfileInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  status?: number
  deptId?: number
  deptName?: string
  postNames?: string
  remark?: string
  userType?: string
  createTime?: string
}

// 认证相关API
export const authApi = {
  // 获取验证码
  getCaptcha(): Promise<CaptchaResult> {
    return request({
      url: '/auth/captcha',
      method: 'get'
    })
  },

  // 发送短信验证码
  sendSmsCode(phone: string): Promise<void> {
    return request({
      url: '/auth/sms-code',
      method: 'post',
      data: { phone }
    })
  },

  // 登录（自动加密密码）
  async login(data: LoginParams): Promise<LoginResult> {
    const encryptedData = await encryptPasswordFields(data, ['password'])
    return request({
      url: '/auth/login',
      method: 'post',
      data: encryptedData
    })
  },

  // 注册（自动加密密码）
  async register(data: RegisterParams): Promise<string> {
    const encryptedData = await encryptPasswordFields(data, ['password'])
    return request({
      url: '/auth/register',
      method: 'post',
      data: encryptedData
    })
  },

  // 退出登录
  logout(): Promise<void> {
    return request({
      url: '/auth/logout',
      method: 'post'
    })
  },

  // 获取用户信息
  getInfo(): Promise<UserInfoResult> {
    return request({
      url: '/auth/info',
      method: 'get'
    })
  },

  // 获取个人信息
  getProfile(): Promise<ProfileInfo> {
    return request({
      url: '/auth/profile',
      method: 'get'
    })
  },

  // 更新个人信息
  updateProfile(data: Partial<ProfileInfo>): Promise<void> {
    return request({
      url: '/auth/profile',
      method: 'put',
      data
    })
  },

  // 修改密码（自动加密密码）
  async updatePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
    const encryptedData = await encryptPasswordFields(data, ['oldPassword', 'newPassword'])
    return request({
      url: '/auth/password',
      method: 'post',
      data: encryptedData
    })
  }
}
