package com.zion.sms;

/**
 * 短信服务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否发送成功
     */
    boolean sendCode(String phone, String code);

    /**
     * 发送通知短信（需在 sys_config_group.sms 配置 templateNotice 模板，占位符 {content}）
     *
     * @param phone   手机号
     * @param title   标题
     * @param content 内容
     * @return 是否发送成功
     */
    boolean sendNotice(String phone, String title, String content);

    /**
     * 获取服务商名称
     */
    String getProviderName();
}
