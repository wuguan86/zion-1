package com.zion.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zion.message.entity.SysNotice;

import java.util.List;

/**
 * 系统通知服务接口
 */
public interface SysNoticeService {

    /**
     * 分页查询通知列表（管理端）
     */
    Page<SysNotice> page(Integer page, Integer pageSize, String title, Integer noticeType, Integer status);

    /**
     * 获取用户通知列表
     */
    Page<SysNotice> getUserNotices(Long userId, Integer page, Integer pageSize, Integer isRead);

    /**
     * 获取通知详情
     */
    SysNotice getById(Long id);

    /**
     * 创建通知
     */
    void create(SysNotice notice);

    /**
     * 更新通知
     */
    void update(SysNotice notice);

    /**
     * 删除通知
     */
    void delete(Long id);

    /**
     * 发布通知（推送给所有用户）
     */
    void publish(Long id);

    /**
     * 标记通知为已读
     */
    void markAsRead(Long userId, Long noticeId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 获取用户未读通知数量
     */
    int getUnreadCount(Long userId);
}
