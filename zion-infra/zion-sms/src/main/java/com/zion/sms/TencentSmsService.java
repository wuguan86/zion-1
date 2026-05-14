package com.zion.sms;

import com.zion.sms.service.SmsLogService;
import com.zion.system.helper.SystemConfigHelper;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 腾讯云短信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentSmsService implements SmsService {

    private final SystemConfigHelper configHelper;
    private final SmsLogService smsLogService;

    @Override
    public boolean sendCode(String phone, String code) {
        String secretId = configHelper.getSmsAccessKeyId();
        String secretKey = configHelper.getSmsAccessKeySecret();
        String appId = configHelper.getSmsTencentAppId();
        String signName = configHelper.getSmsSignName();
        String templateId = configHelper.getSmsTemplateVerifyCode();
        String region = configHelper.getSmsRegion();

        if (secretId.isEmpty() || secretKey.isEmpty()) {
            log.warn("腾讯云短信配置不完整，使用控制台打印模式");
            log.info("============================================");
            log.info("【短信验证码 - 腾讯云(未配置)】");
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
            // 创建认证对象
            Credential cred = new Credential(secretId, secretKey);

            // 配置HTTP连接
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);

            // 配置客户端
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            clientProfile.setSignMethod("HmacSHA256");

            // 创建短信客户端
            SmsClient client = new SmsClient(cred, region, clientProfile);

            // 构建短信发送请求
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(appId);
            request.setSignName(signName);
            request.setTemplateId(templateId);
            // 手机号需要加上+86国家码
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            // 模板参数，与模板中的变量对应
            request.setTemplateParamSet(new String[]{code});

            // 发送短信
            SendSmsResponse response = client.SendSms(request);
            SendStatus[] sendStatusSet = response.getSendStatusSet();

            if (sendStatusSet != null && sendStatusSet.length > 0) {
                SendStatus status = sendStatusSet[0];
                bizId = status.getSerialNo();
                resultMsg = status.getMessage();
                if ("Ok".equals(status.getCode())) {
                    log.info("腾讯云短信发送成功: phone={}, serialNo={}", phone, bizId);
                    success = true;
                } else {
                    log.error("腾讯云短信发送失败: code={}, message={}", status.getCode(), resultMsg);
                }
            } else {
                resultMsg = "响应结果为空";
                log.error("腾讯云短信发送失败: {}", resultMsg);
            }
        } catch (Exception e) {
            log.error("腾讯云短信发送异常", e);
            resultMsg = e.getMessage();
        }

        // 记录发送日志
        smsLogService.logVerifyCode(phone, code, getProviderName(), success, resultMsg, bizId);
        return success;
    }

    @Override
    public boolean sendNotice(String phone, String title, String content) {
        String templateId = configHelper.getSmsTemplateNotice();
        if (templateId == null || templateId.isEmpty()) {
            log.info("【短信通知 - 腾讯云】模板未配置, phone={}, content={}", phone, content);
            return true;
        }
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(configHelper.getSmsTencentAppId());
            request.setSignName(configHelper.getSmsSignName());
            request.setTemplateId(templateId);
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{content != null ? content.substring(0, Math.min(100, content.length())) : ""});
            Credential cred = new Credential(configHelper.getSmsAccessKeyId(), configHelper.getSmsAccessKeySecret());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, configHelper.getSmsRegion(), clientProfile);
            SendSmsResponse response = client.SendSms(request);
            if (response.getSendStatusSet() != null && response.getSendStatusSet().length > 0) {
                return "Ok".equals(response.getSendStatusSet()[0].getCode());
            }
        } catch (Exception e) {
            log.error("腾讯云通知短信发送失败", e);
        }
        return false;
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }
}
