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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 手机验证码登录策略（App端）
 * 手机号+短信验证码，未注册自动注册
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeLoginStrategy implements LoginStrategy {

    private final SysUserService userService;
    private final StringRedisTemplate redisTemplate;
    private final LoginHelper loginHelper;

    private static final String SMS_CODE_KEY = "sms:login:";

    @Override
    public LoginType getType() {
        return LoginType.SMS;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP, ClientType.WEB};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }
        if (smsCode == null || smsCode.isEmpty()) {
            throw new BusinessException("请输入验证码");
        }

        // 校验验证码
        String cacheCode = redisTemplate.opsForValue().get(SMS_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(smsCode)) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_KEY + phone);

        // 查找用户，不存在则自动注册
        SysUser user = userService.lambdaQuery()
                .eq(SysUser::getPhone, phone)
                .one();
        if (user == null) {
            String userType = request.getClientType() == ClientType.APP ? "app" : "pc";
            user = autoRegister(phone, userType);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        return loginHelper.doLogin(user);
    }

    private SysUser autoRegister(String phone, String userType) {
        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(7));
        user.setPassword(BCrypt.hashpw("123456")); // 默认密码
        user.setStatus(1);
        user.setGender(0);
        user.setUserType(userType);
        userService.save(user);
        log.info("手机号登录自动注册: phone={}, userType={}", phone, userType);
        return user;
    }
}
