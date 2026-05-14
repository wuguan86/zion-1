package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群消息实体
 */
@Data
@TableName("sys_chat_group_message")
public class ChatGroupMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 群ID
     */
    private Long groupId;
    
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
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：1-文本 2-图片 3-文件 4-系统消息
     */
    private Integer msgType;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}
