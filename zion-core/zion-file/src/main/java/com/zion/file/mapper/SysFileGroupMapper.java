package com.zion.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.file.entity.SysFileGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件分组 Mapper
 */
@Mapper
public interface SysFileGroupMapper extends BaseMapper<SysFileGroup> {

    /**
     * 查询所有分组并统计文件数量
     */
    @Select("""
        SELECT g.*, 
               (SELECT COUNT(*) FROM sys_file f WHERE f.group_id = g.id) as file_count
        FROM sys_file_group g 
        ORDER BY g.sort ASC, g.id ASC
    """)
    List<SysFileGroup> selectListWithFileCount();

    /**
     * 获取未分组文件数量
     */
    @Select("SELECT COUNT(*) FROM sys_file WHERE group_id IS NULL")
    Integer selectUngroupedFileCount();
}
