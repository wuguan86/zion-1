package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色
     */
    PageResult<SysRole> page(Integer page, Integer pageSize, String name, Integer status);

    /**
     * 获取角色详情
     */
    SysRole getDetail(Long id);

    /**
     * 创建角色
     */
    void create(SysRole role, List<Long> menuIds, List<Long> deptIds);

    /**
     * 更新角色
     */
    void update(SysRole role, List<Long> menuIds, List<Long> deptIds);

    /**
     * 删除角色
     */
    void delete(Long id);

    /**
     * 获取所有启用的角色
     */
    List<SysRole> listEnabled();

    /**
     * 根据用户ID获取角色列表
     */
    List<SysRole> listByUserId(Long userId);

    /**
     * 获取角色菜单ID列表
     */
    List<Long> getMenuIds(Long roleId);

    /**
     * 根据角色编码获取角色
     */
    SysRole getByCode(String code);
}
