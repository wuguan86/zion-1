package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户黑名单
 */
@Data
@TableName("sys_user_blacklist")
public class SysUserBlacklist {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 被拉黑的用户ID
     */
    private Long blockedUserId;
    
    /**
     * 拉黑时间
     */
    private LocalDateTime createTime;
    
    /**
     * 被拉黑用户的昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String blockedUserName;
    
    /**
     * 被拉黑用户的头像（非数据库字段）
     */
    @TableField(exist = false)
    private String blockedUserAvatar;
}
