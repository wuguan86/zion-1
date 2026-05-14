import { request } from '@/utils/request'
import type { PageResult } from './system'

// ==================== 系统通知 ====================
export interface SysNotice {
  id?: number
  title: string
  content: string
  noticeType: number
  channels?: string[]
  targetType?: number
  targetIds?: number[]
  status: number
  createBy?: number
  createName?: string
  createTime?: string
}

export interface NoticeChannelOption {
  code: string
  name: string
  enabled: boolean
}

export const noticeApi = {
  // 分页查询（管理端）
  page(params: { page: number; pageSize: number; title?: string; noticeType?: number; status?: number }): Promise<PageResult<SysNotice>> {
    return request({ url: '/sys/notice/page', method: 'get', params })
  },
  
  // 获取当前用户通知
  myNotices(params: { page: number; pageSize: number; isRead?: number }): Promise<PageResult<SysNotice>> {
    return request({ url: '/sys/notice/my', method: 'get', params })
  },
  
  // 获取详情（含 channels、targetIds 数组）
  detail(id: number): Promise<SysNotice & { channels?: string[]; targetIds?: number[] }> {
    return request({ url: `/sys/notice/${id}`, method: 'get' })
  },
  
  // 创建
  create(data: SysNotice): Promise<void> {
    return request({ url: '/sys/notice', method: 'post', data })
  },
  
  // 更新
  update(data: SysNotice): Promise<void> {
    return request({ url: '/sys/notice', method: 'put', data })
  },
  
  // 删除
  delete(id: number): Promise<void> {
    return request({ url: `/sys/notice/${id}`, method: 'delete' })
  },
  
  // 发布
  publish(id: number): Promise<void> {
    return request({ url: `/sys/notice/${id}/publish`, method: 'post' })
  },
  
  // 标记已读
  markAsRead(id: number): Promise<void> {
    return request({ url: `/sys/notice/${id}/read`, method: 'post' })
  },
  
  // 标记全部已读
  markAllAsRead(): Promise<void> {
    return request({ url: '/sys/notice/read-all', method: 'post' })
  },
  
  // 获取未读数量
  getUnreadCount(): Promise<number> {
    return request({ url: '/sys/notice/unread-count', method: 'get' })
  },
  // 获取可用推送渠道（从 sys_config_group 读取）
  getChannels(): Promise<NoticeChannelOption[]> {
    return request({ url: '/sys/notice/channels', method: 'get' })
  },

  // 获取通知推送记录（触达情况）
  getSendLogs(id: number): Promise<NoticeSendLog[]> {
    return request({ url: `/sys/notice/${id}/send-logs`, method: 'get' })
  },

  // 重试失败渠道的推送
  retry(id: number, channel: string): Promise<void> {
    return request({ url: `/sys/notice/${id}/retry`, method: 'post', params: { channel } })
  }
}

export interface NoticeSendLog {
  id?: number
  noticeId: number
  channel: string
  status: number
  targetCount: number
  successCount: number
  errorMsg?: string
  sendTime: string
}

// ==================== 即时聊天 ====================
export interface ChatMessage {
  id?: number
  senderId: number
  senderName?: string
  senderAvatar?: string
  receiverId: number
  content: string
  msgType: number
  isRead?: number
  sendTime?: string
}

export interface ChatUser {
  id: number
  username: string
  nickname: string
  avatar?: string
  lastMessage?: string
  lastMessageTime?: string
  isBlocked?: boolean
}

export const chatApi = {
  // 发送消息
  send(data: { receiverId: number; content: string; msgType?: number }): Promise<ChatMessage> {
    return request({ url: '/sys/chat/send', method: 'post', data })
  },
  
  // 获取聊天记录
  getHistory(targetId: number, params: { page: number; pageSize: number }): Promise<PageResult<ChatMessage>> {
    return request({ url: `/sys/chat/history/${targetId}`, method: 'get', params })
  },
  
  // 获取最近联系人
  getContacts(): Promise<ChatMessage[]> {
    return request({ url: '/sys/chat/contacts', method: 'get' })
  },
  
  // 获取用户列表
  getUsers(): Promise<ChatUser[]> {
    return request({ url: '/sys/chat/users', method: 'get' })
  },
  
  // 标记已读
  markAsRead(senderId: number): Promise<void> {
    return request({ url: `/sys/chat/read/${senderId}`, method: 'post' })
  },
  
  // 获取未读数量
  getUnreadCount(): Promise<number> {
    return request({ url: '/sys/chat/unread-count', method: 'get' })
  },
  
  // 检查用户是否在线
  isOnline(userId: number): Promise<boolean> {
    return request({ url: `/sys/chat/online/${userId}`, method: 'get' })
  },
  
  // 清空聊天记录
  clearHistory(targetId: number): Promise<void> {
    return request({ url: `/sys/chat/clear/${targetId}`, method: 'delete' })
  },
  
  // 拉黑用户
  blockUser(targetId: number): Promise<void> {
    return request({ url: `/sys/chat/block/${targetId}`, method: 'post' })
  },
  
  // 取消拉黑
  unblockUser(targetId: number): Promise<void> {
    return request({ url: `/sys/chat/block/${targetId}`, method: 'delete' })
  },
  
  // 获取黑名单列表
  getBlacklist(): Promise<any[]> {
    return request({ url: '/sys/chat/blacklist', method: 'get' })
  },
  
  // 检查是否拉黑
  isBlocked(targetId: number): Promise<boolean> {
    return request({ url: `/sys/chat/blocked/${targetId}`, method: 'get' })
  }
}

// ==================== 群聊 ====================
export interface ChatGroup {
  id?: number
  name: string
  avatar?: string
  ownerId: number
  ownerName?: string
  announcement?: string
  maxMembers?: number
  status?: number
  memberCount?: number
  members?: ChatGroupMember[]
  createTime?: string
  updateTime?: string
  lastMessage?: string
  lastMessageTime?: string
}

export interface ChatGroupMember {
  id?: number
  groupId: number
  userId: number
  nickname?: string
  role: number // 0-普通成员 1-管理员 2-群主
  muted?: number
  joinTime?: string
  username?: string
  userNickname?: string
  avatar?: string
}

export interface ChatGroupMessage {
  id?: number
  groupId: number
  senderId: number
  senderName?: string
  senderAvatar?: string
  content: string
  msgType: number
  sendTime?: string
}

export const groupChatApi = {
  // 创建群聊
  create(data: { name: string; memberIds: number[] }): Promise<ChatGroup> {
    return request({ url: '/chat/group/create', method: 'post', data })
  },
  
  // 获取我的群列表
  list(): Promise<ChatGroup[]> {
    return request({ url: '/chat/group/list', method: 'get' })
  },
  
  // 获取群详情
  detail(groupId: number): Promise<ChatGroup> {
    return request({ url: `/chat/group/${groupId}`, method: 'get' })
  },
  
  // 更新群信息
  update(data: ChatGroup): Promise<void> {
    return request({ url: '/chat/group/update', method: 'put', data })
  },
  
  // 解散群聊
  dissolve(groupId: number): Promise<void> {
    return request({ url: `/chat/group/${groupId}`, method: 'delete' })
  },
  
  // 退出群聊
  quit(groupId: number): Promise<void> {
    return request({ url: `/chat/group/${groupId}/quit`, method: 'post' })
  },
  
  // 获取群成员
  members(groupId: number): Promise<ChatGroupMember[]> {
    return request({ url: `/chat/group/${groupId}/members`, method: 'get' })
  },
  
  // 添加成员
  addMembers(groupId: number, userIds: number[]): Promise<void> {
    return request({ url: `/chat/group/${groupId}/members`, method: 'post', data: { userIds } })
  },
  
  // 移除成员
  removeMember(groupId: number, memberId: number): Promise<void> {
    return request({ url: `/chat/group/${groupId}/members/${memberId}`, method: 'delete' })
  },
  
  // 设置管理员
  setAdmin(groupId: number, memberId: number, isAdmin: boolean): Promise<void> {
    return request({ url: `/chat/group/${groupId}/admin/${memberId}`, method: 'post', params: { isAdmin } })
  },
  
  // 设置禁言
  setMuted(groupId: number, memberId: number, muted: boolean): Promise<void> {
    return request({ url: `/chat/group/${groupId}/mute/${memberId}`, method: 'post', params: { muted } })
  },
  
  // 转让群主
  transferOwner(groupId: number, newOwnerId: number): Promise<void> {
    return request({ url: `/chat/group/${groupId}/transfer/${newOwnerId}`, method: 'post' })
  },
  
  // 发送消息
  sendMessage(groupId: number, content: string, msgType?: number): Promise<ChatGroupMessage> {
    return request({ url: `/chat/group/${groupId}/message`, method: 'post', data: { content, msgType } })
  },
  
  // 获取消息历史
  getMessages(groupId: number, page: number = 1, pageSize: number = 50): Promise<PageResult<ChatGroupMessage>> {
    return request({ url: `/chat/group/${groupId}/messages`, method: 'get', params: { page, pageSize } })
  }
}
