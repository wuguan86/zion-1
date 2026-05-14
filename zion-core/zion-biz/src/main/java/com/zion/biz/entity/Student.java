package com.zion.system.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.io.Serializable;

/**
 * 学生表
 * 
 * @author Zion
 * @date 2026-03-01
 */
@Data
@TableName("student")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    /** id */
    @ExcelProperty(value = "id", index = 0)
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号 */
    @ExcelProperty(value = "学号", index = 1)
    private String studentNo;

    /** 姓名 */
    @ExcelProperty(value = "姓名", index = 2)
    private String name;

    /** 性别 */
    @ExcelProperty(value = "性别", index = 3)
    private Integer gender;

    /** 出生日期 */
    @ExcelProperty(value = "出生日期", index = 4)
    private LocalDate birthday;

    /** 手机号 */
    @ExcelProperty(value = "手机号", index = 5)
    private String phone;

    /** 邮箱 */
    @ExcelProperty(value = "邮箱", index = 6)
    private String email;

    /** 地址 */
    @ExcelProperty(value = "地址", index = 7)
    private String address;

    /** 班级ID */
    @ExcelProperty(value = "班级ID", index = 8)
    private Long classId;

    /** 状态 */
    @ExcelProperty(value = "状态", index = 9)
    private Integer status;

    /** deleted */
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
