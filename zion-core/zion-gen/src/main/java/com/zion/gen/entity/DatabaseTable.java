package com.zion.gen.entity;

import lombok.Data;

/**
 * 数据库表信息（用于展示数据库中的表）
 */
@Data
public class DatabaseTable {

    /** 表名 */
    private String tableName;

    /** 表描述 */
    private String tableComment;

    /** 创建时间 */
    private String createTime;

    /** 更新时间 */
    private String updateTime;
}
