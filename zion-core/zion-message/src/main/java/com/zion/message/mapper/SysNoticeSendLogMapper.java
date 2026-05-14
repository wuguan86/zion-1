package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.SysNoticeSendLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知推送记录 Mapper
 */
@Mapper
public interface SysNoticeSendLogMapper extends BaseMapper<SysNoticeSendLog> {
}
