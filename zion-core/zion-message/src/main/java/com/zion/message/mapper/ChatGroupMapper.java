package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 群聊Mapper
 */
@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {
    
    /**
     * 查询用户加入的群列表
     */
    @Select("SELECT g.*, u.nickname as owner_name, " +
            "(SELECT COUNT(*) FROM sys_chat_group_member WHERE group_id = g.id) as member_count " +
            "FROM sys_chat_group g " +
            "LEFT JOIN sys_user u ON g.owner_id = u.id " +
            "WHERE g.status = 1 AND g.id IN " +
            "(SELECT group_id FROM sys_chat_group_member WHERE user_id = #{userId}) " +
            "ORDER BY g.update_time DESC")
    List<ChatGroup> selectUserGroups(@Param("userId") Long userId);
    
    /**
     * 查询群详情
     */
    @Select("SELECT g.*, u.nickname as owner_name, " +
            "(SELECT COUNT(*) FROM sys_chat_group_member WHERE group_id = g.id) as member_count " +
            "FROM sys_chat_group g " +
            "LEFT JOIN sys_user u ON g.owner_id = u.id " +
            "WHERE g.id = #{groupId}")
    ChatGroup selectGroupDetail(@Param("groupId") Long groupId);
}
