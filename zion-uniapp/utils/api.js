/**
 * API 接口定义
 * 消息中心与聊天功能已下线，仅保留登录、个人资料和文件上传接口。
 */
import { get, post, put, upload } from './request.js'

// ==================== 认证相关 ====================

/** 微信小程序登录 */
export const wxLogin = (data) => post('/api/app/auth/login', data)

/** 发送短信验证码 */
export const sendSmsCode = (data) => post('/api/app/auth/sms-code', data)

/** 获取当前用户信息 */
export const getUserInfo = () => get('/api/auth/info')

/** 获取个人资料 */
export const getProfile = () => get('/api/auth/profile')

/** 更新个人资料 */
export const updateProfile = (data) => put('/api/auth/profile', data)

/** App 端获取个人资料 */
export const getAppProfile = () => get('/api/app/auth/profile')

/** App 端更新个人资料 */
export const updateAppProfile = (data) => put('/api/app/auth/profile', data)

/** App 端上传头像 */
export const uploadAvatar = (filePath) => upload('/api/app/auth/upload-avatar', filePath)

/** 修改密码 */
export const changePassword = (data) => post('/api/auth/password', data)

/** App 端修改密码 */
export const changeAppPassword = (data) => post('/api/app/auth/password', data)

/** 退出登录 */
export const logout = () => post('/api/auth/logout')

// ==================== 文件相关 ====================

/** 上传文件 */
export const uploadFile = (filePath) => upload('/api/sys/file/upload', filePath)
