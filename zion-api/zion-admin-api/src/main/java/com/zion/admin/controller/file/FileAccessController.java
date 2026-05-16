package com.zion.admin.controller.file;

import com.zion.oss.FileStorage;
import com.zion.oss.LocalFileStorage;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.system.storage.FileStorageFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件访问控制器
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileAccessController {

    private final SystemConfigHelper configHelper;
    private final FileStorageFactory storageFactory;

    /**
     * 访问本地文件
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) {
        // 获取文件路径
        // 注意：ApiPrefixConfig 给 @RestController 自动加了 /api 前缀
        // 所以实际请求URI是 /api/files/xxx，需要从 /api/files 之后截取
        String requestUri = request.getRequestURI();
        int filesIndex = requestUri.indexOf("/files/");
        String filePath = (filesIndex >= 0) ? requestUri.substring(filesIndex + "/files".length()) : requestUri;

        if (!LocalFileStorage.STORAGE_TYPE.equals(configHelper.getStorageProvider())) {
            return getCurrentStorageFile(filePath);
        }

        // 获取本地存储路径
        String basePath = configHelper.getStorageLocalPath();

        try {
            // 构建完整路径
            Path fullPath = Paths.get(basePath, filePath);

            // 安全检查：确保路径在基础目录内
            if (!fullPath.normalize().startsWith(Paths.get(basePath).normalize())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(fullPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 读取文件
            byte[] bytes = Files.readAllBytes(fullPath);
            String contentType = Files.probeContentType(fullPath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(bytes);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 非本地存储通过当前存储策略读取文件，用于兼容历史返回的 /images/... 访问路径。
     */
    private ResponseEntity<byte[]> getCurrentStorageFile(String filePath) {
        String storagePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        try {
            FileStorage storage = storageFactory.getStorage();
            byte[] bytes = storage.getFile(storagePath);
            String contentType = Files.probeContentType(Paths.get(storagePath));
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(bytes);
        } catch (Exception e) {
            log.error("读取当前存储文件失败: {}", storagePath, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
