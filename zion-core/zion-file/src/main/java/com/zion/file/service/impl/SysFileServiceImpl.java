package com.zion.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.file.entity.SysFile;
import com.zion.file.mapper.SysFileMapper;
import com.zion.file.service.SysFileService;
import com.zion.system.helper.SystemConfigHelper;
import com.zion.oss.FileStorage;
import com.zion.system.storage.FileStorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl implements SysFileService {

    private final SysFileMapper fileMapper;
    private final FileStorageFactory storageFactory;
    private final SystemConfigHelper configHelper;

    @Override
    public Page<SysFile> page(Integer page, Integer pageSize, String originalName, String fileType) {
        Page<SysFile> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFile::getOriginalName, originalName);
        }
        if (StringUtils.hasText(fileType)) {
            wrapper.like(SysFile::getFileType, fileType);
        }

        wrapper.orderByDesc(SysFile::getCreateTime);
        return fileMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<SysFile> pageByGroup(Integer page, Integer pageSize, Long groupId, String fileCategory, String originalName) {
        Page<SysFile> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();

        // 分组过滤：-1表示全部，null表示未分组，其他表示指定分组
        if (groupId != null && groupId != -1) {
            wrapper.eq(SysFile::getGroupId, groupId);
        } else if (groupId == null) {
            wrapper.isNull(SysFile::getGroupId);
        }
        // groupId == -1 时不添加分组条件，查询全部

        // 文件类别过滤
        if (StringUtils.hasText(fileCategory)) {
            switch (fileCategory) {
                case "image" -> wrapper.likeRight(SysFile::getFileType, "image/");
                case "video" -> wrapper.likeRight(SysFile::getFileType, "video/");
                case "audio" -> wrapper.likeRight(SysFile::getFileType, "audio/");
                case "other" -> wrapper.and(w -> w
                        .notLike(SysFile::getFileType, "image/")
                        .notLike(SysFile::getFileType, "video/")
                        .notLike(SysFile::getFileType, "audio/"));
            }
        }

        // 文件名搜索
        if (StringUtils.hasText(originalName)) {
            wrapper.like(SysFile::getOriginalName, originalName);
        }

        wrapper.orderByDesc(SysFile::getCreateTime);
        return fileMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String path) {
        return upload(file, path, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String path, Long groupId) {
        // 验证文件大小
        configHelper.validateFileSize(file.getSize());

        // 验证文件类型
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            configHelper.validateFileType(originalName);
        }

        // 获取存储实例
        FileStorage storage = storageFactory.getStorage();
        String suffix = getFileSuffix(originalName);
        String fileName = generateFileName(suffix);

        // 生成存储路径
        String storagePath = StringUtils.hasText(path) ? path : generatePath();

        try {
            // 上传文件
            String url = storage.upload(file.getInputStream(), storagePath, fileName);

            // 保存文件记录
            SysFile sysFile = new SysFile();
            sysFile.setOriginalName(originalName);
            sysFile.setFileName(fileName);
            sysFile.setFilePath(storagePath + "/" + fileName);
            sysFile.setUrl(url);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileType(file.getContentType());
            sysFile.setFileSuffix(suffix);
            sysFile.setStorageType(storage.getStorageType());
            sysFile.setGroupId(groupId);
            sysFile.setCreateBy(StpUtil.getLoginIdAsString());
            sysFile.setCreateTime(LocalDateTime.now());

            fileMapper.insert(sysFile);
            String publicUrl = resolvePublicUrl(url, sysFile.getId());
            if (!publicUrl.equals(url)) {
                sysFile.setUrl(publicUrl);
                fileMapper.updateById(sysFile);
            }

            log.info("文件上传成功: {} -> {}", originalName, sysFile.getUrl());
            return sysFile;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile uploadImage(MultipartFile file) {
        // 验证是否为图片
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("请上传图片文件");
        }

        // 上传到 images 目录
        return upload(file, "images/" + generatePath());
    }

    @Override
    public SysFile getById(Long id) {
        return fileMapper.selectById(id);
    }

    @Override
    public byte[] getFileBytes(Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            throw new RuntimeException("文件不存在");
        }

        // 使用当前存储获取文件
        FileStorage storage = storageFactory.getStorage();
        return storage.getFile(sysFile.getFilePath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            return;
        }

        // 删除存储文件
        try {
            FileStorage storage = storageFactory.getStorage();
            storage.delete(sysFile.getFilePath());
        } catch (Exception e) {
            log.error("删除存储文件失败", e);
        }

        // 删除记录
        fileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] ids) {
        Arrays.stream(ids).forEach(this::delete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToGroup(Long[] fileIds, Long groupId) {
        if (fileIds == null || fileIds.length == 0) {
            return;
        }
        fileMapper.update(null, new LambdaUpdateWrapper<SysFile>()
                .in(SysFile::getId, Arrays.asList(fileIds))
                .set(SysFile::getGroupId, groupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, String newName) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }
        file.setOriginalName(newName);
        fileMapper.updateById(file);
    }

    /**
     * 获取文件后缀
     */
    private String getFileSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String suffix) {
        return UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    /**
     * 生成存储路径（按日期）
     */
    private String generatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 生成前端可直接访问的文件地址。
     * 对象存储未配置访问域名时，存储实现会返回 /images/... 这类对象路径；
     * 该路径不能通过前端服务直接访问，因此统一兜底为后端预览接口。
     */
    static String resolvePublicUrl(String url, Long fileId) {
        if (!StringUtils.hasText(url)) {
            return url;
        }

        String trimmedUrl = url.trim();
        if (trimmedUrl.matches("^(?i)(https?:)?//.*")
                || trimmedUrl.startsWith("/api/files/")
                || trimmedUrl.startsWith("/api/sys/file/")) {
            return trimmedUrl;
        }

        return "/api/sys/file/preview/" + fileId;
    }
}
