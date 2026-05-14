package com.zion.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.job.entity.SysJobLog;

import java.util.Map;

/**
 * 定时任务日志服务接口
 */
public interface SysJobLogService extends IService<SysJobLog> {

    /**
     * 分页查询
     */
    PageResult<SysJobLog> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status);

    /**
     * 调度统计（总数、成功、失败、近7日趋势）
     */
    Map<String, Object> statistics();

    /**
     * 清空日志
     */
    void clean();
}
