package com.zion.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.message.entity.ChatGroup;
import com.zion.message.entity.ChatGroupMember;
import com.zion.message.entity.ChatGroupMessage;
import com.zion.system.entity.SysUser;
import com.zion.message.mapper.ChatGroupMapper;
import com.zion.message.mapper.ChatGroupMemberMapper;
import com.zion.message.mapper.ChatGroupMessageMapper;
import com.zion.system.mapper.SysUserMapper;
import com.zion.message.service.ChatGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群聊服务实现
 */
@Service
@RequiredArgsConstructor
public class ChatGroupServiceImpl implements ChatGroupService {
    
    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper memberMapper;
    private final ChatGroupMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    
    @Override
    @Transactional
    public ChatGroup createGroup(String name, Long ownerId, List<Long> memberIds) {
        // 创建群
        ChatGroup group = new ChatGroup();
        group.setName(name);
        group.setOwnerId(ownerId);
        group.setStatus(1);
        group.setMaxMembers(200);
        groupMapper.insert(group);
        
        // 添加群主为成员
        ChatGroupMember ownerMember = new ChatGroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(2); // 群主
        memberMapper.insert(ownerMember);
        
        // 添加其他成员
        if (memberIds != null && !memberIds.isEmpty()) {
            for (Long userId : memberIds) {
                if (!userId.equals(ownerId)) {
                    ChatGroupMember member = new ChatGroupMember();
                    member.setGroupId(group.getId());
                    member.setUserId(userId);
                    member.setRole(0); // 普通成员
                    memberMapper.insert(member);
                }
            }
        }
        
        // 发送系统消息
        SysUser owner = userMapper.selectById(ownerId);
        sendSystemMessage(group.getId(), owner.getNickname() + " 创建了群聊");
        
        return group;
    }
    
    @Override
    public List<ChatGroup> getUserGroups(Long userId) {
        List<ChatGroup> groups = groupMapper.selectUserGroups(userId);
        // 填充最新消息
        for (ChatGroup group : groups) {
            ChatGroupMessage latestMsg = messageMapper.selectLatestMessage(group.getId());
            if (latestMsg != null) {
                String content = latestMsg.getContent();
                if (latestMsg.getMsgType() == 2) {
                    content = "[图片]";
                } else if (latestMsg.getMsgType() == 4) {
                    content = "[系统消息] " + content;
                }
                group.setLastMessage(latestMsg.getSenderName() + ": " + content);
                group.setLastMessageTime(latestMsg.getSendTime());
            }
        }
        return groups;
    }
    
    @Override
    public ChatGroup getGroupDetail(Long groupId) {
        ChatGroup group = groupMapper.selectGroupDetail(groupId);
        if (group != null) {
            group.setMembers(memberMapper.selectGroupMembers(groupId));
        }
        return group;
    }
    
    @Override
    public void updateGroup(ChatGroup group) {
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
    }
    
    @Override
    @Transactional
    public void dissolveGroup(Long groupId, Long operatorId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("群不存在");
        }
        if (!group.getOwnerId().equals(operatorId)) {
            throw new RuntimeException("只有群主可以解散群聊");
        }
        
        // 更新群状态为解散
        group.setStatus(0);
        groupMapper.updateById(group);
        
        // 删除所有成员
        memberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>()
                .eq(ChatGroupMember::getGroupId, groupId));
    }
    
    @Override
    @Transactional
    public void quitGroup(Long groupId, Long userId) {
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, userId);
        if (member == null) {
            throw new RuntimeException("你不是该群成员");
        }
        if (member.getRole() == 2) {
            throw new RuntimeException("群主不能退出群聊，请先转让群主或解散群聊");
        }
        
        memberMapper.deleteById(member.getId());
        
        // 发送系统消息
        SysUser user = userMapper.selectById(userId);
        sendSystemMessage(groupId, user.getNickname() + " 退出了群聊");
    }
    
    @Override
    @Transactional
    public void addMembers(Long groupId, List<Long> userIds, Long operatorId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() == 0) {
            throw new RuntimeException("群不存在或已解散");
        }
        
        // 检查操作者权限
        ChatGroupMember operator = memberMapper.selectMemberInfo(groupId, operatorId);
        if (operator == null) {
            throw new RuntimeException("你不是该群成员");
        }
        
        // 获取当前成员数
        int currentCount = memberMapper.selectMemberIds(groupId).size();
        if (currentCount + userIds.size() > group.getMaxMembers()) {
            throw new RuntimeException("超过群最大成员数限制");
        }
        
        SysUser operatorUser = userMapper.selectById(operatorId);
        StringBuilder addedNames = new StringBuilder();
        
        for (Long userId : userIds) {
            // 检查是否已是成员
            ChatGroupMember existing = memberMapper.selectMemberInfo(groupId, userId);
            if (existing == null) {
                ChatGroupMember member = new ChatGroupMember();
                member.setGroupId(groupId);
                member.setUserId(userId);
                member.setRole(0);
                memberMapper.insert(member);
                
                SysUser user = userMapper.selectById(userId);
                if (addedNames.length() > 0) {
                    addedNames.append("、");
                }
                addedNames.append(user.getNickname());
            }
        }
        
        if (addedNames.length() > 0) {
            sendSystemMessage(groupId, operatorUser.getNickname() + " 邀请 " + addedNames + " 加入了群聊");
        }
    }
    
    @Override
    @Transactional
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        ChatGroupMember operator = memberMapper.selectMemberInfo(groupId, operatorId);
        if (operator == null || operator.getRole() < 1) {
            throw new RuntimeException("没有权限移除成员");
        }
        
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, userId);
        if (member == null) {
            throw new RuntimeException("该用户不是群成员");
        }
        if (member.getRole() >= operator.getRole()) {
            throw new RuntimeException("不能移除同级或更高级别的成员");
        }
        
        memberMapper.deleteById(member.getId());
        
        SysUser user = userMapper.selectById(userId);
        sendSystemMessage(groupId, user.getNickname() + " 被移出了群聊");
    }
    
    @Override
    public List<ChatGroupMember> getGroupMembers(Long groupId) {
        return memberMapper.selectGroupMembers(groupId);
    }
    
    @Override
    public void setAdmin(Long groupId, Long userId, boolean isAdmin, Long operatorId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (!group.getOwnerId().equals(operatorId)) {
            throw new RuntimeException("只有群主可以设置管理员");
        }
        
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, userId);
        if (member == null) {
            throw new RuntimeException("该用户不是群成员");
        }
        if (member.getRole() == 2) {
            throw new RuntimeException("不能修改群主角色");
        }
        
        member.setRole(isAdmin ? 1 : 0);
        memberMapper.updateById(member);
        
        SysUser user = userMapper.selectById(userId);
        sendSystemMessage(groupId, user.getNickname() + (isAdmin ? " 被设为管理员" : " 被取消管理员"));
    }
    
    @Override
    public void setMuted(Long groupId, Long userId, boolean muted, Long operatorId) {
        ChatGroupMember operator = memberMapper.selectMemberInfo(groupId, operatorId);
        if (operator == null || operator.getRole() < 1) {
            throw new RuntimeException("没有权限操作");
        }
        
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, userId);
        if (member == null) {
            throw new RuntimeException("该用户不是群成员");
        }
        if (member.getRole() >= operator.getRole()) {
            throw new RuntimeException("不能禁言同级或更高级别的成员");
        }
        
        member.setMuted(muted ? 1 : 0);
        memberMapper.updateById(member);
    }
    
    @Override
    @Transactional
    public void transferOwner(Long groupId, Long newOwnerId, Long operatorId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (!group.getOwnerId().equals(operatorId)) {
            throw new RuntimeException("只有群主可以转让群");
        }
        
        ChatGroupMember newOwner = memberMapper.selectMemberInfo(groupId, newOwnerId);
        if (newOwner == null) {
            throw new RuntimeException("该用户不是群成员");
        }
        
        // 更新群主
        group.setOwnerId(newOwnerId);
        groupMapper.updateById(group);
        
        // 更新原群主为普通成员
        ChatGroupMember oldOwner = memberMapper.selectMemberInfo(groupId, operatorId);
        oldOwner.setRole(0);
        memberMapper.updateById(oldOwner);
        
        // 更新新群主
        newOwner.setRole(2);
        memberMapper.updateById(newOwner);
        
        SysUser oldUser = userMapper.selectById(operatorId);
        SysUser newUser = userMapper.selectById(newOwnerId);
        sendSystemMessage(groupId, oldUser.getNickname() + " 将群主转让给了 " + newUser.getNickname());
    }
    
    @Override
    public ChatGroupMessage sendMessage(Long groupId, Long senderId, String content, Integer msgType) {
        // 检查是否是群成员
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, senderId);
        if (member == null) {
            throw new RuntimeException("你不是该群成员");
        }
        if (member.getMuted() == 1) {
            throw new RuntimeException("你已被禁言");
        }
        
        SysUser sender = userMapper.selectById(senderId);
        
        ChatGroupMessage message = new ChatGroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setSenderName(member.getNickname() != null ? member.getNickname() : sender.getNickname());
        message.setSenderAvatar(sender.getAvatar());
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : 1);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        
        // 更新群的更新时间
        ChatGroup group = new ChatGroup();
        group.setId(groupId);
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
        
        return message;
    }
    
    @Override
    public IPage<ChatGroupMessage> getMessageHistory(Long groupId, int page, int pageSize) {
        return messageMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ChatGroupMessage>()
                        .eq(ChatGroupMessage::getGroupId, groupId)
                        .orderByDesc(ChatGroupMessage::getSendTime)
        );
    }
    
    @Override
    public boolean isMember(Long groupId, Long userId) {
        return memberMapper.selectMemberInfo(groupId, userId) != null;
    }
    
    @Override
    public List<Long> getMemberIds(Long groupId) {
        return memberMapper.selectMemberIds(groupId);
    }
    
    /**
     * 发送系统消息
     */
    private void sendSystemMessage(Long groupId, String content) {
        ChatGroupMessage message = new ChatGroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(0L);
        message.setSenderName("系统消息");
        message.setContent(content);
        message.setMsgType(4); // 系统消息
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
    }
}
