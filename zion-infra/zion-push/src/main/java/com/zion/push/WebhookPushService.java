package com.zion.push;

import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Webhook 推送服务
 * 供 zion-message 等模块调用，向钉钉/飞书/企业微信群机器人发送消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPushService {

    private final SystemConfigHelper configHelper;
    private final WebhookSender webhookSender;

    /**
     * 发送文本消息
     */
    public boolean sendText(String provider, String title, String content) {
        return send(provider, WebhookPayload.builder()
            .title(title)
            .content(content)
            .build());
    }

    /**
     * 发送文本 + 图片（图片为 URL，钉钉可用 Markdown 嵌入）
     */
    public boolean sendTextWithImage(String provider, String title, String content, String imageUrl) {
        return send(provider, WebhookPayload.builder()
            .title(title)
            .content(content)
            .imageUrl(imageUrl)
            .build());
    }

    /**
     * 发送文本 + Base64 图片（企业微信专用）
     */
    public boolean sendTextWithImageBase64(String provider, String title, String content,
                                           String imageBase64, String imageMd5) {
        return send(provider, WebhookPayload.builder()
            .title(title)
            .content(content)
            .imageBase64(imageBase64)
            .imageMd5(imageMd5)
            .build());
    }

    /**
     * 通用发送
     */
    public boolean send(String provider, WebhookPayload payload) {
        String webhookUrl = configHelper.getPushTokenId(provider);
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("Webhook 未配置 provider={}", provider);
            return false;
        }
        String signSecret = configHelper.getPushSignName(provider);
        return webhookSender.send(provider, webhookUrl, signSecret, payload);
    }

    /**
     * 检查指定平台是否已配置 Webhook
     */
    public boolean isConfigured(String provider) {
        return StringUtils.hasText(configHelper.getPushTokenId(provider));
    }
}
