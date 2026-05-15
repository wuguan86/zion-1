package com.zion.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zion.system.entity.WebUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WebUserMapper extends BaseMapper<WebUser> {

    @Select("SELECT * FROM web_user WHERE open_id = #{openId} AND deleted = 0")
    WebUser selectByOpenId(@Param("openId") String openId);
}
