package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API 访问统计日志
 */
@Data
@TableName("sys_api_access_log")
public class ApiAccessLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 请求开始时间
     */
    private LocalDateTime startTime;

    /**
     * 请求结束时间
     */
    private LocalDateTime endTime;

    /**
     * API 路径
     */
    private String apiPath;

    /**
     * HTTP 方法
     */
    private String method;

    /**
     * HTTP 状态码
     */
    private Integer statusCode;

    /**
     * 是否成功(0否 1是)
     */
    private Integer success;

    /**
     * 耗时(毫秒)
     */
    private Long costTime;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 用户 ID(未登录为空)
     */
    private Long userId;
}
