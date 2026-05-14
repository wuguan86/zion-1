package com.zion.system.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.zion.common.util.RsaUtils;
import com.zion.system.entity.SysConfigGroup;
import com.zion.crypto.CryptoService;
import com.zion.system.service.SysConfigGroupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * 加密服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoServiceImpl implements CryptoService {

    private final SysConfigGroupService configGroupService;
    private final ObjectMapper objectMapper;

    /**
     * 配置分组编码
     */
    private static final String CONFIG_GROUP_SECURITY = "security";

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private String publicKey;
    private String privateKey;
    private SecretKey aesKey;
    private String encryptedAesKey; // RSA加密后的AES密钥

    @PostConstruct
    public void init() {
        loadKeys();
        generateAesKey();
    }

    /**
     * 加载密钥
     */
    private void loadKeys() {
        try {
            JsonNode config = getSecurityConfig();
            if (config != null) {
                publicKey = getStringValue(config, "encryptPublicKey");
                privateKey = getStringValue(config, "encryptPrivateKey");
            }

            // 如果没有配置密钥，则生成新的密钥对
            if (publicKey == null || privateKey == null || publicKey.isEmpty() || privateKey.isEmpty()) {
                log.info("RSA密钥未配置，正在生成新密钥对...");
                generateAndSaveKeys();
            } else {
                log.info("RSA密钥加载成功");
            }
        } catch (Exception e) {
            log.error("加载RSA密钥失败", e);
        }
    }

    /**
     * 获取安全配置
     */
    private JsonNode getSecurityConfig() {
        try {
            SysConfigGroup group = configGroupService.getByGroupCode(CONFIG_GROUP_SECURITY);
            if (group != null && group.getConfigValue() != null) {
                return objectMapper.readTree(group.getConfigValue());
            }
        } catch (Exception e) {
            log.error("读取安全配置失败", e);
        }
        return null;
    }

    /**
     * 获取配置字符串值
     */
    private String getStringValue(JsonNode config, String key) {
        JsonNode node = config.get(key);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    /**
     * 获取配置布尔值
     */
    private boolean getBooleanValue(JsonNode config, String key) {
        JsonNode node = config.get(key);
        return node != null && node.asBoolean();
    }

    /**
     * 生成AES密钥
     */
    private void generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(256, new SecureRandom());
            aesKey = keyGen.generateKey();

            // 直接返回AES密钥的Base64编码（通过HTTPS安全传输）
            encryptedAesKey = Base64.getEncoder().encodeToString(aesKey.getEncoded());

            log.info("AES密钥生成成功");
        } catch (Exception e) {
            log.error("生成AES密钥失败", e);
        }
    }

    /**
     * 生成并保存密钥对
     */
    private void generateAndSaveKeys() {
        try {
            Map<String, String> keyPair = RsaUtils.generateKeyPair();
            publicKey = keyPair.get(RsaUtils.PUBLIC_KEY);
            privateKey = keyPair.get(RsaUtils.PRIVATE_KEY);
            log.info("RSA密钥对生成成功");
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
        }
    }

    @Override
    public boolean isEnabled() {
        JsonNode config = getSecurityConfig();
        return config != null && getBooleanValue(config, "encryptEnabled");
    }

    @Override
    public String getPublicKey() {
        if (publicKey == null || publicKey.isEmpty()) {
            loadKeys();
        }
        return publicKey;
    }

    @Override
    public String getEncryptedAesKey() {
        if (encryptedAesKey == null || encryptedAesKey.isEmpty()) {
            generateAesKey();
        }
        return encryptedAesKey;
    }

    @Override
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        try {
            return RsaUtils.decryptByPrivateKey(encryptedData, privateKey);
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new RuntimeException("数据解密失败");
        }
    }

    @Override
    public String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        try {
            return RsaUtils.encryptByPrivateKey(data, privateKey);
        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new RuntimeException("数据加密失败");
        }
    }

    @Override
    public String encryptResponse(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        try {
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // AES-GCM加密
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 返回格式：Base64(IV) + "." + Base64(加密数据)
            return Base64.getEncoder().encodeToString(iv) + "." +
                   Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("AES加密响应失败", e);
            throw new RuntimeException("数据加密失败");
        }
    }

    @Override
    public void refreshKeyPair() {
        generateAndSaveKeys();
        generateAesKey();
        log.info("密钥对已刷新");
    }
}
