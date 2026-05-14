package com.zion.auth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式枚举
 */
@Getter
@AllArgsConstructor
public enum LoginType {

    PASSWORD("password", "密码登录"),
    SMS("sms", "短信验证码登录"),
    MINIPROGRAM("miniprogram", "小程序登录"),
    SOCIAL("social", "三方授权登录");

    private final String code;
    private final String desc;

    @JsonCreator
    public static LoginType of(String code) {
        if (code == null) return PASSWORD;
        for (LoginType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return PASSWORD;
    }
}
