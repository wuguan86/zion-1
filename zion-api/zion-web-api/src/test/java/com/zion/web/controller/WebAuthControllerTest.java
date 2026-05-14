package com.zion.web.controller;

import com.zion.auth.LoginStrategyFactory;
import com.zion.sms.SmsServiceFactory;
import com.zion.system.service.SysUserService;
import com.zion.wechat.WechatOpenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebAuthControllerTest {

    @Mock
    private LoginStrategyFactory loginStrategyFactory;

    @Mock
    private SysUserService userService;

    @Mock
    private SmsServiceFactory smsServiceFactory;

    @Mock
    private WechatOpenService wechatOpenService;

    @InjectMocks
    private WebAuthController webAuthController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webAuthController).build();
    }

    @Test
    void smsCode_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getWechatQrcode_shouldReturnTicketAndUrl() throws Exception {
        WechatOpenService.QrcodeResult qrResult = new WechatOpenService.QrcodeResult();
        qrResult.setTicket("ticket123");
        qrResult.setQrUrl("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket123");
        qrResult.setExpireSeconds(300);

        when(wechatOpenService.isConfigured()).thenReturn(true);
        when(wechatOpenService.createQrcode()).thenReturn(qrResult);

        mockMvc.perform(get("/web/auth/wechat/qrcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticket").value("ticket123"))
                .andExpect(jsonPath("$.data.qrUrl").value("https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=ticket123"));
    }

    @Test
    void getWechatStatus_shouldReturnStatus() throws Exception {
        when(wechatOpenService.getScanStatus("ticket123")).thenReturn("pending");

        mockMvc.perform(get("/web/auth/wechat/status")
                .param("ticket", "ticket123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }
}
