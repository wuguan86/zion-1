package com.zion.system.service;

import com.zion.system.entity.SysUserBlacklist;

import java.util.List;

/**
 * 用户黑名单服务接口
 */
public interface SysUserBlacklistService {
    
    /**
     * 拉黑用户
     */
    void blockUser(Long userId, Long blockedUserId);
    
    /**
     * 取消拉黑
     */
    void unblockUser(Long userId, Long blockedUserId);
    
    /**
     * 获取黑名单列表
     */
    List<SysUserBlacklist> getBlacklist(Long userId);
    
    /**
     * 检查是否被拉黑（检查对方是否拉黑了我）
     */
    boolean isBlocked(Long userId, Long targetUserId);
    
    /**
     * 检查是否在我的黑名单中
     */
    boolean isInMyBlacklist(Long userId, Long targetUserId);
}
