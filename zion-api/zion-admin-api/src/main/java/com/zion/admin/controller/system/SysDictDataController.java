package com.zion.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.SysDictData;
import com.zion.system.service.SysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据控制器
 */
@RestController
@RequestMapping("/sys/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService dictDataService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:dict:list")
    public Result<PageResult<SysDictData>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) Integer status) {
        return Result.ok(dictDataService.page(page, pageSize, dictType, dictLabel, status));
    }

    /**
     * 根据字典类型查询字典数据
     */
    @GetMapping("/type/{dictType}")
    public Result<List<SysDictData>> listByType(@PathVariable String dictType) {
        return Result.ok(dictDataService.listByDictType(dictType));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:dict:list")
    public Result<SysDictData> detail(@PathVariable Long id) {
        return Result.ok(dictDataService.getById(id));
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:dict:add")
    @RepeatSubmit
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysDictData dictData) {
        dictDataService.create(dictData);
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:dict:edit")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysDictData dictData) {
        dictDataService.update(dictData);
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:dict:delete")
    @Log(title = "字典数据", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        dictDataService.delete(id);
        return Result.ok();
    }
}
