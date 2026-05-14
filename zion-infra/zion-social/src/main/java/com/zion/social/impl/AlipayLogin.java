package com.zion.social.impl;

import com.zion.social.SocialLoginService;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付宝三方登录
 * TODO: 接入支付宝开放平台 OAuth2.0 授权登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayLogin implements SocialLoginService {

    private final SystemConfigHelper configHelper;

    @Override
    public String getPlatform() {
        return "alipay";
    }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        // TODO: 构建支付宝授权URL
        // https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=xxx&scope=auth_user&redirect_uri=xxx&state=xxx
        log.warn("支付宝登录尚未实现");
        return null;
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        // TODO: 使用 auth_code 换取用户信息
        log.warn("支付宝登录尚未实现");
        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        return info;
    }
}
