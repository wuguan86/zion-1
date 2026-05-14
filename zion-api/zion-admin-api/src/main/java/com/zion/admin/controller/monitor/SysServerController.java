package com.zion.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.common.result.Result;
import com.zion.system.entity.SysServer;
import com.zion.system.service.SysServerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务器管理 Controller
 */
@RestController
@RequestMapping("/monitor/server-manager")
@RequiredArgsConstructor
public class SysServerController {

    private final SysServerService serverService;

    /**
     * 分页查询服务器列表
     */
    @GetMapping("/list")
    @SaCheckPermission("monitor:server:list")
    public Result<Page<SysServer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<SysServer> result = serverService.pageList(new Page<>(page, pageSize), name, status);
        return Result.ok(result);
    }

    /**
     * 获取所有启用的服务器（不分页）
     */
    @GetMapping("/all")
    @SaCheckPermission("monitor:server:list")
    public Result<List<SysServer>> all() {
        List<SysServer> list = serverService.lambdaQuery()
                .eq(SysServer::getStatus, 1)
                .orderByAsc(SysServer::getSort)
                .list();
        // 清除敏感信息
        list.forEach(server -> {
            server.setPassword(null);
            server.setPrivateKey(null);
            server.setPassphrase(null);
        });
        return Result.ok(list);
    }

    /**
     * 获取服务器详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("monitor:server:query")
    public Result<SysServer> getById(@PathVariable Long id) {
        SysServer server = serverService.getById(id);
        if (server != null) {
            // 清除敏感信息
            server.setPassword(null);
            server.setPrivateKey(null);
            server.setPassphrase(null);
        }
        return Result.ok(server);
    }

    /**
     * 新增服务器
     */
    @PostMapping
    @SaCheckPermission("monitor:server:add")
    public Result<Void> add(@RequestBody SysServer server) {
        serverService.save(server);
        return Result.ok();
    }

    /**
     * 修改服务器
     */
    @PutMapping
    @SaCheckPermission("monitor:server:edit")
    public Result<Void> update(@RequestBody SysServer server) {
        serverService.updateById(server);
        return Result.ok();
    }

    /**
     * 删除服务器
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("monitor:server:remove")
    public Result<Void> remove(@PathVariable Long id) {
        serverService.removeById(id);
        return Result.ok();
    }

    /**
     * 批量删除服务器
     */
    @DeleteMapping("/batch")
    @SaCheckPermission("monitor:server:remove")
    public Result<Void> batchRemove(@RequestBody List<Long> ids) {
        serverService.removeBatchByIds(ids);
        return Result.ok();
    }

    /**
     * 测试服务器连接
     */
    @PostMapping("/test/{id}")
    @SaCheckPermission("monitor:server:test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        boolean success = serverService.testConnection(id);
        if (success) {
            // 更新最后连接时间
            SysServer server = new SysServer();
            server.setId(id);
            server.setLastConnectTime(LocalDateTime.now());
            serverService.updateById(server);
        }
        return Result.ok(success);
    }

    /**
     * 测试服务器连接（通过参数）
     */
    @PostMapping("/test")
    @SaCheckPermission("monitor:server:test")
    public Result<Boolean> testConnectionByParams(@RequestBody TestConnectionRequest request) {
        boolean success = serverService.testConnection(
                request.getHost(), request.getPort(), request.getUsername(),
                request.getAuthType(), request.getPassword(),
                request.getPrivateKey(), request.getPassphrase());
        return Result.ok(success);
    }

    @Data
    public static class TestConnectionRequest {
        private String host;
        private Integer port;
        private String username;
        private Integer authType;
        private String password;
        private String privateKey;
        private String passphrase;
    }
}
