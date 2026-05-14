package com.zion.social.impl;

import com.zion.social.SocialLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Apple三方登录
 * TODO: 接入 Sign in with Apple
 */
@Slf4j
@Service
public class AppleLogin implements SocialLoginService {

    @Override
    public String getPlatform() {
        return "apple";
    }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        // TODO: 构建Apple授权URL
        log.warn("Apple登录尚未实现");
        return null;
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        // TODO: 验证 Apple identity token
        log.warn("Apple登录尚未实现");
        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        return info;
    }
}
