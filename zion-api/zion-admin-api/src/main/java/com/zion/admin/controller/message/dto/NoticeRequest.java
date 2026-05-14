package com.zion.admin.controller.message.dto;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.zion.message.entity.SysNotice;
import lombok.Data;

import java.util.List;

/**
 * 通知请求 DTO（channels、targetIds 为数组，需转换为 JSON 存储）
 */
@Data
public class NoticeRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private String title;
    private String content;
    private Integer noticeType;
    private List<String> channels;
    private Integer targetType;
    private List<Long> targetIds;
    private Integer status;

    public SysNotice toEntity() {
        SysNotice n = new SysNotice();
        n.setId(id);
        n.setTitle(title);
        n.setContent(content);
        n.setNoticeType(noticeType);
        n.setTargetType(targetType);
        n.setStatus(status);
        try {
            if (channels != null) n.setChannels(MAPPER.writeValueAsString(channels));
            if (targetIds != null && !targetIds.isEmpty()) n.setTargetIds(MAPPER.writeValueAsString(targetIds));
        } catch (Exception ignored) {}
        return n;
    }

    public static NoticeRequest fromEntity(SysNotice n) {
        NoticeRequest r = new NoticeRequest();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setContent(n.getContent());
        r.setNoticeType(n.getNoticeType());
        r.setTargetType(n.getTargetType());
        r.setStatus(n.getStatus());
        try {
            if (n.getChannels() != null && !n.getChannels().isEmpty())
                r.setChannels(MAPPER.readValue(n.getChannels(), new TypeReference<>() {}));
            if (n.getTargetIds() != null && !n.getTargetIds().isEmpty())
                r.setTargetIds(MAPPER.readValue(n.getTargetIds(), new TypeReference<>() {}));
        } catch (Exception ignored) {}
        return r;
    }
}
