package com.zion.admin.config;

import tools.jackson.databind.ObjectMapper;
import com.zion.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 演示模式拦截器
 * 在演示模式下，禁止新增、编辑、删除操作
 */
@Component
public class DemoModeInterceptor implements HandlerInterceptor {

    @Value("${Zion.demo-mode:false}")
    private boolean demoMode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 白名单路径（即使在演示模式下也允许操作）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",          // 登录
            "/api/auth/register",       // 注册
            "/api/auth/logout",         // 登出
            "/api/auth/captcha",        // 验证码
            "/api/auth/sms-code",       // 短信验证码
            "/api/sys/chat/send",       // 发送聊天消息
            "/api/sys/chat/read",       // 标记已读
            "/api/sys/notice/read",     // 标记通知已读
            "/api/chat/group"           // 群聊相关
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未开启演示模式，直接放行
        if (!demoMode) {
            return true;
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 只拦截写操作（POST、PUT、DELETE）
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 检查白名单
        for (String path : WHITE_LIST) {
            if (uri.startsWith(path)) {
                return true;
            }
        }

        // 演示模式下，禁止写操作
        responseError(response, "演示模式，不允许操作");
        return false;
    }

    /**
     * 返回错误响应
     */
    private void responseError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.fail(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
