package com.zion.web.controller;

import com.zion.web.service.WechatPcQrLoginService;
import com.zion.wechat.WechatMpService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatMpServerControllerTest {

    @Test
    void verifyServerReturnsEchostrWhenSignatureIsValid() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        when(wechatMpService.checkSignature("sig", "ts", "nonce")).thenReturn(true);
        WechatMpServerController controller = new WechatMpServerController(wechatMpService, mock(WechatPcQrLoginService.class));

        String response = controller.verify("sig", "ts", "nonce", "hello-wechat");

        assertThat(response).isEqualTo("hello-wechat");
    }

    @Test
    void verifyServerReturnsEmptyWhenSignatureIsInvalid() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        when(wechatMpService.checkSignature("bad", "ts", "nonce")).thenReturn(false);
        WechatMpServerController controller = new WechatMpServerController(wechatMpService, mock(WechatPcQrLoginService.class));

        String response = controller.verify("bad", "ts", "nonce", "hello-wechat");

        assertThat(response).isEmpty();
    }

    @Test
    void receiveScanEventConfirmsPcQrLogin() {
        WechatMpService wechatMpService = mock(WechatMpService.class);
        WechatPcQrLoginService qrLoginService = mock(WechatPcQrLoginService.class);
        WechatMpServerController controller = new WechatMpServerController(wechatMpService, qrLoginService);

        String xml = """
                <xml>
                  <ToUserName><![CDATA[to]]></ToUserName>
                  <FromUserName><![CDATA[openid-1]]></FromUserName>
                  <CreateTime>123</CreateTime>
                  <MsgType><![CDATA[event]]></MsgType>
                  <Event><![CDATA[SCAN]]></Event>
                  <EventKey><![CDATA[pc_login_scene]]></EventKey>
                </xml>
                """;

        String response = controller.message("sig", "ts", "nonce", xml);

        assertThat(response).isEmpty();
        verify(qrLoginService).confirmScan("pc_login_scene", "openid-1");
    }
}
