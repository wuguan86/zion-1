# User Login Feature Design

## Overview

Build the user-facing frontend (`zion-ui-user`) with login functionality supporting three methods: WeChat PC QR code scan, SMS verification code, and username/password. After login, a minimal placeholder home page is shown. Backend gaps (WeChat Open Platform login, web SMS endpoint) will be filled.

## Architecture

```
zion-ui-user/          (new frontend, Vue 3 + TS + Naive UI)
  └── login + home

zion-api/zion-web-api/ (existing, needs extension)
  └── WebAuthController + wechat qrcode endpoints

zion-infra/zion-wechat/ (existing, needs new service)
  └── WechatOpenService

zion-infra/zion-social/ (existing, needs new impl)
  └── WechatOpenSocialLogin
```

## Tech Stack

- Vue 3.4 + Composition API + `<script setup lang="ts">`
- Vite 5 + TypeScript (strict)
- Naive UI 2.37+
- Pinia + pinia-plugin-persistedstate
- vue-router 4
- axios
- SCSS (no Tailwind)

Match zion-ui's existing dependency versions.

## Component Tree

```
src/views/login/
├── index.vue                  # Login page root: manages step state (select → form)
├── components/
│   ├── LoginSelector.vue      # Initial card selection: 3 cards, emit login type
│   ├── PasswordLogin.vue      # Username + password + captcha form
│   ├── SmsCodeLogin.vue       # Phone + SMS code + countdown form
│   └── WechatQrcodeLogin.vue  # QR code display + status polling
├── composables/
│   ├── useLogin.ts            # Login logic: call API, store token, navigate
│   └── useCaptcha.ts          # Image captcha fetch/refresh
└── types/
    └── login.ts               # LoginType, LoginFormData type definitions

src/views/home/
└── index.vue                  # Placeholder: user info + logout button

src/router/index.ts            # Routes: /login, /home, guard
src/stores/user.ts             # User state (token, userInfo), login/logout
src/api/auth.ts                # API: login, logout, getUserInfo, sendSmsCode, getCaptcha, getWechatQrcode, pollWechatStatus
```

## Data Flow

1. User clicks card → `LoginSelector` emits `select(loginType)`
2. `index.vue` switches `currentStep='form'`, dynamically renders matching component
3. Form components call `useLogin().login()` on submit
4. `useLogin` calls API, backend returns `{ token, userInfo }`
5. `userStore` stores token, `router.replace('/home')`

## Routes

| Path | Component | Auth Required |
|---|---|---|
| `/login` | `views/login/index.vue` | No |
| `/home` | `views/home/index.vue` | Yes |
| `/` | redirect to `/home` | — |
| `/:pathMatch(.*)*` | redirect to `/home` | — |

Route guard: no token → `/login`; token exists but no userInfo → fetch user info → `/home`.

## Backend Changes

### 1. WebAuthController — add SMS endpoint

```
POST /api/web/auth/sms-code  →  SmsServiceFactory.sendCode(phone)
```

The `SmsCodeLoginStrategy` already supports `ClientType.WEB`, only the endpoint is missing.

### 2. WeChat PC QR Code Login (strategy pattern)

New files:

- `WechatOpenService` — Open Platform API (get access_token, create QR ticket, check scan status via `https://api.weixin.qq.com/cgi-bin/qrcode/create`)
- `WechatOpenSocialLogin` — implements `SocialLoginService`, platform `"wechat_open"`, registered via `SocialLoginFactory`
- Config group `wechatOpen` — appId, appSecret in `sys_config_group`

Flow:
```
Client                   Server                      WeChat Open Platform
  │ GET /web/auth/wechat/qrcode                     │
  │ ──────────────────────►  get access_token         │
  │                          ─────────────────────────►
  │                          get QR ticket             │
  │                          ─────────────────────────►
  │ ◄── { ticket, qrUrl }  ◄── { ticket, url }       │
  │                                                     │
  │ Poll /web/auth/wechat/status?ticket=xxx           │
  │ ──────────────────────►  check scan status         │
  │                          ─────────────────────────►
  │ ◄── { status: 'pending/confirmed/cancelled' }    │
  │                                                     │
  │ On confirmed: POST /web/auth/login                │
  │ { loginType: SOCIAL, platform: wechat_open,        │
  │   authCode: xxx }                                  │
  │ ──────────────────────►  SocialLoginStrategy        │
  │                          ─── WechatOpenSocialLogin  │
  │ ◄── { token, userInfo }                           │
```

### 3. WebAuthController — add wechat endpoints

```
GET  /api/web/auth/wechat/qrcode       →  return { ticket, qrUrl, expire }
GET  /api/web/auth/wechat/status       →  return { status, code? }
```

## Error Handling

| Scenario | Handling |
|---|---|
| Wrong credentials | Red error text below form, captcha refresh |
| Invalid/expired SMS code | Red error below input |
| SMS rate limit | Countdown before re-send allowed |
| QR code expired | "Expired, click to refresh" overlay |
| User cancelled scan | "Cancelled, scan again" prompt |
| Network error | `message.error` global toast |
| 401 token expired | Response interceptor → clear state → redirect `/login` |

## Test Plan

Backend tests:
- `WechatOpenService` — unit test: access_token caching, QR ticket creation, status check
- `WechatOpenSocialLogin` — unit test: authorizeUrl generation, getUserInfo mock flow
- `WebAuthController` — integration test: SMS send, QR code fetch, login flow
- `SmsCodeLoginStrategy` — verify WEB client acceptance

Frontend tests (vitest + @vue/test-utils):
- `LoginSelector` — renders 3 cards, emits correct type on click
- `PasswordLogin` — form validation, submit emits correct data
- `SmsCodeLogin` — countdown timer, phone validation, submit
- `useLogin` — API call on success/failure, token storage, navigation
- Route guard — redirect behavior for authenticated/unauthenticated users
