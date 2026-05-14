package com.zion.auth.strategy;

import cn.hutool.crypto.digest.BCrypt;
import com.zion.auth.LoginHelper;
import com.zion.auth.LoginRequest;
import com.zion.auth.LoginResult;
import com.zion.auth.LoginStrategy;
import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.exception.BusinessException;
import com.zion.system.entity.SysUser;
import com.zion.system.service.SysUserService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 密码登录策略（Admin/Web 通用）
 * 支持用户名+密码登录，含重试次数限制、验证码校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final SysUserService userService;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;
    private final LoginHelper loginHelper;

    private static final String LOGIN_RETRY_KEY = "login:retry:";
    private static final String CAPTCHA_KEY = "captcha:";

    @Override
    public LoginType getType() {
        return LoginType.PASSWORD;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.ADMIN, ClientType.WEB};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        // 1. 验证码校验
        validateCaptcha(request);

        // 2. 重试次数检查
        String retryKey = LOGIN_RETRY_KEY + request.getUsername();
        checkRetryLimit(retryKey, request.getUsername());

        // 3. 用户验证
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null) {
            incrementRetry(retryKey);
            loginHelper.recordFailLog(request.getUsername(), "用户不存在");
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            incrementRetry(retryKey);
            loginHelper.recordFailLog(request.getUsername(), "密码错误");
            int remaining = configHelper.getMaxRetryCount() - getRetryCount(retryKey);
            String msg = remaining > 0 ? "用户名或密码错误，还剩" + remaining + "次机会" : "用户名或密码错误，账号已锁定";
            throw new BusinessException(msg);
        }

        // 4. 状态检查
        checkUserStatus(user);

        // 5. 用户类型校验：admin端只允许admin用户，web端只允许pc或admin用户
        checkUserType(user, request.getClientType());

        // 6. 清除重试、执行登录
        redisTemplate.delete(retryKey);
        return loginHelper.doLogin(user);
    }

    private void validateCaptcha(LoginRequest request) {
        if (!configHelper.isCaptchaEnabled()) return;

        String captchaType = configHelper.getCaptchaType();

        if ("slider".equals(captchaType)) {
            // 滑块验证码（前端已验证，后端简单校验标识）
            if (!"slider_verified".equals(request.getCode())) {
                throw new BusinessException("请完成滑块验证");
            }
        } else if ("sms".equals(captchaType)) {
            // 短信验证码校验
            if (request.getPhone() == null || request.getCode() == null) {
                throw new BusinessException("请输入手机号和验证码");
            }
            String cacheCode = redisTemplate.opsForValue().get("sms:login:" + request.getPhone());
            if (cacheCode == null || !cacheCode.equals(request.getCode())) {
                throw new BusinessException("短信验证码错误或已过期");
            }
            redisTemplate.delete("sms:login:" + request.getPhone());
        } else {
            // 图片验证码（image / math / circle / shear 等）
            if (request.getUuid() == null || request.getCode() == null) {
                throw new BusinessException("请输入验证码");
            }
            String cacheCode = redisTemplate.opsForValue().get(CAPTCHA_KEY + request.getUuid());
            redisTemplate.delete(CAPTCHA_KEY + request.getUuid());
            if (cacheCode == null || !cacheCode.equalsIgnoreCase(request.getCode())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
    }

    private void checkRetryLimit(String retryKey, String username) {
        int retryCount = getRetryCount(retryKey);
        int maxRetry = configHelper.getMaxRetryCount();
        if (retryCount >= maxRetry) {
            Long ttl = redisTemplate.getExpire(retryKey, TimeUnit.MINUTES);
            loginHelper.recordFailLog(username, "账号已锁定");
            throw new BusinessException("账号已锁定，请" + ttl + "分钟后重试");
        }
    }

    private int getRetryCount(String retryKey) {
        String str = redisTemplate.opsForValue().get(retryKey);
        return str != null ? Integer.parseInt(str) : 0;
    }

    private void incrementRetry(String retryKey) {
        int count = getRetryCount(retryKey) + 1;
        redisTemplate.opsForValue().set(retryKey, String.valueOf(count),
                configHelper.getLockTime(), TimeUnit.MINUTES);
    }

    private void checkUserType(SysUser user, ClientType clientType) {
        String userType = user.getUserType();
        if (userType == null || userType.isEmpty()) {
            return; // 兼容历史数据，未设置类型的用户不限制
        }
        if (clientType == ClientType.ADMIN && !"admin".equals(userType)) {
            loginHelper.recordFailLog(user.getUsername(), "非管理端用户，无权登录后台");
            throw new BusinessException("您不是管理端用户，无法登录后台管理系统");
        }
        if (clientType == ClientType.WEB && "app".equals(userType)) {
            loginHelper.recordFailLog(user.getUsername(), "App用户无权登录PC端");
            throw new BusinessException("您是App用户，请使用App登录");
        }
    }

    private void checkUserStatus(SysUser user) {
        if (user.getStatus() == 2) {
            loginHelper.recordFailLog(user.getUsername(), "账号待审核");
            throw new BusinessException("您的账号正在审核中，请等待管理员审核通过后再登录");
        }
        if (user.getStatus() == 3) {
            loginHelper.recordFailLog(user.getUsername(), "审核未通过");
            throw new BusinessException("您的注册申请未通过审核，如有疑问请联系管理员");
        }
        if (user.getStatus() != 1) {
            loginHelper.recordFailLog(user.getUsername(), "用户已被禁用");
            throw new BusinessException("用户已被禁用");
        }
        if (user.getIsQuit() == 1) {
            loginHelper.recordFailLog(user.getUsername(), "用户已离职");
            throw new BusinessException("用户已离职");
        }
    }
}
