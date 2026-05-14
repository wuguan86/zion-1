package com.zion.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.message.entity.ChatGroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 群消息Mapper
 */
@Mapper
public interface ChatGroupMessageMapper extends BaseMapper<ChatGroupMessage> {
    
    /**
     * 获取群的最新一条消息
     */
    @Select("SELECT * FROM sys_chat_group_message WHERE group_id = #{groupId} ORDER BY send_time DESC LIMIT 1")
    ChatGroupMessage selectLatestMessage(@Param("groupId") Long groupId);
}
