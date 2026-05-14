package com.zion.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.zion.system.entity.SysConfigGroup;
import com.zion.system.mapper.SysConfigGroupMapper;
import com.zion.system.service.SysConfigGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置分组 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigGroupServiceImpl implements SysConfigGroupService {
    
    private final SysConfigGroupMapper configGroupMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_KEY_PREFIX = "sys:config:group:";
    
    // 本地缓存
    private final Map<String, SysConfigGroup> localCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        try {
            refreshCache();
        } catch (Exception e) {
            log.error("系统配置缓存初始化失败，跳过预加载，将在首次访问时从数据库加载", e);
        }
    }
    
    @Override
    public List<SysConfigGroup> listAll() {
        LambdaQueryWrapper<SysConfigGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfigGroup::getStatus, 1)
               .orderByAsc(SysConfigGroup::getSort);
        return configGroupMapper.selectList(wrapper);
    }
    
    @Override
    public SysConfigGroup getByGroupCode(String groupCode) {
        // 优先从本地缓存获取
        SysConfigGroup config = localCache.get(groupCode);
        if (config != null) {
            return config;
        }
        
        // 从数据库获取
        config = configGroupMapper.selectByGroupCode(groupCode);
        if (config != null) {
            localCache.put(groupCode, config);
        }
        return config;
    }
    
    @Override
    public void saveConfig(String groupCode, String configValue) {
        SysConfigGroup config = configGroupMapper.selectByGroupCode(groupCode);
        if (config == null) {
            throw new RuntimeException("配置分组不存在: " + groupCode);
        }
        
        config.setConfigValue(configValue);
        configGroupMapper.updateById(config);
        
        // 更新缓存
        localCache.put(groupCode, config);
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + groupCode, configValue);
    }
    
    @Override
    public String getConfigValue(String groupCode, String key) {
        SysConfigGroup config = getByGroupCode(groupCode);
        if (config == null || config.getConfigValue() == null) {
            return null;
        }
        
        try {
            JsonNode root = objectMapper.readTree(config.getConfigValue());
            JsonNode value = root.get(key);
            if (value != null) {
                if (value.isTextual()) {
                    return value.asText();
                }
                return value.toString();
            }
        } catch (Exception e) {
            log.error("解析配置值失败: groupCode={}, key={}", groupCode, key, e);
        }
        return null;
    }
    
    @Override
    public void refreshCache() {
        localCache.clear();
        List<SysConfigGroup> list = configGroupMapper.selectList(null);
        for (SysConfigGroup config : list) {
            localCache.put(config.getGroupCode(), config);
            if (config.getConfigValue() != null) {
                redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + config.getGroupCode(), config.getConfigValue());
            }
        }
        log.info("系统配置缓存刷新完成，共 {} 个分组", list.size());
    }
}
