package com.zion.pay;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付服务工厂
 * 使用策略模式，根据支付类型选择对应的支付服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceFactory {

    private final List<PayService> payServices;

    private final Map<String, PayService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 将所有支付服务注册到Map中
        for (PayService service : payServices) {
            serviceMap.put(service.getPayType(), service);
            log.info("注册支付服务: {} - {}", service.getPayType(), service.getPayTypeName());
        }
    }

    /**
     * 获取指定类型的支付服务
     * @param payType 支付类型 (wechat/alipay)
     */
    public PayService getService(String payType) {
        PayService service = serviceMap.get(payType);
        if (service == null) {
            throw new RuntimeException("不支持的支付类型: " + payType);
        }
        return service;
    }

    /**
     * 检查是否支持该支付类型
     */
    public boolean isSupported(String payType) {
        return serviceMap.containsKey(payType);
    }

    /**
     * 获取所有支持的支付类型
     */
    public List<String> getSupportedTypes() {
        return payServices.stream().map(PayService::getPayType).toList();
    }

    /**
     * 创建测试订单
     */
    public Map<String, String> createTestOrder(String payType) {
        return getService(payType).createTestOrder();
    }
    
    /**
     * 创建小程序支付订单
     */
    public Map<String, String> createMiniProgramPayOrder(String orderNo, java.math.BigDecimal amount, String description, String openId) {
        PayService service = getService("wechat");
        if (service instanceof WechatPayService wechatPayService) {
            return wechatPayService.createMiniProgramOrder(orderNo, amount, description, openId);
        }
        throw new RuntimeException("微信支付服务不可用");
    }
}
