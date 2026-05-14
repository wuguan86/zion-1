package com.zion.auth;

import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;

/**
 * 登录策略接口
 * 各种登录方式实现此接口
 */
public interface LoginStrategy {

    /**
     * 获取策略标识
     */
    LoginType getType();

    /**
     * 支持的客户端类型（null 表示全部支持）
     */
    default ClientType[] supportedClients() {
        return null;
    }

    /**
     * 执行登录
     */
    LoginResult login(LoginRequest request);
}
