package com.zion.system.service;

import com.zion.system.entity.SysConfigGroup;
import java.util.List;

/**
 * 系统配置分组 Service
 */
public interface SysConfigGroupService {
    
    /**
     * 获取所有配置分组
     */
    List<SysConfigGroup> listAll();
    
    /**
     * 根据分组编码获取配置
     */
    SysConfigGroup getByGroupCode(String groupCode);
    
    /**
     * 保存配置
     */
    void saveConfig(String groupCode, String configValue);
    
    /**
     * 获取配置值
     */
    String getConfigValue(String groupCode, String key);
    
    /**
     * 刷新缓存
     */
    void refreshCache();
}
