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
import com.zion.web.service.WechatPcQrLoginService;
import com.zion.wechat.WechatMpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PC端认证控制器
 * 支持密码登录、短信登录、微信公众号OAuth登录
 */
@RestController
@RequestMapping("/web/auth")
@RequiredArgsConstructor
@Slf4j
public class WebAuthController {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SysUserService userService;
    private final SmsServiceFactory smsServiceFactory;
    private final WechatMpService wechatMpService;
    private final WebUserService webUserService;
    private final LoginHelper loginHelper;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;
    private final WechatPcQrLoginService wechatPcQrLoginService;

    private static final String SMS_CODE_KEY = "sms:login:";

    /**
     * 统一登录接口
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest request) {
        request.setClientType(ClientType.WEB);
        if (request.getLoginType() == null) {
            request.setLoginType(LoginType.PASSWORD);
        }
        LoginResult result = loginStrategyFactory.login(request);
        return Result.ok(result);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 获取当前用户信息（支持后台用户和PC用户）
     */
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

    /**
     * 获取支持的登录方式
     */
    @GetMapping("/login-types")
    public Result<?> loginTypes() {
        return Result.ok(loginStrategyFactory.getRegisteredTypes());
    }

    /**
     * 发送短信验证码
     */
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
    public Result<Map<String, String>> authorize(HttpServletRequest request) {
        if (!wechatMpService.isConfigured()) {
            log.warn("[WechatOAuth] authorize rejected: mp config incomplete");
            throw new BusinessException("微信公众号登录未配置");
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = configHelper.getWechatMpOAuthRedirectUrl();
        if (redirectUri == null || redirectUri.isBlank()) {
            log.warn("[WechatOAuth] authorize rejected: oauthRedirectUrl is blank");
            throw new BusinessException("请先配置微信公众号OAuth回调URL");
        }
        validateOAuthRedirectUrl(redirectUri);

        String authorizeUrl = wechatMpService.getOAuthUrl(redirectUri, state, "snsapi_userinfo");

        redisTemplate.opsForValue().set("wechat:mp:state:" + state, "1", 5, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("authorizeUrl", authorizeUrl);
        return Result.ok(data);
    }

    @GetMapping("/wechat/authorize-redirect")
    public void authorizeRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizeUrl = createWechatAuthorizeUrl(request, "authorize-redirect");
        response.sendRedirect(authorizeUrl);
    }

    @PostMapping("/wechat/qr/session")
    public Result<WechatPcQrLoginService.QrLoginSession> createWechatQrLoginSession() {
        return Result.ok(wechatPcQrLoginService.createSession());
    }

    @GetMapping("/wechat/qr/poll")
    public Result<WechatPcQrLoginService.QrLoginStatus> pollWechatQrLogin(@RequestParam String sessionId) {
        return Result.ok(wechatPcQrLoginService.poll(sessionId));
    }

    private String createWechatAuthorizeUrl(HttpServletRequest request, String source) {
        if (!wechatMpService.isConfigured()) {
            log.warn("[WechatOAuth] {} rejected: mp config incomplete", source);
            throw new BusinessException("微信公众号登录未配置");
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        String redirectUri = configHelper.getWechatMpOAuthRedirectUrl();
        if (redirectUri == null || redirectUri.isBlank()) {
            log.warn("[WechatOAuth] {} rejected: oauthRedirectUrl is blank", source);
            throw new BusinessException("请先配置微信公众号OAuth回调URL");
        }
        validateOAuthRedirectUrl(redirectUri);

        String authorizeUrl = wechatMpService.getOAuthUrl(redirectUri, state, "snsapi_userinfo");

        redisTemplate.opsForValue().set("wechat:mp:state:" + state, "1", 5, TimeUnit.MINUTES);
        return authorizeUrl;
    }

    /**
     * 微信公众号OAuth回调 — code换用户信息，登录/注册，302跳转前端
     */
    @GetMapping("/wechat/callback")
    public void callback(@RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        if (code == null || code.isEmpty()) {
            log.warn("[WechatOAuth] callback rejected: missing code");
            response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode("授权失败"));
            return;
        }

        if (state != null && !state.isEmpty()) {
            String stateKey = "wechat:mp:state:" + state;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(stateKey))) {
                log.warn("[WechatOAuth] callback rejected: invalid or expired state={}", state);
                response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode("非法请求"));
                return;
            }
            redisTemplate.delete(stateKey);
        } else {
            log.warn("[WechatOAuth] callback received without state");
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
                log.warn("[WechatOAuth] callback rejected: disabled web user id={}", webUser.getId());
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
            log.error("[WechatOAuth] callback failed", e);
            response.sendRedirect(getFrontendHomeUrl() + "?error=" + encode(e.getMessage()));
        }
    }

    private String getFrontendHomeUrl() {
        String redirectUri = configHelper.getWechatMpOAuthRedirectUrl();
        if (redirectUri == null || redirectUri.isBlank()) {
            return "/home";
        }
        try {
            URI uri = new URI(redirectUri);
            if (uri.getScheme() == null || uri.getRawAuthority() == null) {
                return "/home";
            }
            return uri.getScheme() + "://" + uri.getRawAuthority() + "/home";
        } catch (URISyntaxException e) {
            log.warn("[WechatOAuth] failed to derive frontend home url from oauthRedirectUrl={}", redirectUri, e);
            return "/home";
        }
    }

    private void validateOAuthRedirectUrl(String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme();
            String path = uri.getPath();

            if (scheme == null || uri.getHost() == null ||
                    (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BusinessException("微信公众号OAuth回调URL必须是完整的 http/https 地址");
            }
            if (path == null || path.isBlank()) {
                throw new BusinessException("微信公众号OAuth回调URL必须包含回调路径");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException("微信公众号OAuth回调URL格式不正确");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

}
