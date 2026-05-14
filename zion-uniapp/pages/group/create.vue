<template>
	<view class="page">
		<view class="form-section">
			<view class="form-item">
				<text class="form-label">群名称</text>
				<input class="form-input" v-model="groupName" placeholder="请输入群名称"
					placeholder-class="form-placeholder" maxlength="30" />
			</view>
		</view>

		<view class="section-header">
			<text class="section-title">选择群成员</text>
			<text class="selected-count">已选 {{ selectedIds.length }} 人</text>
		</view>

		<view class="search-wrap">
			<u-icon name="search" color="#999" size="28" style="margin-right: 12rpx;"></u-icon>
			<input class="search-input" v-model="searchText" placeholder="搜索联系人" placeholder-class="form-placeholder" />
		</view>

		<scroll-view scroll-y class="user-list">
			<view class="user-item" v-for="user in filteredUsers" :key="user.id" @tap="toggleSelect(user)">
				<view class="check-box" :class="{ checked: isSelected(user.id) }">
					<u-icon v-if="isSelected(user.id)" name="checkmark" color="#FFFFFF" size="24"></u-icon>
				</view>
				<image class="user-avatar" :src="user.avatar || '/static/default-avatar.png'" mode="aspectFill"></image>
				<text class="user-name">{{ user.nickname || user.username }}</text>
			</view>
			<view class="empty-state" v-if="filteredUsers.length === 0 && !loading">
				<text class="empty-text">暂无可选联系人</text>
			</view>
		</scroll-view>

		<view class="bottom-bar safe-area-bottom">
			<button class="create-btn" :class="{ 'btn-disabled': !canCreate }"
				:disabled="!canCreate" :loading="creating" @tap="handleCreate">
				创建群聊{{ selectedIds.length > 0 ? ` (${selectedIds.length})` : '' }}
			</button>
		</view>
	</view>
</template>

<script>
	import { getChatUsers, createGroup } from '../../utils/api.js'

	export default {
		data() { return { groupName: '', users: [], selectedIds: [], searchText: '', loading: false, creating: false } },
		computed: {
			filteredUsers() {
				if (!this.searchText) return this.users
				const kw = this.searchText.toLowerCase()
				return this.users.filter(u => (u.nickname && u.nickname.toLowerCase().includes(kw)) || (u.username && u.username.toLowerCase().includes(kw)))
			},
			canCreate() { return this.groupName.trim() && this.selectedIds.length >= 2 }
		},
		onLoad() { this.loadUsers() },
		methods: {
			async loadUsers() {
				this.loading = true
				try { const res = await getChatUsers(); if (res.data && Array.isArray(res.data)) this.users = res.data }
				catch (err) { console.error('加载用户列表失败:', err) } finally { this.loading = false }
			},
			isSelected(id) { return this.selectedIds.includes(id) },
			toggleSelect(user) {
				const idx = this.selectedIds.indexOf(user.id)
				if (idx > -1) this.selectedIds.splice(idx, 1); else this.selectedIds.push(user.id)
			},
			async handleCreate() {
				if (!this.canCreate || this.creating) return; this.creating = true
				try {
					const res = await createGroup({ name: this.groupName.trim(), memberIds: this.selectedIds })
					uni.showToast({ title: '群聊创建成功', icon: 'success' })
					setTimeout(() => { const g = res.data; uni.redirectTo({ url: `/pages/group-chat/index?groupId=${g.id}&name=${encodeURIComponent(g.name)}` }) }, 500)
				} catch (err) { console.error('创建群聊失败:', err) } finally { this.creating = false }
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { height: 100vh; display: flex; flex-direction: column; background: #EDEDED; }
	.form-section { background: #FFF; margin-bottom: 16rpx; }
	.form-item { display: flex; align-items: center; padding: 24rpx 32rpx; }
	.form-label { font-size: 30rpx; color: #1A1A1A; font-weight: 500; width: 160rpx; flex-shrink: 0; }
	.form-input { flex: 1; height: 72rpx; font-size: 28rpx; color: #1A1A1A; }
	.form-placeholder { color: #CCC; }
	.section-header { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 32rpx; }
	.section-title { font-size: 28rpx; color: #666; font-weight: 500; }
	.selected-count { font-size: 26rpx; color: #07C160; font-weight: 500; }

	.search-wrap {
		display: flex; align-items: center; margin: 0 24rpx 16rpx;
		height: 68rpx; background: #FFF; border-radius: 8rpx; padding: 0 20rpx;
	}
	.search-input { flex: 1; height: 68rpx; font-size: 28rpx; }

	.user-list { flex: 1; overflow: hidden; }
	.user-item {
		display: flex; align-items: center; padding: 20rpx 32rpx;
		background: #FFF; border-bottom: 1rpx solid #F0F0F0;
		&:active { background: #F0F0F0; }
	}
	.check-box {
		width: 44rpx; height: 44rpx; border-radius: 50%; border: 3rpx solid #CCC;
		display: flex; align-items: center; justify-content: center; margin-right: 20rpx;
	}
	.checked { background: #07C160; border-color: #07C160; }
	.user-avatar { width: 72rpx; height: 72rpx; border-radius: 8rpx; margin-right: 20rpx; background: #E0E0E0; }
	.user-name { font-size: 30rpx; color: #1A1A1A; font-weight: 500; }

	.bottom-bar { padding: 20rpx 32rpx; background: #FFF; border-top: 1rpx solid #F0F0F0; }
	.create-btn {
		height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #059B4B, #07C160);
		color: #FFF; font-size: 32rpx; font-weight: 600; border-radius: 8rpx; border: none;
	}
	.create-btn::after { border: none; }
	.btn-disabled { background: #C0C0C0 !important; }
	.empty-state { padding: 80rpx 40rpx; text-align: center; }
	.empty-text { font-size: 28rpx; color: #999; }
</style>
