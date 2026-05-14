package com.zion.wechat;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zion.system.helper.SystemConfigHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMiniProgramService {

    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY = "wechat:miniprogram:access_token";
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /**
     * 小程序登录 - code换取session信息
     *
     * @param code 小程序登录code
     * @return 登录结果
     */
    public MiniProgramLoginResult login(String code) {
        String appId = configHelper.getMiniProgramAppId();
        String appSecret = configHelper.getMiniProgramAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("小程序配置未完成，请先配置AppID和AppSecret");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(CODE2SESSION_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("小程序登录失败: {}", response);
            throw new RuntimeException("小程序登录失败: " + json.getStr("errmsg"));
        }

        MiniProgramLoginResult result = new MiniProgramLoginResult();
        result.setOpenId(json.getStr("openid"));
        result.setSessionKey(json.getStr("session_key"));
        result.setUnionId(json.getStr("unionid"));

        return result;
    }

    /**
     * 获取小程序AccessToken
     */
    public String getAccessToken() {
        // 先从缓存获取
        String accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (accessToken != null) {
            return accessToken;
        }

        String appId = configHelper.getMiniProgramAppId();
        String appSecret = configHelper.getMiniProgramAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("小程序配置未完成");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("grant_type", "client_credential");

        String response = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("获取AccessToken失败: {}", response);
            throw new RuntimeException("获取AccessToken失败: " + json.getStr("errmsg"));
        }

        accessToken = json.getStr("access_token");
        int expiresIn = json.getInt("expires_in");

        // 缓存token，提前5分钟过期
        redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, expiresIn - 300, TimeUnit.SECONDS);

        return accessToken;
    }

    /**
     * 获取用户手机号
     *
     * @param code 手机号获取凭证
     * @return 手机号
     */
    public String getPhoneNumber(String code) {
        String accessToken = getAccessToken();

        JSONObject body = new JSONObject();
        body.set("code", code);

        String response = HttpUtil.post(PHONE_NUMBER_URL + "?access_token=" + accessToken, body.toString());
        JSONObject json = JSONUtil.parseObj(response);

        if (json.getInt("errcode") != 0) {
            log.error("获取手机号失败: {}", response);
            throw new RuntimeException("获取手机号失败: " + json.getStr("errmsg"));
        }

        JSONObject phoneInfo = json.getJSONObject("phone_info");
        return phoneInfo.getStr("purePhoneNumber");
    }

    /**
     * 检查小程序配置是否完整
     */
    public boolean isConfigured() {
        String appId = configHelper.getMiniProgramAppId();
        String appSecret = configHelper.getMiniProgramAppSecret();
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    /**
     * 小程序登录结果
     */
    @Data
    public static class MiniProgramLoginResult {
        private String openId;
        private String sessionKey;
        private String unionId;
    }
}
