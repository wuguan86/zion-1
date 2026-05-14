package com.zion.sms;

import com.zion.system.helper.SystemConfigHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信服务工厂
 * 使用策略模式，根据配置选择对应的短信服务商
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsServiceFactory {

    private final SystemConfigHelper configHelper;
    private final List<SmsService> smsServices;

    private final Map<String, SmsService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 将所有短信服务注册到Map中
        for (SmsService service : smsServices) {
            serviceMap.put(service.getProviderName(), service);
            log.info("注册短信服务: {}", service.getProviderName());
        }
    }

    /**
     * 获取当前配置的短信服务
     */
    public SmsService getService() {
        String provider = configHelper.getSmsProvider();
        SmsService service = serviceMap.get(provider);

        if (service == null) {
            log.warn("未找到短信服务商: {}, 使用控制台模式", provider);
            service = serviceMap.get("console");
        }

        return service;
    }

    /**
     * 获取指定的短信服务
     */
    public SmsService getService(String provider) {
        return serviceMap.getOrDefault(provider, serviceMap.get("console"));
    }

    /**
     * 发送短信验证码（使用当前配置的服务商）
     */
    public boolean sendCode(String phone, String code) {
        return getService().sendCode(phone, code);
    }

    /**
     * 发送通知短信（使用当前配置的服务商）
     */
    public boolean sendNotice(String phone, String title, String content) {
        return getService().sendNotice(phone, title, content);
    }
}
