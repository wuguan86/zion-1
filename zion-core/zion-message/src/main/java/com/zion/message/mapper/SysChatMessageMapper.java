package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.SysChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface SysChatMessageMapper extends BaseMapper<SysChatMessage> {

    /**
     * 获取用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_chat_message WHERE receiver_id = #{userId} AND is_read = 0")
    int selectUnreadCount(@Param("userId") Long userId);
    
    /**
     * 获取两个用户之间的最新一条消息
     */
    @Select("SELECT * FROM sys_chat_message " +
            "WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) " +
            "   OR (sender_id = #{targetId} AND receiver_id = #{userId}) " +
            "ORDER BY send_time DESC LIMIT 1")
    SysChatMessage selectLatestMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
