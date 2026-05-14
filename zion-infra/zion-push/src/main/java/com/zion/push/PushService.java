package com.zion.push;

import java.util.List;
import java.util.Map;

/**
 * 推送服务接口
 */
public interface PushService {

    /**
     * 获取服务商类型
     */
    String getProviderType();

    /**
     * 获取服务商名称
     */
    String getProviderName();

    /**
     * 推送给单个用户
     *
     * @param userId  用户ID
     * @param title   标题
     * @param content 内容
     * @param extras  扩展数据
     * @return 是否成功
     */
    boolean pushToUser(String userId, String title, String content, Map<String, String> extras);

    /**
     * 推送给多个用户
     *
     * @param userIds 用户ID列表
     * @param title   标题
     * @param content 内容
     * @param extras  扩展数据
     * @return 是否成功
     */
    boolean pushToUsers(List<String> userIds, String title, String content, Map<String, String> extras);

    /**
     * 推送给所有用户
     *
     * @param title   标题
     * @param content 内容
     * @param extras  扩展数据
     * @return 是否成功
     */
    boolean pushToAll(String title, String content, Map<String, String> extras);

    /**
     * 推送给指定标签
     *
     * @param tags    标签列表
     * @param title   标题
     * @param content 内容
     * @param extras  扩展数据
     * @return 是否成功
     */
    boolean pushToTags(List<String> tags, String title, String content, Map<String, String> extras);

    /**
     * 推送给指定设备
     *
     * @param registrationId 设备注册ID
     * @param title          标题
     * @param content        内容
     * @param extras         扩展数据
     * @return 是否成功
     */
    boolean pushToDevice(String registrationId, String title, String content, Map<String, String> extras);
}
