package com.zion.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息表
 */
@Data
@TableName("sys_file")
public class SysFile implements Serializable {

    /**
     * 文件ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件URL
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME类型）
     */
    private String fileType;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 存储类型（local、minio、aliyun）
     */
    private String storageType;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 分组ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;
}
