package com.zion.crypto;

/**
 * 加密服务接口
 */
public interface CryptoService {

    /**
     * 是否启用加密
     */
    boolean isEnabled();

    /**
     * 获取公钥
     */
    String getPublicKey();

    /**
     * 获取AES密钥（用RSA公钥加密后的）
     */
    String getEncryptedAesKey();

    /**
     * RSA解密数据（使用私钥解密请求数据）
     */
    String decrypt(String encryptedData);

    /**
     * RSA加密数据（使用私钥）
     */
    String encrypt(String data);

    /**
     * AES加密响应数据
     */
    String encryptResponse(String data);

    /**
     * 刷新密钥对
     */
    void refreshKeyPair();
}
