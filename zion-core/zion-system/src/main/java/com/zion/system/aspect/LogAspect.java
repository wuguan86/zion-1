package com.zion.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import tools.jackson.databind.ObjectMapper;
import com.zion.system.annotation.Log;
import com.zion.system.entity.SysOperLog;
import com.zion.system.entity.SysUser;
import com.zion.system.service.SysOperLogService;
import com.zion.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperLogService operLogService;
    private final SysUserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 记录开始时间的 ThreadLocal
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 方法执行前记录开始时间
     */
    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(pointcut = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    /**
     * 处理日志
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0);
            operLog.setOperTime(LocalDateTime.now());
            
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operLog.setOperUrl(request.getRequestURI());
                operLog.setOperIp(getIpAddr(request));
                operLog.setRequestMethod(request.getMethod());
            }
            
            // 获取当前操作用户
            try {
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    SysUser user = userService.getById(userId);
                    if (user != null) {
                        operLog.setOperName(user.getUsername());
                    }
                }
            } catch (Exception ex) {
                // 忽略未登录情况
            }
            
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            
            // 设置注解信息
            operLog.setTitle(controllerLog.title());
            operLog.setBusinessType(controllerLog.businessType().getValue());
            
            // 保存请求参数
            if (controllerLog.isSaveRequestData()) {
                setRequestValue(joinPoint, operLog);
            }
            
            // 保存响应数据
            if (controllerLog.isSaveResponseData() && jsonResult != null) {
                String result = objectMapper.writeValueAsString(jsonResult);
                operLog.setJsonResult(result.length() > 2000 ? result.substring(0, 2000) : result);
            }
            
            // 异常处理
            if (e != null) {
                operLog.setStatus(1);
                String errorMsg = e.getMessage();
                operLog.setErrorMsg(errorMsg != null && errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
            }
            
            // 计算耗时
            Long startTime = START_TIME.get();
            if (startTime != null) {
                operLog.setCostTime(System.currentTimeMillis() - startTime);
            }
            
            // 异步保存日志
            operLogService.recordLog(operLog);
            
        } catch (Exception ex) {
            log.error("记录操作日志异常：", ex);
        } finally {
            // 清理ThreadLocal防止内存泄漏
            START_TIME.remove();
        }
    }

    /**
     * 获取请求参数
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                StringBuilder params = new StringBuilder();
                for (Object arg : args) {
                    if (arg != null && !isFilterObject(arg)) {
                        String jsonArg = objectMapper.writeValueAsString(arg);
                        params.append(jsonArg).append(" ");
                    }
                }
                String paramStr = params.toString().trim();
                operLog.setOperParam(paramStr.length() > 2000 ? paramStr.substring(0, 2000) : paramStr);
            }
        } catch (Exception e) {
            log.error("获取请求参数异常：", e);
        }
    }

    /**
     * 判断是否需要过滤的对象
     */
    private boolean isFilterObject(Object obj) {
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) obj;
            for (Object item : collection) {
                return item instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Object value : map.values()) {
                return value instanceof MultipartFile;
            }
        }
        return obj instanceof MultipartFile 
            || obj instanceof HttpServletRequest 
            || obj instanceof HttpServletResponse;
    }

    /**
     * 获取IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
