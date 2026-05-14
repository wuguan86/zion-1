package com.zion.admin.controller.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.zion.admin.websocket.MessageWebSocketHandler;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.system.annotation.Log;
import com.zion.system.annotation.RepeatSubmit;
import com.zion.system.annotation.Log.BusinessType;
import com.zion.admin.controller.message.dto.NoticeRequest;
import com.zion.message.entity.SysNotice;
import com.zion.message.entity.SysNoticeSendLog;
import com.zion.message.service.NoticeSendService;
import com.zion.message.service.SysNoticeService;
import com.zion.message.service.SysNoticeSendLogService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知管理
 */
@RestController
@RequestMapping("/sys/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;
    private final NoticeSendService noticeSendService;
    private final SysNoticeSendLogService sendLogService;
    private final MessageWebSocketHandler webSocketHandler;

    /**
     * 分页查询通知列表（管理端）
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:notice:list")
    public Result<PageResult<SysNotice>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(required = false) Integer status) {
        var result = noticeService.page(page, pageSize, title, noticeType, status);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/my")
    public Result<PageResult<SysNotice>> myNotices(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        Long userId = StpUtil.getLoginIdAsLong();
        var result = noticeService.getUserNotices(userId, page, pageSize, isRead);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public Result<NoticeRequest> detail(@PathVariable Long id) {
        SysNotice notice = noticeService.getById(id);
        return Result.ok(notice != null ? NoticeRequest.fromEntity(notice) : null);
    }

    /**
     * 创建通知
     */
    @PostMapping
    @SaCheckPermission("sys:notice:add")
    @RepeatSubmit
    @Log(title = "新增通知", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody NoticeRequest request) {
        noticeService.create(request.toEntity());
        return Result.ok();
    }

    /**
     * 更新通知
     */
    @PutMapping
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "修改通知", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody NoticeRequest request) {
        SysNotice existing = noticeService.getById(request.getId());
        if (existing == null) {
            return Result.fail("通知不存在");
        }
        SysNotice entity = request.toEntity();
        entity.setCreateBy(existing.getCreateBy());
        entity.setCreateName(existing.getCreateName());
        entity.setCreateTime(existing.getCreateTime());
        noticeService.update(entity);
        return Result.ok();
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:notice:delete")
    @Log(title = "删除通知", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    /**
     * 发布通知
     */
    @PostMapping("/{id}/publish")
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "发布通知", businessType = BusinessType.UPDATE)
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);

        // 通过WebSocket推送通知
        SysNotice notice = noticeService.getById(id);
        if (notice != null) {
            webSocketHandler.sendNotice(null, notice.getTitle(), notice.getContent());
        }

        return Result.ok();
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        noticeService.markAsRead(userId, id);
        return Result.ok();
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        noticeService.markAllAsRead(userId);
        return Result.ok();
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(noticeService.getUnreadCount(userId));
    }

    /**
     * 获取可用推送渠道（从 sys_config_group 读取配置）
     */
    @GetMapping("/channels")
    @SaCheckPermission("sys:notice:list")
    public Result<List<NoticeSendService.ChannelOption>> getChannels() {
        return Result.ok(noticeSendService.getAvailableChannels());
    }

    /**
     * 获取通知推送记录（触达情况）
     */
    @GetMapping("/{id}/send-logs")
    @SaCheckPermission("sys:notice:list")
    public Result<List<SysNoticeSendLog>> getSendLogs(@PathVariable Long id) {
        return Result.ok(sendLogService.listByNoticeId(id));
    }

    /**
     * 重试失败渠道的推送
     */
    @PostMapping("/{id}/retry")
    @SaCheckPermission("sys:notice:edit")
    @Log(title = "重试通知推送", businessType = BusinessType.UPDATE)
    public Result<Void> retry(@PathVariable Long id, @RequestParam String channel) {
        noticeSendService.retryChannel(id, channel);
        return Result.ok();
    }
}
