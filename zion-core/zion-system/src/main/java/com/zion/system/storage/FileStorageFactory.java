package com.zion.system.storage;

import com.zion.oss.*;
import com.zion.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文件存储工厂
 * 使用工厂模式 + 策略模式管理不同的存储实现
 * 从 sys_config_group 的 storage 分组获取配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final SystemConfigHelper configHelper;

    /**
     * 缓存的存储服务实例
     */
    private volatile FileStorage cachedStorage;
    private volatile String cachedProvider;

    /**
     * 获取文件存储实例
     */
    public FileStorage getStorage() {
        String provider = configHelper.getStorageProvider();

        // 如果服务商未变化，使用缓存的实例
        if (cachedStorage != null && provider.equals(cachedProvider)) {
            return cachedStorage;
        }

        synchronized (this) {
            // 双重检查
            if (cachedStorage != null && provider.equals(cachedProvider)) {
                return cachedStorage;
            }

            cachedStorage = createStorage(provider);
            cachedProvider = provider;
            return cachedStorage;
        }
    }

    /**
     * 创建存储实例
     */
    private FileStorage createStorage(String provider) {
        FileStorage storage = switch (provider) {
            case LocalFileStorage.STORAGE_TYPE -> createLocalStorage();
            case MinioFileStorage.STORAGE_TYPE -> createMinioStorage();
            case AliyunOssFileStorage.STORAGE_TYPE -> createAliyunStorage();
            case TencentCosFileStorage.STORAGE_TYPE -> createTencentStorage();
            case RustFsFileStorage.STORAGE_TYPE -> createRustfsStorage();
            default -> {
                log.warn("未知的存储类型: {}，使用本地存储", provider);
                yield createLocalStorage();
            }
        };

        log.info("创建文件存储实例: {} - {}", provider, storage.getStorageType());
        return storage;
    }

    /**
     * 创建本地存储
     */
    private FileStorage createLocalStorage() {
        LocalFileStorage storage = new LocalFileStorage();
        storage.init(
            configHelper.getStorageLocalPath(),
            configHelper.getStorageDomain()
        );
        return storage;
    }

    /**
     * 创建MinIO存储
     */
    private FileStorage createMinioStorage() {
        MinioFileStorage storage = new MinioFileStorage();
        storage.init(
            configHelper.getStorageMinioEndpoint(),
            configHelper.getStorageMinioAccessKey(),
            configHelper.getStorageMinioSecretKey(),
            configHelper.getStorageMinioBucket(),
            configHelper.getStorageDomain()
        );
        return storage;
    }

    /**
     * 创建阿里云OSS存储
     */
    private FileStorage createAliyunStorage() {
        AliyunOssFileStorage storage = new AliyunOssFileStorage();
        storage.init(
            configHelper.getStorageAliyunEndpoint(),
            configHelper.getStorageAliyunAccessKey(),
            configHelper.getStorageAliyunSecretKey(),
            configHelper.getStorageAliyunBucket(),
            configHelper.getStorageDomain()
        );
        return storage;
    }

    /**
     * 创建腾讯云COS存储
     */
    private FileStorage createTencentStorage() {
        TencentCosFileStorage storage = new TencentCosFileStorage();
        storage.init(
            configHelper.getStorageTencentSecretId(),
            configHelper.getStorageTencentSecretKey(),
            configHelper.getStorageTencentRegion(),
            configHelper.getStorageTencentBucket(),
            configHelper.getStorageDomain()
        );
        return storage;
    }

    /**
     * 创建RustFS存储
     */
    private FileStorage createRustfsStorage() {
        RustFsFileStorage storage = new RustFsFileStorage();
        storage.init(
            configHelper.getStorageRustfsEndpoint(),
            configHelper.getStorageRustfsAccessKey(),
            configHelper.getStorageRustfsSecretKey(),
            configHelper.getStorageRustfsBucket(),
            configHelper.getStorageDomain()
        );
        return storage;
    }

    /**
     * 刷新存储实例
     */
    public void refresh() {
        synchronized (this) {
            cachedStorage = null;
            cachedProvider = null;
        }
        log.info("文件存储缓存已清空");
    }

    /**
     * 获取当前存储类型
     */
    public String getCurrentStorageType() {
        return configHelper.getStorageProvider();
    }
}
