package com.zion.job.util;

import com.zion.job.entity.SysJob;
import org.quartz.*;

/**
 * 定时任务工具类
 */
public class ScheduleUtils {

    /**
     * 任务调度参数key
     */
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /**
     * 获取任务的JobKey
     */
    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey("TASK_" + jobId, jobGroup);
    }

    /**
     * 获取任务的TriggerKey
     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey("TASK_" + jobId, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException {
        Class<? extends Job> jobClass = QuartzJobExecution.class;
        // 禁止并发执行
        if (job.getConcurrent() != null && job.getConcurrent() == 1) {
            jobClass = QuartzDisallowConcurrentExecution.class;
        }

        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        
        // 构建job信息
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .build();

        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 构建trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(job.getId(), job.getJobGroup()))
                .withSchedule(cronScheduleBuilder)
                .build();

        // 放入参数，运行时可获取
        jobDetail.getJobDataMap().put(TASK_PROPERTIES, job);

        // 判断是否存在
        if (scheduler.checkExists(jobKey)) {
            // 先删除再创建
            scheduler.deleteJob(jobKey);
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 暂停任务
        if (job.getStatus() != null && job.getStatus() == 0) {
            scheduler.pauseJob(jobKey);
        }
    }

    /**
     * 更新定时任务
     */
    public static void updateScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException {
        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());

        // 判断是否存在
        if (scheduler.checkExists(jobKey)) {
            // 先删除再创建
            scheduler.deleteJob(jobKey);
        }

        createScheduleJob(scheduler, job);
    }

    /**
     * 删除定时任务
     */
    public static void deleteScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException {
        scheduler.deleteJob(getJobKey(job.getId(), job.getJobGroup()));
    }

    /**
     * 暂停任务
     */
    public static void pauseJob(Scheduler scheduler, SysJob job) throws SchedulerException {
        scheduler.pauseJob(getJobKey(job.getId(), job.getJobGroup()));
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(Scheduler scheduler, SysJob job) throws SchedulerException {
        scheduler.resumeJob(getJobKey(job.getId(), job.getJobGroup()));
    }

    /**
     * 立即执行一次
     */
    public static void run(Scheduler scheduler, SysJob job) throws SchedulerException {
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(TASK_PROPERTIES, job);

        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey, dataMap);
        }
    }

    /**
     * 设置定时任务策略
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb) {
        if (job.getMisfirePolicy() == null) {
            return cb;
        }
        switch (job.getMisfirePolicy()) {
            case 1: // 立即执行
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            case 2: // 执行一次
                return cb.withMisfireHandlingInstructionFireAndProceed();
            case 3: // 放弃执行
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                return cb;
        }
    }
}
