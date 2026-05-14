package com.zion.admin.controller.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.file.entity.SysFileGroup;
import com.zion.file.service.SysFileGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件分组管理
 */
@RestController
@RequestMapping("/sys/file-group")
@RequiredArgsConstructor
public class SysFileGroupController {

    private final SysFileGroupService fileGroupService;

    /**
     * 查询分组列表（包含文件数量）
     */
    @GetMapping("/list")
    @SaCheckPermission("sys:file:list")
    public Result<Map<String, Object>> list() {
        List<SysFileGroup> groups = fileGroupService.listWithFileCount();
        Integer ungroupedCount = fileGroupService.getUngroupedFileCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("groups", groups);
        result.put("ungroupedCount", ungroupedCount);
        return Result.ok(result);
    }

    /**
     * 获取分组详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:file:list")
    public Result<SysFileGroup> detail(@PathVariable Long id) {
        return Result.ok(fileGroupService.getById(id));
    }

    /**
     * 创建分组
     */
    @PostMapping
    @SaCheckPermission("sys:file:upload")
    @RepeatSubmit
    @Log(title = "创建文件分组", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody SysFileGroup group) {
        fileGroupService.create(group);
        return Result.ok();
    }

    /**
     * 更新分组
     */
    @PutMapping
    @SaCheckPermission("sys:file:upload")
    @Log(title = "更新文件分组", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody SysFileGroup group) {
        fileGroupService.update(group);
        return Result.ok();
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    @Log(title = "删除文件分组", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        fileGroupService.delete(id);
        return Result.ok();
    }
}
