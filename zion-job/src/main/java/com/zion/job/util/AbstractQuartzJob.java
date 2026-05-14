package com.zion.job.util;

import com.zion.job.entity.SysJob;
import com.zion.job.entity.SysJobLog;
import com.zion.job.service.SysJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * 抽象定时任务
 */
@Slf4j
public abstract class AbstractQuartzJob implements Job {

    /**
     * 线程本地变量
     */
    private static final ThreadLocal<LocalDateTime> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SysJob job = (SysJob) context.getMergedJobDataMap().get(ScheduleUtils.TASK_PROPERTIES);
        
        SysJobLog jobLog = new SysJobLog();
        // 手动复制需要的属性，避免 BeanUtils 问题
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(LocalDateTime.now());
        
        try {
            // 执行前
            before(context, job);
            // 执行
            doExecute(context, job);
            // 执行后
            after(context, job, jobLog, null);
        } catch (Exception e) {
            log.error("任务执行异常：", e);
            after(context, job, jobLog, e);
        }
    }

    /**
     * 执行前
     */
    protected void before(JobExecutionContext context, SysJob job) {
        threadLocal.set(LocalDateTime.now());
    }

    /**
     * 执行后
     */
    protected void after(JobExecutionContext context, SysJob job, SysJobLog jobLog, Exception e) {
        jobLog.setStopTime(LocalDateTime.now());
        
        if (e != null) {
            jobLog.setStatus(1);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String errorMsg = sw.toString();
            jobLog.setExceptionInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
            jobLog.setJobMessage("执行失败");
        } else {
            jobLog.setStatus(0);
            jobLog.setJobMessage("执行成功");
        }
        
        // 保存日志
        try {
            SysJobLogService jobLogService = SpringUtils.getBean(SysJobLogService.class);
            jobLogService.save(jobLog);
        } catch (Exception ex) {
            log.error("保存任务日志失败：", ex);
        }
        
        threadLocal.remove();
    }

    /**
     * 执行方法，由子类重写
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob job) throws Exception;
}
