package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zion.system.entity.SysUserBlacklist;
import com.zion.system.mapper.SysUserBlacklistMapper;
import com.zion.system.service.SysUserBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户黑名单服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserBlacklistServiceImpl implements SysUserBlacklistService {
    
    private final SysUserBlacklistMapper blacklistMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blockUser(Long userId, Long blockedUserId) {
        // 检查是否已经拉黑
        if (isInMyBlacklist(userId, blockedUserId)) {
            throw new RuntimeException("该用户已在黑名单中");
        }
        
        // 不能拉黑自己
        if (userId.equals(blockedUserId)) {
            throw new RuntimeException("不能拉黑自己");
        }
        
        SysUserBlacklist blacklist = new SysUserBlacklist();
        blacklist.setUserId(userId);
        blacklist.setBlockedUserId(blockedUserId);
        blacklist.setCreateTime(LocalDateTime.now());
        blacklistMapper.insert(blacklist);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblockUser(Long userId, Long blockedUserId) {
        LambdaQueryWrapper<SysUserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserBlacklist::getUserId, userId);
        wrapper.eq(SysUserBlacklist::getBlockedUserId, blockedUserId);
        blacklistMapper.delete(wrapper);
    }
    
    @Override
    public List<SysUserBlacklist> getBlacklist(Long userId) {
        return blacklistMapper.selectBlacklistWithUser(userId);
    }
    
    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        // 检查对方是否拉黑了我
        return blacklistMapper.checkBlocked(targetUserId, userId) > 0;
    }
    
    @Override
    public boolean isInMyBlacklist(Long userId, Long targetUserId) {
        // 检查我是否拉黑了对方
        return blacklistMapper.checkBlocked(userId, targetUserId) > 0;
    }
}
