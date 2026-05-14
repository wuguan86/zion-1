package com.zion.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户端类型枚举
 */
@Getter
@AllArgsConstructor
public enum ClientType {

    ADMIN("admin", "后台管理端"),
    WEB("web", "PC前台端"),
    APP("app", "App/小程序端");

    private final String code;
    private final String desc;

    public static ClientType of(String code) {
        if (code == null) return ADMIN;
        for (ClientType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return ADMIN;
    }
}
