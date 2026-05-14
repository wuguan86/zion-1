package com.zion.system.excel;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.List;

/**
 * 用户导入导出 Excel DTO
 */
@Data
@ExcelIgnoreUnannotated
public class SysUserExcel {

    @ExcelProperty("用户名")
    @ColumnWidth(15)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(15)
    private String nickname;

    @ExcelProperty("部门名称")
    @ColumnWidth(15)
    private String deptName;

    @ExcelProperty("邮箱")
    @ColumnWidth(25)
    private String email;

    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty("性别")
    @ColumnWidth(10)
    private String genderStr;

    @ExcelProperty("用户类型")
    @ColumnWidth(15)
    private String userTypeStr;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusStr;

    @ExcelProperty("角色")
    @ColumnWidth(30)
    private String roleNames;

    @ExcelProperty("岗位")
    @ColumnWidth(30)
    private String postNames;

    // ============ 导入时使用的字段（非 Excel 列） ============

    /**
     * 性别(0-未知 1-男 2-女)
     */
    private Integer gender;

    /**
     * 用户类型(admin-后台管理员 pc-PC前台用户 app-App/小程序用户)
     */
    private String userType;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;

    /**
     * 部门ID（导入时根据部门名称查找）
     */
    private Long deptId;

    /**
     * 密码（导入时可选，为空则使用默认密码）
     */
    private String password;

    /**
     * 角色ID列表（导入时根据角色名称查找）
     */
    private List<Long> roleIds;

    /**
     * 岗位ID列表（导入时根据岗位名称查找）
     */
    private List<Long> postIds;
}
