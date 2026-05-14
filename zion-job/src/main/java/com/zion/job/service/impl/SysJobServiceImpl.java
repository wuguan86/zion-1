package com.zion.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.common.exception.BusinessException;
import com.zion.common.result.PageResult;
import com.zion.job.entity.SysJob;
import com.zion.job.mapper.SysJobMapper;
import com.zion.job.service.SysJobService;
import com.zion.job.util.ScheduleUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 定时任务服务实现
 */
@Service
@RequiredArgsConstructor
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements SysJobService {

    private final Scheduler scheduler;

    /**
     * 项目启动时，初始化定时任务
     */
    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.clear();
        List<SysJob> jobList = this.list();
        for (SysJob job : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    @Override
    public PageResult<SysJob> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<SysJob> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(jobName), SysJob::getJobName, jobName)
                .eq(StringUtils.hasText(jobGroup), SysJob::getJobGroup, jobGroup)
                .eq(status != null, SysJob::getStatus, status)
                .orderByDesc(SysJob::getCreateTime);
        return PageResult.of(this.page(pageParam, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysJob job) {
        job.setStatus(0); // 默认暂停
        this.save(job);
        try {
            ScheduleUtils.createScheduleJob(scheduler, job);
        } catch (SchedulerException e) {
            throw new BusinessException("创建定时任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysJob job) {
        SysJob existJob = this.getById(job.getId());
        if (existJob == null) {
            throw new BusinessException("任务不存在");
        }
        this.updateById(job);
        try {
            ScheduleUtils.updateScheduleJob(scheduler, job);
        } catch (SchedulerException e) {
            throw new BusinessException("更新定时任务失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysJob job = this.getById(id);
        if (job != null) {
            this.removeById(id);
            try {
                ScheduleUtils.deleteScheduleJob(scheduler, job);
            } catch (SchedulerException e) {
                throw new BusinessException("删除定时任务失败：" + e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        SysJob job = this.getById(id);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        job.setStatus(status);
        this.updateById(job);
        try {
            if (status == 1) {
                ScheduleUtils.resumeJob(scheduler, job);
            } else {
                ScheduleUtils.pauseJob(scheduler, job);
            }
        } catch (SchedulerException e) {
            throw new BusinessException("修改任务状态失败：" + e.getMessage());
        }
    }

    @Override
    public void run(Long id) {
        SysJob job = this.getById(id);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        try {
            ScheduleUtils.run(scheduler, job);
        } catch (SchedulerException e) {
            throw new BusinessException("执行任务失败：" + e.getMessage());
        }
    }
}
