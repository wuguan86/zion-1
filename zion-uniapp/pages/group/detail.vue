<template>
	<view class="page">
		<scroll-view scroll-y class="content">
			<view class="group-header card">
				<image class="group-avatar" :src="group.avatar || '/static/default-avatar.png'" mode="aspectFill"></image>
				<view class="group-info">
					<text class="group-name">{{ group.name }}</text>
					<text class="group-member-count">{{ members.length }} 人</text>
				</view>
			</view>

			<view class="card">
				<view class="card-title">
					<text>群成员</text>
					<text class="more-text" @tap="showAllMembers = !showAllMembers">{{ showAllMembers ? '收起' : '查看全部' }}</text>
				</view>
				<view class="member-grid">
					<view class="member-item" v-for="member in displayMembers" :key="member.id" @tap="handleMemberTap(member)">
						<image class="member-avatar" :src="member.avatar || '/static/default-avatar.png'" mode="aspectFill"></image>
						<text class="member-name">{{ member.nickname || member.userNickname }}</text>
						<text class="member-role" v-if="member.role === 2">群主</text>
						<text class="member-role admin" v-else-if="member.role === 1">管理</text>
					</view>
					<view class="member-item add-member" @tap="handleAddMember" v-if="isOwnerOrAdmin">
						<view class="add-member-icon">
							<u-icon name="plus" color="#999" size="36"></u-icon>
						</view>
						<text class="member-name">添加</text>
					</view>
				</view>
			</view>

			<view class="card">
				<view class="card-title">
					<text>群公告</text>
					<text class="edit-text" v-if="isOwnerOrAdmin" @tap="editAnnouncement">编辑</text>
				</view>
				<text class="announcement-text">{{ group.announcement || '暂无群公告' }}</text>
			</view>

			<view class="action-section">
				<view class="action-item" @tap="handleClearHistory">
					<u-icon name="trash" color="#666" size="32" style="margin-right: 12rpx;"></u-icon>
					<text class="action-text">清空聊天记录</text>
				</view>
				<view class="action-item danger" v-if="!isOwner" @tap="handleQuitGroup">
					<u-icon name="minus-circle" color="#FA5151" size="32" style="margin-right: 12rpx;"></u-icon>
					<text class="action-text">退出群聊</text>
				</view>
				<view class="action-item danger" v-if="isOwner" @tap="handleDissolveGroup">
					<u-icon name="close-circle" color="#FA5151" size="32" style="margin-right: 12rpx;"></u-icon>
					<text class="action-text">解散群聊</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { getGroupDetail, getGroupMembers, quitGroup, dissolveGroup, updateGroup } from '../../utils/api.js'
	import { getUserInfo } from '../../utils/auth.js'

	export default {
		data() { return { groupId: 0, group: {}, members: [], showAllMembers: false, myUserId: 0 } },
		computed: {
			isOwner() { return this.group.ownerId === this.myUserId },
			isOwnerOrAdmin() { const me = this.members.find(m => m.userId === this.myUserId); return me && me.role >= 1 },
			displayMembers() { return this.showAllMembers ? this.members : this.members.slice(0, 15) }
		},
		onLoad(options) {
			this.groupId = Number(options.groupId); this.myUserId = getUserInfo()?.userId || 0
			this.loadGroupInfo()
		},
		methods: {
			async loadGroupInfo() {
				try {
					const [groupRes, membersRes] = await Promise.all([getGroupDetail(this.groupId), getGroupMembers(this.groupId)])
					if (groupRes.data) this.group = groupRes.data
					if (membersRes.data) this.members = membersRes.data
				} catch (err) { console.error('加载群信息失败:', err) }
			},
			handleMemberTap(member) {
				if (member.userId === this.myUserId) return
				uni.showActionSheet({
					itemList: this.isOwnerOrAdmin ? ['发送消息','设为管理员','移出群聊'] : ['发送消息'],
					success: (res) => {
						if (res.tapIndex === 0) uni.navigateTo({ url: `/pages/chat/index?targetId=${member.userId}&name=${encodeURIComponent(member.userNickname || member.nickname)}&avatar=${encodeURIComponent(member.avatar || '')}` })
					}
				})
			},
			handleAddMember() { uni.showToast({ title: '添加成员功能开发中', icon: 'none' }) },
			editAnnouncement() {
				uni.showModal({ title: '编辑群公告', editable: true, placeholderText: '请输入群公告', content: this.group.announcement || '',
					success: async (res) => {
						if (res.confirm && res.content !== undefined) {
							try { await updateGroup({ id: this.groupId, announcement: res.content }); this.group.announcement = res.content; uni.showToast({ title: '公告已更新', icon: 'success' }) }
							catch (err) { console.error('更新公告失败:', err) }
						}
					}
				})
			},
			handleClearHistory() { uni.showModal({ title: '提示', content: '确定要清空聊天记录吗？', success: (res) => { if (res.confirm) uni.showToast({ title: '已清空', icon: 'success' }) } }) },
			handleQuitGroup() {
				uni.showModal({ title: '退出群聊', content: '确定要退出该群聊吗？', confirmColor: '#FA5151',
					success: async (res) => { if (res.confirm) { try { await quitGroup(this.groupId); uni.showToast({ title: '已退出群聊', icon: 'success' }); setTimeout(() => uni.navigateBack({ delta: 2 }), 500) } catch (err) {} } }
				})
			},
			handleDissolveGroup() {
				uni.showModal({ title: '解散群聊', content: '确定要解散该群聊吗？此操作不可撤销。', confirmColor: '#FA5151',
					success: async (res) => { if (res.confirm) { try { await dissolveGroup(this.groupId); uni.showToast({ title: '群聊已解散', icon: 'success' }); setTimeout(() => uni.navigateBack({ delta: 2 }), 500) } catch (err) {} } }
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page { height: 100vh; background: #EDEDED; }
	.content { height: 100%; }
	.card { background: #FFF; margin: 16rpx; border-radius: 8rpx; padding: 28rpx; }
	.group-header { display: flex; align-items: center; }
	.group-avatar { width: 100rpx; height: 100rpx; border-radius: 8rpx; margin-right: 24rpx; background: #E0E0E0; }
	.group-info { flex: 1; }
	.group-name { font-size: 34rpx; font-weight: 600; color: #1A1A1A; display: block; }
	.group-member-count { font-size: 26rpx; color: #999; margin-top: 8rpx; display: block; }

	.card-title {
		display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx;
		text { font-size: 30rpx; font-weight: 600; color: #1A1A1A; }
	}
	.more-text, .edit-text { font-size: 26rpx; color: #07C160; font-weight: 400 !important; }

	.member-grid { display: flex; flex-wrap: wrap; gap: 20rpx; }
	.member-item { display: flex; flex-direction: column; align-items: center; width: 110rpx; }
	.member-avatar { width: 80rpx; height: 80rpx; border-radius: 8rpx; background: #E0E0E0; }
	.member-name { font-size: 22rpx; color: #666; margin-top: 8rpx; max-width: 110rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; }
	.member-role { font-size: 18rpx; color: #07C160; background: #E8F8EE; padding: 2rpx 8rpx; border-radius: 4rpx; margin-top: 4rpx; }
	.member-role.admin { color: #3182CE; background: #EBF8FF; }
	.add-member .add-member-icon {
		width: 80rpx; height: 80rpx; border-radius: 8rpx; border: 3rpx dashed #CCC;
		display: flex; align-items: center; justify-content: center;
	}
	.announcement-text { font-size: 28rpx; color: #666; line-height: 1.6; }

	.action-section { margin: 32rpx 16rpx; }
	.action-item {
		display: flex; align-items: center; justify-content: center;
		background: #FFF; border-radius: 8rpx; padding: 28rpx 32rpx; margin-bottom: 16rpx;
		&:active { background: #F0F0F0; }
	}
	.action-text { font-size: 30rpx; color: #1A1A1A; }
	.danger .action-text { color: #FA5151; }
</style>
