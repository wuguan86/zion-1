package com.zion.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.file.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface SysFileService {

    /**
     * 分页查询文件列表
     */
    Page<SysFile> page(Integer page, Integer pageSize, String originalName, String fileType);

    /**
     * 分页查询文件列表（支持分组和类型过滤）
     * @param page 页码
     * @param pageSize 每页数量
     * @param groupId 分组ID（null表示查询未分组，-1表示查询全部）
     * @param fileCategory 文件类别（image/video/other）
     * @param originalName 文件名搜索
     */
    Page<SysFile> pageByGroup(Integer page, Integer pageSize, Long groupId, String fileCategory, String originalName);

    /**
     * 上传文件
     */
    SysFile upload(MultipartFile file, String path);

    /**
     * 上传文件到指定分组
     */
    SysFile upload(MultipartFile file, String path, Long groupId);

    /**
     * 上传图片
     */
    SysFile uploadImage(MultipartFile file);

    /**
     * 获取文件详情
     */
    SysFile getById(Long id);

    /**
     * 获取文件字节
     */
    byte[] getFileBytes(Long id);

    /**
     * 删除文件
     */
    void delete(Long id);

    /**
     * 批量删除文件
     */
    void deleteBatch(Long[] ids);

    /**
     * 移动文件到分组
     */
    void moveToGroup(Long[] fileIds, Long groupId);

    /**
     * 重命名文件
     */
    void rename(Long id, String newName);
}
