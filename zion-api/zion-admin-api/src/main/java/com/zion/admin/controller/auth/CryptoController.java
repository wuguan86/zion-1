package com.zion.admin.controller.auth;

import com.zion.common.result.Result;
import com.zion.crypto.CryptoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密控制器
 */
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    /**
     * 获取加密配置（公钥、AES密钥和是否启用）
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", cryptoService.isEnabled());
        result.put("publicKey", cryptoService.getPublicKey());
        result.put("aesKey", cryptoService.getEncryptedAesKey());
        return Result.ok(result);
    }

    /**
     * 获取公钥
     */
    @GetMapping("/publicKey")
    public Result<String> getPublicKey() {
        return Result.ok(cryptoService.getPublicKey());
    }

    /**
     * 解密数据（测试用）
     */
    @PostMapping("/decrypt")
    public Result<String> decrypt(@RequestBody DecryptRequest request) {
        String decrypted = cryptoService.decrypt(request.getData());
        return Result.ok(decrypted);
    }

    @Data
    public static class DecryptRequest {
        private String data;
    }
}
