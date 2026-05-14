package com.zion.auth;

import lombok.Data;

import java.util.Map;

/**
 * 统一登录结果
 */
@Data
public class LoginResult {

    /**
     * Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 扩展信息
     */
    private Map<String, Object> extra;

    public static LoginResult of(String token, Long userId, String username, String nickname, String avatar) {
        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setUserId(userId);
        result.setUsername(username);
        result.setNickname(nickname);
        result.setAvatar(avatar);
        return result;
    }
}
