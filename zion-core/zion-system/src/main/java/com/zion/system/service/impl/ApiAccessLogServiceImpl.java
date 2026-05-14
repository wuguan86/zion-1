package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import tools.jackson.databind.ObjectMapper;
import com.zion.common.result.PageResult;
import com.zion.system.entity.ApiAccessLog;
import com.zion.system.mapper.ApiAccessLogMapper;
import com.zion.system.service.ApiAccessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API 访问日志服务实现
 * 实时统计走 Redis，报表分析走 MySQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiAccessLogServiceImpl extends ServiceImpl<ApiAccessLogMapper, ApiAccessLog>
        implements ApiAccessLogService {

    private static final String REDIS_QUEUE_KEY = "api:access:log:queue";
    private static final int BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void pushToRedis(ApiAccessLog accessLog) {
        try {
            String json = objectMapper.writeValueAsString(accessLog);
            redisTemplate.opsForList().leftPush(REDIS_QUEUE_KEY, json);
        } catch (Exception e) {
            log.error("API 访问日志写入 Redis 失败", e);
        }
    }

    @Override
    public void flushFromRedisToDb() {
        try {
            List<ApiAccessLog> batch = new ArrayList<>();
            for (int i = 0; i < BATCH_SIZE; i++) {
                String json = redisTemplate.opsForList().rightPop(REDIS_QUEUE_KEY);
                if (json == null) break;
                try {
                    ApiAccessLog log = objectMapper.readValue(json, ApiAccessLog.class);
                    batch.add(log);
                } catch (Exception e) {
                    log.warn("解析 API 日志 JSON 失败: {}", json, e);
                }
            }
            if (!batch.isEmpty()) {
                saveBatch(batch);
                log.debug("API 访问日志批量落库: {} 条", batch.size());
            }
        } catch (Exception e) {
            log.error("API 访问日志批量落库失败", e);
        }
    }

    @Override
    public PageResult<ApiAccessLog> page(Integer page, Integer pageSize, String apiPath, String method,
                                        Integer success, LocalDateTime startTime, LocalDateTime endTime) {
        Page<ApiAccessLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<ApiAccessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(apiPath), ApiAccessLog::getApiPath, apiPath)
                .eq(StringUtils.hasText(method), ApiAccessLog::getMethod, method)
                .eq(success != null, ApiAccessLog::getSuccess, success)
                .ge(startTime != null, ApiAccessLog::getStartTime, startTime)
                .le(endTime != null, ApiAccessLog::getEndTime, endTime)
                .orderByDesc(ApiAccessLog::getStartTime);
        return PageResult.of(page(pageParam, wrapper));
    }

    @Override
    public Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<ApiAccessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ApiAccessLog::getStartTime, start, end);

        List<ApiAccessLog> list = list(wrapper);
        long total = list.size();
        long successCount = list.stream().filter(l -> l.getSuccess() != null && l.getSuccess() == 1).count();
        long failCount = total - successCount;

        // 按日期分组统计
        Map<String, Map<String, Long>> dailyStats = new LinkedHashMap<>();
        for (ApiAccessLog log : list) {
            if (log.getStartTime() == null) continue;
            String dateKey = log.getStartTime().toLocalDate().toString();
            dailyStats.computeIfAbsent(dateKey, k -> {
                Map<String, Long> m = new HashMap<>();
                m.put("total", 0L);
                m.put("success", 0L);
                m.put("fail", 0L);
                return m;
            });
            Map<String, Long> dayMap = dailyStats.get(dateKey);
            dayMap.put("total", dayMap.get("total") + 1);
            if (log.getSuccess() != null && log.getSuccess() == 1) {
                dayMap.put("success", dayMap.get("success") + 1);
            } else {
                dayMap.put("fail", dayMap.get("fail") + 1);
            }
        }

        // 按 API 路径 Top10
        Map<String, Long> pathCount = new HashMap<>();
        for (ApiAccessLog log : list) {
            String path = log.getApiPath() != null ? log.getApiPath() : "unknown";
            pathCount.merge(path, 1L, Long::sum);
        }
        List<Map<String, Object>> topPaths = pathCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("apiPath", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        // 按 HTTP 方法统计
        Map<String, Long> methodCount = new HashMap<>();
        for (ApiAccessLog log : list) {
            String m = log.getMethod() != null ? log.getMethod() : "unknown";
            methodCount.merge(m, 1L, Long::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", total);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("dailyStats", dailyStats);
        result.put("topPaths", topPaths);
        result.put("methodCount", methodCount);
        return result;
    }
}
