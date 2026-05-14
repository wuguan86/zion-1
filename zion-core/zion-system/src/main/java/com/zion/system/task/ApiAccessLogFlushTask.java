package com.zion.system.task;

import com.zion.system.service.ApiAccessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * API 访问日志定时落库任务
 * 每 30 秒从 Redis 批量写入 MySQL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessLogFlushTask {

    private final ApiAccessLogService apiAccessLogService;

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void flush() {
        apiAccessLogService.flushFromRedisToDb();
    }
}
