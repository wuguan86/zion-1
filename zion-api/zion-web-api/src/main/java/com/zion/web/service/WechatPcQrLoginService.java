package com.zion.web.service;

import cn.hutool.json.JSONUtil;
import com.zion.auth.LoginHelper;
import com.zion.auth.LoginResult;
import com.zion.system.entity.WebUser;
import com.zion.system.service.WebUserService;
import com.zion.wechat.WechatMpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPcQrLoginService {

    private static final int EXPIRE_SECONDS = 300;
    private static final String SESSION_KEY = "wechat:pc:qr:session:";
    private static final String SCENE_KEY = "wechat:pc:qr:scene:";
    private static final String RESULT_KEY = "wechat:pc:qr:result:";
    private static final String WAITING = "WAITING";
    private static final String CONFIRMED = "CONFIRMED";

    private final WechatMpService wechatMpService;
    private final WebUserService webUserService;
    private final LoginHelper loginHelper;
    private final StringRedisTemplate redisTemplate;

    public QrLoginSession createSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String scene = "pc_login_" + sessionId;
        WechatMpService.MpQrCode qrCode = wechatMpService.createTemporaryQrCode(scene, EXPIRE_SECONDS);

        redisTemplate.opsForValue().set(SESSION_KEY + sessionId, WAITING, EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(SCENE_KEY + scene, sessionId, EXPIRE_SECONDS, TimeUnit.SECONDS);

        QrLoginSession session = new QrLoginSession();
        session.setSessionId(sessionId);
        session.setQrCodeUrl(qrCode.getQrCodeUrl());
        session.setExpiresIn(EXPIRE_SECONDS);
        return session;
    }

    public QrLoginStatus poll(String sessionId) {
        String status = redisTemplate.opsForValue().get(SESSION_KEY + sessionId);
        QrLoginStatus result = new QrLoginStatus();
        result.setStatus(status != null ? status : "EXPIRED");
        if (CONFIRMED.equals(status)) {
            String loginJson = redisTemplate.opsForValue().get(RESULT_KEY + sessionId);
            if (loginJson != null && !loginJson.isBlank()) {
                result.setLoginResult(JSONUtil.toBean(loginJson, LoginResult.class));
            }
        }
        return result;
    }

    public boolean confirmScan(String scene, String openId) {
        String sessionId = redisTemplate.opsForValue().get(SCENE_KEY + scene);
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("[WechatPcQrLogin] scan ignored: scene not found, scene={}", scene);
            return false;
        }

        WebUser webUser = ensureWebUser(openId);
        if (webUser.getStatus() != null && webUser.getStatus() != 1) {
            log.warn("[WechatPcQrLogin] scan rejected: disabled web user id={}", webUser.getId());
            return false;
        }

        LoginResult loginResult = loginHelper.doWebLogin(webUser);
        webUserService.updateLoginInfo(webUser.getId());

        redisTemplate.opsForValue().set(SESSION_KEY + sessionId, CONFIRMED, EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(RESULT_KEY + sessionId, JSONUtil.toJsonStr(loginResult), EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.debug("[WechatPcQrLogin] scan confirmed: sessionId={}, userId={}, tokenPresent={}",
                sessionId, loginResult.getUserId(), loginResult.getToken() != null && !loginResult.getToken().isBlank());
        return true;
    }

    private WebUser ensureWebUser(String openId) {
        WebUser webUser = webUserService.getByOpenId(openId);
        if (webUser != null) {
            return webUser;
        }

        WebUser created = new WebUser();
        created.setOpenId(openId);
        created.setNickname("微信用户");
        created.setStatus(1);
        created.setGender(0);
        webUserService.save(created);
        log.debug("[WechatPcQrLogin] web user created from qr scan: id={}, openIdPresent={}",
                created.getId(), created.getOpenId() != null && !created.getOpenId().isBlank());
        return created;
    }

    @Data
    public static class QrLoginSession {
        private String sessionId;
        private String qrCodeUrl;
        private Integer expiresIn;
    }

    @Data
    public static class QrLoginStatus {
        private String status;
        private LoginResult loginResult;
    }
}
