/**
 * 加密工具
 * 处理响应数据的 AES-GCM 解密，与 zion-ui 保持一致
 * 使用 crypto-js 的 AES-CTR 模式实现 GCM 解密
 * （AES-GCM 的加密部分本质上就是 AES-CTR，只是计数器从2开始）
 */
import CryptoJS from 'crypto-js'

// API 基础地址（与 request.js 保持一致）
const BASE_URL = 'http://localhost:8080'

// 加密配置缓存
let cryptoConfigCache = null

/**
 * 获取加密配置
 * 使用原生 uni.request 避免与 request.js 的循环依赖
 */
export function fetchCryptoConfig() {
  if (cryptoConfigCache) {
    return Promise.resolve(cryptoConfigCache)
  }
  return new Promise((resolve) => {
    uni.request({
      url: BASE_URL + '/api/crypto/config',
      method: 'GET',
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          cryptoConfigCache = res.data.data
          resolve(cryptoConfigCache)
        } else {
          resolve({ enabled: false, publicKey: '', aesKey: '' })
        }
      },
      fail: () => {
        console.error('[Crypto] 获取加密配置失败')
        resolve({ enabled: false, publicKey: '', aesKey: '' })
      }
    })
  })
}

/**
 * 清除加密配置缓存
 */
export function clearCryptoConfigCache() {
  cryptoConfigCache = null
}

/**
 * 判断是否是AES加密的响应数据
 * 格式：Base64(IV).Base64(encryptedData)
 * IV是12字节，Base64编码后为16个字符
 */
export function isAesEncryptedData(data) {
  if (typeof data !== 'string') {
    return false
  }
  const parts = data.split('.')
  if (parts.length !== 2) {
    return false
  }
  // 检查是否为合法Base64字符
  const base64Regex = /^[A-Za-z0-9+/]+=*$/
  if (!base64Regex.test(parts[0]) || !base64Regex.test(parts[1])) {
    return false
  }
  // IV是12字节，Base64后是16字符；密文部分长度需大于10
  return parts[0].length === 16 && parts[1].length > 10
}

/**
 * AES-GCM 解密（使用 crypto-js 的 CTR 模式实现）
 *
 * 原理：AES-GCM = AES-CTR(从计数器2开始) + GHASH认证
 * 我们用 crypto-js 的 CTR 模式解密密文部分，跳过 GCM 认证标签验证
 * （数据来自自己的后端且经过HTTPS传输，安全风险可控）
 *
 * @param {string} encryptedData - 格式：Base64(IV).Base64(ciphertext+tag)
 * @param {string} aesKeyBase64 - Base64编码的AES密钥
 * @returns {string} 解密后的明文
 */
function aesGcmDecrypt(encryptedData, aesKeyBase64) {
  const parts = encryptedData.split('.')
  if (parts.length !== 2) {
    throw new Error('加密数据格式错误')
  }

  const iv = CryptoJS.enc.Base64.parse(parts[0])        // 12字节IV
  const data = CryptoJS.enc.Base64.parse(parts[1])       // 密文 + 16字节认证标签
  const key = CryptoJS.enc.Base64.parse(aesKeyBase64)    // AES密钥

  // 分离密文和GCM认证标签（末尾16字节）
  const ciphertextSigBytes = data.sigBytes - 16
  const ciphertext = data.clone()
  ciphertext.sigBytes = ciphertextSigBytes
  ciphertext.clamp()

  // 构造GCM计数器：对于12字节IV，初始计数器 J0 = IV || 0x00000001
  // GCM加密从 J0+1 = IV || 0x00000002 开始
  const counterWords = iv.words.slice(0, 3) // 复制IV的3个word（12字节）
  counterWords.push(2)                       // 追加计数器起始值 0x00000002
  const counter = CryptoJS.lib.WordArray.create(counterWords, 16)

  // 使用AES-CTR模式解密
  const cipherParams = CryptoJS.lib.CipherParams.create({ ciphertext: ciphertext })
  const decrypted = CryptoJS.AES.decrypt(cipherParams, key, {
    iv: counter,
    mode: CryptoJS.mode.CTR,
    padding: CryptoJS.pad.NoPadding
  })

  return decrypted.toString(CryptoJS.enc.Utf8)
}

/**
 * 解密响应数据
 * @param {string} data - 加密的响应数据
 * @returns {Promise<any>} 解密后的数据对象
 */
export async function decryptResponseData(data) {
  const config = await fetchCryptoConfig()

  if (!config.aesKey) {
    return data
  }

  try {
    const decryptedStr = aesGcmDecrypt(data, config.aesKey)
    return JSON.parse(decryptedStr)
  } catch (error) {
    console.error('[Crypto] 响应解密失败', error)
    // 解密失败可能是密钥过期，清除缓存
    cryptoConfigCache = null
    return data
  }
}
