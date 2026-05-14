<template>
	<view class="page">
		<view class="search-bar-wrap">
			<view class="search-input-area">
				<u-icon name="search" color="rgba(255,255,255,0.55)" size="15"></u-icon>
				<input class="search-input" v-model="searchText" placeholder="搜索"
					placeholder-class="search-placeholder" @input="handleSearch" />
			</view>
		</view>

		<scroll-view scroll-y class="contact-list"
			refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

			<!-- 功能入口 -->
			<view class="func-item" @tap="navigateToCreateGroup">
				<view class="func-icon green">
					<text class="func-icon-text">群</text>
				</view>
				<text class="func-name">群聊</text>
				<u-icon name="arrow-right" color="#CCC" size="14"></u-icon>
			</view>

			<!-- 联系人 -->
			<view class="section-title" v-if="filteredUsers.length > 0">
				<text>联系人 ({{ filteredUsers.length }})</text>
			</view>

			<view class="contact-item" v-for="user in filteredUsers" :key="user.id" @tap="openChat(user)">
				<image v-if="user.avatar" class="contact-avatar" :src="user.avatar" mode="aspectFill"></image>
				<view v-else class="contact-avatar avatar-text" :style="{ background: getAvatarColor(user.nickname || user.username) }">
					<text>{{ getFirstChar(user.nickname || user.username) }}</text>
				</view>
				<view class="contact-info">
					<text class="contact-name">{{ user.nickname || user.username }}</text>
				</view>
			</view>

			<!-- 群聊列表 -->
			<view class="section-title" v-if="groups.length > 0">
				<text>群聊 ({{ groups.length }})</text>
			</view>
			<view class="contact-item" v-for="group in groups" :key="'g-' + group.id" @tap="openGroupChat(group)">
				<image v-if="group.avatar" class="contact-avatar" :src="group.avatar" mode="aspectFill"></image>
				<view v-else class="contact-avatar avatar-text" :style="{ background: getAvatarColor(group.name) }">
					<text>{{ getFirstChar(group.name) }}</text>
				</view>
				<view class="contact-info">
					<text class="contact-name">{{ group.name }}</text>
					<text class="contact-desc" v-if="group.memberCount">{{ group.memberCount }}人</text>
				</view>
			</view>

			<view class="empty-state" v-if="filteredUsers.length === 0 && groups.length === 0 && !loading">
				<u-icon name="account" color="#D0D0D0" size="48"></u-icon>
				<text class="empty-text">暂无联系人</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getChatUsers, getGroupList } from '../../utils/api.js'
	import { checkLogin } from '../../utils/auth.js'

	export default {
		data() { return { users: [], groups: [], searchText: '', loading: false, refreshing: false } },
		computed: {
			filteredUsers() {
				if (!this.searchText) return this.users
				const kw = this.searchText.toLowerCase()
				return this.users.filter(u =>
					(u.nickname && u.nickname.toLowerCase().includes(kw)) ||
					(u.username && u.username.toLowerCase().includes(kw))
				)
			}
		},
		onShow() { if (!checkLogin()) return; this.loadData() },
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
			async loadData() {
				this.loading = true
				try {
					const [usersRes, groupsRes] = await Promise.all([
						getChatUsers().catch(() => ({ data: [] })),
						getGroupList().catch(() => ({ data: [] }))
					])
					if (usersRes.data && Array.isArray(usersRes.data)) this.users = usersRes.data
					if (groupsRes.data && Array.isArray(groupsRes.data)) this.groups = groupsRes.data
				} catch (err) { console.error(err) }
				finally { this.loading = false; this.refreshing = false }
			},
			onRefresh() { this.refreshing = true; this.loadData() },
			handleSearch() {},
			openChat(user) {
				uni.navigateTo({ url: `/pages/chat/index?targetId=${user.id}&name=${encodeURIComponent(user.nickname || user.username)}&avatar=${encodeURIComponent(user.avatar || '')}` })
			},
			openGroupChat(group) {
				uni.navigateTo({ url: `/pages/group-chat/index?groupId=${group.id}&name=${encodeURIComponent(group.name)}` })
			},
			navigateToCreateGroup() { uni.navigateTo({ url: '/pages/group/create' }) }
		}
	}
</script>

<style lang="scss" scoped>
	.page { height: 100vh; display: flex; flex-direction: column; background: #EDEDED; }

	.search-bar-wrap { padding: 12rpx 20rpx; background: #07C160; }
	.search-input-area {
		display: flex; align-items: center; height: 60rpx;
		background: rgba(255,255,255,0.18); border-radius: 6rpx; padding: 0 16rpx;
	}
	.search-input { flex: 1; height: 60rpx; font-size: 26rpx; color: #FFF; margin-left: 8rpx; }
	.search-placeholder { color: rgba(255,255,255,0.55); }

	.contact-list { flex: 1; overflow: hidden; }

	.func-item {
		display: flex; align-items: center; padding: 22rpx 28rpx;
		background: #FFF; border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #ECECEC; }
	}
	.func-icon {
		width: 44rpx; height: 44rpx; border-radius: 6rpx;
		display: flex; align-items: center; justify-content: center; margin-right: 18rpx;
	}
	.func-icon.green { background: linear-gradient(135deg, #06AD56, #07C160); }
	.func-icon-text { font-size: 24rpx; color: #FFF; font-weight: 600; }
	.func-name { flex: 1; font-size: 28rpx; color: #1A1A1A; }

	.section-title {
		padding: 18rpx 28rpx 8rpx;
		text { font-size: 22rpx; color: #999; }
	}

	.contact-item {
		display: flex; align-items: center; padding: 18rpx 28rpx;
		background: #FFF; border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #ECECEC; }
	}
	.contact-avatar { width: 72rpx; height: 72rpx; border-radius: 6rpx; background: #E0E0E0; margin-right: 18rpx; flex-shrink: 0; }
	.avatar-text {
		display: flex; align-items: center; justify-content: center;
		text { font-size: 32rpx; color: #FFF; font-weight: 600; }
	}
	.contact-info { flex: 1; min-width: 0; }
	.contact-name { font-size: 28rpx; color: #1A1A1A; display: block; }
	.contact-desc { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; }

	.empty-state {
		display: flex; flex-direction: column; align-items: center; padding: 120rpx 40rpx;
		.empty-text { font-size: 26rpx; color: #999; margin-top: 14rpx; }
	}
</style>
