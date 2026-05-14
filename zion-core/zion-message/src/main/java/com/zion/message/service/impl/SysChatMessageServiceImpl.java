package com.zion.message.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.message.entity.SysChatMessage;
import com.zion.system.entity.SysUser;
import com.zion.message.mapper.SysChatMessageMapper;
import com.zion.system.mapper.SysUserMapper;
import com.zion.message.service.SysChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysChatMessageServiceImpl implements SysChatMessageService {

    private final SysChatMessageMapper chatMessageMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysChatMessage send(SysChatMessage message) {
        Long senderId = StpUtil.getLoginIdAsLong();
        SysUser sender = userMapper.selectById(senderId);
        
        message.setSenderId(senderId);
        message.setSenderName(sender != null ? sender.getNickname() : "未知用户");
        message.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        message.setIsRead(0);
        message.setSendTime(LocalDateTime.now());
        
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public Page<SysChatMessage> getChatHistory(Long userId, Long targetId, Integer page, Integer pageSize) {
        Page<SysChatMessage> pageParam = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        // 查询两人之间的所有消息
        wrapper.and(w -> w
                .and(w1 -> w1.eq(SysChatMessage::getSenderId, userId).eq(SysChatMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(SysChatMessage::getSenderId, targetId).eq(SysChatMessage::getReceiverId, userId))
        );
        wrapper.orderByDesc(SysChatMessage::getSendTime);
        
        return chatMessageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<SysChatMessage> getRecentContacts(Long userId) {
        // 查询用户最近的消息
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(SysChatMessage::getSenderId, userId)
                .or()
                .eq(SysChatMessage::getReceiverId, userId)
        );
        wrapper.orderByDesc(SysChatMessage::getSendTime);
        wrapper.last("LIMIT 100");
        
        List<SysChatMessage> messages = chatMessageMapper.selectList(wrapper);
        
        // 按联系人分组，取最新一条
        Map<Long, SysChatMessage> contactMap = new LinkedHashMap<>();
        for (SysChatMessage msg : messages) {
            Long contactId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (contactId > 0 && !contactMap.containsKey(contactId)) {
                contactMap.put(contactId, msg);
            }
        }
        
        return new ArrayList<>(contactMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long senderId) {
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId);
        wrapper.eq(SysChatMessage::getSenderId, senderId);
        wrapper.eq(SysChatMessage::getIsRead, 0);
        wrapper.set(SysChatMessage::getIsRead, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return chatMessageMapper.selectUnreadCount(userId);
    }

    @Override
    public int getUnreadCountWithUser(Long userId, Long senderId) {
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId);
        wrapper.eq(SysChatMessage::getSenderId, senderId);
        wrapper.eq(SysChatMessage::getIsRead, 0);
        return Math.toIntExact(chatMessageMapper.selectCount(wrapper));
    }
    
    @Override
    public SysChatMessage getLatestMessage(Long userId, Long targetId) {
        return chatMessageMapper.selectLatestMessage(userId, targetId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId, Long targetId) {
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(SysChatMessage::getSenderId, userId).eq(SysChatMessage::getReceiverId, targetId))
                .or(w2 -> w2.eq(SysChatMessage::getSenderId, targetId).eq(SysChatMessage::getReceiverId, userId))
        );
        chatMessageMapper.delete(wrapper);
    }
}
