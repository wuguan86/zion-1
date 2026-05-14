package com.zion.web.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zion.auth.LoginRequest;
import com.zion.auth.LoginResult;
import com.zion.auth.LoginStrategyFactory;
import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.result.Result;
import com.zion.system.entity.SysUser;
import com.zion.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PC端认证控制器
 * 支持密码登录、短信登录、三方登录
 */
@RestController
@RequestMapping("/web/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SysUserService userService;
    private final com.zion.sms.SmsServiceFactory smsServiceFactory;
    private final com.zion.wechat.WechatOpenService wechatOpenService;

    /**
     * 统一登录接口
     *
     * @param request loginType: password / sms / social
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.WEB);
        if (request.getLoginType() == null) {
            request.setLoginType(LoginType.PASSWORD);
        }
        LoginResult result = loginStrategyFactory.login(request);
        return Result.ok(result);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getDetail(userId);
        List<String> roles = userService.getRoleCodes(userId);
        List<String> permissions = userService.getPermissions(userId);

        Map<String, Object> result = new HashMap<>();
        user.setPassword(null);
        result.put("user", user);
        result.put("roles", roles);
        result.put("permissions", permissions);
        return Result.ok(result);
    }

    /**
     * 获取支持的登录方式
     */
    @GetMapping("/login-types")
    public Result<?> loginTypes() {
        return Result.ok(loginStrategyFactory.getRegisteredTypes());
    }

    /**
     * 发送短信验证码（PC端）
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isEmpty()) {
            throw new com.zion.common.exception.BusinessException("手机号不能为空");
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        smsServiceFactory.sendCode(phone, code);
        return Result.ok();
    }

    /**
     * 获取微信PC扫码二维码
     */
    @GetMapping("/wechat/qrcode")
    public Result<Map<String, Object>> getWechatQrcode() {
        if (!wechatOpenService.isConfigured()) {
            throw new com.zion.common.exception.BusinessException("微信扫码登录未配置");
        }
        com.zion.wechat.WechatOpenService.QrcodeResult qrResult = wechatOpenService.createQrcode();
        Map<String, Object> data = new HashMap<>();
        data.put("ticket", qrResult.getTicket());
        data.put("qrUrl", qrResult.getQrUrl());
        data.put("expireSeconds", qrResult.getExpireSeconds());
        return Result.ok(data);
    }

    /**
     * 查询微信扫码状态
     */
    @GetMapping("/wechat/status")
    public Result<Map<String, String>> getWechatStatus(@RequestParam String ticket) {
        String status = wechatOpenService.getScanStatus(ticket);
        Map<String, String> data = new HashMap<>();
        data.put("status", status);
        return Result.ok(data);
    }
}
