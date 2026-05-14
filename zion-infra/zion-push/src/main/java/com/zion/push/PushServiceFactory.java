package com.zion.push;

import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 推送服务工厂
 * 按配置创建钉钉/飞书/企业微信/控制台推送服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushServiceFactory {

    private final SystemConfigHelper configHelper;
    private final WebhookSender webhookSender;

    private volatile PushService cachedService;
    private volatile String cachedProvider;

    /**
     * 获取当前配置的推送服务（基于 push.provider）
     */
    public PushService getPushService() {
        String provider = configHelper.getPushProvider();
        if (cachedService != null && provider.equals(cachedProvider)) {
            return cachedService;
        }
        synchronized (this) {
            if (cachedService != null && provider.equals(cachedProvider)) {
                return cachedService;
            }
            cachedService = createPushService(provider);
            cachedProvider = provider;
            return cachedService;
        }
    }

    /**
     * 获取指定平台的推送服务（用于多渠道发送）
     */
    public PushService getPushService(String provider) {
        if (DingtalkPushService.PROVIDER_TYPE.equals(provider)
            || FeishuPushService.PROVIDER_TYPE.equals(provider)
            || WechatWorkPushService.PROVIDER_TYPE.equals(provider)) {
            return createPushService(provider);
        }
        return createPushService(configHelper.getPushProvider());
    }

    private PushService createPushService(String provider) {
        String signName = configHelper.getPushSignName(provider);
        String tokenId = configHelper.getPushTokenId(provider);

        PushService service = switch (provider) {
            case DingtalkPushService.PROVIDER_TYPE -> new DingtalkPushService(webhookSender, signName, tokenId);
            case FeishuPushService.PROVIDER_TYPE -> new FeishuPushService(webhookSender, signName, tokenId);
            case WechatWorkPushService.PROVIDER_TYPE -> new WechatWorkPushService(webhookSender, signName, tokenId);
            default -> {
                log.warn("未知推送服务商: {}，使用控制台输出", provider);
                yield new ConsolePushService();
            }
        };

        log.info("创建推送服务: {} - {}", provider, service.getProviderName());
        return service;
    }

    /**
     * 刷新推送服务缓存
     */
    public void refresh() {
        synchronized (this) {
            cachedService = null;
            cachedProvider = null;
        }
        log.info("推送服务缓存已清空");
    }

    /**
     * 推送给单个用户（实际发到群）
     */
    public boolean pushToUser(String userId, String title, String content) {
        return getPushService().pushToUser(userId, title, content, null);
    }

    /**
     * 推送给所有人（实际发到群）
     */
    public boolean pushToAll(String title, String content) {
        return getPushService().pushToAll(title, content, null);
    }
}
