package com.zion.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 阿里云OSS文件存储
 */
@Slf4j
public class AliyunOssFileStorage implements FileStorage {

    public static final String STORAGE_TYPE = "aliyun";

    private OSS ossClient;
    private String bucketName;
    private String domain;

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    /**
     * 初始化配置
     */
    public void init(String endpoint, String accessKey, String secretKey, String bucketName, String domain) {
        this.bucketName = bucketName;
        this.domain = domain;
        
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        
        // 确保存储桶存在
        try {
            if (!ossClient.doesBucketExist(bucketName)) {
                ossClient.createBucket(bucketName);
                log.info("阿里云OSS存储桶创建成功: {}", bucketName);
            }
            log.info("阿里云OSS存储初始化完成, endpoint: {}, bucket: {}", endpoint, bucketName);
        } catch (Exception e) {
            log.error("阿里云OSS初始化失败", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String fileName) {
        try {
            String objectName = path + "/" + fileName;
            ossClient.putObject(bucketName, objectName, inputStream);
            return getUrl(objectName);
        } catch (Exception e) {
            log.error("阿里云OSS文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            ossClient.deleteObject(bucketName, path);
        } catch (Exception e) {
            log.error("阿里云OSS文件删除失败: {}", path, e);
        }
    }

    @Override
    public byte[] getFile(String path) {
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, path);
            try (InputStream inputStream = ossObject.getObjectContent()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("阿里云OSS文件读取失败: {}", path, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    @Override
    public String getUrl(String path) {
        String normalizedPath = path.replace("\\", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return domain + normalizedPath;
    }

    @Override
    public boolean exists(String path) {
        try {
            return ossClient.doesObjectExist(bucketName, path);
        } catch (Exception e) {
            log.error("阿里云OSS检查文件存在失败: {}", path, e);
            return false;
        }
    }

    /**
     * 关闭OSS客户端
     */
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
