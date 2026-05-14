package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.system.config.StpInterfaceImpl;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysRoleMenu;
import com.zion.system.mapper.SysMenuMapper;
import com.zion.system.mapper.SysRoleMapper;
import com.zion.system.mapper.SysRoleMenuMapper;
import com.zion.system.entity.SysRoleDept;
import com.zion.system.mapper.SysRoleDeptMapper;
import com.zion.system.mapper.SysUserRoleMapper;
import com.zion.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    @Override
    public PageResult<SysRole> page(Integer page, Integer pageSize, String name, Integer status) {
        Page<SysRole> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), SysRole::getName, name)
                .eq(status != null, SysRole::getStatus, status)
                .orderByAsc(SysRole::getSort);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public SysRole getDetail(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysRole role, List<Long> menuIds, List<Long> deptIds) {
        // 检查角色编码是否存在
        if (this.getOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.getCode())) != null) {
            throw new BusinessException("角色编码已存在");
        }
        this.save(role);
        // 保存角色菜单关联
        saveRoleMenus(role.getId(), menuIds);
        // 保存角色部门关联
        if (Integer.valueOf(2).equals(role.getDataScope())) {
            saveRoleDepts(role.getId(), deptIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role, List<Long> menuIds, List<Long> deptIds) {
        SysRole existRole = this.getById(role.getId());
        if (existRole == null) {
            throw new BusinessException("角色不存在");
        }
        // 检查角色编码是否存在
        SysRole byCode = this.getOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, role.getCode()));
        if (byCode != null && !byCode.getId().equals(role.getId())) {
            throw new BusinessException("角色编码已存在");
        }
        this.updateById(role);
        // 更新角色菜单关联
        roleMenuMapper.deleteByRoleId(role.getId());
        saveRoleMenus(role.getId(), menuIds);
        // 更新角色部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getId()));
        if (Integer.valueOf(2).equals(role.getDataScope())) {
            saveRoleDepts(role.getId(), deptIds);
        }
        // 角色权限变更，清除该角色下所有用户的权限缓存
        clearCacheByRoleId(role.getId());
    }

    private void saveRoleDepts(Long roleId, List<Long> deptIds) {
        if (deptIds != null && !deptIds.isEmpty()) {
            roleDeptMapper.insertBatch(roleId, deptIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 先清除缓存（删除关联前查询受影响用户）
        clearCacheByRoleId(id);
        this.removeById(id);
        roleMenuMapper.deleteByRoleId(id);
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
    }

    @Override
    public List<SysRole> listEnabled() {
        return this.list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    @Override
    public List<SysRole> listByUserId(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<Long> getMenuIds(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public SysRole getByCode(String code) {
        return this.getOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
    }

    /**
     * 清除指定角色下所有用户的权限缓存
     */
    private void clearCacheByRoleId(Long roleId) {
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(roleId);
        if (userIds != null) {
            userIds.forEach(StpInterfaceImpl::clearPermissionCache);
        }
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
    }
}
