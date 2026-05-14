package com.zion.system.config;

import cn.dev33.satoken.SaManager;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 动态配置加载器
 * 应用启动时从数据库加载Token配置并覆盖yml默认值
 * 管理员在安全配置页面保存Token配置后，调用 applyConfig() 即时生效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenConfigLoader implements ApplicationRunner {

    private final SystemConfigHelper configHelper;

    @Override
    public void run(ApplicationArguments args) {
        applyConfig();
    }

    /**
     * 从数据库加载配置并应用到 Sa-Token
     * 启动时自动调用，保存安全配置后也可手动调用
     */
    public void applyConfig() {
        try {
            cn.dev33.satoken.config.SaTokenConfig config = SaManager.getConfig();

            config.setTokenName(configHelper.getTokenName());
            config.setTimeout(configHelper.getTokenTimeout());
            config.setActiveTimeout(configHelper.getTokenActiveTimeout());
            config.setIsConcurrent(configHelper.isTokenConcurrent());
            config.setIsShare(configHelper.isTokenShare());
            config.setTokenStyle(configHelper.getTokenStyle());
            config.setIsLog(configHelper.isTokenLog());
            config.setIsReadBody(configHelper.isTokenReadBody());
            config.setIsReadCookie(configHelper.isTokenReadCookie());
            config.setIsReadHeader(configHelper.isTokenReadHeader());
            config.setIsPrint(configHelper.isTokenPrint());

            log.info("Sa-Token配置已从数据库加载并应用: tokenName={}, timeout={}s, activeTimeout={}s, " +
                            "isConcurrent={}, isShare={}, tokenStyle={}",
                    config.getTokenName(), config.getTimeout(), config.getActiveTimeout(),
                    config.getIsConcurrent(), config.getIsShare(), config.getTokenStyle());
        } catch (Exception e) {
            log.warn("从数据库加载Sa-Token配置失败，使用yml默认配置: {}", e.getMessage());
        }
    }
}
