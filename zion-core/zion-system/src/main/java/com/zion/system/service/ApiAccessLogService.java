package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.system.entity.ApiAccessLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API 访问日志服务
 */
public interface ApiAccessLogService extends IService<ApiAccessLog> {

    /**
     * 异步写入 Redis（供 AOP 采集调用）
     */
    void pushToRedis(ApiAccessLog log);

    /**
     * 定时任务：从 Redis 批量落库 MySQL
     */
    void flushFromRedisToDb();

    /**
     * 分页查询（用于报表）
     */
    PageResult<ApiAccessLog> page(Integer page, Integer pageSize, String apiPath, String method,
                                  Integer success, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计数据（用于 ECharts 可视化）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate);
}
