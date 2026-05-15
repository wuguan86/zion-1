package com.zion.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.WebUser;
import com.zion.system.service.SysLoginLogService;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.util.IpUtils;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录辅助工具
 * 提供登录后的公共逻辑：Token生成、Session写入、日志记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHelper {

    private final SysLoginLogService loginLogService;
    private final SystemConfigHelper configHelper;

    /**
     * 执行登录并构建结果（通用流程）
     */
    public LoginResult doLogin(SysUser user) {
        RequestInfo info = getRequestInfo();

        // 单点登录：踢掉其他设备
        if (configHelper.isSingleLogin()) {
            StpUtil.logout(user.getId());
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 写入Session
        SaSession session = StpUtil.getSession();
        session.set("loginName", user.getUsername());
        session.set("ipaddr", info.ip);
        session.set("loginLocation", IpUtils.getAddressByIp(info.ip));
        session.set("browser", info.browser);
        session.set("os", info.os);
        session.set("status", 1);
        session.set("loginTime", System.currentTimeMillis());

        // 记录登录日志
        loginLogService.recordLog(user.getUsername(), 0, "登录成功", info.ip, info.browser, info.os);

        // 构建结果
        return LoginResult.of(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        );
    }

    /**
     * 执行PC用户端登录并构建结果
     */
    public LoginResult doWebLogin(WebUser user) {
        RequestInfo info = getRequestInfo();

        if (configHelper.isSingleLogin()) {
            StpUtil.logout(user.getId());
        }

        StpUtil.login(user.getId());

        SaSession session = StpUtil.getSession();
        session.set("loginName", user.getNickname() != null ? user.getNickname() : user.getUsername());
        session.set("userSource", "web");
        session.set("ipaddr", info.ip);
        session.set("loginLocation", IpUtils.getAddressByIp(info.ip));
        session.set("browser", info.browser);
        session.set("os", info.os);
        session.set("status", 1);
        session.set("loginTime", System.currentTimeMillis());

        loginLogService.recordLog(user.getNickname() != null ? user.getNickname() : user.getUsername(),
                0, "PC用户端登录成功", info.ip, info.browser, info.os);

        return LoginResult.of(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        );
    }

    /**
     * 记录登录失败日志
     */
    public void recordFailLog(String username, String message) {
        RequestInfo info = getRequestInfo();
        loginLogService.recordLog(username, 1, message, info.ip, info.browser, info.os);
    }

    /**
     * 提取当前请求的 IP、浏览器、操作系统信息
     */
    private RequestInfo getRequestInfo() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String ip = IpUtils.getIpAddr(request);
        UserAgent ua = request != null ? UserAgentUtil.parse(request.getHeader("User-Agent")) : null;
        String browser = ua != null ? ua.getBrowser().getName() : "Unknown";
        String os = ua != null ? ua.getOs().getName() : "Unknown";
        return new RequestInfo(ip, browser, os);
    }

    /**
     * 请求信息
     */
    private record RequestInfo(String ip, String browser, String os) {}
}
