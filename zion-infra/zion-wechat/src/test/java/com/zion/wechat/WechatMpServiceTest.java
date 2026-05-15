package com.zion.wechat;

import com.zion.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatMpServiceTest {

    @Test
    void getOAuthUrlEncodesRedirectUriForWechatAuthorizeEndpoint() {
        SystemConfigHelper configHelper = mock(SystemConfigHelper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(configHelper.getWechatMpAppId()).thenReturn("wx-test");

        WechatMpService service = new WechatMpService(configHelper, redisTemplate);

        String url = service.getOAuthUrl(
                "https://wuguan.vip.cpolar.cn/api/web/auth/wechat/callback",
                "state123",
                "snsapi_userinfo"
        );

        assertThat(url).contains("appid=wx-test");
        assertThat(url).contains("redirect_uri=https%3A%2F%2Fwuguan.vip.cpolar.cn%2Fapi%2Fweb%2Fauth%2Fwechat%2Fcallback");
        assertThat(url).contains("scope=snsapi_userinfo");
        assertThat(url).contains("state=state123");
        assertThat(url).endsWith("#wechat_redirect");
    }
}
