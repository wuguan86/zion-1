package com.zion.gen.entity;

import lombok.Data;

/**
 * 数据库列信息（用于读取数据库中的列）
 */
@Data
public class DatabaseColumn {

    /** 列名 */
    private String columnName;

    /** 列描述 */
    private String columnComment;

    /** 列类型 */
    private String dataType;

    /** 列完整类型 */
    private String columnType;

    /** 是否可为空 */
    private String isNullable;

    /** 列键类型（PRI主键） */
    private String columnKey;

    /** 额外信息（auto_increment自增） */
    private String extra;

    /** 排序 */
    private Integer ordinalPosition;
}
