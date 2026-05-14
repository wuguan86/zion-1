package com.zion.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.sms.entity.SmsLog;

/**
 * 短信发送记录 Service
 */
public interface SmsLogService extends IService<SmsLog> {

    /**
     * 记录短信发送日志
     *
     * @param phone          手机号
     * @param content        内容/验证码
     * @param smsType        短信类型
     * @param templateId     模板ID
     * @param templateParams 模板参数
     * @param provider       服务商
     * @param success        是否成功
     * @param resultMsg      结果消息
     * @param bizId          服务商返回的消息ID
     * @param userId         用户ID
     * @param bizType        业务类型
     * @param ip             IP地址
     */
    void log(String phone, String content, String smsType, String templateId,
             String templateParams, String provider, boolean success,
             String resultMsg, String bizId, Long userId, String bizType, String ip);

    /**
     * 记录验证码短信日志（简化方法）
     */
    void logVerifyCode(String phone, String code, String provider, boolean success, String resultMsg, String bizId);
}
