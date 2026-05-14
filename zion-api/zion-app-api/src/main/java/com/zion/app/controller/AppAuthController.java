package com.zion.app.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zion.auth.LoginRequest;
import com.zion.auth.LoginResult;
import com.zion.auth.LoginStrategyFactory;
import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.Result;
import com.zion.file.entity.SysFile;
import com.zion.file.service.SysFileService;
import com.zion.sms.SmsServiceFactory;
import com.zion.system.entity.SysUser;
import com.zion.system.service.SysUserService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * App端认证控制器
 * 支持：小程序登录、手机验证码登录、三方授权登录
 */
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SmsServiceFactory smsServiceFactory;
    private final SysFileService fileService;
    private final SysUserService userService;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_KEY = "sms:login:";

    /**
     * 统一登录接口
     *
     * @param request loginType: miniprogram / sms / social
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.APP);
        if (request.getLoginType() == null) {
            request.setLoginType(LoginType.MINIPROGRAM);
        }
        LoginResult result = loginStrategyFactory.login(request);
        return Result.ok(result);
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody SmsCodeRequest request) {
        String phone = request.getPhone();
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }

        String limitKey = "sms:limit:" + phone;
        if (redisTemplate.hasKey(limitKey)) {
            throw new BusinessException("发送太频繁，请稍后再试");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            throw new BusinessException("短信发送失败");
        }

        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        return Result.ok();
    }

    /**
     * App端头像上传（无需文件管理权限）
     */
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        SysFile sysFile = fileService.uploadImage(file);
        return Result.ok(sysFile.getUrl());
    }

    /**
     * App端获取个人信息
     */
    @GetMapping("/profile")
    public Result<SysUser> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getDetail(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 清除敏感字段
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * App端更新个人信息（头像、昵称、邮箱、手机、性别）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody ProfileRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            String nickname = request.getNickname().trim();
            user.setNickname(nickname);
            user.setUsername(nickname);
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * App端修改密码
     */
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        configHelper.validatePassword(request.getNewPassword());
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    @Data
    public static class ProfileRequest {
        private String nickname;
        private String avatar;
        private String email;
        private String phone;
        private Integer gender;
    }

    @Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class SmsCodeRequest {
        private String phone;
    }
}
