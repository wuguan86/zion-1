package com.zion.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.job.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 定时任务日志Mapper
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    /**
     * 近7日调度统计（日期、成功数、失败数）
     */
    @Select("SELECT TO_CHAR(start_time, 'YYYY-MM-DD') AS exec_date, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS fail_count " +
            "FROM sys_job_log " +
            "WHERE start_time >= CURRENT_DATE - INTERVAL '7 days' " +
            "GROUP BY TO_CHAR(start_time, 'YYYY-MM-DD') " +
            "ORDER BY exec_date")
    List<Map<String, Object>> selectDailyStats();
}
