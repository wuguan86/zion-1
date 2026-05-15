package com.zion.web.controller;

import com.zion.auth.LoginHelper;
import com.zion.auth.LoginStrategyFactory;
import com.zion.sms.SmsServiceFactory;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.service.WebUserService;
import com.zion.system.service.SysUserService;
import com.zion.web.service.WechatPcQrLoginService;
import com.zion.wechat.WechatMpService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebAuthControllerTest {

    @Test
    void callbackFailureRedirectsToPublicFrontendHomeDerivedFromOAuthCallbackUrl() throws Exception {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        SystemConfigHelper configHelper = mock(SystemConfigHelper.class);
        when(configHelper.getWechatMpOAuthRedirectUrl())
                .thenReturn("https://wuguan.vip.cpolar.cn/api/web/auth/wechat/callback");
        when(wechatMpService.oauthLogin("bad-code"))
                .thenThrow(new RuntimeException("invalid appsecret"));

        WebAuthController controller = new WebAuthController(
                mock(LoginStrategyFactory.class),
                mock(SysUserService.class),
                mock(SmsServiceFactory.class),
                wechatMpService,
                mock(WebUserService.class),
                mock(LoginHelper.class),
                configHelper,
                mock(StringRedisTemplate.class),
                mock(WechatPcQrLoginService.class)
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.callback("bad-code", null, mock(HttpServletRequest.class), response);

        assertThat(response.getRedirectedUrl())
                .startsWith("https://wuguan.vip.cpolar.cn/home?error=");
    }
}
