package com.zion.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zion.message.entity.SysNoticeSendLog;
import com.zion.message.mapper.SysNoticeSendLogMapper;
import com.zion.message.service.SysNoticeSendLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知推送记录服务实现
 */
@Service
@RequiredArgsConstructor
public class SysNoticeSendLogServiceImpl implements SysNoticeSendLogService {

    private final SysNoticeSendLogMapper sendLogMapper;

    @Override
    public void log(Long noticeId, String channel, int status, int targetCount, int successCount, String errorMsg) {
        SysNoticeSendLog log = new SysNoticeSendLog();
        log.setNoticeId(noticeId);
        log.setChannel(channel);
        log.setStatus(status);
        log.setTargetCount(targetCount);
        log.setSuccessCount(successCount);
        log.setErrorMsg(errorMsg);
        log.setSendTime(LocalDateTime.now());
        sendLogMapper.insert(log);
    }

    @Override
    public List<SysNoticeSendLog> listByNoticeId(Long noticeId) {
        return sendLogMapper.selectList(
            new LambdaQueryWrapper<SysNoticeSendLog>()
                .eq(SysNoticeSendLog::getNoticeId, noticeId)
                .orderByDesc(SysNoticeSendLog::getSendTime)
        );
    }
}
