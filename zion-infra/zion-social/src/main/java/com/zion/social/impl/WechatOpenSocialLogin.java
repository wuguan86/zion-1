package com.zion.social.impl;

import com.zion.social.SocialLoginService;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.wechat.WechatOpenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信开放平台 PC 扫码登录
 * 使用 QR Connect 模式的 SocialLoginService 实现
 * 注意：PC 扫码不使用 OAuth 重定向，而是二维码 + 轮询模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOpenSocialLogin implements SocialLoginService {

    private final SystemConfigHelper configHelper;
    private final WechatOpenService wechatOpenService;

    @Override
    public String getPlatform() {
        return "wechat_open";
    }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        // PC 扫码登录使用二维码而非重定向，此方法不使用
        return null;
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        WechatOpenService.QrOAuthResult oauthResult = wechatOpenService.getUserInfoByCode(code);

        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        info.setOpenId(oauthResult.getOpenId());
        info.setUnionId(oauthResult.getUnionId());
        info.setNickname(oauthResult.getNickname());
        info.setAvatar(oauthResult.getHeadImgUrl());
        info.setGender(oauthResult.getSex());
        return info;
    }
}
