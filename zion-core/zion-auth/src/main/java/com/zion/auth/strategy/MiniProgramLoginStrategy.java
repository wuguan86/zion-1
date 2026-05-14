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
import com.zion.wechat.WechatMiniProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信小程序登录策略（App端）
 * 通过 wx.login() 的 code 换取 openId 进行登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniProgramLoginStrategy implements LoginStrategy {

    private final WechatMiniProgramService wechatMiniProgramService;
    private final SysUserService userService;
    private final LoginHelper loginHelper;

    @Override
    public LoginType getType() {
        return LoginType.MINIPROGRAM;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.APP};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        if (request.getWxCode() == null || request.getWxCode().isEmpty()) {
            throw new BusinessException("微信授权码不能为空");
        }

        // 1. code 换取 openId
        WechatMiniProgramService.MiniProgramLoginResult wxResult = wechatMiniProgramService.login(request.getWxCode());
        String openId = wxResult.getOpenId();

        // 2. 从 sys_user 查找用户，不存在则自动注册
        SysUser user = userService.getByOpenId(openId);
        if (user == null) {
            user = autoRegister(openId);
            log.info("小程序新用户注册: openId={}", openId);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 3. 获取手机号（如果有 phoneCode）
        if (request.getPhoneCode() != null && !request.getPhoneCode().isEmpty()) {
            try {
                String phoneNumber = wechatMiniProgramService.getPhoneNumber(request.getPhoneCode());
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    user.setPhone(phoneNumber);
                    userService.updateById(user);
                }
            } catch (Exception e) {
                log.warn("获取手机号失败: {}", e.getMessage());
            }
        }

        // 4. 执行登录
        return loginHelper.doLogin(user);
    }

    /**
     * 小程序用户自动注册到 sys_user
     */
    private SysUser autoRegister(String openId) {
        SysUser user = new SysUser();
        user.setUsername("wx_" + openId.substring(0, Math.min(openId.length(), 10)));
        user.setNickname("微信用户");
        user.setPassword(BCrypt.hashpw("123456")); // 默认密码
        user.setOpenId(openId);
        user.setStatus(1);
        user.setGender(0);
        user.setUserType("app");
        userService.save(user);
        return user;
    }
}
