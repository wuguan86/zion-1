package com.zion.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 控制台短信服务（开发测试用）
 * 将验证码打印到控制台，不实际发送
 */
@Slf4j
@Service
public class ConsoleSmsService implements SmsService {

    @Override
    public boolean sendCode(String phone, String code) {
        log.info("============================================");
        log.info("【短信验证码 - 控制台模式】");
        log.info("手机号: {}", phone);
        log.info("验证码: {}", code);
        log.info("有效期: 5分钟");
        log.info("============================================");
        return true;
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        log.info("【短信通知 - 控制台模式】phone={}, title={}, content={}", phone, title, content);
        return true;
    }

    @Override
    public String getProviderName() {
        return "console";
    }
}
