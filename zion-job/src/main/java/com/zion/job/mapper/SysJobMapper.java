package com.zion.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.job.entity.SysJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务Mapper
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {
}
