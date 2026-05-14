package com.zion.admin.controller.message;

import cn.dev33.satoken.stp.StpUtil;
import com.zion.admin.websocket.MessageWebSocketHandler;
import com.zion.common.result.PageResult;
import com.zion.common.result.Result;
import com.zion.message.entity.ChatGroup;
import com.zion.message.entity.ChatGroupMember;
import com.zion.message.entity.ChatGroupMessage;
import com.zion.message.service.ChatGroupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群聊控制器
 */
@RestController
@RequestMapping("/chat/group")
@RequiredArgsConstructor
public class ChatGroupController {

    private final ChatGroupService groupService;
    private final MessageWebSocketHandler webSocketHandler;

    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public Result<ChatGroup> create(@RequestBody CreateGroupRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChatGroup group = groupService.createGroup(request.getName(), userId, request.getMemberIds());
        return Result.ok(group);
    }

    /**
     * 获取我的群列表
     */
    @GetMapping("/list")
    public Result<List<ChatGroup>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(groupService.getUserGroups(userId));
    }

    /**
     * 获取群详情
     */
    @GetMapping("/{groupId}")
    public Result<ChatGroup> detail(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupDetail(groupId));
    }

    /**
     * 更新群信息
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody ChatGroup group) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChatGroup existing = groupService.getGroupDetail(group.getId());
        if (existing == null) {
            return Result.fail("群不存在");
        }
        // 只有群主和管理员可以修改
        ChatGroupMember member = groupService.getGroupMembers(group.getId()).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (member == null || member.getRole() < 1) {
            return Result.fail("没有权限修改群信息");
        }
        groupService.updateGroup(group);
        return Result.ok();
    }

    /**
     * 解散群聊
     */
    @DeleteMapping("/{groupId}")
    public Result<Void> dissolve(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.dissolveGroup(groupId, userId);
        return Result.ok();
    }

    /**
     * 退出群聊
     */
    @PostMapping("/{groupId}/quit")
    public Result<Void> quit(@PathVariable Long groupId) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.quitGroup(groupId, userId);
        return Result.ok();
    }

    /**
     * 获取群成员列表
     */
    @GetMapping("/{groupId}/members")
    public Result<List<ChatGroupMember>> members(@PathVariable Long groupId) {
        return Result.ok(groupService.getGroupMembers(groupId));
    }

    /**
     * 添加群成员
     */
    @PostMapping("/{groupId}/members")
    public Result<Void> addMembers(@PathVariable Long groupId, @RequestBody MemberIdsRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.addMembers(groupId, request.getUserIds(), userId);
        return Result.ok();
    }

    /**
     * 移除群成员
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public Result<Void> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.removeMember(groupId, memberId, userId);
        return Result.ok();
    }

    /**
     * 设置/取消管理员
     */
    @PostMapping("/{groupId}/admin/{memberId}")
    public Result<Void> setAdmin(@PathVariable Long groupId, @PathVariable Long memberId,
                                  @RequestParam boolean isAdmin) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.setAdmin(groupId, memberId, isAdmin, userId);
        return Result.ok();
    }

    /**
     * 设置/取消禁言
     */
    @PostMapping("/{groupId}/mute/{memberId}")
    public Result<Void> setMuted(@PathVariable Long groupId, @PathVariable Long memberId,
                                  @RequestParam boolean muted) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.setMuted(groupId, memberId, muted, userId);
        return Result.ok();
    }

    /**
     * 转让群主
     */
    @PostMapping("/{groupId}/transfer/{newOwnerId}")
    public Result<Void> transferOwner(@PathVariable Long groupId, @PathVariable Long newOwnerId) {
        Long userId = StpUtil.getLoginIdAsLong();
        groupService.transferOwner(groupId, newOwnerId, userId);
        return Result.ok();
    }

    /**
     * 发送群消息
     */
    @PostMapping("/{groupId}/message")
    public Result<ChatGroupMessage> sendMessage(@PathVariable Long groupId,
                                                 @RequestBody SendMessageRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChatGroupMessage message = groupService.sendMessage(groupId, userId, request.getContent(), request.getMsgType());

        // 通过WebSocket推送给所有群成员
        List<Long> memberIds = groupService.getMemberIds(groupId);
        String msgJson = "{\"type\":\"groupChat\",\"groupId\":" + groupId +
                ",\"senderId\":" + message.getSenderId() +
                ",\"senderName\":\"" + message.getSenderName() + "\"" +
                ",\"senderAvatar\":\"" + (message.getSenderAvatar() != null ? message.getSenderAvatar() : "") + "\"" +
                ",\"content\":\"" + message.getContent().replace("\"", "\\\"") + "\"" +
                ",\"msgType\":" + message.getMsgType() +
                ",\"time\":" + System.currentTimeMillis() + "}";

        for (Long memberId : memberIds) {
            if (!memberId.equals(userId)) {
                webSocketHandler.sendToUser(memberId, msgJson);
            }
        }

        return Result.ok(message);
    }

    /**
     * 获取群消息历史
     */
    @GetMapping("/{groupId}/messages")
    public Result<PageResult<ChatGroupMessage>> messages(@PathVariable Long groupId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "50") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!groupService.isMember(groupId, userId)) {
            return Result.fail("你不是该群成员");
        }
        return Result.ok(PageResult.of(groupService.getMessageHistory(groupId, page, pageSize)));
    }

    @Data
    public static class CreateGroupRequest {
        private String name;
        private List<Long> memberIds;
    }

    @Data
    public static class MemberIdsRequest {
        private List<Long> userIds;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
        private Integer msgType;
    }
}
