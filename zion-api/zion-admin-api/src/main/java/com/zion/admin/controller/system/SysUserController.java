package com.zion.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.crypto.EncryptResponse;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.system.entity.SysRole;
import com.zion.system.entity.SysUser;
import com.zion.common.exception.BusinessException;
import com.zion.system.excel.SysUserExcel;
import com.zion.system.service.SysRoleService;
import com.zion.system.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SysRoleService roleService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:user:list")
    @EncryptResponse
    public Result<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long postId) {
        return Result.ok(userService.page(page, pageSize, username, status, userType, deptId, postId));
    }

    /**
     * 获取详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:user:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = userService.getDetail(id);
        List<SysRole> roles = roleService.listByUserId(id);
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        // 获取岗位关联
        List<Long> postIds = userService.getPostIds(id);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roleIds", roleIds);
        result.put("postIds", postIds);
        return Result.ok(result);
    }

    /**
     * 创建
     */
    @PostMapping
    @SaCheckPermission("sys:user:add")
    @RepeatSubmit
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody UserRequest request) {
        userService.create(request.getUser(), request.getRoleIds(), request.getPostIds());
        return Result.ok();
    }

    /**
     * 更新
     */
    @PutMapping
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody UserRequest request) {
        userService.update(request.getUser(), request.getRoleIds(), request.getPostIds());
        return Result.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:user:delete")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    @SaCheckPermission("sys:user:delete")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的用户");
        }
        userService.deleteBatch(ids);
        return Result.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    /**
     * 切换离职状态
     */
    @PostMapping("/{id}/quit")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> toggleQuit(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setIsQuit(user.getIsQuit() == 1 ? 0 : 1);
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 审核通过
     */
    @PostMapping("/{id}/approve")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> approve(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != 2) {
            throw new BusinessException("该用户不在待审核状态");
        }
        user.setStatus(1);
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 审核拒绝
     */
    @PostMapping("/{id}/reject")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    public Result<Void> reject(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != 2) {
            throw new BusinessException("该用户不在待审核状态");
        }
        user.setStatus(3);
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 导出用户
     */
    @GetMapping("/export")
    @SaCheckPermission("sys:user:export")
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String ids) throws IOException {
        // 解析逗号分隔的ID列表
        List<Long> idList = null;
        if (ids != null && !ids.isEmpty()) {
            idList = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        List<SysUserExcel> list = userService.exportUsers(username, status, userType, deptId, idList);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SysUserExcel.class).sheet("用户数据").doWrite(list);
    }

    /**
     * 导入用户
     */
    @PostMapping("/import")
    @SaCheckPermission("sys:user:import")
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    public Result<Map<String, Object>> importData(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }
        Map<String, Object> result = userService.importUsers(file);
        return Result.ok(result);
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        // 写入空数据作为模板
        EasyExcel.write(response.getOutputStream(), SysUserExcel.class).sheet("用户数据").doWrite(new ArrayList<>());
    }

    @Data
    public static class UserRequest {
        private SysUser user;
        private List<Long> roleIds;
        private List<Long> postIds;
    }
}
