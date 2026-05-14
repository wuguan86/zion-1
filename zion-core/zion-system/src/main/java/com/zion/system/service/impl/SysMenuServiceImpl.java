package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.system.entity.SysMenu;
import com.zion.system.mapper.SysMenuMapper;
import com.zion.system.service.SysMenuService;
import com.zion.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Lazy
    private final SysUserService userService;

    @Override
    public List<SysMenu> tree(String name, Integer status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysMenu::getName, name)
                .eq(status != null, SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getSort);
        List<SysMenu> menus = this.list(wrapper);
        return buildTree(menus);
    }

    @Override
    public List<SysMenu> getUserMenuTree(Long userId) {
        // 超级管理员(admin)直接返回全量启用菜单，避免因关联表/层级问题导致菜单缺失
        List<String> roleCodes = userService.getRoleCodes(userId);
        if (roleCodes != null && roleCodes.contains("admin")) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus, 1)
                    .eq(SysMenu::getDeleted, 0)
                    .ne(SysMenu::getType, 3)
                    .orderByAsc(SysMenu::getSort);
            return buildTree(this.list(wrapper));
        }

        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        // 只返回目录和菜单类型，不返回按钮
        menus = menus.stream()
                .filter(menu -> menu.getType() != 3)
                .collect(Collectors.toList());

        // 自动补充缺失的父级菜单，确保树结构完整
        menus = fillParentMenus(menus);

        return buildTree(menus);
    }
    
    /**
     * 补充缺失的父级菜单
     */
    private List<SysMenu> fillParentMenus(List<SysMenu> menus) {
        // 获取已有菜单的ID集合
        java.util.Set<Long> existIds = menus.stream()
                .map(SysMenu::getId)
                .collect(Collectors.toSet());
        
        // 收集需要补充的父级ID
        java.util.Set<Long> parentIds = new java.util.HashSet<>();
        for (SysMenu menu : menus) {
            Long parentId = menu.getParentId();
            while (parentId != null && parentId != 0 && !existIds.contains(parentId)) {
                parentIds.add(parentId);
                // 继续查找上级
                SysMenu parent = this.getById(parentId);
                if (parent != null) {
                    existIds.add(parentId); // 防止重复添加
                    parentId = parent.getParentId();
                } else {
                    break;
                }
            }
        }
        
        // 查询并添加缺失的父级菜单
        if (!parentIds.isEmpty()) {
            List<SysMenu> parentMenus = this.listByIds(parentIds);
            menus.addAll(parentMenus);
        }
        
        return menus;
    }

    @Override
    public void create(SysMenu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        this.save(menu);
    }

    @Override
    public void update(SysMenu menu) {
        SysMenu existMenu = this.getById(menu.getId());
        if (existMenu == null) {
            throw new BusinessException("菜单不存在");
        }
        if (menu.getParentId() != null && menu.getParentId().equals(menu.getId())) {
            throw new BusinessException("上级菜单不能选择自己");
        }
        this.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        // 检查是否有子菜单
        long count = this.count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        this.removeById(id);
    }

    @Override
    public List<SysMenu> listAll() {
        return this.list(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                menu.setChildren(getChildren(menu.getId(), menus));
                tree.add(menu);
            }
        }
        return tree;
    }

    /**
     * 递归获取子菜单
     */
    private List<SysMenu> getChildren(Long parentId, List<SysMenu> menus) {
        List<SysMenu> children = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(getChildren(menu.getId(), menus));
                children.add(menu);
            }
        }
        return children.isEmpty() ? null : children;
    }
}
