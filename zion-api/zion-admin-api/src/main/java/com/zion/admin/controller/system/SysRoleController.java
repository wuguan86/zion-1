package com.zion.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.SysRole;
import com.zion.system.mapper.SysRoleDeptMapper;
import com.zion.system.service.SysRoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final SysRoleDeptMapper roleDeptMapper;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:role:list")
    public Result<PageResult<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.page(page, pageSize, name, status));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:role:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysRole role = roleService.getDetail(id);
        List<Long> menuIds = roleService.getMenuIds(id);
        List<Long> deptIds = roleDeptMapper.selectDeptIdsByRoleId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("role", role);
        result.put("menuIds", menuIds);
        result.put("deptIds", deptIds);
        return Result.ok(result);
    }

    /**
     * 获取所有启用的角色
     */
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.listEnabled());
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:role:add")
    @RepeatSubmit
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody RoleRequest request) {
        roleService.create(request.getRole(), request.getMenuIds(), request.getDeptIds());
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody RoleRequest request) {
        roleService.update(request.getRole(), request.getMenuIds(), request.getDeptIds());
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:role:delete")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Data
    public static class RoleRequest {
        private SysRole role;
        private List<Long> menuIds;
        private List<Long> deptIds;
    }
}
