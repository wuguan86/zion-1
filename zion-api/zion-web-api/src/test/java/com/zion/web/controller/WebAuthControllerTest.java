package com.zion.web.controller;

import com.zion.auth.LoginStrategyFactory;
import com.zion.common.exception.GlobalExceptionHandler;
import com.zion.sms.SmsServiceFactory;
import com.zion.system.service.SysUserService;
import com.zion.wechat.WechatOpenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WebAuthController webAuthController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webAuthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- SMS code happy path ----

    @Test
    void smsCode_shouldReturnOk() throws Exception {
        when(redisTemplate.hasKey(eq("sms:limit:13800138000"))).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(smsServiceFactory.sendCode(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(redisTemplate).hasKey(eq("sms:limit:13800138000"));
        verify(smsServiceFactory).sendCode(eq("13800138000"), anyString());
        verify(valueOperations).set(eq("sms:login:13800138000"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq("sms:limit:13800138000"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
    }

    // ---- SMS code edge cases ----

    @Test
    void smsCode_shouldRejectNullPhone() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请输入正确的手机号"));
    }

    @Test
    void smsCode_shouldRejectEmptyPhone() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请输入正确的手机号"));
    }

    @Test
    void smsCode_shouldRejectInvalidPhoneFormat() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"12345678901\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请输入正确的手机号"));
    }

    @Test
    void smsCode_shouldRejectNonNumericPhone() throws Exception {
        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"abcdefghijk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("请输入正确的手机号"));
    }

    @Test
    void smsCode_shouldRejectRateLimit() throws Exception {
        when(redisTemplate.hasKey(eq("sms:limit:13800138000"))).thenReturn(true);

        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("发送太频繁，请稍后再试"));

        verify(redisTemplate).hasKey(eq("sms:limit:13800138000"));
    }

    @Test
    void smsCode_shouldRejectSendFailure() throws Exception {
        when(redisTemplate.hasKey(eq("sms:limit:13800138000"))).thenReturn(false);
        when(smsServiceFactory.sendCode(eq("13800138000"), anyString())).thenReturn(false);

        mockMvc.perform(post("/web/auth/sms-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("短信发送失败"));

        verify(smsServiceFactory).sendCode(eq("13800138000"), anyString());
    }

    // ---- Wechat QR code ----

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

        verify(wechatOpenService).isConfigured();
        verify(wechatOpenService).createQrcode();
    }

    @Test
    void getWechatQrcode_shouldRejectWhenNotConfigured() throws Exception {
        when(wechatOpenService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/web/auth/wechat/qrcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("微信扫码登录未配置"));

        verify(wechatOpenService).isConfigured();
    }

    @Test
    void getWechatStatus_shouldReturnStatus() throws Exception {
        when(wechatOpenService.getScanStatus("ticket123")).thenReturn("pending");

        mockMvc.perform(get("/web/auth/wechat/status")
                .param("ticket", "ticket123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("pending"));

        verify(wechatOpenService).getScanStatus("ticket123");
    }
}
