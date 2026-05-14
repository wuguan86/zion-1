package com.zion.admin.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zion.auth.LoginRequest;
import com.zion.auth.LoginResult;
import com.zion.auth.LoginStrategyFactory;
import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.Result;
import com.zion.system.entity.SysMenu;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.SysUserRole;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.service.*;
import com.zion.sms.SmsServiceFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 后台认证控制器
 * 登录通过 zion-auth 统一策略工厂处理
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysUserService userService;
    private final SysMenuService menuService;
    private final SysRoleService roleService;
    private final SysUserRoleService userRoleService;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;
    private final SmsServiceFactory smsServiceFactory;
    private final LoginStrategyFactory loginStrategyFactory;

    private static final String CAPTCHA_KEY = "captcha:";
    private static final String SMS_CODE_KEY = "sms:login:";

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        String uuid = IdUtil.simpleUUID();
        String captchaType = configHelper.getCaptchaType();
        String code;
        String imageBase64;

        switch (captchaType) {
            case "math":
                cn.hutool.captcha.generator.MathGenerator mathGenerator = new cn.hutool.captcha.generator.MathGenerator(1);
                LineCaptcha mathCaptcha = CaptchaUtil.createLineCaptcha(130, 48);
                mathCaptcha.setGenerator(mathGenerator);
                mathCaptcha.createCode();
                code = mathCaptcha.getCode();
                imageBase64 = "data:image/png;base64," + mathCaptcha.getImageBase64();
                break;
            case "circle":
                CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(130, 48, 4, 20);
                code = circleCaptcha.getCode();
                imageBase64 = "data:image/png;base64," + circleCaptcha.getImageBase64();
                break;
            case "shear":
                ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(130, 48, 4, 4);
                code = shearCaptcha.getCode();
                imageBase64 = "data:image/png;base64," + shearCaptcha.getImageBase64();
                break;
            default:
                LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 50);
                code = lineCaptcha.getCode();
                imageBase64 = "data:image/png;base64," + lineCaptcha.getImageBase64();
        }

        redisTemplate.opsForValue().set(CAPTCHA_KEY + uuid, code.toLowerCase(), 5, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("img", imageBase64);
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
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("发送太频繁，请稍后再试");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            throw new BusinessException("短信发送失败，请稍后重试");
        }

        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        return Result.ok();
    }

    /**
     * 登录（通过 zion-auth 统一策略）
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.ADMIN);
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
        List<SysMenu> menus = menuService.getUserMenuTree(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("menus", menus);
        return Result.ok(result);
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    public Result<SysUser> profile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.getDetail(userId));
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateProfile(userId, user);
        return Result.ok();
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody PasswordRequest request) {
        configHelper.validatePassword(request.getNewPassword());
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        if (!configHelper.isRegisterEnabled()) {
            throw new BusinessException("系统暂未开放注册");
        }

        if (configHelper.isCaptchaEnabled()) {
            if (request.getUuid() == null || request.getCode() == null) {
                throw new BusinessException("请输入验证码");
            }
            String cacheCode = redisTemplate.opsForValue().get(CAPTCHA_KEY + request.getUuid());
            redisTemplate.delete(CAPTCHA_KEY + request.getUuid());
            if (cacheCode == null || !cacheCode.equalsIgnoreCase(request.getCode())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        if (!request.getUsername().matches("^[a-zA-Z0-9_]{4,20}$")) {
            throw new BusinessException("用户名只能包含字母、数字、下划线，长度4-20位");
        }

        SysUser existUser = userService.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        configHelper.validatePassword(request.getPassword());

        if (configHelper.isRegisterVerifyEmail()) {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new BusinessException("请输入邮箱");
            }
        }
        if (configHelper.isRegisterVerifyPhone()) {
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new BusinessException("请输入手机号");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setGender(0);
        user.setUserType("admin");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(configHelper.isRegisterNeedAudit() ? 2 : 1);

        userService.save(user);

        String defaultRoleCode = configHelper.getRegisterDefaultRole();
        if (defaultRoleCode != null && !defaultRoleCode.isEmpty()) {
            SysRole role = roleService.getByCode(defaultRoleCode);
            if (role != null) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRoleService.save(userRole);
            }
        }

        if (configHelper.isRegisterNeedAudit()) {
            Result<String> result = new Result<>();
            result.setCode(200);
            result.setMessage("注册成功，请等待管理员审核通过后再登录");
            result.setData("needAudit");
            return result;
        }
        return Result.ok();
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
        private String email;
        private String phone;
        private String uuid;
        private String code;
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
