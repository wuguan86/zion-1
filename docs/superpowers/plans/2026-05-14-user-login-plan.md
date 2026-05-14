# User Login Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build zion-ui-user frontend with WeChat PC QR scan, SMS code, and password login. Fill backend gaps (WeChat Open Platform service, web SMS endpoint) using existing strategy/factory patterns.

**Architecture:** Backend adds `WechatOpenService` + `WechatOpenSocialLogin` following the existing `WechatMpService` / `WechatMpSocialLogin` pattern, and extends `WebAuthController` with SMS and QR endpoints. Frontend scaffolds a new Vue 3 + TS + Naive UI project at `zion-ui-user/` with a card-selection login flow and placeholder home page, matching zion-ui's tech versions and code patterns.

**Tech Stack:** Java 25, Spring Boot 4.0.6, Sa-Token 1.45.0, MyBatis Plus 3.5.15, Hutool 5.8.44. Vue 3.4, TypeScript 5.3, Vite 5, Naive UI 2.37, Pinia 2.1, vue-router 4.2, axios 1.6, SCSS.

---

### Task 1: Add wechatOpen config group to SystemConfigHelper

**Files:**
- Modify: `E:\project\Zion-Admin\zion-core\zion-system\src\main\java\com\zion\system\helper\SystemConfigHelper.java`

- [ ] **Step 1: Add wechatOpen config constants and getter methods**

Add after the wechatMp section (after line ~1019):

```java
// ============ 微信开放平台配置 ============

public static final String GROUP_WECHAT_OPEN = "wechatOpen";

/**
 * 是否启用微信开放平台
 */
public boolean isWechatOpenEnabled() {
    return getBoolean(GROUP_WECHAT_OPEN, "enabled");
}

/**
 * 获取微信开放平台 AppID
 */
public String getWechatOpenAppId() {
    return getString(GROUP_WECHAT_OPEN, "appId", "");
}

/**
 * 获取微信开放平台 AppSecret
 */
public String getWechatOpenAppSecret() {
    return getString(GROUP_WECHAT_OPEN, "appSecret", "");
}
```

- [ ] **Step 2: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/helper/SystemConfigHelper.java
git commit -m "feat: add wechatOpen config group getter methods"
```

---

### Task 2: Create WechatOpenService

**Files:**
- Create: `E:\project\Zion-Admin\zion-infra\zion-wechat\src\main\java\com\zion\wechat\WechatOpenService.java`
- Create: `E:\project\Zion-Admin\zion-infra\zion-wechat\src\test\java\com\zion\wechat\WechatOpenServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.zion.wechat;

import com.zion.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatOpenServiceTest {

    @Mock
    private SystemConfigHelper configHelper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WechatOpenService wechatOpenService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(configHelper.getWechatOpenAppId()).thenReturn("wx123456");
        when(configHelper.getWechatOpenAppSecret()).thenReturn("secret123");
    }

    @Test
    void isConfigured_shouldReturnTrueWhenAppIdAndSecretExist() {
        assertTrue(wechatOpenService.isConfigured());
    }

    @Test
    void isConfigured_shouldReturnFalseWhenAppIdMissing() {
        when(configHelper.getWechatOpenAppId()).thenReturn("");
        assertFalse(wechatOpenService.isConfigured());
    }

    @Test
    void createQrcode_shouldThrowWhenNotConfigured() {
        when(configHelper.getWechatOpenAppId()).thenReturn("");
        assertThrows(RuntimeException.class, () -> wechatOpenService.createQrcode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-infra/zion-wechat -Dtest=WechatOpenServiceTest -DfailIfNoTests=false
```
Expected: FAIL — WechatOpenService class does not exist.

- [ ] **Step 3: Write WechatOpenService implementation**

```java
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

        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("开放平台AppID未配置");
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
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-infra/zion-wechat -Dtest=WechatOpenServiceTest -DfailIfNoTests=false
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add zion-infra/zion-wechat/src/main/java/com/zion/wechat/WechatOpenService.java zion-infra/zion-wechat/src/test/java/com/zion/wechat/WechatOpenServiceTest.java
git commit -m "feat: add WechatOpenService for PC QR code login"
```

---

### Task 3: Create WechatOpenSocialLogin

**Files:**
- Create: `E:\project\Zion-Admin\zion-infra\zion-social\src\main\java\com\zion\social\impl\WechatOpenSocialLogin.java`
- Create: `E:\project\Zion-Admin\zion-infra\zion-social\src\test\java\com\zion\social\impl\WechatOpenSocialLoginTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.zion.social.impl;

import com.zion.social.SocialLoginService;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.wechat.WechatOpenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatOpenSocialLoginTest {

    @Mock
    private SystemConfigHelper configHelper;

    @Mock
    private WechatOpenService wechatOpenService;

    @InjectMocks
    private WechatOpenSocialLogin socialLogin;

    @Test
    void getPlatform_shouldReturnWechatOpen() {
        assertEquals("wechat_open", socialLogin.getPlatform());
    }

    @Test
    void getAuthorizeUrl_shouldReturnNullBecausePCUsesQrNotRedirect() {
        // PC 扫码登录不使用 OAuth 重定向，而是用二维码
        assertNull(socialLogin.getAuthorizeUrl("http://callback", "state"));
    }

    @Test
    void getUserInfo_shouldDelegateToWechatOpenService() {
        WechatOpenService.QrOAuthResult oauthResult = new WechatOpenService.QrOAuthResult();
        oauthResult.setOpenId("open123");
        oauthResult.setNickname("Test");
        oauthResult.setHeadImgUrl("http://avatar");
        oauthResult.setSex(1);

        when(wechatOpenService.getUserInfoByCode("auth_code_123")).thenReturn(oauthResult);

        SocialLoginService.SocialUserInfo info = socialLogin.getUserInfo("auth_code_123");

        assertEquals("wechat_open", info.getPlatform());
        assertEquals("open123", info.getOpenId());
        assertEquals("Test", info.getNickname());
        assertEquals("http://avatar", info.getAvatar());
        assertEquals(1, info.getGender());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-infra/zion-social -Dtest=WechatOpenSocialLoginTest -DfailIfNoTests=false
```
Expected: FAIL — WechatOpenSocialLogin class does not exist.

- [ ] **Step 3: Write WechatOpenSocialLogin implementation**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-infra/zion-social -Dtest=WechatOpenSocialLoginTest -DfailIfNoTests=false
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add zion-infra/zion-social/src/main/java/com/zion/social/impl/WechatOpenSocialLogin.java zion-infra/zion-social/src/test/java/com/zion/social/impl/WechatOpenSocialLoginTest.java
git commit -m "feat: add WechatOpenSocialLogin for PC QR code social login"
```

---

### Task 4: Extend WebAuthController with SMS and WeChat QR endpoints

**Files:**
- Modify: `E:\project\Zion-Admin\zion-api\zion-web-api\src\main\java\com\zion\web\controller\WebAuthController.java`
- Create: `E:\project\Zion-Admin\zion-api\zion-web-api\src\test\java\com\zion\web\controller\WebAuthControllerTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
package com.zion.web.controller;

import com.zion.auth.LoginRequest;
import com.zion.auth.LoginStrategyFactory;
import com.zion.auth.enums.ClientType;
import com.zion.system.entity.SysUser;
import com.zion.system.service.SysUserService;
import com.zion.wechat.WechatOpenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebAuthController.class)
class WebAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginStrategyFactory loginStrategyFactory;

    @MockBean
    private SysUserService userService;

    @MockBean
    private WechatOpenService wechatOpenService;

    @Test
    void smsCode_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getWechatQrcode_shouldReturnTicketAndUrl() throws Exception {
        WechatOpenService.QrcodeResult qrResult = new WechatOpenService.QrcodeResult();
        qrResult.setTicket("ticket123");
        qrResult.setQrUrl("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket123");
        qrResult.setExpireSeconds(300);

        when(wechatOpenService.isConfigured()).thenReturn(true);
        when(wechatOpenService.createQrcode()).thenReturn(qrResult);

        mockMvc.perform(get("/web/auth/wechat/qrcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticket").value("ticket123"))
                .andExpect(jsonPath("$.data.qrUrl").value("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket123"));
    }

    @Test
    void getWechatStatus_shouldReturnStatus() throws Exception {
        when(wechatOpenService.getScanStatus("ticket123")).thenReturn("pending");

        mockMvc.perform(get("/web/auth/wechat/status")
                .param("ticket", "ticket123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-api/zion-web-api -Dtest=WebAuthControllerTest -DfailIfNoTests=false
```
Expected: FAIL — sms-code and wechat endpoints not found (404 or 405).

- [ ] **Step 3: Modify WebAuthController to add endpoints**

Add these three methods to `WebAuthController.java` (before the closing `}`):

```java
    private final com.zion.sms.SmsServiceFactory smsServiceFactory;
    private final com.zion.wechat.WechatOpenService wechatOpenService;

    /**
     * 发送短信验证码（PC端）
     */
    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isEmpty()) {
            throw new com.zion.common.exception.BusinessException("手机号不能为空");
        }
        smsServiceFactory.sendCode(phone);
        return Result.ok();
    }

    /**
     * 获取微信PC扫码二维码
     */
    @GetMapping("/wechat/qrcode")
    public Result<Map<String, Object>> getWechatQrcode() {
        if (!wechatOpenService.isConfigured()) {
            throw new com.zion.common.exception.BusinessException("微信扫码登录未配置");
        }
        com.zion.wechat.WechatOpenService.QrcodeResult qrResult = wechatOpenService.createQrcode();
        Map<String, Object> data = new HashMap<>();
        data.put("ticket", qrResult.getTicket());
        data.put("qrUrl", qrResult.getQrUrl());
        data.put("expireSeconds", qrResult.getExpireSeconds());
        return Result.ok(data);
    }

    /**
     * 查询微信扫码状态
     */
    @GetMapping("/wechat/status")
    public Result<Map<String, String>> getWechatStatus(@RequestParam String ticket) {
        String status = wechatOpenService.getScanStatus(ticket);
        Map<String, String> data = new HashMap<>();
        data.put("status", status);
        return Result.ok(data);
    }
```

Also update the constructor injection by replacing `@RequiredArgsConstructor` with explicit constructor or adding the new fields. Since the class uses `@RequiredArgsConstructor`, add the two new fields as `private final`:

Replace the field declarations section:

```java
@RestController
@RequestMapping("/web/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SysUserService userService;
    private final com.zion.sms.SmsServiceFactory smsServiceFactory;
    private final com.zion.wechat.WechatOpenService wechatOpenService;
```

Note: The existing fields `loginStrategyFactory` and `userService` are already `private final`, so Lombok `@RequiredArgsConstructor` will generate the constructor for all 4 fields automatically.

- [ ] **Step 4: Run test to verify it passes**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-api/zion-web-api -Dtest=WebAuthControllerTest -DfailIfNoTests=false
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add zion-api/zion-web-api/src/main/java/com/zion/web/controller/WebAuthController.java zion-api/zion-web-api/src/test/java/com/zion/web/controller/WebAuthControllerTest.java
git commit -m "feat: add SMS code and WeChat QR endpoints to WebAuthController"
```

---

### Task 5: Scaffold zion-ui-user project

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\package.json`
- Create: `E:\project\Zion-Admin\zion-ui-user\vite.config.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\tsconfig.json`
- Create: `E:\project\Zion-Admin\zion-ui-user\tsconfig.node.json`
- Create: `E:\project\Zion-Admin\zion-ui-user\index.html`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\vite-env.d.ts`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "zion-ui-user",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@vicons/ionicons5": "^0.12.0",
    "axios": "^1.6.5",
    "naive-ui": "^2.37.3",
    "pinia": "^2.1.7",
    "pinia-plugin-persistedstate": "^3.2.1",
    "vue": "^3.4.15",
    "vue-router": "^4.2.5"
  },
  "devDependencies": {
    "@types/node": "^20.11.5",
    "@vitejs/plugin-vue": "^5.0.3",
    "sass": "^1.70.0",
    "typescript": "^5.3.3",
    "vite": "^5.0.11",
    "vue-tsc": "^1.8.27"
  }
}
```

- [ ] **Step 2: Create vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  base: '/',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    outDir: resolve(__dirname, '../zion-starter/src/main/resources/static-user'),
    emptyOutDir: true
  },
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: Create tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Zion - 用户端</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 6: Create src/vite-env.d.ts**

```typescript
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface Window {
  $message: import('naive-ui').MessageApi
  $dialog: import('naive-ui').DialogApi
  $loadingBar: import('naive-ui').LoadingBarApi
}
```

- [ ] **Step 7: Install dependencies**

```bash
cd E:/project/Zion-Admin/zion-ui-user && npm install
```

- [ ] **Step 8: Commit**

```bash
git add zion-ui-user/package.json zion-ui-user/vite.config.ts zion-ui-user/tsconfig.json zion-ui-user/tsconfig.node.json zion-ui-user/index.html zion-ui-user/src/vite-env.d.ts
git commit -m "feat: scaffold zion-ui-user project with Vue 3 + TS + Vite"
```

---

### Task 6: Create main.ts, App.vue, and global styles

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\main.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\App.vue`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\styles\index.scss`

- [ ] **Step 1: Create src/main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { createDiscreteApi } from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/index.scss'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

app.use(router)

const { message, dialog, loadingBar } = createDiscreteApi(['message', 'dialog', 'loadingBar'])
window.$message = message
window.$dialog = dialog
window.$loadingBar = loadingBar

app.mount('#app')
```

- [ ] **Step 2: Create src/App.vue**

```vue
<script setup lang="ts">
import { darkTheme, zhCN, dateZhCN } from 'naive-ui'
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
```

- [ ] **Step 3: Create src/styles/index.scss**

```scss
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

- [ ] **Step 4: Commit**

```bash
git add zion-ui-user/src/main.ts zion-ui-user/src/App.vue zion-ui-user/src/styles/index.scss
git commit -m "feat: add app entry, root component, and global styles"
```

---

### Task 7: Create types and API layer

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\types\login.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\utils\request.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\api\auth.ts`

- [ ] **Step 1: Create src/types/login.ts**

```typescript
export type LoginType = 'password' | 'sms' | 'wechat'

export interface LoginRequest {
  loginType: LoginType
  username?: string
  password?: string
  phone?: string
  smsCode?: string
  platform?: string
  authCode?: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  gender: number
  status: number
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
  avatar: string
}

export interface CaptchaResult {
  uuid: string
  img: string
}

export interface QrcodeResult {
  ticket: string
  qrUrl: string
  expireSeconds: number
}

export interface ScanStatusResult {
  status: 'pending' | 'scanned' | 'confirmed' | 'cancelled' | 'expired'
}
```

- [ ] **Step 2: Create src/utils/request.ts**

```typescript
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = userStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

let isLoggingOut = false

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (res.code !== 200) {
      const isLogoutRequest = response.config.url?.includes('/auth/logout')

      if (res.code === 401 && !isLoggingOut && !isLogoutRequest) {
        isLoggingOut = true
        window.$message?.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        isLoggingOut = false
        return Promise.reject(new Error('登录已过期'))
      }

      if (!isLogoutRequest) {
        window.$message?.error(res.message || '请求失败')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    window.$message?.error(message)
    return Promise.reject(error)
  }
)

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as Promise<T>
}

export default service
```

- [ ] **Step 3: Create src/api/auth.ts**

```typescript
import { request } from '@/utils/request'
import type { LoginResult, CaptchaResult, QrcodeResult, ScanStatusResult } from '@/types/login'

export const authApi = {
  /** 获取图形验证码 */
  getCaptcha(): Promise<CaptchaResult> {
    return request({ url: '/web/auth/captcha', method: 'get' })
  },

  /** 发送短信验证码 */
  sendSmsCode(phone: string): Promise<void> {
    return request({ url: '/web/auth/sms-code', method: 'post', data: { phone } })
  },

  /** 登录 */
  login(data: { loginType: string } & Record<string, any>): Promise<LoginResult> {
    return request({ url: '/web/auth/login', method: 'post', data })
  },

  /** 退出登录 */
  logout(): Promise<void> {
    return request({ url: '/web/auth/logout', method: 'post' })
  },

  /** 获取当前用户信息 */
  getInfo(): Promise<{ user: import('@/types/login').UserInfo; roles: string[]; permissions: string[] }> {
    return request({ url: '/web/auth/info', method: 'get' })
  },

  /** 获取微信扫码二维码 */
  getWechatQrcode(): Promise<QrcodeResult> {
    return request({ url: '/web/auth/wechat/qrcode', method: 'get' })
  },

  /** 查询微信扫码状态 */
  getWechatStatus(ticket: string): Promise<ScanStatusResult> {
    return request({ url: '/web/auth/wechat/status', method: 'get', params: { ticket } })
  }
}
```

Note: The backend does not currently have a `/web/auth/captcha` endpoint. The `getCaptcha()` API calls the admin endpoint for now (`/auth/captcha`). If the backend needs a web-specific captcha endpoint, add it to `WebAuthController` later.

Wait — let me fix this. The admin captcha endpoint is `/api/auth/captcha`, but the web API uses `/web/auth/` prefix. Let me make `getCaptcha` call the admin endpoint:

Actually, for password login via `WebAuthController`, the captcha check happens in the `PasswordLoginStrategy`, which is shared. The captcha endpoint at `/auth/captcha` (admin) stores the captcha in Redis with a UUID key. The web login should use the same endpoint. So:

```typescript
  /** 获取图形验证码 */
  getCaptcha(): Promise<CaptchaResult> {
    return request({ url: '/auth/captcha', method: 'get' })
  },
```

- [ ] **Step 4: Commit**

```bash
git add zion-ui-user/src/types/login.ts zion-ui-user/src/utils/request.ts zion-ui-user/src/api/auth.ts
git commit -m "feat: add types, request utils, and auth API layer"
```

---

### Task 8: Create user store and router

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\stores\user.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\router\index.ts`

- [ ] **Step 1: Create src/stores/user.ts**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/login'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isLogin = computed(() => !!token.value)
  const nickname = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')

  async function login(loginData: { loginType: string } & Record<string, any>) {
    const res = await authApi.login(loginData)
    token.value = res.token
    user.value = {
      id: res.userId,
      username: res.username,
      nickname: res.nickname,
      avatar: res.avatar || '',
      email: '',
      phone: '',
      gender: 0,
      status: 1
    }
    await getInfo()
    return res
  }

  async function getInfo() {
    const res = await authApi.getInfo()
    user.value = res.user
    roles.value = res.roles
    permissions.value = res.permissions
    return res
  }

  async function logout() {
    const hadToken = !!token.value
    token.value = null
    user.value = null
    roles.value = []
    permissions.value = []

    if (hadToken) {
      try {
        await authApi.logout()
      } catch {
        // ignore
      }
    }

    router.push('/login')
  }

  return {
    token,
    user,
    roles,
    permissions,
    isLogin,
    nickname,
    avatar,
    login,
    getInfo,
    logout
  }
}, {
  persist: {
    key: 'Zion-user',
    paths: ['token']
  }
})
```

- [ ] **Step 2: Create src/router/index.ts**

```typescript
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { requiresAuth: true, title: '主页' }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'Zion 用户端'

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const userStore = useUserStore()

  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.user) {
    userStore.getInfo()
      .then(() => next({ ...to, replace: true }))
      .catch(() => {
        userStore.logout()
        next('/login')
      })
    return
  }

  next()
})

export default router
```

- [ ] **Step 3: Commit**

```bash
git add zion-ui-user/src/stores/user.ts zion-ui-user/src/router/index.ts
git commit -m "feat: add user store with token persistence and router with auth guard"
```

---

### Task 9: Create composables

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\composables\useCaptcha.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\composables\useLogin.ts`

- [ ] **Step 1: Create src/composables/useCaptcha.ts**

```typescript
import { ref } from 'vue'
import { authApi } from '@/api/auth'

export function useCaptcha() {
  const captchaUuid = ref('')
  const captchaImg = ref('')
  const loading = ref(false)

  async function refreshCaptcha() {
    loading.value = true
    try {
      const res = await authApi.getCaptcha()
      captchaUuid.value = res.uuid
      captchaImg.value = res.img
    } catch {
      // ignore
    } finally {
      loading.value = false
    }
  }

  return {
    captchaUuid,
    captchaImg,
    loading,
    refreshCaptcha
  }
}
```

- [ ] **Step 2: Create src/composables/useLogin.ts**

```typescript
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useLogin() {
  const router = useRouter()
  const userStore = useUserStore()
  const loading = ref(false)
  const errorMsg = ref('')

  async function login(data: { loginType: string } & Record<string, any>) {
    loading.value = true
    errorMsg.value = ''
    try {
      await userStore.login(data)
      router.replace('/home')
    } catch (e: any) {
      errorMsg.value = e.message || '登录失败'
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    errorMsg,
    login
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add zion-ui-user/src/composables/useCaptcha.ts zion-ui-user/src/composables/useLogin.ts
git commit -m "feat: add useCaptcha and useLogin composables"
```

---

### Task 10: Create LoginSelector component

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\LoginSelector.vue`

- [ ] **Step 1: Create LoginSelector.vue**

```vue
<script setup lang="ts">
import type { LoginType } from '@/types/login'
import { Key, PhonePortrait, QrCode } from '@vicons/ionicons5'

defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  select: [type: LoginType]
}>()

const cards = [
  {
    type: 'password' as LoginType,
    title: '账号密码',
    desc: '使用账号密码登录',
    icon: Key,
    color: '#667eea'
  },
  {
    type: 'sms' as LoginType,
    title: '短信验证码',
    desc: '手机号快速验证登录',
    icon: PhonePortrait,
    color: '#f093fb'
  },
  {
    type: 'wechat' as LoginType,
    title: '微信扫码',
    desc: '微信扫一扫安全登录',
    icon: QrCode,
    color: '#4facfe'
  }
]
</script>

<template>
  <div class="login-selector">
    <div class="selector-header">
      <h1 class="selector-title">欢迎回来</h1>
      <p class="selector-subtitle">请选择登录方式</p>
    </div>

    <div class="cards-grid">
      <div
        v-for="card in cards"
        :key="card.type"
        class="login-card"
        :class="{ disabled }"
        @click="!disabled && emit('select', card.type)"
      >
        <div class="card-icon" :style="{ background: card.color }">
          <n-icon size="32">
            <component :is="card.icon" />
          </n-icon>
        </div>
        <div class="card-content">
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </div>
        <div class="card-arrow">
          <n-icon size="20">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </n-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-selector {
  max-width: 480px;
  margin: 0 auto;
  padding: 40px 24px;
}

.selector-header {
  text-align: center;
  margin-bottom: 40px;
}

.selector-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.selector-subtitle {
  font-size: 15px;
  color: #6b7280;
}

.cards-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.login-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: #667eea;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.12);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }

  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.card-content {
  flex: 1;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
    margin-bottom: 4px;
  }

  p {
    font-size: 13px;
    color: #9ca3af;
  }
}

.card-arrow {
  color: #d1d5db;
  flex-shrink: 0;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/LoginSelector.vue
git commit -m "feat: add LoginSelector card component"
```

---

### Task 11: Create PasswordLogin component

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\PasswordLogin.vue`

- [ ] **Step 1: Create PasswordLogin.vue**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { LockClosed, Person } from '@vicons/ionicons5'
import { useCaptcha } from '@/composables/useCaptcha'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { captchaUuid, captchaImg, refreshCaptcha } = useCaptcha()
const { loading, errorMsg, login } = useLogin()

const form = ref({
  username: '',
  password: '',
  captcha: ''
})

onMounted(() => {
  refreshCaptcha()
})

async function handleSubmit() {
  await login({
    loginType: 'password',
    username: form.value.username,
    password: form.value.password,
    uuid: captchaUuid.value,
    code: form.value.captcha
  })
  if (!errorMsg.value) {
    refreshCaptcha()
  }
}
</script>

<template>
  <div class="password-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #667eea">
        <n-icon size="28" color="#fff"><Person /></n-icon>
      </div>
      <h2>账号密码登录</h2>
    </div>

    <n-form @submit.prevent="handleSubmit">
      <n-form-item>
        <n-input
          v-model:value="form.username"
          placeholder="请输入用户名"
          size="large"
          :round="false"
          clearable
        >
          <template #prefix>
            <n-icon><Person /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <n-input
          v-model:value="form.password"
          type="password"
          placeholder="请输入密码"
          size="large"
          show-password-on="click"
        >
          <template #prefix>
            <n-icon><LockClosed /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <div class="captcha-row">
          <n-input
            v-model:value="form.captcha"
            placeholder="验证码"
            size="large"
          />
          <div class="captcha-img" @click="refreshCaptcha">
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
            <n-spin v-else size="small" />
          </div>
        </div>
      </n-form-item>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <n-button
        type="primary"
        size="large"
        block
        attr-type="submit"
        :loading="loading"
        :disabled="!form.username || !form.password"
      >
        登录
      </n-button>
    </n-form>
  </div>
</template>

<style lang="scss" scoped>
.password-login {
  max-width: 400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s;

  &:hover {
    color: #374151;
  }
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .n-spin {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/PasswordLogin.vue
git commit -m "feat: add PasswordLogin form component"
```

---

### Task 12: Create SmsCodeLogin component

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\SmsCodeLogin.vue`

- [ ] **Step 1: Create SmsCodeLogin.vue**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { PhonePortrait } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { loading, errorMsg, login } = useLogin()

const form = ref({
  phone: '',
  smsCode: ''
})

const smsLoading = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

async function sendSmsCode() {
  if (countdown.value > 0 || !form.value.phone) return
  smsLoading.value = true
  try {
    await authApi.sendSmsCode(form.value.phone)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (e: any) {
    // error handled by interceptor
  } finally {
    smsLoading.value = false
  }
}

async function handleSubmit() {
  await login({
    loginType: 'sms',
    phone: form.value.phone,
    smsCode: form.value.smsCode
  })
}
</script>

<template>
  <div class="sms-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #f093fb">
        <n-icon size="28" color="#fff"><PhonePortrait /></n-icon>
      </div>
      <h2>短信验证码登录</h2>
    </div>

    <n-form @submit.prevent="handleSubmit">
      <n-form-item>
        <n-input
          v-model:value="form.phone"
          placeholder="请输入手机号"
          size="large"
          maxlength="11"
        >
          <template #prefix>
            <n-icon><PhonePortrait /></n-icon>
          </template>
        </n-input>
      </n-form-item>

      <n-form-item>
        <div class="sms-row">
          <n-input
            v-model:value="form.smsCode"
            placeholder="验证码"
            size="large"
            maxlength="6"
          />
          <n-button
            size="large"
            :loading="smsLoading"
            :disabled="countdown > 0 || !form.phone"
            @click="sendSmsCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </n-button>
        </div>
      </n-form-item>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <n-button
        type="primary"
        size="large"
        block
        attr-type="submit"
        :loading="loading"
        :disabled="!form.phone || !form.smsCode"
      >
        登录
      </n-button>
    </n-form>
  </div>
</template>

<style lang="scss" scoped>
.sms-login {
  max-width: 400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s;

  &:hover {
    color: #374151;
  }
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.sms-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/SmsCodeLogin.vue
git commit -m "feat: add SmsCodeLogin form component"
```

---

### Task 13: Create WechatQrcodeLogin component

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\WechatQrcodeLogin.vue`

- [ ] **Step 1: Create WechatQrcodeLogin.vue**

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { QrCode } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'
import { useLogin } from '@/composables/useLogin'

const emit = defineEmits<{
  back: []
}>()

const { loading, errorMsg, login } = useLogin()

const qrUrl = ref('')
const ticket = ref('')
const statusText = ref('正在加载二维码...')
const isExpired = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await loadQrcode()
})

onUnmounted(() => {
  stopPolling()
})

async function loadQrcode() {
  isExpired.value = false
  statusText.value = '正在加载二维码...'
  try {
    const res = await authApi.getWechatQrcode()
    qrUrl.value = res.qrUrl
    ticket.value = res.ticket
    statusText.value = '请使用微信扫一扫'
    startPolling()
  } catch (e: any) {
    statusText.value = '加载二维码失败：' + (e.message || '未知错误')
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await authApi.getWechatStatus(ticket.value)
      switch (res.status) {
        case 'scanned':
          statusText.value = '扫码成功，请在手机上确认登录'
          break
        case 'confirmed':
          statusText.value = '登录中...'
          stopPolling()
          // 微信扫描确认后会返回一个 auth_code，这里用 ticket 对应的信息完成登录
          await login({
            loginType: 'social',
            platform: 'wechat_open',
            authCode: ticket.value
          })
          break
        case 'cancelled':
          statusText.value = '已取消，可重新扫码'
          stopPolling()
          break
        case 'expired':
          statusText.value = '二维码已过期'
          isExpired.value = true
          stopPolling()
          break
      }
    } catch {
      // ignore poll errors
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
</script>

<template>
  <div class="wechat-login">
    <button class="back-btn" @click="emit('back')">
      <n-icon size="20">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </n-icon>
      <span>返回选择</span>
    </button>

    <div class="form-header">
      <div class="form-icon" style="background: #4facfe">
        <n-icon size="28" color="#fff"><QrCode /></n-icon>
      </div>
      <h2>微信扫码登录</h2>
    </div>

    <div class="qrcode-area">
      <div class="qrcode-wrapper" :class="{ expired: isExpired }">
        <img v-if="qrUrl" :src="qrUrl" alt="微信扫码" class="qrcode-img" />
        <n-spin v-else size="large" />
        <div v-if="isExpired" class="qrcode-overlay" @click="loadQrcode">
          <n-icon size="32">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </n-icon>
          <span>点击刷新</span>
        </div>
      </div>
      <p class="status-text" :class="{ error: isExpired }">{{ statusText }}</p>
    </div>

    <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
  </div>
</template>

<style lang="scss" scoped>
.wechat-login {
  max-width: 400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 20px;
  transition: color 0.2s;

  &:hover {
    color: #374151;
  }
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.form-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
}

.qrcode-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-wrapper {
  width: 220px;
  height: 220px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #fff;

  &.expired {
    .qrcode-img {
      filter: blur(4px) opacity(0.3);
    }
  }
}

.qrcode-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qrcode-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  color: #4facfe;
  font-size: 14px;
  font-weight: 500;
}

.status-text {
  margin-top: 16px;
  font-size: 14px;
  color: #6b7280;
  text-align: center;

  &.error {
    color: #ef4444;
  }
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 12px;
  text-align: center;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/WechatQrcodeLogin.vue
git commit -m "feat: add WechatQrcodeLogin component with polling"
```

---

### Task 14: Create Login page view

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\index.vue`

- [ ] **Step 1: Create login/index.vue**

```vue
<script setup lang="ts">
import { ref, type Component } from 'vue'
import type { LoginType } from '@/types/login'
import LoginSelector from './components/LoginSelector.vue'
import PasswordLogin from './components/PasswordLogin.vue'
import SmsCodeLogin from './components/SmsCodeLogin.vue'
import WechatQrcodeLogin from './components/WechatQrcodeLogin.vue'

type Step = 'select' | 'form'

const currentStep = ref<Step>('select')
const activeType = ref<LoginType>('password')
const loginLoading = ref(false)

const formComponents: Record<LoginType, Component> = {
  password: PasswordLogin,
  sms: SmsCodeLogin,
  wechat: WechatQrcodeLogin
}

function handleSelect(type: LoginType) {
  activeType.value = type
  currentStep.value = 'form'
}

function handleBack() {
  currentStep.value = 'select'
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-shape shape-1" />
      <div class="bg-shape shape-2" />
      <div class="bg-shape shape-3" />
    </div>

    <div class="login-container">
      <Transition name="slide-fade" mode="out-in">
        <LoginSelector
          v-if="currentStep === 'select'"
          key="select"
          :disabled="loginLoading"
          @select="handleSelect"
        />
        <component
          v-else
          :is="formComponents[activeType]"
          key="form"
          @back="handleBack"
        />
      </Transition>
    </div>

    <footer class="login-footer">
      <span>&copy; 2026 Zion. All rights reserved.</span>
    </footer>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;

  &.shape-1 {
    width: 600px;
    height: 600px;
    background: #667eea;
    top: -200px;
    right: -100px;
  }

  &.shape-2 {
    width: 400px;
    height: 400px;
    background: #f093fb;
    bottom: -100px;
    left: -80px;
  }

  &.shape-3 {
    width: 300px;
    height: 300px;
    background: #4facfe;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.06),
    0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.login-footer {
  position: absolute;
  bottom: 24px;
  font-size: 13px;
  color: #9ca3af;

  span {
    opacity: 0.7;
  }
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/index.vue
git commit -m "feat: add login page view with animated step transitions"
```

---

### Task 15: Create Home placeholder page

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\home\index.vue`

- [ ] **Step 1: Create home/index.vue**

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { LogOut, Person } from '@vicons/ionicons5'

const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
}
</script>

<template>
  <div class="home-page">
    <header class="home-header">
      <h1 class="app-name">Zion</h1>
      <n-button text @click="handleLogout">
        <template #icon>
          <n-icon><LogOut /></n-icon>
        </template>
        退出登录
      </n-button>
    </header>

    <main class="home-main">
      <div class="welcome-card">
        <n-avatar
          :src="userStore.avatar"
          :size="80"
          round
          fallback-src=""
        >
          <n-icon size="40"><Person /></n-icon>
        </n-avatar>
        <h2>欢迎回来，{{ userStore.nickname }}</h2>
        <p class="username">@{{ userStore.user?.username }}</p>
        <n-divider />
        <p class="placeholder-hint">更多功能即将上线，敬请期待</p>
      </div>
    </main>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e5e7eb;

  .app-name {
    font-size: 20px;
    font-weight: 700;
    color: #1a1a2e;
  }
}

.home-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-card {
  text-align: center;
  background: #fff;
  padding: 48px 64px;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.06);

  h2 {
    margin-top: 20px;
    font-size: 22px;
    font-weight: 600;
    color: #1a1a2e;
  }

  .username {
    margin-top: 6px;
    font-size: 14px;
    color: #9ca3af;
  }

  .placeholder-hint {
    font-size: 14px;
    color: #9ca3af;
    margin-top: 8px;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/home/index.vue
git commit -m "feat: add home placeholder page with user info and logout"
```

---

### Task 16: Frontend component tests

**Files:**
- Create: `E:\project\Zion-Admin\zion-ui-user\vitest.config.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\__tests__\LoginSelector.spec.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\__tests__\PasswordLogin.spec.ts`
- Create: `E:\project\Zion-Admin\zion-ui-user\src\views\login\components\__tests__\SmsCodeLogin.spec.ts`
- Modify: `E:\project\Zion-Admin\zion-ui-user\package.json` (add test deps and script)

- [ ] **Step 1: Add test dependencies to package.json**

```bash
cd E:/project/Zion-Admin/zion-ui-user && npm install --save-dev vitest @vue/test-utils jsdom @vitejs/plugin-vue
```

- [ ] **Step 2: Create vitest.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'jsdom',
    globals: true
  }
})
```

- [ ] **Step 3: Add test script to package.json**

In package.json scripts, add: `"test": "vitest run"`

- [ ] **Step 4: Create LoginSelector.spec.ts**

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginSelector from '../LoginSelector.vue'

describe('LoginSelector', () => {
  it('renders 3 login cards', () => {
    const wrapper = mount(LoginSelector)
    const cards = wrapper.findAll('.login-card')
    expect(cards).toHaveLength(3)
  })

  it('emits select with "password" when clicking password card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[0].trigger('click')
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')![0]).toEqual(['password'])
  })

  it('emits select with "sms" when clicking SMS card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[1].trigger('click')
    expect(wrapper.emitted('select')![0]).toEqual(['sms'])
  })

  it('emits select with "wechat" when clicking WeChat card', async () => {
    const wrapper = mount(LoginSelector)
    await wrapper.findAll('.login-card')[2].trigger('click')
    expect(wrapper.emitted('select')![0]).toEqual(['wechat'])
  })

  it('does not emit when disabled', async () => {
    const wrapper = mount(LoginSelector, {
      props: { disabled: true }
    })
    await wrapper.findAll('.login-card')[0].trigger('click')
    expect(wrapper.emitted('select')).toBeFalsy()
  })
})
```

- [ ] **Step 5: Create PasswordLogin.spec.ts**

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PasswordLogin from '../PasswordLogin.vue'

vi.mock('@/api/auth', () => ({
  authApi: {
    getCaptcha: vi.fn().mockResolvedValue({ uuid: 'test-uuid', img: 'data:image/png;base64,xxx' }),
    login: vi.fn().mockResolvedValue({ token: 'token123', userId: 1, username: 'test', nickname: 'Test', avatar: '' })
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn() })
}))

describe('PasswordLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders back button and form', () => {
    const wrapper = mount(PasswordLogin)
    expect(wrapper.find('.back-btn').exists()).toBe(true)
    expect(wrapper.find('h2').text()).toBe('账号密码登录')
  })

  it('emits back when clicking back button', async () => {
    const wrapper = mount(PasswordLogin)
    await wrapper.find('.back-btn').trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('disables login button when fields are empty', () => {
    const wrapper = mount(PasswordLogin)
    const btn = wrapper.findComponent({ name: 'NButton' })
    expect(btn.props('disabled')).toBe(true)
  })
})
```

- [ ] **Step 6: Create SmsCodeLogin.spec.ts**

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import SmsCodeLogin from '../SmsCodeLogin.vue'

vi.mock('@/api/auth', () => ({
  authApi: {
    sendSmsCode: vi.fn().mockResolvedValue(undefined),
    login: vi.fn().mockResolvedValue({ token: 'token123', userId: 1, username: '13800138000', nickname: '用户0000', avatar: '' })
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn() })
}))

describe('SmsCodeLogin', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders phone input and SMS code input', () => {
    const wrapper = mount(SmsCodeLogin)
    expect(wrapper.find('h2').text()).toBe('短信验证码登录')
  })

  it('emits back when clicking back button', async () => {
    const wrapper = mount(SmsCodeLogin)
    await wrapper.find('.back-btn').trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('disables login button when fields are empty', () => {
    const wrapper = mount(SmsCodeLogin)
    const btn = wrapper.findComponent({ name: 'NButton' })
    expect(btn.props('disabled')).toBe(true)
  })
})
```

- [ ] **Step 7: Run tests**

```bash
cd E:/project/Zion-Admin/zion-ui-user && npx vitest run
```
Expected: PASS (7 tests)

- [ ] **Step 8: Commit**

```bash
git add zion-ui-user/vitest.config.ts zion-ui-user/src/views/login/components/__tests__/LoginSelector.spec.ts zion-ui-user/src/views/login/components/__tests__/PasswordLogin.spec.ts zion-ui-user/src/views/login/components/__tests__/SmsCodeLogin.spec.ts zion-ui-user/package.json
git commit -m "test: add component tests for login components"
```

---

### Task 17: End-to-end verification

- [ ] **Step 1: Build and verify backend compiles**

```bash
cd E:/project/Zion-Admin && ./mvnw compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all backend tests**

```bash
cd E:/project/Zion-Admin && ./mvnw test -pl zion-infra/zion-wechat,zion-infra/zion-social,zion-api/zion-web-api
```
Expected: All tests PASS

- [ ] **Step 3: Build frontend**

```bash
cd E:/project/Zion-Admin/zion-ui-user && npx vite build
```
Expected: Build succeeds, outputs to `zion-starter/src/main/resources/static-user/`

- [ ] **Step 4: Verify dev server starts**

```bash
cd E:/project/Zion-Admin/zion-ui-user && npx vite --host 0.0.0.0 &
sleep 3
curl http://localhost:3001 | head -20
```
Expected: Returns index.html content

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: final verification and adjustments"
```
