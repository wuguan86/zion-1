package com.zion.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zion.system.entity.WebUser;
import com.zion.system.mapper.WebUserMapper;
import com.zion.system.service.WebUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class WebUserServiceImpl extends ServiceImpl<WebUserMapper, WebUser> implements WebUserService {

    @Override
    public WebUser getByOpenId(String openId) {
        return baseMapper.selectByOpenId(openId);
    }

    @Override
    public WebUser getByPhone(String phone) {
        return lambdaQuery().eq(WebUser::getPhone, phone).one();
    }

    @Override
    public void updateLoginInfo(Long id) {
        WebUser user = new WebUser();
        user.setId(id);
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
    }
}
