package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.SysConfigGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置分组 Mapper
 */
@Mapper
public interface SysConfigGroupMapper extends BaseMapper<SysConfigGroup> {
    
    @Select("SELECT * FROM sys_config_group WHERE group_code = #{groupCode}")
    SysConfigGroup selectByGroupCode(String groupCode);
}
