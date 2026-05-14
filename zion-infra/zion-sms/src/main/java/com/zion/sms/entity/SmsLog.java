package com.zion.sms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信发送记录
 */
@Data
@TableName("sys_sms_log")
public class SmsLog implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信内容/验证码
     */
    private String content;

    /**
     * 短信类型：verify_code-验证码 notice-通知 marketing-营销
     */
    private String smsType;

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 模板参数（JSON格式）
     */
    private String templateParams;

    /**
     * 服务商：aliyun-阿里云 tencent-腾讯云 console-控制台
     */
    private String provider;

    /**
     * 发送状态：0-发送中 1-成功 2-失败
     */
    private Integer status;

    /**
     * 发送结果消息
     */
    private String resultMsg;

    /**
     * 服务商返回的消息ID
     */
    private String bizId;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 用户ID（如果有关联用户）
     */
    private Long userId;

    /**
     * 业务类型：login-登录 register-注册 reset_password-重置密码 bind_phone-绑定手机
     */
    private String bizType;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
