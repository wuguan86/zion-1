package com.zion.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.common.result.PageResult;
import com.zion.system.entity.SysPost;

import java.util.List;

/**
 * 岗位服务接口
 */
public interface SysPostService extends IService<SysPost> {

    /**
     * 分页查询岗位
     */
    PageResult<SysPost> page(Integer page, Integer pageSize, String postCode, String postName, Integer status);

    /**
     * 获取所有岗位
     */
    List<SysPost> listAll();

    /**
     * 创建岗位
     */
    void create(SysPost post);

    /**
     * 更新岗位
     */
    void update(SysPost post);

    /**
     * 删除岗位
     */
    void delete(Long id);

    /**
     * 获取岗位树
     */
    List<SysPost> tree();

    /**
     * 移动岗位
     */
    void move(Long id, Long parentId);
}
