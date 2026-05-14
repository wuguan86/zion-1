package com.zion.db.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // 启用异步支持，用于异步记录日志等
}
