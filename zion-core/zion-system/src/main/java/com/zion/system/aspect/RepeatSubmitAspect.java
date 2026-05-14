package com.zion.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import tools.jackson.databind.ObjectMapper;
import com.zion.common.exception.BusinessException;
import com.zion.system.annotation.RepeatSubmit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 * 基于 Redis 实现，使用 用户Token + 请求URI + 请求参数摘要 作为唯一键
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "repeat_submit:";

    @Before("@annotation(repeatSubmit)")
    public void before(JoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return;
        }

        // 构建唯一标识：用户token + 请求URI + 参数摘要
        String cacheKey = buildCacheKey(request, joinPoint);

        // 尝试设置 Redis key，如果已存在则说明是重复提交
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(cacheKey, "1", repeatSubmit.interval(), TimeUnit.MILLISECONDS);

        if (Boolean.FALSE.equals(success)) {
            log.warn("重复提交拦截: URI={}, Key={}", request.getRequestURI(), cacheKey);
            throw new BusinessException(repeatSubmit.message());
        }
    }

    /**
     * 构建缓存 key
     * 格式: repeat_submit:{userId}:{uri}:{paramsHash}
     */
    private String buildCacheKey(HttpServletRequest request, JoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder(CACHE_KEY_PREFIX);

        // 用户标识
        try {
            sb.append(StpUtil.getLoginId()).append(":");
        } catch (Exception e) {
            // 未登录场景使用 SessionId
            sb.append(request.getSession().getId()).append(":");
        }

        // 请求 URI
        sb.append(request.getRequestURI()).append(":");

        // 请求参数摘要
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                StringBuilder paramBuilder = new StringBuilder();
                for (Object arg : args) {
                    if (arg == null || arg instanceof HttpServletRequest
                            || arg instanceof jakarta.servlet.http.HttpServletResponse
                            || arg instanceof org.springframework.web.multipart.MultipartFile) {
                        continue;
                    }
                    paramBuilder.append(objectMapper.writeValueAsString(arg));
                }
                sb.append(md5(paramBuilder.toString()));
            }
        } catch (Exception e) {
            log.debug("参数序列化失败，使用无参数key", e);
        }

        return sb.toString();
    }

    /**
     * MD5 摘要（用于缩短参数 hash）
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
