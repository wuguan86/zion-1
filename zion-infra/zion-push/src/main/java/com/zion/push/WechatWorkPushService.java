package com.zion.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 企业微信 Webhook 推送服务
 * 支持文本、图片（Base64+MD5）
 */
@Slf4j
public class WechatWorkPushService implements PushService {

    public static final String PROVIDER_TYPE = "wechat_work";

    private final WebhookSender webhookSender;
    private final String signSecret;
    private final String webhookUrl;

    public WechatWorkPushService(WebhookSender webhookSender, String signSecret, String webhookUrl) {
        this.webhookSender = webhookSender;
        this.signSecret = signSecret;
        this.webhookUrl = webhookUrl;
        log.info("初始化企业微信推送服务, webhook 已配置={}", StringUtils.hasText(webhookUrl));
    }

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getProviderName() {
        return "企业微信";
    }

    @Override
    public boolean pushToUser(String userId, String title, String content, Map<String, String> extras) {
        return pushToAll(title, content, extras);
    }

    @Override
    public boolean pushToUsers(List<String> userIds, String title, String content, Map<String, String> extras) {
        return pushToAll(title, content, extras);
    }

    @Override
    public boolean pushToAll(String title, String content, Map<String, String> extras) {
        String imageBase64 = extras != null ? extras.get("imageBase64") : null;
        String imageMd5 = extras != null ? extras.get("imageMd5") : null;
        WebhookPayload payload = WebhookPayload.builder()
            .title(title)
            .content(content)
            .imageBase64(imageBase64)
            .imageMd5(imageMd5)
            .build();
        return webhookSender.send(PROVIDER_TYPE, webhookUrl, signSecret, payload);
    }

    @Override
    public boolean pushToTags(List<String> tags, String title, String content, Map<String, String> extras) {
        return pushToAll(title, content, extras);
    }

    @Override
    public boolean pushToDevice(String registrationId, String title, String content, Map<String, String> extras) {
        return pushToAll(title, content, extras);
    }
}
