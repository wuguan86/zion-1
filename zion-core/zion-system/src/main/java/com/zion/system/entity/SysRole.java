package com.zion.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zion.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;

    /**
     * 数据范围(1全部 2自定义 3本部门 4本部门及以下 5仅本人)
     */
    private Integer dataScope;

    /**
     * 备注
     */
    private String remark;
}
