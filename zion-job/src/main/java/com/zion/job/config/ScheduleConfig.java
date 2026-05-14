package com.zion.job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 定时任务配置
 */
@Configuration
@EnableAsync
public class ScheduleConfig {
    // 定时任务相关配置 - 使用 Quartz 管理定时任务
}
