package com.zion.gen.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 代码生成 - 表信息
 */
@Data
@TableName("gen_table")
public class GenTable implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 表名 */
    private String tableName;

    /** 表描述 */
    private String tableComment;

    /** 实体类名称 */
    private String className;

    /** 包路径 */
    private String packageName;

    /** 模块名 */
    private String moduleName;

    /** 业务名 */
    private String businessName;

    /** 功能名称 */
    private String functionName;

    /** 作者 */
    private String author;

    /** 生成类型（crud-单表操作 tree-树表操作） */
    private String genType;

    /** 生成路径（不填默认项目路径） */
    private String genPath;

    /** 前端模板类型（element-plus / naive-ui） */
    private String frontType;

    /** 表单布局（vertical-从上到下 / grid-一行两列） */
    private String formLayout;

    /** 父菜单ID */
    private Long parentMenuId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 表列信息 */
    @TableField(exist = false)
    private List<GenTableColumn> columns;

    /** 主键列 */
    @TableField(exist = false)
    private GenTableColumn pkColumn;
}
