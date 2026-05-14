package com.zion.auth;

import com.zion.auth.enums.ClientType;
import com.zion.auth.enums.LoginType;
import lombok.Data;

/**
 * 统一登录请求
 */
@Data
public class LoginRequest {

    /**
     * 登录方式
     */
    private LoginType loginType;

    /**
     * 客户端类型
     */
    private ClientType clientType;

    // ========== 密码登录 ==========

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码key
     */
    private String uuid;

    /**
     * 验证码
     */
    private String code;

    /**
     * 是否记住我
     */
    private Boolean rememberMe;

    // ========== 手机号登录 ==========

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码
     */
    private String smsCode;

    // ========== 小程序登录 ==========

    /**
     * 小程序 wx.login() 的 code
     */
    private String wxCode;

    /**
     * 获取手机号的 code
     */
    private String phoneCode;

    // ========== 三方登录 ==========

    /**
     * 三方平台：wechat_mp / alipay / apple
     */
    private String platform;

    /**
     * 授权码
     */
    private String authCode;
}
