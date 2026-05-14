package com.zion.admin.controller.gen;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.gen.entity.DatabaseTable;
import com.zion.gen.entity.GenTable;
import com.zion.gen.service.GenTableService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 代码生成
 */
@RestController
@RequestMapping("/tool/gen")
@RequiredArgsConstructor
public class GenController {

    private final GenTableService genTableService;

    /**
     * 分页查询数据库中可导入的表列表
     */
    @GetMapping("/db/list")
    @SaCheckPermission("tool:gen:list")
    public Result<PageResult<DatabaseTable>> dbTableList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tableName) {
        var result = genTableService.selectDbTableList(page, pageSize, tableName);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 导入表结构
     */
    @PostMapping("/import")
    @SaCheckPermission("tool:gen:import")
    @RepeatSubmit
    @Log(title = "导入表结构", businessType = BusinessType.INSERT)
    public Result<Void> importTable(@RequestBody String[] tableNames) {
        genTableService.importTable(tableNames);
        return Result.ok();
    }

    /**
     * 分页查询已导入的表
     */
    @GetMapping("/page")
    @SaCheckPermission("tool:gen:list")
    public Result<PageResult<GenTable>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tableName) {
        var result = genTableService.page(page, pageSize, tableName);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取表详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("tool:gen:query")
    public Result<GenTable> getInfo(@PathVariable Long id) {
        return Result.ok(genTableService.getTableById(id));
    }

    /**
     * 修改表配置
     */
    @PutMapping
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "修改代码生成配置", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody GenTable table) {
        genTableService.updateTable(table);
        return Result.ok();
    }

    /**
     * 删除表
     */
    @DeleteMapping("/{ids}")
    @SaCheckPermission("tool:gen:remove")
    @Log(title = "删除代码生成表", businessType = BusinessType.DELETE)
    public Result<Void> remove(@PathVariable Long[] ids) {
        genTableService.deleteTable(ids);
        return Result.ok();
    }

    /**
     * 预览代码
     */
    @GetMapping("/preview/{id}")
    @SaCheckPermission("tool:gen:preview")
    public Result<Map<String, String>> preview(@PathVariable Long id) {
        return Result.ok(genTableService.previewCode(id));
    }

    /**
     * 生成代码（下载 zip）
     */
    @GetMapping("/download")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "生成代码", businessType = BusinessType.EXPORT)
    public void download(@RequestParam Long[] ids, HttpServletResponse response) throws IOException {
        byte[] data = genTableService.generateCode(ids);
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=code.zip");
        response.setContentType("application/octet-stream");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    /**
     * 预览将要生成的文件列表
     */
    @GetMapping("/preview-generate/{id}")
    @SaCheckPermission("tool:gen:code")
    public Result<List<String>> previewGenerateFiles(@PathVariable Long id) {
        List<String> files = genTableService.previewGenerateFiles(id);
        return Result.ok(files);
    }

    /**
     * 生成代码到项目
     */
    @PostMapping("/generate/{id}")
    @SaCheckPermission("tool:gen:code")
    @RepeatSubmit
    @Log(title = "生成代码到项目", businessType = BusinessType.INSERT)
    public Result<List<String>> generateToProject(@PathVariable Long id) {
        List<String> files = genTableService.generateToProject(id);
        return Result.ok(files);
    }

    /**
     * 预览将要移除的文件列表
     */
    @GetMapping("/preview-remove/{id}")
    @SaCheckPermission("tool:gen:code")
    public Result<List<String>> previewRemoveFiles(@PathVariable Long id) {
        List<String> files = genTableService.previewRemoveFiles(id);
        return Result.ok(files);
    }

    /**
     * 移除已生成的代码
     */
    @DeleteMapping("/remove-code/{id}")
    @SaCheckPermission("tool:gen:code")
    @Log(title = "移除已生成代码", businessType = BusinessType.DELETE)
    public Result<List<String>> removeGeneratedCode(@PathVariable Long id) {
        List<String> files = genTableService.removeGeneratedCode(id);
        return Result.ok(files);
    }

    /**
     * 同步表结构
     */
    @PostMapping("/sync/{id}")
    @SaCheckPermission("tool:gen:edit")
    @Log(title = "同步表结构", businessType = BusinessType.UPDATE)
    public Result<Void> sync(@PathVariable Long id) {
        genTableService.syncTable(id);
        return Result.ok();
    }
}
