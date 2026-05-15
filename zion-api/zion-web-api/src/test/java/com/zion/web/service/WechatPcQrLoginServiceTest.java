package com.zion.web.service;

import com.zion.auth.LoginHelper;
import com.zion.auth.LoginResult;
import com.zion.system.entity.WebUser;
import com.zion.system.service.WebUserService;
import com.zion.wechat.WechatMpService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WechatPcQrLoginServiceTest {

    @Test
    void createSessionStoresSceneMappingAndReturnsQrCodeUrl() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        WechatMpService.MpQrCode qrCode = new WechatMpService.MpQrCode();
        qrCode.setTicket("ticket-1");
        qrCode.setUrl("https://weixin.qq.com/q/test");
        qrCode.setQrCodeUrl("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket-1");
        qrCode.setExpireSeconds(300);
        when(wechatMpService.createTemporaryQrCode(startsWith("pc_login_"), eq(300))).thenReturn(qrCode);

        WechatPcQrLoginService service = new WechatPcQrLoginService(
                wechatMpService,
                mock(WebUserService.class),
                mock(LoginHelper.class),
                redisTemplate
        );

        WechatPcQrLoginService.QrLoginSession session = service.createSession();

        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getQrCodeUrl()).isEqualTo(qrCode.getQrCodeUrl());
        assertThat(session.getExpiresIn()).isEqualTo(300);
        verify(valueOperations).set(eq("wechat:pc:qr:session:" + session.getSessionId()), eq("WAITING"), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(startsWith("wechat:pc:qr:scene:pc_login_"), eq(session.getSessionId()), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void confirmScanStoresLoginResultForPolling() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wechat:pc:qr:scene:pc_login_scene")).thenReturn("session-1");

        WebUser webUser = new WebUser();
        webUser.setId(100L);
        webUser.setOpenId("openid-1");
        webUser.setNickname("微信用户");
        webUser.setStatus(1);

        WebUserService webUserService = mock(WebUserService.class);
        when(webUserService.getByOpenId("openid-1")).thenReturn(webUser);

        LoginResult loginResult = LoginResult.of("token-1", 100L, null, "微信用户", "");
        LoginHelper loginHelper = mock(LoginHelper.class);
        when(loginHelper.doWebLogin(webUser)).thenReturn(loginResult);

        WechatPcQrLoginService service = new WechatPcQrLoginService(
                mock(WechatMpService.class),
                webUserService,
                loginHelper,
                redisTemplate
        );

        boolean confirmed = service.confirmScan("pc_login_scene", "openid-1");

        assertThat(confirmed).isTrue();
        verify(valueOperations).set(eq("wechat:pc:qr:session:session-1"), eq("CONFIRMED"), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("wechat:pc:qr:result:session-1"), contains("\"token\":\"token-1\""), eq(300L), eq(TimeUnit.SECONDS));
        verify(webUserService).updateLoginInfo(100L);
    }
}
