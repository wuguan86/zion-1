/**
 * 网络请求封装
 * 统一处理请求头、Token、错误码等
 */
import { isAesEncryptedData, decryptResponseData } from './crypto.js'

// API 基础地址 - 根据环境切换
const BASE_URL = 'http://localhost:8080'

/**
 * 通用请求方法
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header['Authorization'] = token
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          // 后端统一返回格式 { code, msg, data }
          if (data.code === 200 || data.code === 0) {
            // 检查响应数据是否是AES加密的，自动解密
            if (isAesEncryptedData(data.data)) {
              decryptResponseData(data.data)
                .then((decrypted) => {
                  data.data = decrypted
                  resolve(data)
                })
                .catch(() => {
                  resolve(data) // 解密失败返回原始数据
                })
            } else {
              resolve(data)
            }
          } else if (data.code === 401) {
            // Token过期，跳转登录
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/login/index' })
            reject(new Error(data.msg || '登录已过期'))
          } else {
            uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
            reject(new Error(data.msg || '请求失败'))
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('登录已过期'))
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(new Error('网络错误'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * GET 请求
 */
export const get = (url, data) => request({ url, method: 'GET', data })

/**
 * POST 请求
 */
export const post = (url, data) => request({ url, method: 'POST', data })

/**
 * PUT 请求
 */
export const put = (url, data) => request({ url, method: 'PUT', data })

/**
 * DELETE 请求
 */
export const del = (url, data) => request({ url, method: 'DELETE', data })

/**
 * 文件上传
 */
export const upload = (url, filePath, name = 'file') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      header: {
        'Authorization': token || ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = JSON.parse(res.data)
          if (data.code === 200 || data.code === 0) {
            // 检查上传响应数据是否加密
            if (isAesEncryptedData(data.data)) {
              decryptResponseData(data.data)
                .then((decrypted) => {
                  data.data = decrypted
                  resolve(data)
                })
                .catch(() => resolve(data))
            } else {
              resolve(data)
            }
          } else {
            reject(new Error(data.msg || '上传失败'))
          }
        } else {
          reject(new Error('上传失败'))
        }
      },
      fail: reject
    })
  })
}

export { BASE_URL }
export default request
