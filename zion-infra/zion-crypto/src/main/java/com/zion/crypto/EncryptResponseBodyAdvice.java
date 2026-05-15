package com.zion.crypto;

import com.zion.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * 响应体加密处理器
 * 支持全局加密和部分加密两种模式：
 * - 全局加密：所有接口返回都加密（排除公开接口）
 * - 部分加密：只对标注了 @EncryptResponse 的方法进行加密
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final CryptoConfigProvider configProvider;

    /**
     * 不需要加密的公开接口路径
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/crypto/",
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/auth/sms-code",
            "/web/auth/wechat/qr/",
            "/api/mall/",
            "/api/wechat/mp/server",
            "/api/wechat/miniprogram/",
            "/sys/config-group/public",
            "/file/",
            "/sys/file/"
    );

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!cryptoService.isEnabled()) {
            return false;
        }

        if (configProvider.isGlobalEncrypt()) {
            return true;
        } else {
            return returnType.hasMethodAnnotation(EncryptResponse.class);
        }
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            if (body == null || body instanceof byte[]) {
                return body;
            }

            if (configProvider.isGlobalEncrypt()) {
                String path = request.getURI().getPath();
                for (String excludePath : EXCLUDE_PATHS) {
                    if (path.contains(excludePath)) {
                        return body;
                    }
                }
            }

            if (body instanceof Result<?> result) {
                Object data = result.getData();
                if (data != null) {
                    String jsonData = objectMapper.writeValueAsString(data);
                    String encryptedData = cryptoService.encryptResponse(jsonData);
                    return Result.ok(encryptedData);
                }
                return body;
            }

            String jsonData;
            if (body instanceof String) {
                jsonData = (String) body;
            } else {
                jsonData = objectMapper.writeValueAsString(body);
            }

            String encryptedData = cryptoService.encryptResponse(jsonData);
            return Result.ok(encryptedData);

        } catch (Exception e) {
            log.error("响应加密失败", e);
            return body;
        }
    }
}
