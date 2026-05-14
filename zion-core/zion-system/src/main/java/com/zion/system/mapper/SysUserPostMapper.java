package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.SysUserPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户岗位关联 Mapper
 */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {

    /**
     * 根据用户ID删除关联
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据岗位ID获取关联的用户ID列表
     */
    List<Long> selectUserIdsByPostId(@Param("postId") Long postId);
}

