package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 群成员Mapper
 */
@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {
    
    /**
     * 查询群成员列表（带用户信息）
     */
    @Select("SELECT m.*, u.username, u.nickname as user_nickname, u.avatar " +
            "FROM sys_chat_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.role DESC, m.join_time ASC")
    List<ChatGroupMember> selectGroupMembers(@Param("groupId") Long groupId);
    
    /**
     * 查询用户在群中的信息
     */
    @Select("SELECT m.*, u.username, u.nickname as user_nickname, u.avatar " +
            "FROM sys_chat_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} AND m.user_id = #{userId}")
    ChatGroupMember selectMemberInfo(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 查询群内所有成员ID
     */
    @Select("SELECT user_id FROM sys_chat_group_member WHERE group_id = #{groupId}")
    List<Long> selectMemberIds(@Param("groupId") Long groupId);
}
