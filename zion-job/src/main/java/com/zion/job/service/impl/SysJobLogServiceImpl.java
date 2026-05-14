package com.zion.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.result.PageResult;
import com.zion.job.entity.SysJobLog;
import com.zion.job.mapper.SysJobLogMapper;
import com.zion.job.service.SysJobLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements SysJobLogService {

    @Override
    public PageResult<SysJobLog> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJobLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(jobName), SysJobLog::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJobLog::getJobGroup, jobGroup)
                .eq(status != null, SysJobLog::getStatus, status)
                .orderByDesc(SysJobLog::getStartTime);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        long total = this.count();
        long successCount = this.count(new LambdaQueryWrapper<SysJobLog>().eq(SysJobLog::getStatus, 0));
        long failCount = this.count(new LambdaQueryWrapper<SysJobLog>().eq(SysJobLog::getStatus, 1));
        List<Map<String, Object>> dailyStats = baseMapper.selectDailyStats();
        result.put("totalCount", total);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("dailyStats", dailyStats);
        return result;
    }

    @Override
    public void clean() {
        this.remove(new LambdaQueryWrapper<>());
    }
}
