package com.zion.gen.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.gen.entity.DatabaseTable;
import com.zion.gen.entity.GenTable;

import java.util.List;
import java.util.Map;

/**
 * 代码生成 Service
 */
public interface GenTableService {

    /**
     * 分页查询数据库中可导入的表列表
     */
    Page<DatabaseTable> selectDbTableList(Integer page, Integer pageSize, String tableName);

    /**
     * 导入表结构
     */
    void importTable(String[] tableNames);

    /**
     * 分页查询已导入的表
     */
    Page<GenTable> page(Integer page, Integer pageSize, String tableName);

    /**
     * 查询表详情（包含列信息）
     */
    GenTable getTableById(Long id);

    /**
     * 更新表和列配置
     */
    void updateTable(GenTable table);

    /**
     * 删除表
     */
    void deleteTable(Long[] ids);

    /**
     * 预览代码
     */
    Map<String, String> previewCode(Long tableId);

    /**
     * 生成代码（下载zip）
     */
    byte[] generateCode(Long[] tableIds);

    /**
     * 预览将要生成的文件列表
     */
    List<String> previewGenerateFiles(Long tableId);

    /**
     * 生成代码到项目
     */
    List<String> generateToProject(Long tableId);

    /**
     * 预览将要移除的文件列表
     */
    List<String> previewRemoveFiles(Long tableId);

    /**
     * 移除已生成的代码
     */
    List<String> removeGeneratedCode(Long tableId);

    /**
     * 同步数据库表结构
     */
    void syncTable(Long tableId);
}
