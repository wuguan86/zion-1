package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员实体
 */
@Data
@TableName("sys_chat_group_member")
public class ChatGroupMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 群ID
     */
    private Long groupId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 群内昵称
     */
    private String nickname;
    
    /**
     * 角色：0-普通成员 1-管理员 2-群主
     */
    private Integer role;
    
    /**
     * 是否禁言：0-否 1-是
     */
    private Integer muted;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
    
    /**
     * 用户名（非数据库字段）
     */
    @TableField(exist = false)
    private String username;
    
    /**
     * 用户昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String userNickname;
    
    /**
     * 用户头像（非数据库字段）
     */
    @TableField(exist = false)
    private String avatar;
}
