package com.zion.message.service;

import com.zion.message.entity.SysNotice;

import java.util.List;

/**
 * 通知发送服务
 * 从 sys_config_group 读取配置，支持站内信/邮件/短信/Webhook 多渠道推送
 */
public interface NoticeSendService {

    /**
     * 发送通知（根据 channels、targetType、targetIds）
     *
     * @param notice 通知内容
     */
    void send(SysNotice notice);

    /**
     * 获取可选推送渠道列表（根据 sys_config_group 配置是否可用）
     */
    List<ChannelOption> getAvailableChannels();

    /**
     * 重试指定渠道的推送
     *
     * @param noticeId 通知ID
     * @param channel  渠道：station/email/dingtalk/feishu/wechat_work
     */
    void retryChannel(Long noticeId, String channel);

    /**
     * 推送渠道选项
     */
    record ChannelOption(String code, String name, boolean enabled) {}
}
