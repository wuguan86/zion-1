package com.zion.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 请求体解密处理器
 * 自动解密加密的请求数据
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class DecryptRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return cryptoService.isEnabled();
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        if (isMiniProgramRequest()) {
            return inputMessage;
        }

        String body;
        try {
            body = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("读取请求体失败", e);
            return inputMessage;
        }

        if (body == null || body.isEmpty()) {
            return new DecryptedHttpInputMessage(inputMessage, "");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(body);

            if (jsonNode.has("encryptedData")) {
                String encryptedData = jsonNode.get("encryptedData").asText();
                String decryptedBody = cryptoService.decrypt(encryptedData);
                log.debug("请求体已解密");
                return new DecryptedHttpInputMessage(inputMessage, decryptedBody);
            }

            if (jsonNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                boolean modified = false;

                for (String field : new String[]{"password", "oldPassword", "newPassword"}) {
                    if (objectNode.has(field) && objectNode.get(field).isTextual()) {
                        String encrypted = objectNode.get(field).asText();
                        if (isEncrypted(encrypted)) {
                            try {
                                objectNode.put(field, cryptoService.decrypt(encrypted));
                                modified = true;
                                log.debug("{}字段已解密", field);
                            } catch (Exception e) {
                                log.debug("{}字段解密失败，可能是明文", field);
                            }
                        }
                    }
                }

                if (modified) {
                    return new DecryptedHttpInputMessage(inputMessage, objectMapper.writeValueAsString(objectNode));
                }
            }

            return new DecryptedHttpInputMessage(inputMessage, body);
        } catch (Exception e) {
            log.error("请求解密处理异常", e);
            return new DecryptedHttpInputMessage(inputMessage, body);
        }
    }

    private boolean isEncrypted(String data) {
        if (data == null || data.length() < 100) {
            return false;
        }
        try {
            java.util.Base64.getDecoder().decode(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMiniProgramRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return false;
        String path = attributes.getRequest().getRequestURI();
        if (path == null) return false;
        return path.startsWith("/api/mall/") ||
                path.startsWith("/api/web/auth/wechat/qr/") ||
                path.startsWith("/api/wechat/mp/server") ||
                path.startsWith("/api/wechat/miniprogram/");
    }

    private static class DecryptedHttpInputMessage implements HttpInputMessage {
        private final HttpInputMessage original;
        private final String body;

        public DecryptedHttpInputMessage(HttpInputMessage original, String body) {
            this.original = original;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return original.getHeaders();
        }
    }
}
