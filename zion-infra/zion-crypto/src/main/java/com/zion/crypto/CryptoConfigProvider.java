package com.zion.crypto;

/**
 * 加密配置提供者接口
 * 由业务层（zion-system）实现，提供加密配置
 * 避免基础设施层直接依赖业务层的 SystemConfigHelper
 */
public interface CryptoConfigProvider {

    /**
     * 是否全局加密
     */
    boolean isGlobalEncrypt();
}
