package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysDictType;

import java.util.List;

/**
 * 字典类型服务接口
 */
public interface SysDictTypeService extends IService<SysDictType> {

    /**
     * 分页查询字典类型
     */
    PageResult<SysDictType> page(Integer page, Integer pageSize, String dictName, String dictType, Integer status);

    /**
     * 获取所有字典类型
     */
    List<SysDictType> listAll();

    /**
     * 创建字典类型
     */
    void create(SysDictType dictType);

    /**
     * 更新字典类型
     */
    void update(SysDictType dictType);

    /**
     * 删除字典类型
     */
    void delete(Long id);
}
