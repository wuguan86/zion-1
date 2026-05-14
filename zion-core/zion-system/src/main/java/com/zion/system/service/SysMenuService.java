package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.system.entity.SysMenu;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 获取菜单树
     */
    List<SysMenu> tree(String name, Integer status);

    /**
     * 获取用户菜单树
     */
    List<SysMenu> getUserMenuTree(Long userId);

    /**
     * 创建菜单
     */
    void create(SysMenu menu);

    /**
     * 更新菜单
     */
    void update(SysMenu menu);

    /**
     * 删除菜单
     */
    void delete(Long id);

    /**
     * 获取所有菜单列表（用于分配权限）
     */
    List<SysMenu> listAll();
}
