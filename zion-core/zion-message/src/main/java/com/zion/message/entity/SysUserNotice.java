package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知关联表（记录已读状态）
 */
@Data
@TableName("sys_user_notice")
public class SysUserNotice implements Serializable {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知ID
     */
    private Long noticeId;

    /**
     * 是否已读（0未读 1已读）
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
}
