package com.zion.message.service.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.zion.mail.EmailService;
import com.zion.message.entity.SysNotice;
import com.zion.message.entity.SysUserNotice;
import com.zion.message.mapper.SysNoticeMapper;
import com.zion.message.mapper.SysUserNoticeMapper;
import com.zion.message.entity.SysNoticeSendLog;
import com.zion.message.service.NoticeSendService;
import com.zion.message.service.SysNoticeSendLogService;
import com.zion.push.WebhookPushService;
import com.zion.system.entity.SysUser;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 通知发送服务实现
 * 配置从 sys_config_group 读取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeSendServiceImpl implements NoticeSendService {

    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;
    private final SysUserNoticeMapper userNoticeMapper;
    private final EmailService emailService;
    private final SystemConfigHelper configHelper;
    private final WebhookPushService webhookPushService;
    private final SysNoticeSendLogService sendLogService;
    private final ObjectMapper objectMapper;

    private static final String CH_STATION = "station";
    private static final String CH_EMAIL = "email";

    private static final int TARGET_USER = 1;
    private static final int TARGET_DEPT = 2;
    private static final int TARGET_ALL = 3;

    @Override
    public void send(SysNotice notice) {
        List<String> channels = parseChannels(notice.getChannels());
        List<SysUser> targetUsers = resolveTargetUsers(notice.getTargetType(), notice.getTargetIds());

        for (String ch : channels) {
            try {
                switch (ch) {
                    case CH_STATION -> sendStation(notice.getId(), targetUsers);
                    case CH_EMAIL -> sendEmail(notice, targetUsers);
                    case "dingtalk", "feishu", "wechat_work" -> sendWebhookToProvider(notice, ch);
                    default -> log.warn("未知推送渠道: {}", ch);
                }
            } catch (Exception e) {
                sendLogService.log(notice.getId(), ch, SysNoticeSendLog.STATUS_FAIL, 0, 0, e.getMessage());
                log.error("通知推送失败 channel={}, noticeId={}", ch, notice.getId(), e);
            }
        }
    }

    private List<String> parseChannels(String channelsJson) {
        if (!StringUtils.hasText(channelsJson)) return List.of(CH_STATION);
        try {
            List<String> list = objectMapper.readValue(channelsJson, new TypeReference<>() {});
            if (list == null || list.isEmpty()) return List.of(CH_STATION);
            // 兼容旧数据：webhook 展开为 dingtalk, feishu, wechat_work
            List<String> result = new ArrayList<>();
            for (String ch : list) {
                if ("webhook".equals(ch)) {
                    result.addAll(List.of("dingtalk", "feishu", "wechat_work"));
                } else {
                    result.add(ch);
                }
            }
            return result;
        } catch (Exception e) {
            return List.of(CH_STATION);
        }
    }

    private List<SysUser> resolveTargetUsers(Integer targetType, String targetIdsJson) {
        int type = targetType != null ? targetType : TARGET_ALL;
        if (type == TARGET_ALL) {
            return userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, 1));
        }
        List<Long> ids = parseIds(targetIdsJson);
        if (ids == null || ids.isEmpty()) return List.of();

        if (type == TARGET_USER) {
            return userMapper.selectBatchIds(ids).stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .toList();
        }
        if (type == TARGET_DEPT) {
            return userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getDeptId, ids)
                    .eq(SysUser::getStatus, 1));
        }
        return List.of();
    }

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            List<Number> list = objectMapper.readValue(json, new TypeReference<>() {});
            if (list == null) return List.of();
            return list.stream().map(Number::longValue).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void sendStation(Long noticeId, List<SysUser> users) {
        var existing = userNoticeMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getNoticeId, noticeId)
                .select(SysUserNotice::getUserId));
        var existingIds = existing.stream().map(SysUserNotice::getUserId).collect(java.util.stream.Collectors.toSet());
        long toSend = users.stream().filter(u -> !existingIds.contains(u.getId())).count();
        int inserted = 0;
        for (SysUser user : users) {
            if (existingIds.contains(user.getId())) continue;
            SysUserNotice un = new SysUserNotice();
            un.setUserId(user.getId());
            un.setNoticeId(noticeId);
            un.setIsRead(0);
            userNoticeMapper.insert(un);
            inserted++;
        }
        int total = users.size();
        int successCount = (int) (total - toSend + inserted);
        int status = (toSend == 0 || inserted == toSend) ? SysNoticeSendLog.STATUS_SUCCESS : SysNoticeSendLog.STATUS_FAIL;
        sendLogService.log(noticeId, CH_STATION, status, total, successCount, toSend > 0 && inserted < toSend ? "部分发送失败" : null);
        log.info("站内信推送完成, noticeId={}, 目标={}, 新增={}", noticeId, total, inserted);
    }

    private void sendEmail(SysNotice notice, List<SysUser> users) {
        if (!configHelper.isEmailEnabled()) {
            sendLogService.log(notice.getId(), CH_EMAIL, SysNoticeSendLog.STATUS_FAIL, 0, 0, "邮件服务未启用");
            log.warn("邮件服务未启用，跳过");
            return;
        }
        int targetCount = (int) users.stream().filter(u -> StringUtils.hasText(u.getEmail())).count();
        int successCount = 0;
        String lastError = null;
        for (SysUser user : users) {
            if (StringUtils.hasText(user.getEmail())) {
                try {
                    emailService.sendSimpleMail(user.getEmail(), notice.getTitle(), notice.getContent());
                    successCount++;
                } catch (Exception e) {
                    lastError = e.getMessage();
                    log.warn("邮件发送失败 to={}", user.getEmail(), e);
                }
            }
        }
        int status = (targetCount > 0 && successCount == targetCount) ? SysNoticeSendLog.STATUS_SUCCESS : SysNoticeSendLog.STATUS_FAIL;
        sendLogService.log(notice.getId(), CH_EMAIL, status, targetCount, successCount, lastError);
        log.info("邮件推送完成, noticeId={}, 发送数={}", notice.getId(), successCount);
    }

    private void sendWebhookToProvider(SysNotice notice, String provider) {
        boolean ok = webhookPushService.sendText(provider, notice.getTitle(), notice.getContent());
        sendLogService.log(notice.getId(), provider, ok ? SysNoticeSendLog.STATUS_SUCCESS : SysNoticeSendLog.STATUS_FAIL, 1, ok ? 1 : 0, ok ? null : "推送失败");
        log.info("Webhook推送 noticeId={}, provider={}, success={}", notice.getId(), provider, ok);
    }

    @Override
    public void retryChannel(Long noticeId, String channel) {
        SysNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new RuntimeException("通知不存在");
        }
        List<SysUser> targetUsers = resolveTargetUsers(notice.getTargetType(), notice.getTargetIds());
        try {
            switch (channel) {
                case CH_STATION -> sendStation(noticeId, targetUsers);
                case CH_EMAIL -> sendEmail(notice, targetUsers);
                case "dingtalk", "feishu", "wechat_work" -> sendWebhookToProvider(notice, channel);
                default -> throw new IllegalArgumentException("不支持的渠道: " + channel);
            }
        } catch (Exception e) {
            sendLogService.log(noticeId, channel, SysNoticeSendLog.STATUS_FAIL, 0, 0, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ChannelOption> getAvailableChannels() {
        List<ChannelOption> list = new ArrayList<>();
        list.add(new ChannelOption(CH_STATION, "站内信", true));
        list.add(new ChannelOption(CH_EMAIL, "邮件", configHelper.isEmailEnabled()));
        list.add(new ChannelOption("dingtalk", "钉钉", webhookPushService.isConfigured("dingtalk")));
        list.add(new ChannelOption("feishu", "飞书", webhookPushService.isConfigured("feishu")));
        list.add(new ChannelOption("wechat_work", "企业微信", webhookPushService.isConfigured("wechat_work")));
        return list;
    }
}
