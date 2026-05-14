package com.zion.admin.controller.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.file.entity.SysFile;
import com.zion.file.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理
 */
@RestController
@RequestMapping("/sys/file")
@RequiredArgsConstructor
public class SysFileController {

    private final SysFileService fileService;

    /**
     * 分页查询文件列表
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:file:list")
    public Result<PageResult<SysFile>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String originalName,
            @RequestParam(required = false) String fileType) {
        var result = fileService.page(page, pageSize, originalName, fileType);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 分页查询文件列表（支持分组和类型过滤）
     */
    @GetMapping("/page-by-group")
    @SaCheckPermission("sys:file:list")
    public Result<PageResult<SysFile>> pageByGroup(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String originalName) {
        var result = fileService.pageByGroup(page, pageSize, groupId, fileCategory, originalName);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:file:list")
    public Result<SysFile> detail(@PathVariable Long id) {
        return Result.ok(fileService.getById(id));
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @SaCheckPermission("sys:file:upload")
    @Log(title = "上传文件", businessType = BusinessType.INSERT)
    public Result<SysFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Long groupId) {
        return Result.ok(fileService.upload(file, path, groupId));
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    @SaCheckPermission("sys:file:upload")
    @Log(title = "上传图片", businessType = BusinessType.INSERT)
    public Result<SysFile> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.uploadImage(file));
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        SysFile sysFile = fileService.getById(id);
        if (sysFile == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = fileService.getFileBytes(id);

        String encodedName = URLEncoder.encode(sysFile.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CONTENT_TYPE, sysFile.getFileType())
                .body(bytes);
    }

    /**
     * 预览文件
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        SysFile sysFile = fileService.getById(id);
        if (sysFile == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = fileService.getFileBytes(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, sysFile.getFileType())
                .body(bytes);
    }

    /**
     * 获取文本文件内容
     */
    @GetMapping("/text/{id}")
    @SaCheckPermission("sys:file:list")
    public Result<String> getTextContent(@PathVariable Long id) {
        SysFile sysFile = fileService.getById(id);
        if (sysFile == null) {
            return Result.fail("文件不存在");
        }

        // 限制文件大小，避免内存溢出（最大 5MB）
        if (sysFile.getFileSize() > 5 * 1024 * 1024) {
            return Result.fail("文件过大，无法预览");
        }

        byte[] bytes = fileService.getFileBytes(id);
        String content = new String(bytes, StandardCharsets.UTF_8);
        return Result.ok(content);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    @Log(title = "删除文件", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok();
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/batch")
    @SaCheckPermission("sys:file:delete")
    @Log(title = "批量删除文件", businessType = BusinessType.DELETE)
    public Result<Void> deleteBatch(@RequestBody Long[] ids) {
        fileService.deleteBatch(ids);
        return Result.ok();
    }

    /**
     * 移动文件到分组
     */
    @PostMapping("/move")
    @SaCheckPermission("sys:file:upload")
    @Log(title = "移动文件", businessType = BusinessType.UPDATE)
    public Result<Void> moveToGroup(@RequestBody MoveFileRequest request) {
        fileService.moveToGroup(request.getFileIds(), request.getGroupId());
        return Result.ok();
    }

    /**
     * 重命名文件
     */
    @PutMapping("/{id}/rename")
    @SaCheckPermission("sys:file:upload")
    @Log(title = "重命名文件", businessType = BusinessType.UPDATE)
    public Result<Void> rename(@PathVariable Long id, @RequestBody RenameRequest request) {
        fileService.rename(id, request.getNewName());
        return Result.ok();
    }

    /**
     * 移动文件请求
     */
    @lombok.Data
    public static class MoveFileRequest {
        private Long[] fileIds;
        private Long groupId;
    }

    /**
     * 重命名请求
     */
    @lombok.Data
    public static class RenameRequest {
        private String newName;
    }
}
