package com.zion.wechat;

import com.zion.system.helper.SystemConfigHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WechatOpenServiceTest {

    @Mock
    private SystemConfigHelper configHelper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WechatOpenService wechatOpenService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(configHelper.getWechatOpenAppId()).thenReturn("wx123456");
        when(configHelper.getWechatOpenAppSecret()).thenReturn("secret123");
    }

    @Test
    void isConfigured_shouldReturnTrueWhenAppIdAndSecretExist() {
        assertTrue(wechatOpenService.isConfigured());
    }

    @Test
    void isConfigured_shouldReturnFalseWhenAppIdMissing() {
        when(configHelper.getWechatOpenAppId()).thenReturn("");
        assertFalse(wechatOpenService.isConfigured());
    }

    @Test
    void createQrcode_shouldThrowWhenNotConfigured() {
        when(configHelper.getWechatOpenAppId()).thenReturn("");
        assertThrows(RuntimeException.class, () -> wechatOpenService.createQrcode());
    }
}
