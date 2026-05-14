package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.system.entity.SysDept;

import java.util.List;

/**
 * 部门服务接口
 */
public interface SysDeptService extends IService<SysDept> {

    /**
     * 查询部门树
     */
    List<SysDept> tree(String deptName, Integer status);

    /**
     * 获取部门列表
     */
    List<SysDept> listAll();

    /**
     * 创建部门
     */
    void create(SysDept dept);

    /**
     * 更新部门
     */
    void update(SysDept dept);

    /**
     * 删除部门
     */
    void delete(Long id);

    /**
     * 移动部门
     */
    void move(Long id, Long parentId, Integer sort);
}
