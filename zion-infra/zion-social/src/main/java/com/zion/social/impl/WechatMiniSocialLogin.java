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
 * 微信小程序三方登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMiniSocialLogin implements SocialLoginService {

    private final SystemConfigHelper configHelper;

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public String getPlatform() {
        return "wechat_mini";
    }

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        // 小程序无需授权URL，客户端直接调用 wx.login() 获取 code
        return null;
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        String appId = configHelper.getMiniProgramAppId();
        String appSecret = configHelper.getMiniProgramAppSecret();

        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(CODE2SESSION_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("微信小程序登录失败: {}", response);
            throw new RuntimeException("小程序登录失败: " + json.getStr("errmsg"));
        }

        SocialUserInfo info = new SocialUserInfo();
        info.setPlatform(getPlatform());
        info.setOpenId(json.getStr("openid"));
        info.setUnionId(json.getStr("unionid"));
        info.setRawJson(response);
        return info;
    }
}
