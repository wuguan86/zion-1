package com.zion.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.message.entity.SysChatMessage;

import java.util.List;

/**
 * 聊天消息服务接口
 */
public interface SysChatMessageService {

    /**
     * 发送消息
     */
    SysChatMessage send(SysChatMessage message);

    /**
     * 获取聊天记录（两人之间）
     */
    Page<SysChatMessage> getChatHistory(Long userId, Long targetId, Integer page, Integer pageSize);

    /**
     * 获取最近联系人列表
     */
    List<SysChatMessage> getRecentContacts(Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long userId, Long senderId);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount(Long userId);

    /**
     * 获取与某人的未读消息数量
     */
    int getUnreadCountWithUser(Long userId, Long senderId);
    
    /**
     * 获取与某人的最新一条消息
     */
    SysChatMessage getLatestMessage(Long userId, Long targetId);
    
    /**
     * 清空与某人的聊天记录
     */
    void clearHistory(Long userId, Long targetId);
}
