package com.zion.push;

import lombok.Builder;
import lombok.Data;

/**
 * Webhook 推送消息体
 * 支持文本、图片（URL 或 Base64）
 */
@Data
@Builder
public class WebhookPayload {

    /** 标题（可与 content 拼接为正文） */
    private String title;

    /** 正文内容 */
    private String content;

    /** 图片 URL（钉钉/飞书 Markdown 嵌入） */
    private String imageUrl;

    /** 图片 Base64（企业微信 image 类型必填） */
    private String imageBase64;

    /** 图片 MD5（企业微信 image 类型必填） */
    private String imageMd5;

    /**
     * 获取完整文本（标题 + 内容）
     */
    public String getFullText() {
        if (title == null) title = "";
        if (content == null) content = "";
        return title.isEmpty() ? content : (content.isEmpty() ? title : title + "\n\n" + content);
    }
}
