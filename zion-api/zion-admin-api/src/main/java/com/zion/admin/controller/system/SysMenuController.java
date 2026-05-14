package com.zion.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.SysMenu;
import com.zion.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @SaCheckPermission("sys:menu:list")
    public Result<List<SysMenu>> tree(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.ok(menuService.tree(name, status));
    }

    /**
     * 获取所有菜单（用于分配权限）
     */
    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        return Result.ok(menuService.listAll());
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:menu:list")
    public Result<SysMenu> detail(@PathVariable Long id) {
        return Result.ok(menuService.getById(id));
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:menu:add")
    @RepeatSubmit
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysMenu menu) {
        menuService.create(menu);
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:menu:edit")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:menu:delete")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
