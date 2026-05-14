package com.zion.oss;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * MinIO文件存储
 */
@Slf4j
public class MinioFileStorage implements FileStorage {

    public static final String STORAGE_TYPE = "minio";

    private MinioClient minioClient;
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
        
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        
        // 确保存储桶存在
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("MinIO存储桶创建成功: {}", bucketName);
            }
            log.info("MinIO存储初始化完成, endpoint: {}, bucket: {}", endpoint, bucketName);
        } catch (Exception e) {
            log.error("MinIO初始化失败", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String fileName) {
        try {
            String objectName = path + "/" + fileName;
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, -1, 10485760) // 10MB分片
                    .build());
            
            return getUrl(objectName);
        } catch (Exception e) {
            log.error("MinIO文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("MinIO文件删除失败: {}", path, e);
        }
    }

    @Override
    public byte[] getFile(String path) {
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(path)
                .build())) {
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("MinIO文件读取失败: {}", path, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }

    @Override
    public String getUrl(String path) {
        String normalizedPath = path.replace("\\", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return domain + "/" + bucketName + normalizedPath;
    }

    @Override
    public boolean exists(String path) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            log.error("MinIO检查文件存在失败: {}", path, e);
            return false;
        }
    }
}
