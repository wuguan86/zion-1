package com.zion.message.service;

import com.zion.message.entity.SysNoticeSendLog;

import java.util.List;

/**
 * 通知推送记录服务
 */
public interface SysNoticeSendLogService {

    /**
     * 记录推送结果
     */
    void log(Long noticeId, String channel, int status, int targetCount, int successCount, String errorMsg);

    /**
     * 查询某条通知的推送记录（按时间倒序）
     */
    List<SysNoticeSendLog> listByNoticeId(Long noticeId);
}
