package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置分组
 */
@Data
@TableName("sys_config_group")
public class SysConfigGroup {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 分组编码 */
    private String groupCode;
    
    /** 分组名称 */
    private String groupName;
    
    /** 分组图标 */
    private String groupIcon;
    
    /** 配置值(JSON格式) */
    private String configValue;
    
    /** 排序 */
    private Integer sort;
    
    /** 状态(0-禁用 1-启用) */
    private Integer status;
    
    /** 备注 */
    private String remark;
    
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
