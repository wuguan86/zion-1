package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.system.entity.WebUser;

public interface WebUserService extends IService<WebUser> {

    WebUser getByOpenId(String openId);

    WebUser getByPhone(String phone);

    void updateLoginInfo(Long id);
}
