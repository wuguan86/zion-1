package com.zion.admin.controller.monitor;

import com.zion.common.result.Result;
import com.zion.system.mapper.SysMenuMapper;
import com.zion.system.mapper.SysRoleMapper;
import com.zion.system.mapper.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 获取首页统计数据
     */
    @GetMapping("/stats")
    public Result<DashboardStats> stats() {
        DashboardStats stats = new DashboardStats();

        // 用户总数
        stats.setUserCount(userMapper.selectCount(null));
        // 角色数量
        stats.setRoleCount(roleMapper.selectCount(null));
        // 菜单数量
        stats.setMenuCount(menuMapper.selectCount(null));
        // 权限数量（按钮类型的菜单）
        stats.setPermissionCount(menuMapper.selectPermissionCount());

        return Result.ok(stats);
    }

    @Data
    public static class DashboardStats {
        private Long userCount;
        private Long roleCount;
        private Long menuCount;
        private Long permissionCount;
    }
}
