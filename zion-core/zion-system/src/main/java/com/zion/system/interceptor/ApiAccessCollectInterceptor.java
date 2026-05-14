package com.zion.system.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.zion.system.entity.ApiAccessLog;
import com.zion.system.service.ApiAccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * API 访问采集拦截器
 * 采集请求信息，异步写入 Redis，不影响主业务性能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessCollectInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "apiAccessStartTime";

    private final ApiAccessLogService apiAccessLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            String path = request.getRequestURI();
            // 排除非 API 及监控类路径
            if (shouldExclude(path)) {
                return;
            }

            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime == null) startTime = System.currentTimeMillis();

            long costTime = System.currentTimeMillis() - startTime;
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTimeDt = endTime.minusNanos(costTime * 1_000_000);

            ApiAccessLog log = new ApiAccessLog();
            log.setStartTime(startTimeDt);
            log.setEndTime(endTime);
            log.setApiPath(path);
            log.setMethod(request.getMethod());
            log.setStatusCode(response.getStatus());
            log.setSuccess(response.getStatus() >= 200 && response.getStatus() < 400 ? 1 : 0);
            log.setCostTime(costTime);
            log.setIp(getIpAddr(request));

            try {
                if (StpUtil.isLogin()) {
                    log.setUserId(StpUtil.getLoginIdAsLong());
                }
            } catch (Exception e) {
                // 未登录
            }

            apiAccessLogService.pushToRedis(log);
        } catch (Exception e) {
            log.warn("API 访问采集异常", e);
        }
    }

    private boolean shouldExclude(String path) {
        if (path == null) return true;
        return path.startsWith("/actuator") || path.startsWith("/error") || path.contains(".");
    }

    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
