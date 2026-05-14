package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知表
 */
@Data
@TableName("sys_notice")
public class SysNotice implements Serializable {

    /**
     * 通知ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型（1通知 2公告）
     */
    private Integer noticeType;

    /**
     * 推送渠道(JSON): station站内信,email邮件,dingtalk钉钉,feishu飞书,wechat_work企业微信
     */
    private String channels;

    /**
     * 推送对象类型(1指定用户 2按部门 3全部)
     */
    private Integer targetType;

    /**
     * 推送对象ID(JSON): 用户ID或部门ID数组
     */
    private String targetIds;

    /**
     * 状态（0草稿 1发布）
     */
    private Integer status;

    /**
     * 创建者ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建者名称
     */
    private String createName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标识
     */
    @TableLogic
    private Integer deleted;
}
