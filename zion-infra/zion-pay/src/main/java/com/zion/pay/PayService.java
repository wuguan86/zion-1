package com.zion.pay;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 创建测试订单（Native扫码支付）
     * @return 包含 orderNo, qrcode, payUrl 等信息
     */
    Map<String, String> createTestOrder();

    /**
     * 获取支付方式名称
     */
    String getPayType();

    /**
     * 获取支付方式显示名称
     */
    String getPayTypeName();
}
