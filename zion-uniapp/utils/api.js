/**
 * API 接口定义
 * 所有后端接口统一管理
 */
import { get, post, put, del, upload } from './request.js'

// ==================== 认证相关 ====================

/** 微信小程序登录 */
export const wxLogin = (data) => post('/api/app/auth/login', data)

/** 发送短信验证码 */
export const sendSmsCode = (data) => post('/api/app/auth/sms-code', data)

/** 获取当前用户信息 */
export const getUserInfo = () => get('/api/auth/info')

/** 获取个人资料 */
export const getProfile = () => get('/api/auth/profile')

/** 更新个人资料 */
export const updateProfile = (data) => put('/api/auth/profile', data)

/** App端获取个人资料 */
export const getAppProfile = () => get('/api/app/auth/profile')

/** App端更新个人资料（头像、昵称、邮箱、手机、性别） */
export const updateAppProfile = (data) => put('/api/app/auth/profile', data)

/** App端上传头像（无需文件管理权限） */
export const uploadAvatar = (filePath) => upload('/api/app/auth/upload-avatar', filePath)

/** 修改密码 */
export const changePassword = (data) => post('/api/auth/password', data)

/** App端修改密码 */
export const changeAppPassword = (data) => post('/api/app/auth/password', data)

/** 退出登录 */
export const logout = () => post('/api/auth/logout')

// ==================== 私聊相关 ====================

/** 发送私聊消息 */
export const sendMessage = (data) => post('/api/sys/chat/send', data)

/** 获取聊天记录 */
export const getChatHistory = (targetId, page = 1, pageSize = 20) =>
  get(`/api/sys/chat/history/${targetId}`, { page, pageSize })

/** 获取最近联系人 */
export const getRecentContacts = () => get('/api/sys/chat/contacts')

/** 获取用户列表（通讯录） */
export const getChatUsers = () => get('/api/sys/chat/users')

/** 标记消息已读 */
export const markAsRead = (senderId) => post(`/api/sys/chat/read/${senderId}`)

/** 获取未读消息数 */
export const getUnreadCount = () => get('/api/sys/chat/unread-count')

/** 获取消息统计 */
export const getMessageStats = () => get('/api/sys/chat/stats')

/** 检查用户是否在线 */
export const checkOnline = (userId) => get(`/api/sys/chat/online/${userId}`)

/** 清空聊天记录 */
export const clearChatHistory = (targetId) => del(`/api/sys/chat/clear/${targetId}`)

/** 拉黑用户 */
export const blockUser = (targetId) => post(`/api/sys/chat/block/${targetId}`)

/** 取消拉黑 */
export const unblockUser = (targetId) => del(`/api/sys/chat/block/${targetId}`)

/** 获取黑名单 */
export const getBlacklist = () => get('/api/sys/chat/blacklist')

/** 检查是否拉黑 */
export const checkBlocked = (targetId) => get(`/api/sys/chat/blocked/${targetId}`)

// ==================== 群聊相关 ====================

/** 创建群聊 */
export const createGroup = (data) => post('/api/chat/group/create', data)

/** 获取我的群列表 */
export const getGroupList = () => get('/api/chat/group/list')

/** 获取群详情 */
export const getGroupDetail = (groupId) => get(`/api/chat/group/${groupId}`)

/** 更新群信息 */
export const updateGroup = (data) => put('/api/chat/group/update', data)

/** 解散群聊 */
export const dissolveGroup = (groupId) => del(`/api/chat/group/${groupId}`)

/** 退出群聊 */
export const quitGroup = (groupId) => post(`/api/chat/group/${groupId}/quit`)

/** 获取群成员列表 */
export const getGroupMembers = (groupId) => get(`/api/chat/group/${groupId}/members`)

/** 添加群成员 */
export const addGroupMembers = (groupId, userIds) =>
  post(`/api/chat/group/${groupId}/members`, { userIds })

/** 移除群成员 */
export const removeGroupMember = (groupId, memberId) =>
  del(`/api/chat/group/${groupId}/members/${memberId}`)

/** 发送群消息 */
export const sendGroupMessage = (groupId, data) =>
  post(`/api/chat/group/${groupId}/message`, data)

/** 获取群消息历史 */
export const getGroupMessages = (groupId, page = 1, pageSize = 50) =>
  get(`/api/chat/group/${groupId}/messages`, { page, pageSize })

/** 转让群主 */
export const transferGroupOwner = (groupId, newOwnerId) =>
  post(`/api/chat/group/${groupId}/transfer/${newOwnerId}`)

// ==================== 文件相关 ====================

/** 上传文件 */
export const uploadFile = (filePath) => upload('/api/sys/file/upload', filePath)

// ==================== 系统通知 ====================

/** 获取通知列表 */
export const getNoticeList = (page = 1, pageSize = 10) =>
  get('/api/sys/notice/my', { page, pageSize })

/** 标记通知已读 */
export const readNotice = (id) => post(`/api/sys/notice/read/${id}`)
