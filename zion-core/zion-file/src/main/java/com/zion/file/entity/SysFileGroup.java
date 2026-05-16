package com.zion.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分组表
 */
@Data
@TableName("sys_file_group")
public class SysFileGroup implements Serializable {

    /**
     * 分组ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 文件数量（非数据库字段）
     */
    @TableField(exist = false)
    private Integer fileCount;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

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
}
