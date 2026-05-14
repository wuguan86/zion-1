package com.zion.push;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 控制台推送服务（开发测试用）
 */
@Slf4j
public class ConsolePushService implements PushService {

    public static final String PROVIDER_TYPE = "console";

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }

    @Override
    public String getProviderName() {
        return "控制台输出";
    }

    @Override
    public boolean pushToUser(String userId, String title, String content, Map<String, String> extras) {
        log.info("【控制台推送】推送给用户: {}", userId);
        log.info("  标题: {}", title);
        log.info("  内容: {}", content);
        log.info("  扩展: {}", extras);
        return true;
    }

    @Override
    public boolean pushToUsers(List<String> userIds, String title, String content, Map<String, String> extras) {
        log.info("【控制台推送】推送给多个用户: {}", userIds);
        log.info("  标题: {}", title);
        log.info("  内容: {}", content);
        log.info("  扩展: {}", extras);
        return true;
    }

    @Override
    public boolean pushToAll(String title, String content, Map<String, String> extras) {
        log.info("【控制台推送】推送给所有用户");
        log.info("  标题: {}", title);
        log.info("  内容: {}", content);
        log.info("  扩展: {}", extras);
        return true;
    }

    @Override
    public boolean pushToTags(List<String> tags, String title, String content, Map<String, String> extras) {
        log.info("【控制台推送】推送给标签: {}", tags);
        log.info("  标题: {}", title);
        log.info("  内容: {}", content);
        log.info("  扩展: {}", extras);
        return true;
    }

    @Override
    public boolean pushToDevice(String registrationId, String title, String content, Map<String, String> extras) {
        log.info("【控制台推送】推送给设备: {}", registrationId);
        log.info("  标题: {}", title);
        log.info("  内容: {}", content);
        log.info("  扩展: {}", extras);
        return true;
    }
}
