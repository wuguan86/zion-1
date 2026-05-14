package com.zion.common.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA加解密工具类
 * 支持分段加密和AES混合加密
 */
public class RsaUtils {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    // RSA加密最大块大小（2048位密钥 = 256字节，减去11字节填充）
    private static final int MAX_ENCRYPT_BLOCK = 245;
    // RSA解密最大块大小
    private static final int MAX_DECRYPT_BLOCK = 256;

    public static final String PUBLIC_KEY = "publicKey";
    public static final String PRIVATE_KEY = "privateKey";

    /**
     * 生成RSA密钥对
     */
    public static Map<String, String> generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        Map<String, String> keyMap = new HashMap<>();
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        return keyMap;
    }

    /**
     * 公钥加密（支持分段加密大数据）
     */
    public static String encryptByPublicKey(String data, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedData = segmentEncrypt(dataBytes, publicKey, Cipher.ENCRYPT_MODE);
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * 私钥解密（支持分段解密大数据）
     */
    public static String decryptByPrivateKey(String encryptedData, String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        byte[] dataBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = segmentDecrypt(dataBytes, privateKey, Cipher.DECRYPT_MODE);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 私钥加密（支持分段加密大数据）
     */
    public static String encryptByPrivateKey(String data, String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedData = segmentEncrypt(dataBytes, privateKey, Cipher.ENCRYPT_MODE);
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * 公钥解密（支持分段解密大数据）
     */
    public static String decryptByPublicKey(String encryptedData, String publicKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        byte[] dataBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = segmentDecrypt(dataBytes, publicKey, Cipher.DECRYPT_MODE);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 分段加密
     */
    private static byte[] segmentEncrypt(byte[] data, Key key, int mode) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(mode, key);
        
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offset, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 分段解密
     */
    private static byte[] segmentDecrypt(byte[] data, Key key, int mode) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(mode, key);
        
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offset, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_DECRYPT_BLOCK;
        }
        
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    // ==================== AES + RSA 混合加密（推荐用于大数据） ====================

    /**
     * 混合加密（AES加密数据，RSA加密AES密钥）
     * 返回格式：Base64(RSA加密的AES密钥) + "." + Base64(IV) + "." + Base64(AES加密的数据)
     */
    public static String hybridEncrypt(String data, String publicKeyStr) throws Exception {
        // 生成AES密钥
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE, new SecureRandom());
        SecretKey aesKey = keyGen.generateKey();
        
        // 生成IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        // AES加密数据
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encryptedData = aesCipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // RSA加密AES密钥
        String encryptedAesKey = encryptByPublicKey(
            Base64.getEncoder().encodeToString(aesKey.getEncoded()), 
            publicKeyStr
        );
        
        // 组合结果
        return encryptedAesKey + "." + 
               Base64.getEncoder().encodeToString(iv) + "." + 
               Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * 混合解密
     */
    public static String hybridDecrypt(String encryptedData, String privateKeyStr) throws Exception {
        String[] parts = encryptedData.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("加密数据格式错误");
        }
        
        String encryptedAesKey = parts[0];
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] data = Base64.getDecoder().decode(parts[2]);
        
        // RSA解密AES密钥
        String aesKeyStr = decryptByPrivateKey(encryptedAesKey, privateKeyStr);
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyStr);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
        
        // AES解密数据
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] decryptedData = aesCipher.doFinal(data);
        
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
