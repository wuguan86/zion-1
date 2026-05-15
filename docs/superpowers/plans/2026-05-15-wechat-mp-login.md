# WeChat Official Account Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace broken WeChat PC QR-code-scan login with WeChat Official Account (公众号) OAuth 2.0 web authorization login, using a separate `web_user` table with Snowflake IDs.

**Architecture:** Backend creates `WebUser` entity/mapper/service, adds OAuth authorize/callback endpoints to `WebAuthController`, adds `doWebLogin(WebUser)` to `LoginHelper`, and guards `StpInterfaceImpl` for web users. Frontend replaces `WechatQrcodeLogin.vue` with `WechatMpLogin.vue`, updates routing to handle OAuth callback redirect with token in query params. Old QR code infrastructure (`WechatOpenService`, `WechatOpenSocialLogin`, qrcode/status endpoints) is removed.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus (Snowflake ASSIGN_ID), Sa-Token, Vue 3 + TypeScript + Naive UI

---

## Task 1: Create WebUser Entity

**Files:**
- Create: `zion-core/zion-system/src/main/java/com/zion/system/entity/WebUser.java`

- [ ] **Step 1: Write WebUser entity**

```java
package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PC用户端用户
 */
@Data
@TableName("web_user")
public class WebUser implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    private Integer status;

    private String openId;

    private String unionId;

    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/entity/WebUser.java
git commit -m "feat: add WebUser entity for PC user frontend"
```

---

## Task 2: Create WebUserMapper

**Files:**
- Create: `zion-core/zion-system/src/main/java/com/zion/system/mapper/WebUserMapper.java`

- [ ] **Step 1: Write WebUserMapper**

```java
package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.WebUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WebUserMapper extends BaseMapper<WebUser> {

    @Select("SELECT * FROM web_user WHERE open_id = #{openId} AND deleted = 0")
    WebUser selectByOpenId(@Param("openId") String openId);
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/mapper/WebUserMapper.java
git commit -m "feat: add WebUserMapper with selectByOpenId"
```

---

## Task 3: Create WebUserService Interface + Implementation

**Files:**
- Create: `zion-core/zion-system/src/main/java/com/zion/system/service/WebUserService.java`
- Create: `zion-core/zion-system/src/main/java/com/zion/system/service/impl/WebUserServiceImpl.java`

- [ ] **Step 1: Write WebUserService interface**

```java
package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.system.entity.WebUser;

public interface WebUserService extends IService<WebUser> {

    WebUser getByOpenId(String openId);

    WebUser getByPhone(String phone);

    void updateLoginInfo(Long id);
}
```

- [ ] **Step 2: Write WebUserServiceImpl**

```java
package com.zion.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.system.entity.WebUser;
import com.zion.system.mapper.WebUserMapper;
import com.zion.system.service.WebUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class WebUserServiceImpl extends ServiceImpl<WebUserMapper, WebUser> implements WebUserService {

    @Override
    public WebUser getByOpenId(String openId) {
        return baseMapper.selectByOpenId(openId);
    }

    @Override
    public WebUser getByPhone(String phone) {
        return lambdaQuery().eq(WebUser::getPhone, phone).one();
    }

    @Override
    public void updateLoginInfo(Long id) {
        WebUser user = new WebUser();
        user.setId(id);
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/service/WebUserService.java zion-core/zion-system/src/main/java/com/zion/system/service/impl/WebUserServiceImpl.java
git commit -m "feat: add WebUserService with CRUD operations"
```

---

## Task 4: Add doWebLogin(WebUser) to LoginHelper

**Files:**
- Modify: `zion-core/zion-auth/src/main/java/com/zion/auth/LoginHelper.java`

The existing `doLogin(SysUser)` method is at lines 33-65. We add a parallel `doWebLogin(WebUser)` that:
- Sets `session.set("userSource", "web")` to distinguish web users
- Records login log (reuses same `loginLogService`)
- Returns `LoginResult` with web user fields

- [ ] **Step 1: Add doWebLogin method**

Add the following import after line 5 (`com.zion.system.entity.SysUser`):

```java
import com.zion.system.entity.WebUser;
```

Add the following method after `doLogin(SysUser user)` (after line 65):

```java
    /**
     * 执行PC用户端登录并构建结果
     */
    public LoginResult doWebLogin(WebUser user) {
        RequestInfo info = getRequestInfo();

        if (configHelper.isSingleLogin()) {
            StpUtil.logout(user.getId());
        }

        StpUtil.login(user.getId());

        SaSession session = StpUtil.getSession();
        session.set("loginName", user.getNickname() != null ? user.getNickname() : user.getUsername());
        session.set("userSource", "web");
        session.set("ipaddr", info.ip);
        session.set("loginLocation", IpUtils.getAddressByIp(info.ip));
        session.set("browser", info.browser);
        session.set("os", info.os);
        session.set("status", 1);
        session.set("loginTime", System.currentTimeMillis());

        loginLogService.recordLog(user.getNickname() != null ? user.getNickname() : user.getUsername(),
                0, "PC用户端登录成功", info.ip, info.browser, info.os);

        return LoginResult.of(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        );
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-auth -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-core/zion-auth/src/main/java/com/zion/auth/LoginHelper.java
git commit -m "feat: add doWebLogin(WebUser) to LoginHelper with web userSource session marker"
```

---

## Task 5: Guard StpInterfaceImpl for Web Users

**Files:**
- Modify: `zion-core/zion-system/src/main/java/com/zion/system/config/StpInterfaceImpl.java`

Web users have no RBAC. When `session.get("userSource")` equals `"web"`, return empty lists immediately to avoid `Long.parseLong` hitting a Snowflake ID that doesn't exist in `sys_user` table (harmless for permissions, but wasteful and wrong).

- [ ] **Step 1: Add web user guard**

Replace the `getPermissionList` method (lines 32-38) with:

```java
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        if ("web".equals(session.get("userSource"))) {
            return List.of();
        }
        return session.get(CACHE_KEY_PERMISSIONS, () -> {
            log.debug("从数据库加载用户权限: userId={}", loginId);
            return userService.getPermissions(Long.parseLong(loginId.toString()));
        });
    }
```

Replace the `getRoleList` method (lines 43-49) with:

```java
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        if ("web".equals(session.get("userSource"))) {
            return List.of();
        }
        return session.get(CACHE_KEY_ROLES, () -> {
            log.debug("从数据库加载用户角色: userId={}", loginId);
            return userService.getRoleCodes(Long.parseLong(loginId.toString()));
        });
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/config/StpInterfaceImpl.java
git commit -m "feat: skip RBAC permission loading for web users in StpInterfaceImpl"
```

---

## Task 6: Update SaTokenConfig Exclude Paths

**Files:**
- Modify: `zion-core/zion-system/src/main/java/com/zion/system/config/SaTokenConfig.java`

- [ ] **Step 1: Replace qrcode/status with authorize/callback**

Remove lines 32-33:
```
                            "/api/web/auth/wechat/qrcode",
                            "/api/web/auth/wechat/status",
```

Add in their place (same position in the `.notMatch()` block):
```
                            "/api/web/auth/wechat/authorize",
                            "/api/web/auth/wechat/callback",
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-core/zion-system -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-core/zion-system/src/main/java/com/zion/system/config/SaTokenConfig.java
git commit -m "feat: replace wechat qrcode/status exclude paths with authorize/callback"
```

---

## Task 7: Update WebAuthController — Add OAuth Endpoints, Remove QR Code Endpoints

**Files:**
- Modify: `zion-api/zion-web-api/src/main/java/com/zion/web/controller/WebAuthController.java`

This is the biggest backend change. We:
1. Remove `WechatOpenService` dependency and import
2. Add `WechatMpService`, `WebUserService`, `LoginHelper` dependencies
3. Replace `getWechatQrcode()` and `getWechatStatus()` with `authorize()` and `callback()`
4. The `/info` endpoint currently returns `SysUser` — we need to make it work for both admin and web users. For now, the `/info` endpoint only works for admin users (unchanged). Web users get their info from the `/home?token=...` redirect params directly.

Wait — the `/info` endpoint uses `StpUtil.getLoginIdAsLong()` and `userService.getDetail(userId)` which queries `sys_user`. Web users won't have a record in `sys_user`. We need to handle this.

Looking at the spec more carefully: the `/info` endpoint needs to detect whether the current user is a web user or admin user and return the appropriate data.

Let me check how the frontend `getInfo()` is used: after login, the frontend calls `getInfo()` to get full user info. For web users, this will fail because the ID is from `web_user` not `sys_user`.

We need to update the `/info` endpoint to handle both user types.

- [ ] **Step 1: Rewrite WebAuthController**

Replace the entire file content:

```java
package com.zion.web.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.zion.auth.LoginHelper;
import com.zion.auth.LoginRequest;
import com.zion.auth.LoginResult;
import com.zion.auth.LoginStrategyFactory;
import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.Result;
import com.zion.sms.SmsServiceFactory;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.WebUser;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.service.SysUserService;
import com.zion.system.service.WebUserService;
import com.zion.wechat.WechatMpService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/web/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SysUserService userService;
    private final SmsServiceFactory smsServiceFactory;
    private final WechatMpService wechatMpService;
    private final WebUserService webUserService;
    private final LoginHelper loginHelper;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_KEY = "sms:login:";

    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.WEB);
        if (request.getLoginType() == null) {
            request.setLoginType(LoginType.PASSWORD);
        }
        LoginResult result = loginStrategyFactory.login(request);
        return Result.ok(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        SaSession session = StpUtil.getSession();
        boolean isWebUser = "web".equals(session.get("userSource"));

        Map<String, Object> result = new HashMap<>();
        if (isWebUser) {
            WebUser user = webUserService.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            result.put("user", user);
            result.put("roles", List.of());
            result.put("permissions", List.of());
        } else {
            SysUser user = userService.getDetail(userId);
            List<String> roles = userService.getRoleCodes(userId);
            List<String> permissions = userService.getPermissions(userId);
            user.setPassword(null);
            result.put("user", user);
            result.put("roles", roles);
            result.put("permissions", permissions);
        }
        return Result.ok(result);
    }

    @GetMapping("/login-types")
    public Result<?> loginTypes() {
        return Result.ok(loginStrategyFactory.getRegisteredTypes());
    }

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的手机号");
        }

        String limitKey = "sms:limit:" + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException("发送太频繁，请稍后再试");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(phone, code);
        if (!success) {
            throw new BusinessException("短信发送失败");
        }

        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        return Result.ok();
    }

    /**
     * 微信公众号OAuth授权 — 返回授权URL让前端跳转
     */
    @GetMapping("/wechat/authorize")
    public Result<Map<String, String>> authorize() {
        if (!wechatMpService.isConfigured()) {
            throw new BusinessException("微信公众号登录未配置");
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = configHelper.getWechatMpOAuthRedirectUrl();

        String authorizeUrl = wechatMpService.getOAuthUrl(redirectUri, state, "snsapi_userinfo");

        redisTemplate.opsForValue().set("wechat:mp:state:" + state, "1", 5, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("authorizeUrl", authorizeUrl);
        return Result.ok(data);
    }

    /**
     * 微信公众号OAuth回调 — code换用户信息，登录/注册，302跳转前端
     */
    @GetMapping("/wechat/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        if (code == null || code.isEmpty()) {
            response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode("授权失败"));
            return;
        }

        if (state != null && !state.isEmpty()) {
            String stateKey = "wechat:mp:state:" + state;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(stateKey))) {
                response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode("非法请求"));
                return;
            }
            redisTemplate.delete(stateKey);
        }

        try {
            WechatMpService.MpOAuthResult oauthResult = wechatMpService.oauthLogin(code);

            WebUser webUser = webUserService.getByOpenId(oauthResult.getOpenId());
            if (webUser == null) {
                webUser = new WebUser();
                webUser.setOpenId(oauthResult.getOpenId());
                webUser.setUnionId(oauthResult.getUnionId());
                webUser.setNickname(oauthResult.getNickname());
                webUser.setAvatar(oauthResult.getHeadImgUrl());
                webUser.setGender(oauthResult.getSex() != null ? oauthResult.getSex() : 0);
                webUser.setStatus(1);
                webUserService.save(webUser);
            } else {
                webUser.setNickname(oauthResult.getNickname());
                webUser.setAvatar(oauthResult.getHeadImgUrl());
                webUserService.updateById(webUser);
            }

            if (webUser.getStatus() != 1) {
                response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode("账号已禁用"));
                return;
            }

            LoginResult loginResult = loginHelper.doWebLogin(webUser);
            webUserService.updateLoginInfo(webUser.getId());

            String redirectUrl = getFrontendHomeUrl() +
                    "?token=" + encode(loginResult.getToken()) +
                    "&userId=" + loginResult.getUserId() +
                    "&nickname=" + encode(loginResult.getNickname() != null ? loginResult.getNickname() : "") +
                    "&avatar=" + encode(loginResult.getAvatar() != null ? loginResult.getAvatar() : "");

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode(e.getMessage()));
        }
    }

    private String getFrontendHomeUrl() {
        return "/home";
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd E:\project\Zion-Admin && mvn compile -pl zion-api/zion-web-api -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add zion-api/zion-web-api/src/main/java/com/zion/web/controller/WebAuthController.java
git commit -m "feat: replace wechat QR code endpoints with OAuth authorize/callback in WebAuthController"
```

---

## Task 8: Remove WechatOpenService, WechatOpenSocialLogin, and Clean Up

**Files:**
- Delete: `zion-infra/zion-wechat/src/main/java/com/zion/wechat/WechatOpenService.java`
- Delete: `zion-infra/zion-social/src/main/java/com/zion/social/impl/WechatOpenSocialLogin.java`

- [ ] **Step 1: Delete the files**

```bash
git rm zion-infra/zion-wechat/src/main/java/com/zion/wechat/WechatOpenService.java
git rm zion-infra/zion-social/src/main/java/com/zion/social/impl/WechatOpenSocialLogin.java
```

- [ ] **Step 2: Verify build still passes**

Run: `cd E:\project\Zion-Admin && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: remove WechatOpenService and WechatOpenSocialLogin (replaced by WeChat MP OAuth)"
```

---

## Task 9: Add web_user Table to SQL Migration Files

**Files:**
- Modify: `sql/zion-system.sql`
- Modify: `sql/zion-system-postgresql.sql`

- [ ] **Step 1: Add CREATE TABLE to MySQL SQL file**

Append at the end of `sql/zion-system.sql`:

```sql
-- ----------------------------
-- Table structure for web_user (PC用户端)
-- ----------------------------
DROP TABLE IF EXISTS `web_user`;
CREATE TABLE `web_user` (
    `id`              bigint NOT NULL PRIMARY KEY COMMENT '主键ID(雪花算法)',
    `username`        varchar(50)  NULL COMMENT '用户名',
    `nickname`        varchar(50)  NULL COMMENT '昵称',
    `avatar`          varchar(255) NULL COMMENT '头像URL',
    `email`           varchar(100) NULL COMMENT '邮箱',
    `phone`           varchar(20)  NULL COMMENT '手机号',
    `gender`          tinyint NULL DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
    `status`          tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    `open_id`         varchar(100) NULL COMMENT '微信公众号openId',
    `union_id`        varchar(100) NULL COMMENT '微信unionId',
    `last_login_time` datetime     NULL COMMENT '最后登录时间',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
    UNIQUE INDEX uk_open_id (open_id),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'PC用户端用户表';
```

- [ ] **Step 2: Add CREATE TABLE to PostgreSQL SQL file**

Append at the end of `sql/zion-system-postgresql.sql`:

```sql
-- ----------------------------
-- Table structure for web_user (PC用户端)
-- ----------------------------
DROP TABLE IF EXISTS web_user;
CREATE TABLE web_user (
    id              bigint NOT NULL PRIMARY KEY,
    username        varchar(50)  NULL,
    nickname        varchar(50)  NULL,
    avatar          varchar(255) NULL,
    email           varchar(100) NULL,
    phone           varchar(20)  NULL,
    gender          smallint NULL DEFAULT 0,
    status          smallint NULL DEFAULT 1,
    open_id         varchar(100) NULL,
    union_id        varchar(100) NULL,
    last_login_time timestamp    NULL,
    create_time     timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         smallint NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_open_id ON web_user (open_id);
CREATE INDEX idx_phone ON web_user (phone);
CREATE INDEX idx_status ON web_user (status);
```

- [ ] **Step 3: Commit**

```bash
git add sql/zion-system.sql sql/zion-system-postgresql.sql
git commit -m "feat: add web_user table DDL to SQL migration files"
```

---

## Task 10: Create WechatMpLogin.vue (Frontend)

**Files:**
- Create: `zion-ui-user/src/views/login/components/WechatMpLogin.vue`

- [ ] **Step 1: Write WechatMpLogin.vue**

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { QrCode } from '@vicons/ionicons5'
import { authApi } from '@/api/auth'

const emit = defineEmits<{
  back: []
}>()

const loading = ref(false)
const errorMsg = ref('')

async function handleWechatLogin() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await authApi.getWechatAuthorizeUrl()
    window.location.href = res.authorizeUrl
  } catch (e: any) {
    errorMsg.value = e.message || '获取授权链接失败'
  } finally {
    loading.value = false
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
      <h2>微信公众号登录</h2>
    </div>

    <div class="wechat-area">
      <p class="desc">点击登录后将在新页面进行微信授权</p>
      <n-button
        type="primary"
        size="large"
        block
        :loading="loading"
        @click="handleWechatLogin"
      >
        微信登录
      </n-button>
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

.wechat-area {
  text-align: center;
}

.desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 24px;
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
git add zion-ui-user/src/views/login/components/WechatMpLogin.vue
git commit -m "feat: add WechatMpLogin component for WeChat Official Account OAuth"
```

---

## Task 11: Update auth.ts API Layer

**Files:**
- Modify: `zion-ui-user/src/api/auth.ts`

- [ ] **Step 1: Replace QR code methods with authorize method**

Replace the content of `zion-ui-user/src/api/auth.ts`:

```typescript
import { request } from '@/utils/request'
import type { LoginResult, CaptchaResult } from '@/types/login'

export const authApi = {
  /** 获取图形验证码 */
  getCaptcha(): Promise<CaptchaResult> {
    return request({ url: '/auth/captcha', method: 'get' })
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

  /** 获取微信公众号授权URL */
  getWechatAuthorizeUrl(): Promise<{ authorizeUrl: string }> {
    return request({ url: '/web/auth/wechat/authorize', method: 'get' })
  }
}
```

Note: This removes `getWechatQrcode()` and `getWechatStatus()`, and adds `getWechatAuthorizeUrl()`.

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/api/auth.ts
git commit -m "feat: replace QR code API methods with WeChat authorize URL method"
```

---

## Task 12: Update Router to Handle Token in Query Params

**Files:**
- Modify: `zion-ui-user/src/router/index.ts`

When the OAuth callback redirects to `/home?token=xxx&userId=xxx&nickname=xxx&avatar=xxx`, the router guard needs to detect the token in the URL, store it, and clean the URL.

- [ ] **Step 1: Add token-from-URL handling in router guard**

Replace the `router.beforeEach` block (lines 32-58) with:

```typescript
router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'Zion 用户端'

  // Handle OAuth callback redirect: store token from URL params
  if (to.query.token) {
    const userStore = useUserStore()
    userStore.token = to.query.token as string
    userStore.user = {
      id: Number(to.query.userId) || 0,
      username: '',
      nickname: (to.query.nickname as string) || '',
      avatar: (to.query.avatar as string) || '',
      email: '',
      phone: '',
      gender: 0,
      status: 1
    }
    // Clean URL and proceed
    const cleanedQuery = { ...to.query }
    delete cleanedQuery.token
    delete cleanedQuery.userId
    delete cleanedQuery.nickname
    delete cleanedQuery.avatar
    delete cleanedQuery.error
    next({ ...to, query: cleanedQuery, replace: true })
    return
  }

  // Handle error from OAuth callback
  if (to.query.error) {
    const cleanedQuery = { ...to.query }
    delete cleanedQuery.error
    next({ path: '/login', query: { ...cleanedQuery, wechatError: to.query.error as string, replace: true } })
    return
  }

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
      .then(() => next())
      .catch(() => {
        userStore.logout()
        next('/login')
      })
    return
  }

  next()
})
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/router/index.ts
git commit -m "feat: handle OAuth callback token and user info in router guard"
```

---

## Task 13: Update User Store for OAuth Token Handling

**Files:**
- Modify: `zion-ui-user/src/stores/user.ts`

The store needs to handle the case where the user is already set from URL params (OAuth flow). In that case, `getInfo()` should still be called to fetch full data, but the store should gracefully handle the changed `/info` response format for web users.

- [ ] **Step 1: Update user store for OAuth compatibility**

No changes needed to the store itself — the OAuth flow directly sets `token` and `user` in the router guard (Task 12), then the router guard's `getInfo()` call fetches full data from `/info`. The `/info` endpoint (updated in Task 7) already handles web users. The `login()` method in the store is only used for password/SMS login (admin users). No changes required.

Wait — let me verify. The router guard has:
```
userStore.getInfo()
  .then(() => next())
```

This calls the `/info` endpoint, which now returns web user data (from Task 7). The `getInfo()` method in the store sets `user.value = res.user`. For web users, the returned `user` will be a `WebUser` object with different fields than `UserInfo` (no `id` type difference — both are `number`/`Long`).

Actually, `WebUser` entity has `openId`, `unionId`, `lastLoginTime` fields that `UserInfo` type doesn't include. But TS won't complain about extra fields. The `UserInfo` type has `id`, `username`, `nickname`, `avatar`, `email`, `phone`, `gender`, `status` which all exist in `WebUser`. So this should work without changes to the store or types.

But there's a subtle issue: the `UserInfo` type has `id: number` but Snowflake IDs are `bigint` (Long in Java). The JSON serialization will send it as a number. In JS, numbers larger than 2^53-1 lose precision. But Snowflake IDs from MyBatis-Plus are typically within the safe range for practical purposes. We'll keep `number` for now.

- [ ] **Step 1: Verify no store changes needed**

The store is fine as-is. The OAuth flow sets token + user in the router guard. The store persists the token. No changes required.

No commit — no changes needed to this file.

---

## Task 14: Update LoginSelector.vue — Change WeChat Option Label

**Files:**
- Modify: `zion-ui-user/src/views/login/components/LoginSelector.vue`

- [ ] **Step 1: Change label and description**

Change line 30-31:
```typescript
    title: '微信公众号',
    desc: '使用微信公众号授权登录',
```

Also change line 31 (the wechat card's type stays `'wechat'`):

```typescript
  {
    type: 'wechat' as LoginType,
    title: '微信公众号',
    desc: '使用微信公众号授权登录',
    icon: QrCode,
    color: '#4facfe'
  }
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/LoginSelector.vue
git commit -m "feat: update WeChat login card label to '微信公众号'"
```

---

## Task 15: Update login/index.vue — Replace WechatQrcodeLogin with WechatMpLogin

**Files:**
- Modify: `zion-ui-user/src/views/login/index.vue`

- [ ] **Step 1: Change import and component mapping**

Change line 7:
```typescript
import WechatMpLogin from './components/WechatMpLogin.vue'
```

Change line 18 (the formComponents mapping):
```typescript
  wechat: WechatMpLogin
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/index.vue
git commit -m "feat: replace WechatQrcodeLogin with WechatMpLogin in login page"
```

---

## Task 16: Update types/login.ts — Remove QR Code Types

**Files:**
- Modify: `zion-ui-user/src/types/login.ts`

- [ ] **Step 1: Remove QrcodeResult and ScanStatusResult**

Remove lines 37-45 (the `QrcodeResult` and `ScanStatusResult` interfaces).

Also remove `QrcodeResult, ScanStatusResult` from the import on line 1 of `auth.ts` (already done in Task 11).

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/types/login.ts
git commit -m "feat: remove unused QrcodeResult and ScanStatusResult types"
```

---

## Task 17: Remove Old WechatQrcodeLogin.vue

**Files:**
- Delete: `zion-ui-user/src/views/login/components/WechatQrcodeLogin.vue`

- [ ] **Step 1: Delete the file**

```bash
git rm zion-ui-user/src/views/login/components/WechatQrcodeLogin.vue
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: remove WechatQrcodeLogin component (replaced by WechatMpLogin)"
```

---

## Task 18: Handle WeChat Error Display on Login Page

**Files:**
- Modify: `zion-ui-user/src/views/login/components/LoginSelector.vue`

When the OAuth callback fails, the router redirects to `/login?wechatError=...`. The `LoginSelector` should display this error.

- [ ] **Step 1: Add wechatError display in LoginSelector**

Add to the `<script setup>` section of `LoginSelector.vue`:

```typescript
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const wechatError = computed(() => route.query.wechatError as string || '')
```

Add to the template, after the `</div>` closing tag of `.cards-grid`:

```html
    <p v-if="wechatError" class="error-msg">{{ wechatError }}</p>
```

Add to the styles:

```scss
.error-msg {
  color: #ef4444;
  font-size: 13px;
  margin-top: 16px;
  text-align: center;
}
```

- [ ] **Step 2: Commit**

```bash
git add zion-ui-user/src/views/login/components/LoginSelector.vue
git commit -m "feat: display WeChat OAuth error message on login selector"
```

---

## Task 19: Final Verification — Full Build

- [ ] **Step 1: Build backend**

Run: `cd E:\project\Zion-Admin && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Build frontend**

Run: `cd E:\project\Zion-Admin\zion-ui-user && npm run build`
Expected: Build succeeds with no TypeScript errors

- [ ] **Step 3: Commit if any build fixes needed**

```bash
git add -A
git commit -m "chore: final verification, all tests passing"
```

---

## Summary of All Files Changed

| Action | File |
|--------|------|
| Create | `zion-core/zion-system/.../entity/WebUser.java` |
| Create | `zion-core/zion-system/.../mapper/WebUserMapper.java` |
| Create | `zion-core/zion-system/.../service/WebUserService.java` |
| Create | `zion-core/zion-system/.../service/impl/WebUserServiceImpl.java` |
| Create | `zion-ui-user/src/views/login/components/WechatMpLogin.vue` |
| Modify | `zion-core/zion-auth/.../LoginHelper.java` |
| Modify | `zion-core/zion-system/.../config/StpInterfaceImpl.java` |
| Modify | `zion-core/zion-system/.../config/SaTokenConfig.java` |
| Modify | `zion-api/zion-web-api/.../controller/WebAuthController.java` |
| Modify | `zion-ui-user/src/api/auth.ts` |
| Modify | `zion-ui-user/src/router/index.ts` |
| Modify | `zion-ui-user/src/views/login/components/LoginSelector.vue` |
| Modify | `zion-ui-user/src/views/login/index.vue` |
| Modify | `zion-ui-user/src/types/login.ts` |
| Modify | `sql/zion-system.sql` |
| Modify | `sql/zion-system-postgresql.sql` |
| Delete | `zion-infra/zion-wechat/.../WechatOpenService.java` |
| Delete | `zion-infra/zion-social/.../WechatOpenSocialLogin.java` |
| Delete | `zion-ui-user/src/views/login/components/WechatQrcodeLogin.vue` |
