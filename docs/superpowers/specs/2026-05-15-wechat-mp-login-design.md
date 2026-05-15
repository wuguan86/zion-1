# WeChat Official Account Login for PC User Frontend

## Overview

Replace the broken WeChat PC QR-code-scan login in `zion-ui-user` with WeChat Official Account (公众号) OAuth 2.0 web authorization login. Separate PC user data from admin data by creating `web_user` table. All PC users (any login type) are stored in `web_user`. Remove the QR-code polling approach entirely.

## Architecture

```
zion-ui-user/                          (frontend, modify)
  └── replace WechatQrcodeLogin → WechatMpLogin

zion-api/zion-web-api/                 (modify)
  └── WebAuthController               add authorize/callback, remove qrcode/status endpoints

zion-core/zion-system/                 (new)
  ├── entity/WebUser.java             new
  ├── mapper/WebUserMapper.java       new
  └── service/WebUserService.java     new interface + impl

zion-core/zion-auth/                   (modify)
  └── LoginHelper                     add doWebLogin(WebUser) method

zion-infra/zion-wechat/                (reuse as-is)
  └── WechatMpService.oauthLogin()    already complete

sql/                                   (modify)
  └── zion-system.sql                  add CREATE TABLE web_user
```

**Key design decision:** The OAuth callback endpoint (`GET /web/auth/wechat/callback`) handles the login process directly — no LoginStrategy needed. The OAuth callback flow (GET + 302 redirect) is fundamentally different from JSON username/password login (POST + JSON response). The callback endpoint calls `WechatMpService.oauthLogin(code)`, finds/registers the web_user, then calls `LoginHelper.doWebLogin()`. No new LoginStrategy or LoginType enum value is needed.

## Database — `web_user`

```sql
CREATE TABLE web_user (
    id              bigint NOT NULL PRIMARY KEY COMMENT '主键ID(雪花算法)',
    username        varchar(50)  NULL COMMENT '用户名',
    nickname        varchar(50)  NULL COMMENT '昵称',
    avatar          varchar(255) NULL COMMENT '头像URL',
    email           varchar(100) NULL COMMENT '邮箱',
    phone           varchar(20)  NULL COMMENT '手机号',
    gender          tinyint NULL DEFAULT 0 COMMENT '性别(0-未知 1-男 2-女)',
    status          tinyint NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    open_id         varchar(100) NULL COMMENT '微信公众号openId',
    union_id        varchar(100) NULL COMMENT '微信unionId',
    last_login_time datetime     NULL COMMENT '最后登录时间',
    create_time     datetime     NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         tinyint NULL DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
    UNIQUE INDEX uk_open_id (open_id),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
);
```

No RBAC join tables needed. This is a standalone lightweight user table.

## Login Flow (OAuth 2.0)

```
User (browser)                    Frontend (Vue)                  Backend (Spring)                WeChat Server
───────────────────────────────────────────────────────────────────────────────────────────────────────────
1. Click "微信登录"
                                   2. GET /web/auth/wechat/authorize
                                                                  3. Construct OAuth URL:
                                                                     https://open.weixin.qq.com/connect/oauth2/authorize
                                                                       ?appid=xxx&redirect_uri=xxx&response_type=code
                                                                       &scope=snsapi_userinfo&state=STATE#wechat_redirect
                                                                  4. Return { authorizeUrl }

                                   5. window.location.href redirect
                                                                                                     6. User authorizes on WeChat

                                                                                                     7. Redirect back to:
                                                                                                        /api/web/auth/wechat/callback?code=CODE&state=STATE

                                                                  8. code → WechatMpService.oauthLogin(code)
                                                                                                     9. GET /sns/oauth2/access_token → { access_token, openId }
                                                                                                     10. GET /sns/userinfo → { nickname, headimgurl, ... }

                                                                  11. Find web_user by openId
                                                                      - Not found → auto-register (save to web_user)
                                                                      - Found → update nickname/avatar

                                                                  12. StpUtil.login(webUserId)
                                                                      Generate token

                                                                  13. 302 Redirect to frontend:
                                                                      /home?token=TOKEN&userId=ID&nickname=NICK&avatar=AVATAR

14. Frontend /home renders
    URL params → store token + user → enter home page
```

## Backend Changes

### 1. `WebUser` Entity

File: `zion-core/zion-system/src/main/java/com/zion/system/entity/WebUser.java`

- `@TableName("web_user")`，不继承 `BaseEntity`（避免 `IdType.AUTO` 冲突，且 web 用户不需要审计字段）
- `@TableId(type = IdType.ASSIGN_ID) private Long id` — MyBatis-Plus 雪花算法生成全局唯一 Long ID
- 字段：`username`, `nickname`, `avatar`, `email`, `phone`, `gender`, `status`, `openId`, `unionId`, `lastLoginTime`
- 逻辑删除字段：`deleted`（`@TableLogic`）
- 时间字段：`createTime`, `updateTime`（`@TableField(fill = ...)`）

### 2. `WebUserMapper`

File: `zion-core/zion-system/src/main/java/com/zion/system/mapper/WebUserMapper.java`

- Extends `BaseMapper<WebUser>`
- `@Select("SELECT * FROM web_user WHERE open_id = #{openId} AND deleted = 0")` → `WebUser selectByOpenId(String openId)`

### 3. `WebUserService` + `WebUserServiceImpl`

File: `zion-core/zion-system/src/main/java/com/zion/system/service/WebUserService.java`
File: `zion-core/zion-system/src/main/java/com/zion/system/service/impl/WebUserServiceImpl.java`

- `getByOpenId(String openId)` → find user by openId
- `register(WebUser user)` → save new web user
- `updateLoginInfo(Long id)` → update `lastLoginTime`
- `getById(Long id)` → get user detail (no extra joins)
- `updateById(WebUser user)` → update profile
- `getByPhone(String phone)` → find by phone (SMS login reuses this)

### 4. `WebAuthController` — add/remove endpoints

**New:**
- `GET /web/auth/wechat/authorize` — construct and return `{ authorizeUrl }` (URL user must visit)
- `GET /web/auth/wechat/callback` — OAuth callback, handles code exchange, login/register, redirects to frontend `/home?token=...`

**Remove:**
- `GET /web/auth/wechat/qrcode` — no longer needed
- `GET /web/auth/wechat/status` — no longer needed

### 5. `LoginHelper` — add `doWebLogin(WebUser)`

Same pattern as `doLogin(SysUser)` but takes `WebUser`. Sets session with web user info (loginName, ipaddr, browser, os, loginTime), plus `session.set("userSource", "web")` to distinguish web users from admin users. Records login log, returns `LoginResult`.

Snowflake IDs are globally unique across tables, so no collision between `web_user` and `sys_user` IDs. No prefix needed — just use the raw Long ID.

### 6. `StpInterfaceImpl` — skip permission loading for web users

Add a guard at the top: check `session.get("userSource")`. If `"web"`, return empty lists immediately — web users have no RBAC.

### 7. SaTokenConfig — update exclude paths

Add `/api/web/auth/wechat/callback` and `/api/web/auth/wechat/authorize` to the exclude list.

### 8. SQL Migration

Add `CREATE TABLE IF NOT EXISTS web_user` to `sql/zion-system.sql` and `sql/zion-system-postgresql.sql`.

## Frontend Changes

### 1. Replace `WechatQrcodeLogin.vue` → `WechatMpLogin.vue`

No more QR code + polling. Simple component:

```vue
<script setup lang="ts">
import { authApi } from '@/api/auth'

const loading = ref(false)

async function handleWechatLogin() {
  loading.value = true
  try {
    const res = await authApi.getWechatAuthorizeUrl()
    window.location.href = res.authorizeUrl  // redirect to WeChat
  } catch (e: any) {
    // error handling
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <div>
    <p>点击登录后将在新页面进行微信授权</p>
    <n-button @click="handleWechatLogin" :loading="loading">微信登录</n-button>
  </div>
</template>
```

### 2. `api/auth.ts` changes

```ts
// Add
getWechatAuthorizeUrl(): Promise<{ authorizeUrl: string }> {
    return request({ url: '/web/auth/wechat/authorize', method: 'get' })
},

// Remove
getWechatQrcode(): Promise<QrcodeResult> { ... }
getWechatStatus(ticket: string): Promise<ScanStatusResult> { ... }
```

### 3. `router/index.ts` — handle token in URL

In the route guard (or home page), parse URL query params `?token=xxx&userId=xxx&nickname=xxx&avatar=xxx`:
- If token present, store it in `userStore`, set user info from params, clean URL
- This handles the redirect-back from WeChat OAuth callback

### 4. `LoginSelector.vue` — update label

Change "微信扫码登录" to "微信公众号登录", keep the QR icon or change to WeChat icon.

### 5. `types/login.ts` — remove unused types

Remove: `QrcodeResult`, `ScanStatusResult`

## Configuration

Reuse existing `wechatMp` config group (already in admin UI). Required fields:

| Key | Purpose |
|-----|---------|
| `enabled` | Show/hide WeChat login on PC frontend |
| `appId` | 公众号 AppID |
| `appSecret` | 公众号 AppSecret |
| `oauthRedirectUrl` | Callback URL: `https://domain/api/web/auth/wechat/callback` |

No new config group needed.

## SaTokenConfig CORS & Exclude Paths

Exclude from auth interceptor:
```
/api/web/auth/wechat/authorize   ← new
/api/web/auth/wechat/callback    ← new
```

Remove from exclude list:
```
/api/web/auth/wechat/qrcode      ← removed
/api/web/auth/wechat/status      ← removed
```

## Error Handling

| Scenario | Backend | Frontend |
|----------|---------|----------|
| WeChat returns error | throw BusinessException("微信授权失败") | Show error toast |
| openId not found | Auto-register new web_user | Normal login flow |
| web_user status=0 (disabled) | throw BusinessException("账号已禁用") | Show error on login page |
| Invalid callback state | throw BusinessException("非法请求") | Show error toast |
| Config not set (appId empty) | throw BusinessException("微信登录未配置") | Frontend hides WeChat option |

## What Gets Removed

| File | Action |
|------|--------|
| `zion-infra/zion-wechat/WechatOpenService.java` | Remove (uses 公众号 QR code API, wrong approach) |
| `zion-infra/zion-social/WechatOpenSocialLogin.java` | Remove |
| `zion-core/zion-auth/strategy/MiniProgramLoginStrategy.java` | Keep (unrelated, for app) |
| `zion-ui-user/views/login/components/WechatQrcodeLogin.vue` | Remove, replace with WechatMpLogin |
| `zion-api/zion-web-api/.../WebAuthController.java` wechat/qrcode + wechat/status endpoints | Remove |

## Test Plan

Backend:
- `WebUserService` — CRUD, getByOpenId, register
- `WebAuthController` — authorize URL construction, callback flow (code exchange, find/register web_user, doWebLogin, redirect)
- `WechatMpService.oauthLogin()` — already exists, verify it handles errors
- `StpInterfaceImpl` — verify web users return empty permissions, no NPE

Frontend:
- `WechatMpLogin` — renders WeChat login button, triggers redirect on click
- `useLogin` — handles URL token params after OAuth callback redirect
- Route guard — recognizes token in query params
