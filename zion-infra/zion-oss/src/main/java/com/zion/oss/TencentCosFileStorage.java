package com.zion.oss;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 腾讯云COS文件存储
 */
@Slf4j
public class TencentCosFileStorage implements FileStorage {

    public static final String STORAGE_TYPE = "tencent";

    private COSClient cosClient;
    private String bucketName;
    private String domain;

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    /**
     * 初始化配置
     *
     * @param secretId  腾讯云SecretId
     * @param secretKey 腾讯云SecretKey
     * @param region    地域，如 ap-guangzhou
     * @param bucketName 存储桶名称
     * @param domain    访问域名
     */
    public void init(String secretId, String secretKey, String region, String bucketName, String domain) {
        this.bucketName = bucketName;
        this.domain = domain;

        // 初始化身份信息
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        
        // 设置地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        
        // 创建COS客户端
        this.cosClient = new COSClient(credentials, clientConfig);

        // 检查存储桶是否存在
        try {
            if (!cosClient.doesBucketExist(bucketName)) {
                cosClient.createBucket(bucketName);
                log.info("腾讯云COS存储桶创建成功: {}", bucketName);
            }
            log.info("腾讯云COS存储初始化完成, region: {}, bucket: {}", region, bucketName);
        } catch (Exception e) {
            log.error("腾讯云COS初始化失败", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String fileName) {
        try {
            String objectKey = path + "/" + fileName;
            
            ObjectMetadata metadata = new ObjectMetadata();
            // 设置内容长度（如果已知可以设置，否则SDK会自动处理）
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, inputStream, metadata);
            cosClient.putObject(putObjectRequest);
            
            return getUrl(objectKey);
        } catch (Exception e) {
            log.error("腾讯云COS文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            cosClient.deleteObject(bucketName, path);
        } catch (Exception e) {
            log.error("腾讯云COS文件删除失败: {}", path, e);
        }
    }

    @Override
    public byte[] getFile(String path) {
        try {
            COSObject cosObject = cosClient.getObject(bucketName, path);
            try (InputStream inputStream = cosObject.getObjectContent()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("腾讯云COS文件读取失败: {}", path, e);
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
            return cosClient.doesObjectExist(bucketName, path);
        } catch (Exception e) {
            log.error("腾讯云COS检查文件存在失败: {}", path, e);
            return false;
        }
    }

    /**
     * 关闭COS客户端
     */
    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }
}
