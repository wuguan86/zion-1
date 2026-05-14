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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 微信开放平台服务（PC 扫码登录）
 * 使用微信开放平台 QR Connect 接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatOpenService {

    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY = "wechat:open:access_token";
    private static final String QR_TICKET_KEY = "wechat:open:qr:";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String QRCODE_CREATE_URL = "https://api.weixin.qq.com/cgi-bin/qrcode/create";
    private static final String QRCODE_IMG_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode";

    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        String appId = configHelper.getWechatOpenAppId();
        String appSecret = configHelper.getWechatOpenAppSecret();
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    /**
     * 获取 AccessToken（缓存）
     */
    public String getAccessToken() {
        String accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (accessToken != null) {
            return accessToken;
        }

        String appId = configHelper.getWechatOpenAppId();
        String appSecret = configHelper.getWechatOpenAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("微信开放平台未配置，请先配置 AppID 和 AppSecret");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("grant_type", "client_credential");

        String response = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("获取开放平台AccessToken失败: {}", response);
            throw new RuntimeException("获取AccessToken失败: " + json.getStr("errmsg"));
        }

        accessToken = json.getStr("access_token");
        int expiresIn = json.getInt("expires_in");

        redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, expiresIn - 300, TimeUnit.SECONDS);
        return accessToken;
    }

    /**
     * 创建二维码票据
     * 返回 { ticket, qrUrl, expireSeconds }
     */
    public QrcodeResult createQrcode() {
        if (!isConfigured()) {
            throw new RuntimeException("微信开放平台未配置，请先配置 AppID 和 AppSecret");
        }

        String accessToken = getAccessToken();

        // 生成一个随机 scene_str 用于后续状态查询
        String sceneStr = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> actionInfo = new HashMap<>();
        actionInfo.put("scene", Map.of("scene_str", sceneStr));

        Map<String, Object> body = new HashMap<>();
        body.put("expire_seconds", 300); // 5分钟过期
        body.put("action_name", "QR_STR_SCENE");
        body.put("action_info", actionInfo);

        String url = QRCODE_CREATE_URL + "?access_token=" + accessToken;
        String response = HttpUtil.post(url, JSONUtil.toJsonStr(body));
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("创建二维码失败: {}", response);
            throw new RuntimeException("创建二维码失败: " + json.getStr("errmsg"));
        }

        String ticket = json.getStr("ticket");
        int expireSeconds = json.getInt("expire_seconds");
        String qrUrl = QRCODE_IMG_URL + "?ticket=" + cn.hutool.core.net.URLEncodeUtil.encode(ticket);

        // 缓存 scene -> ticket 映射，初始状态为 pending
        redisTemplate.opsForValue().set(QR_TICKET_KEY + sceneStr, "pending", expireSeconds, TimeUnit.SECONDS);
        // 缓存 ticket -> scene 映射，方便通过 ticket 查询
        redisTemplate.opsForValue().set(QR_TICKET_KEY + "ticket:" + ticket, sceneStr, expireSeconds, TimeUnit.SECONDS);

        QrcodeResult result = new QrcodeResult();
        result.setTicket(ticket);
        result.setQrUrl(qrUrl);
        result.setExpireSeconds(expireSeconds);
        return result;
    }

    /**
     * 查询扫码状态
     * 返回: pending / scanned / confirmed / cancelled / expired
     */
    public String getScanStatus(String ticket) {
        if (ticket == null || ticket.isEmpty()) {
            return "expired";
        }
        String sceneStr = redisTemplate.opsForValue().get(QR_TICKET_KEY + "ticket:" + ticket);
        if (sceneStr == null) {
            return "expired";
        }
        String status = redisTemplate.opsForValue().get(QR_TICKET_KEY + sceneStr);
        return status != null ? status : "expired";
    }

    /**
     * 处理微信服务器事件推送（扫码/确认/取消）
     * 在 WeChat 回调中调用此方法更新状态
     */
    public void updateScanStatus(String sceneStr, String status) {
        redisTemplate.opsForValue().set(QR_TICKET_KEY + sceneStr, status, 5, TimeUnit.MINUTES);
    }

    /**
     * 通过 OAuth2 code 换取用户信息（PC 扫码后的第二步）
     * 使用 https://api.weixin.qq.com/sns/oauth2/access_token
     */
    public QrOAuthResult getUserInfoByCode(String code) {
        String appId = configHelper.getWechatOpenAppId();
        String appSecret = configHelper.getWechatOpenAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("微信开放平台未配置，请先配置 AppID 和 AppSecret");
        }

        // 1. code 换取 access_token + openid
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";
        String response = HttpUtil.get(accessTokenUrl, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode")) {
            log.error("微信扫码登录 code 换 token 失败: {}", response);
            throw new RuntimeException("微信登录失败: " + json.getStr("errmsg"));
        }

        String accessToken = json.getStr("access_token");
        String openId = json.getStr("openid");
        String unionId = json.getStr("unionid");

        // 2. 获取用户信息
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("access_token", accessToken);
        userParams.put("openid", openId);
        userParams.put("lang", "zh_CN");

        String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";
        String userResponse = HttpUtil.get(userInfoUrl, userParams);
        JSONObject userJson = JSONUtil.parseObj(userResponse);

        QrOAuthResult result = new QrOAuthResult();
        result.setOpenId(openId);
        result.setUnionId(unionId);
        result.setNickname(userJson.getStr("nickname"));
        result.setHeadImgUrl(userJson.getStr("headimgurl"));
        result.setSex(userJson.getInt("sex"));
        result.setRawJson(userResponse);
        return result;
    }

    @Data
    public static class QrcodeResult {
        private String ticket;
        private String qrUrl;
        private Integer expireSeconds;
    }

    @Data
    public static class QrOAuthResult {
        private String openId;
        private String unionId;
        private String nickname;
        private String headImgUrl;
        private Integer sex;
        private String rawJson;
    }
}
