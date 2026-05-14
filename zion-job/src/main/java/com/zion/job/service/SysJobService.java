package com.zion.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.job.entity.SysJob;

/**
 * 定时任务服务接口
 */
public interface SysJobService extends IService<SysJob> {

    /**
     * 分页查询
     */
    PageResult<SysJob> page(Integer page, Integer pageSize, String jobName, String jobGroup, Integer status);

    /**
     * 创建任务
     */
    void create(SysJob job);

    /**
     * 更新任务
     */
    void update(SysJob job);

    /**
     * 删除任务
     */
    void delete(Long id);

    /**
     * 更改任务状态
     */
    void changeStatus(Long id, Integer status);

    /**
     * 立即执行一次
     */
    void run(Long id);
}
