package com.zion.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zion.file.entity.SysFileGroup;

import java.util.List;

/**
 * 文件分组 Service
 */
public interface SysFileGroupService extends IService<SysFileGroup> {

    /**
     * 查询所有分组（包含文件数量）
     */
    List<SysFileGroup> listWithFileCount();

    /**
     * 获取未分组文件数量
     */
    Integer getUngroupedFileCount();

    /**
     * 创建分组
     */
    void create(SysFileGroup group);

    /**
     * 更新分组
     */
    void update(SysFileGroup group);

    /**
     * 删除分组（将分组内的文件移动到未分组）
     */
    void delete(Long id);
}
