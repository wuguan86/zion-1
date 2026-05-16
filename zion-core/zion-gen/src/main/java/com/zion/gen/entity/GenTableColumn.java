package com.zion.gen.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码生成 - 表字段信息
 */
@Data
@TableName("gen_table_column")
public class GenTableColumn implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属表ID */
    private Long tableId;

    /** 列名 */
    private String columnName;

    /** 列描述 */
    private String columnComment;

    /** 列类型 */
    private String columnType;

    /** Java类型 */
    private String javaType;

    /** Java字段名 */
    private String javaField;

    /** 是否主键（1是） */
    private Integer isPk;

    /** 是否自增（1是） */
    private Integer isIncrement;

    /** 是否必填（1是） */
    private Integer isRequired;

    /** 是否为插入字段（1是） */
    private Integer isInsert;

    /** 是否编辑字段（1是） */
    private Integer isEdit;

    /** 是否列表字段（1是） */
    private Integer isList;

    /** 是否查询字段（1是） */
    private Integer isQuery;

    /** 查询方式（EQ等于、NE不等于、GT大于、LT小于、LIKE模糊、BETWEEN范围） */
    private String queryType;

    /** 显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件、image图片上传、upload文件上传、editor富文本控件） */
    private String htmlType;

    /** 字典类型 */
    private String dictType;

    /** 排序 */
    private Integer sort;

    /**
     * 判断是否是字符串类型
     */
    public boolean isStringType() {
        return "String".equals(javaType);
    }

    /**
     * 判断是否是时间类型
     */
    public boolean isTimeType() {
        return "LocalDateTime".equals(javaType) || "LocalDate".equals(javaType) || "Date".equals(javaType);
    }

    /**
     * 判断是否是数字类型
     */
    public boolean isNumberType() {
        return "Integer".equals(javaType) || "Long".equals(javaType) || "Double".equals(javaType) || "BigDecimal".equals(javaType);
    }
}
