package com.zion.social.impl;

import com.zion.social.SocialLoginService;
import com.zion.system.helper.SystemConfigHelper;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号三方登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpSocialLogin implements SocialLoginService {

    private final SystemConfigHelper configHelper;

    private static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    @Override
    public String getPlatform() {
        return "wechat_mp";
    }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        String appId = configHelper.getWechatMpAppId();
        return AUTHORIZE_URL +
                "?appid=" + appId +
                "&redirect_uri=" + cn.hutool.core.net.URLEncodeUtil.encode(redirectUri) +
                "&response_type=code" +
                "&scope=snsapi_userinfo" +
                "&state=" + state +
                "#wechat_redirect";
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        String appId = configHelper.getWechatMpAppId();
        String appSecret = configHelper.getWechatMpAppSecret();

        // 1. code换取access_token
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode")) {
            log.error("微信公众号登录失败: {}", response);
            throw new RuntimeException("微信登录失败: " + json.getStr("errmsg"));
        }

        String accessToken = json.getStr("access_token");
        String openId = json.getStr("openid");

        // 2. 获取用户信息
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("access_token", accessToken);
        userParams.put("openid", openId);
        userParams.put("lang", "zh_CN");

        String userResponse = HttpUtil.get(USER_INFO_URL, userParams);
        JSONObject userJson = JSONUtil.parseObj(userResponse);

        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        info.setOpenId(openId);
        info.setUnionId(json.getStr("unionid"));
        info.setNickname(userJson.getStr("nickname"));
        info.setAvatar(userJson.getStr("headimgurl"));
        info.setGender(userJson.getInt("sex"));
        info.setRawJson(userResponse);
        return info;
    }
}
