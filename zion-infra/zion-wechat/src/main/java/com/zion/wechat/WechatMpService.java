package com.zion.wechat;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zion.system.helper.SystemConfigHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众号服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpService {

    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String ACCESS_TOKEN_KEY = "wechat:mp:access_token";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String OAUTH_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String OAUTH_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    private static final String MENU_CREATE_URL = "https://api.weixin.qq.com/cgi-bin/menu/create";
    private static final String MENU_GET_URL = "https://api.weixin.qq.com/cgi-bin/menu/get";
    private static final String MENU_DELETE_URL = "https://api.weixin.qq.com/cgi-bin/menu/delete";
    private static final String QR_CODE_CREATE_URL = "https://api.weixin.qq.com/cgi-bin/qrcode/create";
    private static final String QR_CODE_SHOW_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode";

    /**
     * 验证消息签名
     */
    public boolean checkSignature(String signature, String timestamp, String nonce) {
        String token = configHelper.getWechatMpToken();
        if (token == null || token.isEmpty()) {
            log.error("公众号Token未配置");
            return false;
        }

        String[] arr = {token, timestamp, nonce};
        Arrays.sort(arr);
        String content = String.join("", arr);
        String calculatedSignature = SecureUtil.sha1(content);

        return calculatedSignature.equalsIgnoreCase(signature);
    }

    /**
     * 获取OAuth授权URL
     *
     * @param redirectUri 回调地址
     * @param state       状态参数
     * @param scope       授权范围 snsapi_base 或 snsapi_userinfo
     * @return 授权URL
     */
    public String getOAuthUrl(String redirectUri, String state, String scope) {
        String appId = configHelper.getWechatMpAppId();
        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("公众号AppID未配置");
        }

        return OAUTH_AUTHORIZE_URL +
                "?appid=" + appId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + scope +
                "&state=" + state +
                "#wechat_redirect";
    }

    /**
     * 通过code获取用户信息（OAuth登录）
     *
     * @param code 授权code
     * @return 用户信息
     */
    public MpOAuthResult oauthLogin(String code) {
        String appId = configHelper.getWechatMpAppId();
        String appSecret = configHelper.getWechatMpAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("公众号配置未完成，请先配置AppID和AppSecret");
        }
        // 获取access_token
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(OAUTH_ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode")) {
            log.error("公众号OAuth登录失败: {}", response);
            throw new RuntimeException("公众号登录失败: " + json.getStr("errmsg"));
        }

        String accessToken = json.getStr("access_token");
        String openId = json.getStr("openid");
        String scope = json.getStr("scope");

        MpOAuthResult result = new MpOAuthResult();
        result.setOpenId(openId);
        result.setUnionId(json.getStr("unionid"));

        // 如果是snsapi_userinfo，获取用户详细信息
        if ("snsapi_userinfo".equals(scope)) {
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("access_token", accessToken);
            userParams.put("openid", openId);
            userParams.put("lang", "zh_CN");

            String userResponse = HttpUtil.get(USER_INFO_URL, userParams);
            JSONObject userJson = JSONUtil.parseObj(userResponse);

            if (!userJson.containsKey("errcode")) {
                result.setNickname(userJson.getStr("nickname"));
                result.setHeadImgUrl(userJson.getStr("headimgurl"));
                result.setSex(userJson.getInt("sex"));
            }
        }

        return result;
    }

    /**
     * 获取公众号AccessToken
     */
    public String getAccessToken() {
        // 先从缓存获取
        String accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (accessToken != null) {
            return accessToken;
        }

        String appId = configHelper.getWechatMpAppId();
        String appSecret = configHelper.getWechatMpAppSecret();

        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            throw new RuntimeException("公众号配置未完成");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("grant_type", "client_credential");

        String response = HttpUtil.get(ACCESS_TOKEN_URL, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("获取公众号AccessToken失败: {}", response);
            throw new RuntimeException("获取AccessToken失败: " + json.getStr("errmsg"));
        }

        accessToken = json.getStr("access_token");
        int expiresIn = json.getInt("expires_in");

        // 缓存token，提前5分钟过期
        redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, accessToken, expiresIn - 300, TimeUnit.SECONDS);

        return accessToken;
    }

    /**
     * 创建自定义菜单
     *
     * @param menuJson 菜单JSON
     */
    public void createMenu(String menuJson) {
        String accessToken = getAccessToken();
        String url = MENU_CREATE_URL + "?access_token=" + accessToken;

        String response = HttpUtil.post(url, menuJson);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.getInt("errcode") != 0) {
            log.error("创建菜单失败: {}", response);
            throw new RuntimeException("创建菜单失败: " + json.getStr("errmsg"));
        }

        log.info("公众号菜单创建成功");
    }

    /**
     * 获取当前菜单配置
     */
    public String getMenu() {
        String accessToken = getAccessToken();
        String url = MENU_GET_URL + "?access_token=" + accessToken;

        String response = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            // 46003 表示菜单不存在，返回空
            if (json.getInt("errcode") == 46003) {
                return null;
            }
            log.error("获取菜单失败: {}", response);
            throw new RuntimeException("获取菜单失败: " + json.getStr("errmsg"));
        }

        return response;
    }

    /**
     * 删除菜单
     */
    public void deleteMenu() {
        String accessToken = getAccessToken();
        String url = MENU_DELETE_URL + "?access_token=" + accessToken;

        String response = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.getInt("errcode") != 0) {
            log.error("删除菜单失败: {}", response);
            throw new RuntimeException("删除菜单失败: " + json.getStr("errmsg"));
        }

        log.info("公众号菜单删除成功");
    }

    public MpQrCode createTemporaryQrCode(String scene, int expireSeconds) {
        String accessToken = getAccessToken();
        String url = QR_CODE_CREATE_URL + "?access_token=" + accessToken;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("expire_seconds", expireSeconds);
        body.put("action_name", "QR_STR_SCENE");
        body.put("action_info", Map.of("scene", Map.of("scene_str", scene)));

        String response = HttpUtil.post(url, JSONUtil.toJsonStr(body));
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("创建公众号临时二维码失败: {}", response);
            throw new RuntimeException("创建公众号临时二维码失败: " + json.getStr("errmsg"));
        }

        String ticket = json.getStr("ticket");
        MpQrCode qrCode = new MpQrCode();
        qrCode.setTicket(ticket);
        qrCode.setUrl(json.getStr("url"));
        qrCode.setExpireSeconds(json.getInt("expire_seconds", expireSeconds));
        qrCode.setQrCodeUrl(QR_CODE_SHOW_URL + "?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8));
        return qrCode;
    }

    /**
     * 处理微信消息回调
     *
     * @param xmlContent XML消息内容
     * @return 响应XML
     */
    public String handleMessage(String xmlContent) {
        // 这里可以根据业务需求解析和处理不同类型的消息
        // 目前返回空字符串表示不回复
        log.debug("收到微信消息: {}", xmlContent);
        return "";
    }

    /**
     * 检查公众号配置是否完整
     */
    public boolean isConfigured() {
        String appId = configHelper.getWechatMpAppId();
        String appSecret = configHelper.getWechatMpAppSecret();
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    /**
     * OAuth登录结果
     */
    @Data
    public static class MpOAuthResult {
        private String openId;
        private String unionId;
        private String nickname;
        private String headImgUrl;
        private Integer sex;
    }

    @Data
    public static class MpQrCode {
        private String ticket;
        private String url;
        private String qrCodeUrl;
        private Integer expireSeconds;
    }
}
