/**
 * 认证工具
 * 管理登录状态、Token、用户信息
 */

const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'

/**
 * 获取 Token
 */
export const getToken = () => {
  return uni.getStorageSync(TOKEN_KEY)
}

/**
 * 设置 Token
 */
export const setToken = (token) => {
  uni.setStorageSync(TOKEN_KEY, token)
}

/**
 * 移除 Token
 */
export const removeToken = () => {
  uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  const info = uni.getStorageSync(USER_KEY)
  return info ? (typeof info === 'string' ? JSON.parse(info) : info) : null
}

/**
 * 设置用户信息
 */
export const setUserInfo = (userInfo) => {
  uni.setStorageSync(USER_KEY, userInfo)
}

/**
 * 移除用户信息
 */
export const removeUserInfo = () => {
  uni.removeStorageSync(USER_KEY)
}

/**
 * 是否已登录
 */
export const isLoggedIn = () => {
  return !!getToken()
}

/**
 * 清除所有登录信息
 */
export const clearAuth = () => {
  removeToken()
  removeUserInfo()
}

/**
 * 检查登录状态，未登录跳转登录页
 */
export const checkLogin = () => {
  if (!isLoggedIn()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}
