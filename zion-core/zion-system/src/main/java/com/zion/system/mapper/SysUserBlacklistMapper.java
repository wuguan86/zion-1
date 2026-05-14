package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.SysUserBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户黑名单 Mapper
 */
@Mapper
public interface SysUserBlacklistMapper extends BaseMapper<SysUserBlacklist> {
    
    /**
     * 查询用户的黑名单列表（带用户信息）
     */
    @Select("SELECT b.*, u.nickname as blocked_user_name, u.avatar as blocked_user_avatar " +
            "FROM sys_user_blacklist b " +
            "LEFT JOIN sys_user u ON b.blocked_user_id = u.id " +
            "WHERE b.user_id = #{userId} " +
            "ORDER BY b.create_time DESC")
    List<SysUserBlacklist> selectBlacklistWithUser(@Param("userId") Long userId);
    
    /**
     * 检查是否拉黑
     */
    @Select("SELECT COUNT(*) FROM sys_user_blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int checkBlocked(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
}
