package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群聊实体
 */
@Data
@TableName("sys_chat_group")
public class ChatGroup {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 群名称
     */
    private String name;
    
    /**
     * 群头像
     */
    private String avatar;
    
    /**
     * 群主ID
     */
    private Long ownerId;
    
    /**
     * 群公告
     */
    private String announcement;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 状态：0-解散 1-正常
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 群主名称（非数据库字段）
     */
    @TableField(exist = false)
    private String ownerName;
    
    /**
     * 成员数量（非数据库字段）
     */
    @TableField(exist = false)
    private Integer memberCount;
    
    /**
     * 成员列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<ChatGroupMember> members;
    
    /**
     * 最新消息（非数据库字段）
     */
    @TableField(exist = false)
    private String lastMessage;
    
    /**
     * 最新消息时间（非数据库字段）
     */
    @TableField(exist = false)
    private LocalDateTime lastMessageTime;
}
