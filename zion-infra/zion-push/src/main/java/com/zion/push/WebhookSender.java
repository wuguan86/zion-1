package com.zion.push;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Webhook 消息发送器
 * 负责钉钉、飞书、企业微信的 Webhook HTTP 调用，支持文本和图片
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSender {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DINGTALK = "dingtalk";
    private static final String FEISHU = "feishu";
    private static final String WECHAT_WORK = "wechat_work";

    /**
     * 发送消息到 Webhook
     *
     * @param provider   平台：dingtalk / feishu / wechat_work
     * @param webhookUrl  Webhook 完整 URL
     * @param signSecret  加签密钥（钉钉必填，其他可空）
     * @param payload     消息体
     * @return 是否成功
     */
    public boolean send(String provider, String webhookUrl, String signSecret, WebhookPayload payload) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("Webhook URL 为空 provider={}", provider);
            return false;
        }
        String url = webhookUrl;
        if (DINGTALK.equals(provider) && StringUtils.hasText(signSecret)) {
            url = appendDingtalkSign(url, signSecret);
        }
        Map<String, Object> body = buildBody(provider, payload);
        return doPost(url, body);
    }

    /**
     * 钉钉加签：URL 追加 timestamp 和 sign
     */
    public String appendDingtalkSign(String url, String secret) {
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signBytes), StandardCharsets.UTF_8);
            String sep = url.contains("?") ? "&" : "?";
            return url + sep + "timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("钉钉加签失败", e);
            return url;
        }
    }

    private Map<String, Object> buildBody(String provider, WebhookPayload payload) {
        String text = payload.getFullText();
        String imageUrl = payload.getImageUrl();
        String imageBase64 = payload.getImageBase64();
        String imageMd5 = payload.getImageMd5();

        return switch (provider) {
            case DINGTALK -> buildDingtalkBody(text, imageUrl);
            case FEISHU -> buildFeishuBody(text, imageUrl);
            case WECHAT_WORK -> buildWechatWorkBody(text, imageBase64, imageMd5);
            default -> Map.of("msgtype", "text", "text", Map.of("content", text));
        };
    }

    private Map<String, Object> buildDingtalkBody(String text, String imageUrl) {
        if (StringUtils.hasText(imageUrl)) {
            String markdown = text + "\n\n![图片](" + imageUrl + ")";
            return Map.of(
                "msgtype", "markdown",
                "markdown", Map.of("title", "消息", "text", markdown)
            );
        }
        return Map.of("msgtype", "text", "text", Map.of("content", text));
    }

    private Map<String, Object> buildFeishuBody(String text, String imageUrl) {
        if (StringUtils.hasText(imageUrl)) {
            text = text + "\n\n图片: " + imageUrl;
        }
        return Map.of("msg_type", "text", "content", Map.of("text", text));
    }

    private Map<String, Object> buildWechatWorkBody(String text, String imageBase64, String imageMd5) {
        if (StringUtils.hasText(imageBase64) && StringUtils.hasText(imageMd5)) {
            return Map.of(
                "msgtype", "image",
                "image", Map.of("base64", imageBase64, "md5", imageMd5)
            );
        }
        return Map.of("msgtype", "text", "text", Map.of("content", text));
    }

    private boolean doPost(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class
            );
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Webhook 请求失败 url={}", url, e);
            return false;
        }
    }
}
