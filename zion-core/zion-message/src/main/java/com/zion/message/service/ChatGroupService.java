package com.zion.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zion.message.entity.ChatGroup;
import com.zion.message.entity.ChatGroupMember;
import com.zion.message.entity.ChatGroupMessage;

import java.util.List;

/**
 * 群聊服务接口
 */
public interface ChatGroupService {
    
    /**
     * 创建群聊
     */
    ChatGroup createGroup(String name, Long ownerId, List<Long> memberIds);
    
    /**
     * 获取用户的群列表
     */
    List<ChatGroup> getUserGroups(Long userId);
    
    /**
     * 获取群详情
     */
    ChatGroup getGroupDetail(Long groupId);
    
    /**
     * 更新群信息
     */
    void updateGroup(ChatGroup group);
    
    /**
     * 解散群聊
     */
    void dissolveGroup(Long groupId, Long operatorId);
    
    /**
     * 退出群聊
     */
    void quitGroup(Long groupId, Long userId);
    
    /**
     * 添加群成员
     */
    void addMembers(Long groupId, List<Long> userIds, Long operatorId);
    
    /**
     * 移除群成员
     */
    void removeMember(Long groupId, Long userId, Long operatorId);
    
    /**
     * 获取群成员列表
     */
    List<ChatGroupMember> getGroupMembers(Long groupId);
    
    /**
     * 设置管理员
     */
    void setAdmin(Long groupId, Long userId, boolean isAdmin, Long operatorId);
    
    /**
     * 设置群成员禁言
     */
    void setMuted(Long groupId, Long userId, boolean muted, Long operatorId);
    
    /**
     * 转让群主
     */
    void transferOwner(Long groupId, Long newOwnerId, Long operatorId);
    
    /**
     * 发送群消息
     */
    ChatGroupMessage sendMessage(Long groupId, Long senderId, String content, Integer msgType);
    
    /**
     * 获取群消息历史
     */
    IPage<ChatGroupMessage> getMessageHistory(Long groupId, int page, int pageSize);
    
    /**
     * 检查用户是否是群成员
     */
    boolean isMember(Long groupId, Long userId);
    
    /**
     * 获取群内所有成员ID
     */
    List<Long> getMemberIds(Long groupId);
}
