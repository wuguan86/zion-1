package com.zion.social;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 三方登录工厂（工厂模式）
 * 根据平台名称获取对应的三方登录服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLoginFactory {

    private final List<SocialLoginService> socialLoginServices;

    private final Map<String, SocialLoginService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (SocialLoginService service : socialLoginServices) {
            serviceMap.put(service.getPlatform(), service);
            log.info("注册三方登录服务: {}", service.getPlatform());
        }
    }

    /**
     * 根据平台获取三方登录服务
     *
     * @param platform 平台名称（wechat_mp / wechat_mini / alipay / apple / github 等）
     * @return 三方登录服务
     */
    public SocialLoginService getService(String platform) {
        SocialLoginService service = serviceMap.get(platform);
        if (service == null) {
            throw new RuntimeException("不支持的三方登录平台: " + platform);
        }
        return service;
    }

    /**
     * 获取所有已注册的平台列表
     */
    public Set<String> getSupportedPlatforms() {
        return serviceMap.keySet();
    }

    /**
     * 检查平台是否支持
     */
    public boolean isSupported(String platform) {
        return serviceMap.containsKey(platform);
    }
}
