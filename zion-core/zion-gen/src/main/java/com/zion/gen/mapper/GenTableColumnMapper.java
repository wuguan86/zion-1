package com.zion.gen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.gen.entity.GenTableColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成表字段 Mapper
 */
@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {

    /**
     * 根据表ID查询列列表
     */
    default List<GenTableColumn> selectByTableId(Long tableId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GenTableColumn>()
                .eq(GenTableColumn::getTableId, tableId)
                .orderByAsc(GenTableColumn::getSort));
    }

    /**
     * 根据表ID删除列
     */
    default int deleteByTableId(Long tableId) {
        return delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GenTableColumn>()
                .eq(GenTableColumn::getTableId, tableId));
    }
}
