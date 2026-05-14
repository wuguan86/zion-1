package com.zion.auth;

import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录策略工厂
 * 根据 LoginType + ClientType 选择对应的登录策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStrategyFactory {

    private final List<LoginStrategy> strategies;

    private final Map<LoginType, LoginStrategy> strategyMap = new EnumMap<>(LoginType.class);

    @PostConstruct
    public void init() {
        for (LoginStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
            log.info("注册登录策略: type={}, clients={}", strategy.getType().getCode(),
                    strategy.supportedClients() != null
                            ? Arrays.stream(strategy.supportedClients()).map(ClientType::getCode).collect(Collectors.joining(","))
                            : "all");
        }
    }

    /**
     * 获取登录策略
     */
    public LoginStrategy getStrategy(LoginType loginType, ClientType clientType) {
        LoginStrategy strategy = strategyMap.get(loginType);
        if (strategy == null) {
            throw new BusinessException("不支持的登录方式: " + loginType.getDesc());
        }

        // 检查客户端类型是否支持
        ClientType[] supported = strategy.supportedClients();
        if (supported != null && supported.length > 0) {
            boolean match = Arrays.stream(supported).anyMatch(c -> c == clientType);
            if (!match) {
                throw new BusinessException("该登录方式不支持当前客户端");
            }
        }

        return strategy;
    }

    /**
     * 执行登录
     */
    public LoginResult login(LoginRequest request) {
        LoginType loginType = request.getLoginType() != null ? request.getLoginType() : LoginType.PASSWORD;
        ClientType clientType = request.getClientType() != null ? request.getClientType() : ClientType.ADMIN;
        LoginStrategy strategy = getStrategy(loginType, clientType);
        return strategy.login(request);
    }

    /**
     * 获取所有已注册的登录方式
     */
    public Set<LoginType> getRegisteredTypes() {
        return strategyMap.keySet();
    }
}
