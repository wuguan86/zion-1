package com.zion.oss;

import java.io.InputStream;

/**
 * 文件存储策略接口
 */
public interface FileStorage {

    /**
     * 获取存储类型
     */
    String getStorageType();

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param path        存储路径（相对路径）
     * @param fileName    文件名
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String path, String fileName);

    /**
     * 删除文件
     *
     * @param path 文件路径
     */
    void delete(String path);

    /**
     * 获取文件
     *
     * @param path 文件路径
     * @return 文件字节数组
     */
    byte[] getFile(String path);

    /**
     * 获取文件访问URL
     *
     * @param path 文件路径
     * @return 访问URL
     */
    String getUrl(String path);

    /**
     * 检查文件是否存在
     *
     * @param path 文件路径
     * @return 是否存在
     */
    boolean exists(String path);
}
