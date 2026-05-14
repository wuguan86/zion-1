package com.zion.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.entity.SysLoginLog;
import com.zion.system.service.SysLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志控制器
 */
@RestController
@RequestMapping("/monitor/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @SaCheckPermission("monitor:loginlog:list")
    public Result<PageResult<SysLoginLog>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.ok(loginLogService.page(page, pageSize, username, status));
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("monitor:loginlog:delete")
    public Result<Void> delete(@PathVariable Long id) {
        loginLogService.removeById(id);
        return Result.ok();
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/clean")
    @SaCheckPermission("monitor:loginlog:delete")
    public Result<Void> clean() {
        loginLogService.clean();
        return Result.ok();
    }
}
