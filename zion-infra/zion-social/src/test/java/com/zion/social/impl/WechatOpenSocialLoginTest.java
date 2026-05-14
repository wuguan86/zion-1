package com.zion.social.impl;

import com.zion.social.SocialLoginService;
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
        oauthResult.setUnionId("union123");
        oauthResult.setNickname("Test");
        oauthResult.setHeadImgUrl("http://avatar");
        oauthResult.setSex(1);
        oauthResult.setRawJson("{\"nickname\":\"Test\"}");

        when(wechatOpenService.getUserInfoByCode("auth_code_123")).thenReturn(oauthResult);

        SocialLoginService.SocialUserInfo info = socialLogin.getUserInfo("auth_code_123");

        assertEquals("wechat_open", info.getPlatform());
        assertEquals("open123", info.getOpenId());
        assertEquals("union123", info.getUnionId());
        assertEquals("Test", info.getNickname());
        assertEquals("http://avatar", info.getAvatar());
        assertEquals(1, info.getGender());
        assertEquals("{\"nickname\":\"Test\"}", info.getRawJson());

        verify(wechatOpenService).getUserInfoByCode("auth_code_123");
    }
}
