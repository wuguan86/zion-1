package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysOperLog;
import com.zion.system.mapper.SysOperLogMapper;
import com.zion.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> page(Integer page, Integer pageSize, String title, String operName, Integer status) {
        Page<SysOperLog> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), SysOperLog::getTitle, title)
                .like(StringUtils.hasText(operName), SysOperLog::getOperName, operName)
                .eq(status != null, SysOperLog::getStatus, status)
                .orderByDesc(SysOperLog::getOperTime);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    @Async
    public void recordLog(SysOperLog operLog) {
        this.save(operLog);
    }

    @Override
    public void clean() {
        this.remove(new LambdaQueryWrapper<>());
    }
}
