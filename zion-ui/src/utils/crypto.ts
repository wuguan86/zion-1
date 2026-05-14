import JSEncrypt from 'jsencrypt'
import { fetchCryptoConfig, clearCryptoConfigCache } from './request'

// 加密配置
interface CryptoConfig {
  enabled: boolean
  publicKey: string
}

// 缓存加密配置
let cryptoConfig: CryptoConfig | null = null

/**
 * 获取加密配置
 */
export async function getCryptoConfig(): Promise<CryptoConfig> {
  if (cryptoConfig) {
    return cryptoConfig
  }
  
  cryptoConfig = await fetchCryptoConfig()
  return cryptoConfig
}

/**
 * 清除缓存的配置
 */
export function clearCryptoConfig() {
  cryptoConfig = null
  clearCryptoConfigCache()
}

/**
 * RSA加密
 */
export function rsaEncrypt(data: string, publicKey: string): string {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  const encrypted = encrypt.encrypt(data)
  if (!encrypted) {
    throw new Error('RSA加密失败')
  }
  return encrypted
}

/**
 * RSA解密（使用公钥解密私钥加密的数据）
 */
export function rsaDecrypt(data: string, publicKey: string): string {
  const decrypt = new JSEncrypt()
  decrypt.setPublicKey(publicKey)
  const decrypted = decrypt.decrypt(data)
  if (!decrypted) {
    throw new Error('RSA解密失败')
  }
  return decrypted
}

/**
 * 加密密码字段
 */
export async function encryptPassword(password: string): Promise<string> {
  const config = await getCryptoConfig()
  
  if (!config.enabled || !config.publicKey) {
    return password
  }
  
  return rsaEncrypt(password, config.publicKey)
}

/**
 * 加密对象中的密码字段
 */
export async function encryptPasswordFields<T extends Record<string, any>>(
  data: T,
  fields: string[] = ['password', 'oldPassword', 'newPassword']
): Promise<T> {
  const config = await getCryptoConfig()
  
  if (!config.enabled || !config.publicKey) {
    return data
  }
  
  const result = { ...data }
  
  for (const field of fields) {
    if (result[field] && typeof result[field] === 'string') {
      result[field] = rsaEncrypt(result[field], config.publicKey)
    }
  }
  
  return result
}

/**
 * 检查是否启用加密
 */
export async function isEncryptEnabled(): Promise<boolean> {
  const config = await getCryptoConfig()
  return config.enabled
}
