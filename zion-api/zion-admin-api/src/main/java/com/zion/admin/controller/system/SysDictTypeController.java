package com.zion.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.SysDictType;
import com.zion.system.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类型控制器
 */
@RestController
@RequestMapping("/sys/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final SysDictTypeService dictTypeService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:dict:list")
    public Result<PageResult<SysDictType>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) Integer status) {
        return Result.ok(dictTypeService.page(page, pageSize, dictName, dictType, status));
    }

    /**
     * 获取所有字典类型
     */
    @GetMapping("/list")
    public Result<List<SysDictType>> list() {
        return Result.ok(dictTypeService.listAll());
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:dict:list")
    public Result<SysDictType> detail(@PathVariable Long id) {
        return Result.ok(dictTypeService.getById(id));
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:dict:add")
    @RepeatSubmit
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysDictType dictType) {
        dictTypeService.create(dictType);
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:dict:edit")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysDictType dictType) {
        dictTypeService.update(dictType);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:dict:delete")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        dictTypeService.delete(id);
        return Result.ok();
    }
}
