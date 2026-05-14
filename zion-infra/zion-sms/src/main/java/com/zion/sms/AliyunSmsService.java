package com.zion.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.zion.sms.service.SmsLogService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsService implements SmsService {

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        String accessKeyId = configHelper.getSmsAccessKeyId();
        String accessKeySecret = configHelper.getSmsAccessKeySecret();
        String signName = configHelper.getSmsSignName();
        String templateCode = configHelper.getSmsTemplateVerifyCode();
        String region = configHelper.getSmsRegion();

        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("阿里云短信配置不完整，使用控制台打印模式");
            log.info("============================================");
            log.info("【短信验证码 - 阿里云(未配置)】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("有效期: 5分钟");
            log.info("============================================");
            // 记录日志（控制台模式）
            smsLogService.logVerifyCode(phone, code, "console", true, "控制台打印模式", null);
            return true;
        }

        String bizId = null;
        String resultMsg = null;
        boolean success = false;

        try {
            // 创建阿里云短信客户端
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setRegionId(region)
                    .setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(config);

            // 构建短信发送请求
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            // 发送短信
            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody().getCode();
            bizId = response.getBody().getBizId();
            resultMsg = response.getBody().getMessage();

            if ("OK".equals(respCode)) {
                log.info("阿里云短信发送成功: phone={}, bizId={}", phone, bizId);
                success = true;
            } else {
                log.error("阿里云短信发送失败: code={}, message={}", respCode, resultMsg);
            }
        } catch (Exception e) {
            log.error("阿里云短信发送异常", e);
            resultMsg = e.getMessage();
        }

        // 记录发送日志
        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        String templateCode = configHelper.getSmsTemplateNotice();
        if (templateCode == null || templateCode.isEmpty()) {
            log.warn("通知短信模板未配置，使用控制台打印");
            log.info("【短信通知】phone={}, title={}, content={}", phone, title, content);
            smsLogService.logVerifyCode(phone, content != null ? content.substring(0, Math.min(20, content.length())) : "", "aliyun", true, "模板未配置", null);
            return true;
        }
        String accessKeyId = configHelper.getSmsAccessKeyId();
        String accessKeySecret = configHelper.getSmsAccessKeySecret();
        String signName = configHelper.getSmsSignName();
        String region = configHelper.getSmsRegion();
        if (accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("阿里云短信配置不完整");
            return false;
        }
        try {
            String param = "{\"content\":\"" + (content != null ? content.replace("\"", "\\\"").substring(0, Math.min(100, content.length())) : "") + "\"}";
            Config config = new Config().setAccessKeyId(accessKeyId).setAccessKeySecret(accessKeySecret).setRegionId(region).setEndpoint("dysmsapi.aliyuncs.com");
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam(param);
            SendSmsResponse response = client.sendSms(request);
            boolean success = "OK".equals(response.getBody().getCode());
            smsLogService.logVerifyCode(phone, content != null ? content.substring(0, Math.min(20, content.length())) : "", "aliyun", success, response.getBody().getMessage(), response.getBody().getBizId());
            return success;
        } catch (Exception e) {
            log.error("阿里云通知短信发送失败", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }
}
