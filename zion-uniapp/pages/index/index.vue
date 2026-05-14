<template>
	<view class="page">
		<view class="search-bar">
			<view class="search-input" @tap="navigateToSearch">
				<u-icon name="search" color="rgba(255,255,255,0.55)" size="15"></u-icon>
				<text class="search-placeholder">搜索</text>
			</view>
			<view class="add-btn" @tap="showAddMenu = !showAddMenu">
				<u-icon name="plus-circle" color="rgba(255,255,255,0.9)" size="22"></u-icon>
			</view>
		</view>

		<view class="add-menu" v-if="showAddMenu" @tap="showAddMenu = false">
			<view class="menu-content" @tap.stop>
				<view class="menu-item" @tap="handleCreateGroup">
					<text class="menu-icon-text">群</text>
					<text>发起群聊</text>
				</view>
			</view>
		</view>

		<scroll-view scroll-y class="conversation-list" @scrolltolower="loadMore"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

			<view class="conversation-item" v-for="item in conversations" :key="'c-' + item.contactId"
				@tap="openChat(item)" @longpress="handleLongPress(item)">
				<view class="avatar-wrap">
					<image v-if="item.avatar" class="conv-avatar" :src="item.avatar" mode="aspectFill"></image>
					<view v-else class="conv-avatar avatar-text" :style="{ background: getAvatarColor(item.nickname || item.username) }">
						<text>{{ getFirstChar(item.nickname || item.username) }}</text>
					</view>
					<view class="avatar-badge" v-if="item.unreadCount > 0">
						<text>{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</text>
					</view>
				</view>
				<view class="conv-info">
					<view class="conv-top">
						<text class="conv-name">{{ item.nickname || item.username }}</text>
						<text class="conv-time">{{ formatTime(item.sendTime) }}</text>
					</view>
					<view class="conv-bottom">
						<text class="conv-msg">{{ item.lastMessage }}</text>
					</view>
				</view>
			</view>

			<view class="conversation-item" v-for="group in groups" :key="'g-' + group.id" @tap="openGroupChat(group)">
				<view class="avatar-wrap">
					<image v-if="group.avatar" class="conv-avatar" :src="group.avatar" mode="aspectFill"></image>
					<view v-else class="conv-avatar avatar-text" :style="{ background: getAvatarColor(group.name) }">
						<text>{{ getFirstChar(group.name) }}</text>
					</view>
					<view class="avatar-badge" v-if="group.unreadCount > 0">
						<text>{{ group.unreadCount > 99 ? '99+' : group.unreadCount }}</text>
					</view>
				</view>
				<view class="conv-info">
					<view class="conv-top">
						<text class="conv-name">{{ group.name }}</text>
						<text class="conv-time">{{ formatTime(group.lastMessageTime) }}</text>
					</view>
					<view class="conv-bottom">
						<text class="conv-msg">{{ group.lastMessage || '暂无消息' }}</text>
					</view>
				</view>
			</view>

			<view class="empty-state" v-if="conversations.length === 0 && groups.length === 0 && !loading">
				<u-icon name="chat" color="#D0D0D0" size="56"></u-icon>
				<text class="empty-text">暂无消息</text>
				<text class="empty-hint">去通讯录找人聊天吧</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getRecentContacts, getGroupList, getChatUsers } from '../../utils/api.js'
	import { getUserInfo, checkLogin } from '../../utils/auth.js'
	import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				conversations: [],
				groups: [],
				loading: false,
				refreshing: false,
				showAddMenu: false,
				userInfo: null
			}
		},
		onShow() {
			if (!checkLogin()) return
			this.userInfo = getUserInfo()
			this.loadData()
			this.setupWebSocket()
		},
		onHide() {
			this.removeWebSocketListeners()
		},
		methods: {
			getFirstChar(name) {
				if (!name) return '?'
				return name.charAt(0).toUpperCase()
			},
			getAvatarColor(name) {
				const colors = ['#25B7D3', '#F56C6C', '#E6A23C', '#67C23A', '#409EFF', '#9B59B6', '#1ABC9C', '#E74C3C', '#3498DB', '#2ECC71']
				if (!name) return colors[0]
				let hash = 0
				for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
				return colors[Math.abs(hash) % colors.length]
			},
			// 按时间倒序排列会话
			sortConversations() {
				this.conversations.sort((a, b) => {
					const ta = a.sendTime ? new Date(a.sendTime).getTime() : 0
					const tb = b.sendTime ? new Date(b.sendTime).getTime() : 0
					return tb - ta
				})
			},
			async loadData() {
				this.loading = true
				try {
					const [contactsRes, groupsRes, usersRes] = await Promise.all([
						getRecentContacts().catch(() => ({ data: [] })),
						getGroupList().catch(() => ({ data: [] })),
						getChatUsers().catch(() => ({ data: [] }))
					])
					// 构建用户信息查找表
					const userMap = {}
					if (usersRes.data && Array.isArray(usersRes.data)) {
						usersRes.data.forEach(u => {
							userMap[String(u.id)] = u
						})
					}
					const userId = this.userInfo?.userId
					if (contactsRes.data && Array.isArray(contactsRes.data)) {
						// 保留当前的未读数
						const oldUnreadMap = {}
						this.conversations.forEach(c => {
							if (c.unreadCount > 0) oldUnreadMap[String(c.contactId)] = c.unreadCount
						})
						this.conversations = contactsRes.data.map(msg => {
							const isMe = String(msg.senderId) === String(userId)
							const otherId = isMe ? msg.receiverId : msg.senderId
							const otherUser = userMap[String(otherId)] || {}
							const otherName = isMe
								? (msg.receiverName || msg.receiverNickname || otherUser.nickname || otherUser.username || '')
								: (msg.senderName || '')
							const otherAvatar = isMe
								? (msg.receiverAvatar || otherUser.avatar || '')
								: (msg.senderAvatar || '')
							return {
								contactId: otherId,
								nickname: otherName,
								avatar: otherAvatar,
								lastMessage: msg.msgType === 2 ? '[图片]' : msg.content,
								sendTime: msg.sendTime,
								unreadCount: oldUnreadMap[String(otherId)] || 0
							}
						})
						this.sortConversations()
					}
					if (groupsRes.data && Array.isArray(groupsRes.data)) this.groups = groupsRes.data
				} catch (err) { console.error(err) }
				finally { this.loading = false; this.refreshing = false }
			},
			setupWebSocket() {
				// 先移除旧的监听，防止重复
				this.removeWebSocketListeners()
				this.chatHandler = (data) => {
					const senderId = data.senderId
					const senderName = data.senderName || ''
					const senderAvatar = data.senderAvatar || ''
					const content = data.msgType === 2 ? '[图片]' : (data.content || '')
					const now = new Date().toISOString()
					const idx = this.conversations.findIndex(c => String(c.contactId) === String(senderId))
					if (idx > -1) {
						const conv = this.conversations[idx]
						conv.lastMessage = content
						conv.sendTime = now
						conv.unreadCount = (conv.unreadCount || 0) + 1
						if (senderName && !conv.nickname) conv.nickname = senderName
						if (senderAvatar && !conv.avatar) conv.avatar = senderAvatar
						// 将这个会话移到最前面
						this.conversations.splice(idx, 1)
						this.conversations.unshift(conv)
					} else {
						this.conversations.unshift({
							contactId: senderId,
							nickname: senderName,
							avatar: senderAvatar,
							lastMessage: content,
							sendTime: now,
							unreadCount: 1
						})
					}
					// 触发视图更新
					this.conversations = [...this.conversations]
					uni.vibrateShort()
				}
				this.groupChatHandler = (data) => {
					const groupId = data.groupId
					const content = data.msgType === 2 ? '[图片]' : (data.content || '')
					const now = new Date().toISOString()
					const idx = this.groups.findIndex(g => String(g.id) === String(groupId))
					if (idx > -1) {
						this.groups[idx].lastMessage = content
						this.groups[idx].lastMessageTime = now
						this.groups[idx].unreadCount = (this.groups[idx].unreadCount || 0) + 1
						this.groups = [...this.groups]
					}
					uni.vibrateShort()
				}
				wsClient.on('chat', this.chatHandler)
				wsClient.on('groupChat', this.groupChatHandler)
			},
			removeWebSocketListeners() {
				if (this.chatHandler) { wsClient.off('chat', this.chatHandler); this.chatHandler = null }
				if (this.groupChatHandler) { wsClient.off('groupChat', this.groupChatHandler); this.groupChatHandler = null }
			},
			onRefresh() { this.refreshing = true; this.loadData() },
			loadMore() {},
			openChat(item) {
				item.unreadCount = 0
				this.conversations = [...this.conversations]
				uni.navigateTo({ url: `/pages/chat/index?targetId=${item.contactId}&name=${encodeURIComponent(item.nickname || '聊天')}&avatar=${encodeURIComponent(item.avatar || '')}` })
			},
			openGroupChat(group) {
				group.unreadCount = 0
				this.groups = [...this.groups]
				uni.navigateTo({ url: `/pages/group-chat/index?groupId=${group.id}&name=${encodeURIComponent(group.name)}` })
			},
			handleCreateGroup() { this.showAddMenu = false; uni.navigateTo({ url: '/pages/group/create' }) },
			navigateToSearch() { uni.showToast({ title: '搜索功能开发中', icon: 'none' }) },
			handleLongPress(item) {
				uni.showActionSheet({
					itemList: ['删除会话', '标记已读'],
					success: (res) => {
						if (res.tapIndex === 0) {
							this.conversations = this.conversations.filter(c => c.contactId !== item.contactId)
						} else if (res.tapIndex === 1) {
							item.unreadCount = 0
							this.conversations = [...this.conversations]
						}
					}
				})
			},
			formatTime(time) {
				if (!time) return ''
				const d = new Date(time), now = new Date(), diff = now - d
				if (diff < 60000) return '刚刚'
				if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
				if (diff < 86400000) return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
				if (diff < 172800000) return '昨天'
				if (diff < 604800000) return '周' + ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
				return `${d.getMonth() + 1}/${d.getDate()}`
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { height: 100vh; display: flex; flex-direction: column; background: #EDEDED; }

	.search-bar { display: flex; align-items: center; padding: 12rpx 20rpx; background: #07C160; gap: 12rpx; }
	.search-input {
		flex: 1; display: flex; align-items: center; height: 60rpx;
		background: rgba(255,255,255,0.18); border-radius: 6rpx; padding: 0 16rpx;
	}
	.search-placeholder { font-size: 24rpx; color: rgba(255,255,255,0.55); margin-left: 8rpx; }
	.add-btn { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; }

	.add-menu { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 100; }
	.menu-content {
		position: absolute; top: calc(var(--status-bar-height) + 90rpx); right: 20rpx;
		background: #4C4C4C; border-radius: 6rpx; padding: 4rpx 0; min-width: 260rpx;
	}
	.menu-item {
		display: flex; align-items: center; padding: 20rpx 28rpx;
		text { font-size: 26rpx; color: #FFF; }
	}
	.menu-icon-text {
		width: 36rpx; height: 36rpx; border-radius: 6rpx;
		background: #07C160; font-size: 20rpx; color: #FFF; font-weight: 600;
		display: inline-flex; align-items: center; justify-content: center; margin-right: 14rpx;
	}

	.conversation-list { flex: 1; overflow: hidden; }
	.conversation-item {
		display: flex; align-items: center; padding: 22rpx 24rpx;
		background: #FFF; border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #ECECEC; }
	}

	.avatar-wrap {
		position: relative; margin-right: 20rpx; flex-shrink: 0;
	}
	.conv-avatar { width: 84rpx; height: 84rpx; border-radius: 8rpx; background: #E0E0E0; }
	.avatar-text {
		display: flex; align-items: center; justify-content: center;
		text { font-size: 36rpx; color: #FFF; font-weight: 600; }
	}
	.avatar-badge {
		position: absolute; top: -8rpx; right: -8rpx; z-index: 2;
		min-width: 32rpx; height: 32rpx; padding: 0 8rpx; border-radius: 32rpx;
		background: #FA5151; border: 3rpx solid #FFF;
		display: flex; align-items: center; justify-content: center;
		text { font-size: 18rpx; color: #FFF; font-weight: 600; line-height: 1; }
	}
	.conv-info { flex: 1; min-width: 0; }
	.conv-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6rpx; }
	.conv-name { font-size: 28rpx; font-weight: 500; color: #1A1A1A; }
	.conv-time { font-size: 20rpx; color: #B0B0B0; flex-shrink: 0; margin-left: 12rpx; }
	.conv-bottom { display: flex; align-items: center; justify-content: space-between; }
	.conv-msg { font-size: 24rpx; color: #999; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 140rpx 40rpx;
		.empty-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }
		.empty-hint { font-size: 24rpx; color: #CCC; margin-top: 6rpx; }
	}
</style>
