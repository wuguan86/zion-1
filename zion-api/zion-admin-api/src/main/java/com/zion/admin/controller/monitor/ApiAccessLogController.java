package com.zion.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.entity.ApiAccessLog;
import com.zion.system.service.ApiAccessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 访问统计控制器
 */
@RestController
@RequestMapping("/monitor/api-access")
@RequiredArgsConstructor
public class ApiAccessLogController {

    private final ApiAccessLogService apiAccessLogService;

    /**
     * 分页查询 API 访问日志
     */
    @GetMapping("/page")
    @SaCheckPermission("monitor:apiAccess:list")
    public Result<PageResult<ApiAccessLog>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(apiAccessLogService.page(page, pageSize, apiPath, method, success, startTime, endTime));
    }

    /**
     * 获取统计数据（用于 ECharts 可视化）
     */
    @GetMapping("/statistics")
    @SaCheckPermission("monitor:apiAccess:list")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        return Result.ok(apiAccessLogService.getStatistics(startDate, endDate));
    }
}
