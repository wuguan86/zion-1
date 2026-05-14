package com.zion.admin.controller.message;

import cn.dev33.satoken.stp.StpUtil;
import com.zion.admin.websocket.MessageWebSocketHandler;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.message.entity.SysChatMessage;
import com.zion.system.entity.SysUser;
import com.zion.system.entity.SysUserBlacklist;
import com.zion.message.service.SysChatMessageService;
import com.zion.system.service.SysUserBlacklistService;
import com.zion.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 即时聊天
 */
@RestController
@RequestMapping("/sys/chat")
@RequiredArgsConstructor
public class SysChatController {

    private final SysChatMessageService chatMessageService;
    private final SysUserService userService;
    private final SysUserBlacklistService blacklistService;
    private final MessageWebSocketHandler webSocketHandler;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<SysChatMessage> send(@RequestBody SysChatMessage message) {
        Long senderId = StpUtil.getLoginIdAsLong();

        // 检查是否被对方拉黑
        if (blacklistService.isBlocked(senderId, message.getReceiverId())) {
            return Result.fail("消息发送失败，对方已将你拉黑");
        }

        // 检查自己是否拉黑了对方（如果拉黑了就不能发消息）
        if (blacklistService.isInMyBlacklist(senderId, message.getReceiverId())) {
            return Result.fail("请先移除黑名单后再发送消息");
        }

        SysChatMessage result = chatMessageService.send(message);

        // 通过WebSocket推送消息
        if (message.getReceiverId() != null && message.getReceiverId() > 0) {
            webSocketHandler.sendToUser(message.getReceiverId(),
                    "{\"type\":\"chat\",\"senderId\":" + result.getSenderId() +
                    ",\"senderName\":\"" + result.getSenderName() + "\"" +
                    ",\"content\":\"" + result.getContent().replace("\"", "\\\"") + "\"" +
                    ",\"msgType\":" + result.getMsgType() +
                    ",\"time\":" + System.currentTimeMillis() + "}");
        }

        return Result.ok(result);
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/history/{targetId}")
    public Result<PageResult<SysChatMessage>> getChatHistory(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        var result = chatMessageService.getChatHistory(userId, targetId, page, pageSize);
        return Result.ok(PageResult.of(result));
    }

    /**
     * 获取最近联系人
     */
    @GetMapping("/contacts")
    public Result<List<SysChatMessage>> getRecentContacts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatMessageService.getRecentContacts(userId));
    }

    /**
     * 获取用户列表（用于选择聊天对象）
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers() {
        Long userId = StpUtil.getLoginIdAsLong();
        // 获取所有用户（排除自己）
        List<SysUser> users = userService.listAll();
        users.removeIf(u -> u.getId().equals(userId));

        // 构建返回数据，包含用户信息和最新消息
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("avatar", user.getAvatar());

            // 获取与该用户的最新消息
            var latestMsg = chatMessageService.getLatestMessage(userId, user.getId());
            if (latestMsg != null) {
                item.put("lastMessage", latestMsg.getMsgType() == 2 ? "[图片]" : latestMsg.getContent());
                item.put("lastMessageTime", latestMsg.getSendTime());
            }

            // 检查拉黑状态
            item.put("isBlocked", blacklistService.isInMyBlacklist(userId, user.getId()));

            result.add(item);
        }
        return Result.ok(result);
    }

    /**
     * 标记消息为已读
     */
    @PostMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatMessageService.markAsRead(userId, senderId);
        return Result.ok();
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(chatMessageService.getUnreadCount(userId));
    }

    /**
     * 获取消息统计（通知+聊天）
     */
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getMessageStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Integer> stats = new HashMap<>();
        stats.put("chatCount", chatMessageService.getUnreadCount(userId));
        return Result.ok(stats);
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{userId}")
    public Result<Boolean> isOnline(@PathVariable Long userId) {
        return Result.ok(webSocketHandler.isOnline(userId));
    }

    /**
     * 清空与某人的聊天记录
     */
    @DeleteMapping("/clear/{targetId}")
    public Result<Void> clearHistory(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatMessageService.clearHistory(userId, targetId);
        return Result.ok();
    }

    /**
     * 拉黑用户
     */
    @PostMapping("/block/{targetId}")
    public Result<Void> blockUser(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        blacklistService.blockUser(userId, targetId);
        return Result.ok();
    }

    /**
     * 取消拉黑
     */
    @DeleteMapping("/block/{targetId}")
    public Result<Void> unblockUser(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        blacklistService.unblockUser(userId, targetId);
        return Result.ok();
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<List<SysUserBlacklist>> getBlacklist() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(blacklistService.getBlacklist(userId));
    }

    /**
     * 检查是否拉黑
     */
    @GetMapping("/blocked/{targetId}")
    public Result<Boolean> isBlocked(@PathVariable Long targetId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(blacklistService.isInMyBlacklist(userId, targetId));
    }
}
