package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息表
 */
@Data
@TableName("sys_chat_message")
public class SysChatMessage implements Serializable {

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 接收者ID（0表示群发/广播）
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（1文本 2图片 3文件）
     */
    private Integer msgType;

    /**
     * 是否已读（0未读 1已读）
     */
    private Integer isRead;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}
