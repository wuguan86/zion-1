package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务器管理实体
 */
@Data
@TableName("sys_server")
public class SysServer {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 服务器名称
     */
    private String name;
    
    /**
     * 服务器地址
     */
    private String host;
    
    /**
     * SSH端口
     */
    private Integer port;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 认证方式：1-密码 2-密钥
     */
    private Integer authType;
    
    /**
     * 密码（加密存储）
     */
    private String password;
    
    /**
     * 私钥内容
     */
    private String privateKey;
    
    /**
     * 私钥密码（加密存储）
     */
    private String passphrase;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 最后连接时间
     */
    private LocalDateTime lastConnectTime;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
