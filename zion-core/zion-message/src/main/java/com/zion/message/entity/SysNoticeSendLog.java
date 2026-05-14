package com.zion.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知推送记录表
 */
@Data
@TableName("sys_notice_send_log")
public class SysNoticeSendLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知ID */
    private Long noticeId;

    /** 推送渠道: station,email,dingtalk,feishu,wechat_work */
    private String channel;

    /** 状态: 1成功 2失败 */
    private Integer status;

    /** 推送目标数量 */
    private Integer targetCount;

    /** 成功数量(邮件/站内信) */
    private Integer successCount;

    /** 失败原因 */
    private String errorMsg;

    /** 推送时间 */
    private LocalDateTime sendTime;

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 2;
}
