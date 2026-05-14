package com.zion.sms;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.zion.sms.service.SmsLogService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * 七牛云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QiniuSmsService implements SmsService {

    private static final String API_ENDPOINT = "https://sms.qiniuapi.com/v1/message/single";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        String accessKey = configHelper.getSmsAccessKeyId();
        String secretKey = configHelper.getSmsAccessKeySecret();
        String templateId = configHelper.getSmsTemplateVerifyCode();

        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            log.warn("七牛云短信配置不完整，使用控制台打印模式");
            log.info("============================================");
            log.info("【短信验证码 - 七牛云(未配置)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("有效期: 5分钟");
            log.info("============================================");
            // 记录日志（控制台模式）
            smsLogService.logVerifyCode(phone, code, "console", true, "控制台打印模式", null);
            return true;
        }

        String bizId = null;
        String resultMsg = null;
        boolean success = false;

        try {
            // 构建请求体
            String requestBody = String.format(
                    "{\"template_id\":\"%s\",\"mobile\":\"%s\",\"parameters\":{\"code\":\"%s\"}}",
                    templateId, phone, code
            );

            // 计算签名
            String authorization = generateAuthorization(accessKey, secretKey, requestBody);

            // 创建 HTTP 客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.body());
            
            if (response.statusCode() == 200) {
                bizId = responseJson.has("message_id") ? responseJson.get("message_id").asText() : null;
                log.info("七牛云短信发送成功: phone={}, messageId={}", phone, bizId);
                success = true;
                resultMsg = "发送成功";
            } else {
                resultMsg = responseJson.has("message") ? responseJson.get("message").asText() : "未知错误";
                String errorCode = responseJson.has("error") ? responseJson.get("error").asText() : "unknown";
                log.error("七牛云短信发送失败: statusCode={}, error={}, message={}", 
                        response.statusCode(), errorCode, resultMsg);
            }
        } catch (Exception e) {
            log.error("七牛云短信发送异常", e);
            resultMsg = e.getMessage();
        }

        // 记录发送日志
        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    /**
     * 生成七牛云 API 认证头
     */
    private String generateAuthorization(String accessKey, String secretKey, String body) throws Exception {
        // 构建签名字符串
        String signingStr = "POST /v1/message/single\nHost: sms.qiniuapi.com\nContent-Type: application/json\n\n" + body;
        
        // 计算 HMAC-SHA1 签名
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(signingStr.getBytes(StandardCharsets.UTF_8));
        
        // Base64 编码（URL 安全）
        String encodedSign = Base64.getUrlEncoder().withoutPadding().encodeToString(signData);
        
        return "Qiniu " + accessKey + ":" + encodedSign;
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        String templateId = configHelper.getSmsTemplateNotice();
        if (templateId == null || templateId.isEmpty()) {
            log.info("【短信通知 - 七牛云】模板未配置, phone={}, content={}", phone, content);
            return true;
        }
        return sendCode(phone, content != null ? content.substring(0, Math.min(20, content.length())) : "");
    }

    @Override
    public String getProviderName() {
        return "qiniu";
    }
}
