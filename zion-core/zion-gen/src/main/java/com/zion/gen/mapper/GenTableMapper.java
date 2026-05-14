package com.zion.gen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.gen.entity.DatabaseColumn;
import com.zion.gen.entity.DatabaseTable;
import com.zion.gen.entity.GenTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 代码生成表 Mapper
 */
@Mapper
public interface GenTableMapper extends BaseMapper<GenTable> {

    /**
     * 查询数据库中的表列表（带搜索和分页）
     */
    @Select("""
        <script>
        SELECT table_name as tableName, table_comment as tableComment, 
               create_time as createTime, update_time as updateTime
        FROM information_schema.tables 
        WHERE table_schema = (SELECT DATABASE()) 
          AND table_type = 'BASE TABLE'
          AND table_name NOT LIKE 'gen_%'
          AND table_name NOT IN (SELECT table_name FROM gen_table)
          <if test="tableName != null and tableName != ''">
            AND table_name LIKE CONCAT('%', #{tableName}, '%')
          </if>
        ORDER BY create_time DESC
        LIMIT #{offset}, #{limit}
        </script>
    """)
    List<DatabaseTable> selectDbTableList(@Param("tableName") String tableName, 
                                          @Param("offset") int offset, 
                                          @Param("limit") int limit);

    /**
     * 查询数据库表总数
     */
    @Select("""
        <script>
        SELECT COUNT(*)
        FROM information_schema.tables 
        WHERE table_schema = (SELECT DATABASE()) 
          AND table_type = 'BASE TABLE'
          AND table_name NOT LIKE 'gen_%'
          AND table_name NOT IN (SELECT table_name FROM gen_table)
          <if test="tableName != null and tableName != ''">
            AND table_name LIKE CONCAT('%', #{tableName}, '%')
          </if>
        </script>
    """)
    long countDbTable(@Param("tableName") String tableName);

    /**
     * 根据表名查询数据库表
     */
    @Select("""
        SELECT table_name as tableName, table_comment as tableComment,
               create_time as createTime, update_time as updateTime
        FROM information_schema.tables 
        WHERE table_schema = (SELECT DATABASE()) 
          AND table_type = 'BASE TABLE'
          AND table_name = #{tableName}
    """)
    DatabaseTable selectDbTableByName(@Param("tableName") String tableName);

    /**
     * 根据表名查询列信息
     */
    @Select("""
        SELECT column_name as columnName, column_comment as columnComment,
               data_type as dataType, column_type as columnType,
               is_nullable as isNullable, column_key as columnKey,
               extra, ordinal_position as ordinalPosition
        FROM information_schema.columns
        WHERE table_schema = (SELECT DATABASE()) 
          AND table_name = #{tableName}
        ORDER BY ordinal_position
    """)
    List<DatabaseColumn> selectDbColumnsByTableName(@Param("tableName") String tableName);
}
