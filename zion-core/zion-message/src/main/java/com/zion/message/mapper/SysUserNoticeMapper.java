package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.SysUserNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户通知关联 Mapper
 */
@Mapper
public interface SysUserNoticeMapper extends BaseMapper<SysUserNotice> {

    /**
     * 获取用户未读通知数量
     */
    @Select("SELECT COUNT(*) FROM sys_user_notice WHERE user_id = #{userId} AND is_read = 0")
    int selectUnreadCount(@Param("userId") Long userId);
}
