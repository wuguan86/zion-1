package com.zion.admin.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.common.result.Result;
import com.zion.system.config.SaTokenConfigLoader;
import com.zion.system.entity.SysConfigGroup;
import com.zion.mail.EmailService;
import com.zion.sms.SmsServiceFactory;
import com.zion.sms.entity.SmsLog;
import com.zion.sms.service.SmsLogService;
import com.zion.system.service.SysConfigGroupService;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.pay.PayServiceFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置分组 Controller
 */
@RestController
@RequestMapping("/sys/config-group")
@RequiredArgsConstructor
public class SysConfigGroupController {

    private final SysConfigGroupService configGroupService;
    private final SystemConfigHelper configHelper;
    private final PayServiceFactory payServiceFactory;
    private final EmailService emailService;
    private final SaTokenConfigLoader saTokenConfigLoader;
    private final SmsServiceFactory smsServiceFactory;
    private final SmsLogService smsLogService;

    /**
     * 获取所有配置分组
     */
    @GetMapping("/list")
    public Result<List<SysConfigGroup>> list() {
        return Result.ok(configGroupService.listAll());
    }

    /**
     * 获取指定分组配置
     */
    @GetMapping("/{groupCode}")
    public Result<SysConfigGroup> getByCode(@PathVariable String groupCode) {
        return Result.ok(configGroupService.getByGroupCode(groupCode));
    }

    /**
     * 保存配置
     */
    @PostMapping("/{groupCode}")
    public Result<Void> save(@PathVariable String groupCode, @RequestBody Map<String, Object> config) {
        try {
            String configValue = new tools.jackson.databind.ObjectMapper().writeValueAsString(config);
            configGroupService.saveConfig(groupCode, configValue);

            // 如果保存的是安全配置，重新加载 Sa-Token 配置使其即时生效
            if ("security".equals(groupCode)) {
                saTokenConfigLoader.applyConfig();
            }

            return Result.ok();
        } catch (Exception e) {
            return Result.fail("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 刷新缓存
     */
    @PostMapping("/refresh")
    public Result<Void> refresh() {
        configGroupService.refreshCache();
        return Result.ok();
    }

    /**
     * 获取公开配置（不需要登录）
     * 用于前端显示站点信息、登录配置等
     */
    @GetMapping("/public")
    public Result<Map<String, Object>> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();

        // 系统配置
        Map<String, Object> system = new HashMap<>();
        system.put("siteName", configHelper.getSiteName());
        system.put("siteDescription", configHelper.getSiteDescription());
        system.put("siteLogo", configHelper.getSiteLogo());
        system.put("copyright", configHelper.getCopyright());
        system.put("icp", configHelper.getIcp());
        system.put("watermarkEnabled", configHelper.isWatermarkEnabled());
        system.put("watermarkType", configHelper.getWatermarkType());
        system.put("watermarkCustomText", configHelper.getWatermarkCustomText());
        system.put("watermarkOpacity", configHelper.getWatermarkOpacity());
        config.put("system", system);

        // 登录配置
        Map<String, Object> login = new HashMap<>();
        login.put("captchaEnabled", configHelper.isCaptchaEnabled());
        login.put("captchaType", configHelper.getCaptchaType()); // image, slider, sms
        login.put("maxRetryCount", configHelper.getMaxRetryCount());
        login.put("rememberMe", configHelper.isRememberMeEnabled());
        config.put("login", login);

        // 注册配置
        Map<String, Object> register = new HashMap<>();
        register.put("enabled", configHelper.isRegisterEnabled());
        register.put("verifyEmail", configHelper.isRegisterVerifyEmail());
        register.put("verifyPhone", configHelper.isRegisterVerifyPhone());
        register.put("needAudit", configHelper.isRegisterNeedAudit());
        config.put("register", register);

        // 密码配置（规则提示）
        Map<String, Object> password = new HashMap<>();
        password.put("minLength", configHelper.getPasswordMinLength());
        password.put("maxLength", configHelper.getPasswordMaxLength());
        password.put("requireUppercase", configHelper.isPasswordRequireUppercase());
        password.put("requireLowercase", configHelper.isPasswordRequireLowercase());
        password.put("requireNumber", configHelper.isPasswordRequireNumber());
        password.put("requireSpecial", configHelper.isPasswordRequireSpecial());
        config.put("password", password);

        // 文件上传配置
        Map<String, Object> storage = new HashMap<>();
        storage.put("maxSize", configHelper.getStorageMaxSize());
        storage.put("allowTypes", configHelper.getStorageAllowTypes());
        config.put("storage", storage);

        // 安全配置（仅返回前端需要的配置）
        Map<String, Object> security = new HashMap<>();
        security.put("disableDevtool", configHelper.isDisableDevtool());
        config.put("security", security);

        return Result.ok(config);
    }

    /**
     * 测试支付
     */
    @PostMapping("/test-payment")
    public Result<Map<String, String>> testPayment(@RequestBody TestPaymentRequest request) {
        try {
            if (!payServiceFactory.isSupported(request.getType())) {
                return Result.fail("不支持的支付类型: " + request.getType());
            }
            Map<String, String> result = payServiceFactory.createTestOrder(request.getType());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Data
    public static class TestPaymentRequest {
        private String type; // wechat 或 alipay
    }

    /**
     * 测试发送邮件
     */
    @PostMapping("/test-email")
    public Result<Void> testEmail(@RequestBody TestEmailRequest request) {
        try {
            emailService.sendTestMail(request.getTo());
            return Result.ok();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Data
    public static class TestEmailRequest {
        private String to; // 收件人邮箱
    }

    /**
     * 生成RSA密钥对
     */
    @PostMapping("/generate-keys")
    public Result<Map<String, String>> generateKeys() {
        try {
            Map<String, String> keyPair = com.zion.common.util.RsaUtils.generateKeyPair();
            return Result.ok(keyPair);
        } catch (Exception e) {
            return Result.fail("生成密钥失败: " + e.getMessage());
        }
    }

    /**
     * 测试发送短信
     */
    @PostMapping("/test-sms")
    public Result<Void> testSms(@RequestBody TestSmsRequest request) {
        try {
            // 生成随机验证码
            String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
            boolean success = smsServiceFactory.sendCode(request.getPhone(), code);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail("短信发送失败");
            }
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Data
    public static class TestSmsRequest {
        private String phone; // 手机号
    }

    /**
     * 获取最近短信发送记录
     */
    @GetMapping("/sms-logs/recent")
    public Result<List<SmsLog>> getRecentSmsLogs(@RequestParam(defaultValue = "5") Integer limit) {
        LambdaQueryWrapper<SmsLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SmsLog::getCreateTime)
                .last("LIMIT " + limit);
        return Result.ok(smsLogService.list(wrapper));
    }

    /**
     * 分页查询短信发送记录
     */
    @GetMapping("/sms-logs")
    public Result<IPage<SmsLog>> getSmsLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SmsLog> wrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(SmsLog::getPhone, phone);
        }
        if (status != null) {
            wrapper.eq(SmsLog::getStatus, status);
        }
        wrapper.orderByDesc(SmsLog::getCreateTime);
        return Result.ok(smsLogService.page(new Page<>(page, size), wrapper));
    }
}
