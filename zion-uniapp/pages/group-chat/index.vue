<template>
	<view class="chat-page">
		<scroll-view class="message-list" scroll-y :scroll-into-view="scrollToId"
			:scroll-with-animation="true" @scrolltoupper="loadMoreHistory" @tap="hideExtra">
			<view class="loading-wrap" v-if="hasMore" @tap="loadMoreHistory">
				<text>{{ loadingMore ? '加载中...' : '查看更多消息' }}</text>
			</view>

			<view v-for="(msg, index) in messages" :key="msg.id" :id="'msg-' + msg.id" class="message-item">
				<view class="time-divider" v-if="showTimeDivider(index)">
					<text>{{ formatFullTime(msg.sendTime) }}</text>
				</view>

				<!-- 系统消息 -->
				<view class="system-msg" v-if="msg.msgType === 4">
					<text>{{ msg.content }}</text>
				</view>

				<!-- 对方消息：头像在左，气泡在右 -->
				<view class="message-row msg-row-other" v-else-if="!isSelf(msg.senderId)">
					<image v-if="msg.senderAvatar" class="msg-avatar"
						:src="msg.senderAvatar" mode="aspectFill"></image>
					<view v-else class="msg-avatar avatar-text"
						:style="{ background: getAvatarColor(msg.senderName) }">
						<text>{{ getFirstChar(msg.senderName) }}</text>
					</view>
					<view class="bubble-box">
						<text class="msg-name">{{ msg.senderName }}</text>
						<view class="bubble-inner">
							<view class="bubble-arrow-left"></view>
							<view class="msg-bubble bubble-other" @longpress="handleMsgLongPress(msg)">
								<text class="msg-text" v-if="msg.msgType === 1" selectable>{{ msg.content }}</text>
								<image v-else-if="msg.msgType === 2" class="msg-image" :src="msg.content"
									mode="widthFix" @tap="previewImage(msg.content)"></image>
							</view>
						</view>
					</view>
				</view>

				<!-- 自己消息：气泡在左，头像在右 -->
				<view class="message-row msg-row-self" v-else>
					<view class="bubble-box bubble-box-self">
						<view class="bubble-inner">
							<view class="msg-bubble bubble-self" @longpress="handleMsgLongPress(msg)">
								<text class="msg-text" v-if="msg.msgType === 1" selectable>{{ msg.content }}</text>
								<image v-else-if="msg.msgType === 2" class="msg-image" :src="msg.content"
									mode="widthFix" @tap="previewImage(msg.content)"></image>
							</view>
							<view class="bubble-arrow-right"></view>
						</view>
					</view>
					<image v-if="myAvatar" class="msg-avatar"
						:src="myAvatar" mode="aspectFill"></image>
					<view v-else class="msg-avatar avatar-text"
						:style="{ background: getAvatarColor(myName) }">
						<text>{{ getFirstChar(myName) }}</text>
					</view>
				</view>
			</view>
			<view id="msg-bottom" style="height: 20rpx;"></view>
		</scroll-view>

		<!-- 微信风格底部输入栏 -->
		<view class="input-bar safe-area-bottom">
			<view class="input-bar-main">
				<view class="bar-icon-btn" @tap="toggleVoice">
					<u-icon :name="isVoiceMode ? 'edit-pen' : 'mic'" color="#181818" size="28"></u-icon>
				</view>

				<view v-if="isVoiceMode" class="voice-btn"
					@touchstart="onVoiceStart" @touchend="onVoiceEnd" @touchcancel="onVoiceEnd">
					<text>{{ isRecording ? '松开 结束' : '按住 说话' }}</text>
				</view>

				<view v-else class="input-wrap">
					<textarea class="msg-input" v-model="inputText" placeholder="输入消息"
						placeholder-class="input-placeholder" :auto-height="true"
						:show-confirm-bar="false" confirm-type="send"
						:adjust-position="true" @confirm="handleSend" @focus="onInputFocus"
						:maxlength="-1" />
				</view>

				<view class="bar-icon-btn" @tap="toggleEmoji">
					<u-icon :name="showEmojiPanel ? 'edit-pen' : 'red-packet'" color="#181818" size="28"></u-icon>
				</view>

				<view v-if="inputText.trim()" class="send-btn-new" @tap="handleSend">
					<text>发送</text>
				</view>
				<view v-else class="bar-icon-btn" @tap="toggleExtra">
					<u-icon name="plus-circle" color="#181818" size="30"></u-icon>
				</view>
			</view>

			<view class="emoji-panel" v-if="showEmojiPanel">
				<scroll-view scroll-y class="emoji-scroll">
					<view class="emoji-grid">
						<view class="emoji-item" v-for="(emoji, idx) in emojis" :key="idx" @tap="insertEmoji(emoji)">
							<text>{{ emoji }}</text>
						</view>
					</view>
				</scroll-view>
			</view>

			<view class="extra-panel" v-if="showExtraPanel">
				<view class="extra-grid">
					<view class="extra-item" @tap="handleChooseImage">
						<view class="extra-icon" style="background: #3CC51F;">
							<u-icon name="photo" color="#FFF" size="28"></u-icon>
						</view>
						<text class="extra-label">相册</text>
					</view>
					<view class="extra-item" @tap="handleTakePhoto">
						<view class="extra-icon" style="background: #3CC51F;">
							<u-icon name="camera" color="#FFF" size="28"></u-icon>
						</view>
						<text class="extra-label">拍摄</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getGroupMessages, sendGroupMessage, uploadFile } from '../../utils/api.js'
	import { getUserInfo } from '../../utils/auth.js'
	import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				groupId: 0, groupName: '', myUserId: 0, myAvatar: '', myName: '',
				messages: [], inputText: '', scrollToId: '', page: 1, hasMore: true, loadingMore: false,
				isVoiceMode: false, isRecording: false,
				showEmojiPanel: false, showExtraPanel: false,
				emojis: ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','🥰','😘','😗','😙','😚','🙂','🤗','🤔','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','😤','😢','😭','😦','😧','😨','😩','🤯','😬','😰','😱','🥵','🥶','😳','🤪','😵','😡','😠','🤬','😷','🤒','🤕','🤢','🤮','🤧','😇','🥳','🥺','🤡','👍','👎','👌','✌️','🤞','👋','🙏','💪','❤️','💔','💯','🔥','⭐','🎉','🎊','💐']
			}
		},
		onLoad(options) {
			this.groupId = Number(options.groupId)
			this.groupName = decodeURIComponent(options.name || '群聊')
			const u = getUserInfo()
			this.myUserId = u?.userId || 0
			this.myAvatar = u?.avatar || ''
			this.myName = u?.nickname || u?.username || '我'
			uni.setNavigationBarTitle({ title: this.groupName })
			this.loadMessages(); this.setupWebSocket()
		},
		onUnload() { this.removeWebSocketListeners() },
		methods: {
			isSelf(senderId) {
				return String(senderId) === String(this.myUserId)
			},
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
			toggleVoice() {
				this.isVoiceMode = !this.isVoiceMode
				this.showEmojiPanel = false
				this.showExtraPanel = false
			},
			toggleEmoji() {
				this.showEmojiPanel = !this.showEmojiPanel
				this.showExtraPanel = false
				this.isVoiceMode = false
			},
			toggleExtra() {
				this.showExtraPanel = !this.showExtraPanel
				this.showEmojiPanel = false
				this.isVoiceMode = false
			},
			hideExtra() {
				this.showEmojiPanel = false
				this.showExtraPanel = false
			},
			insertEmoji(emoji) { this.inputText += emoji },
			onInputFocus() {
				this.showEmojiPanel = false
				this.showExtraPanel = false
				this.$nextTick(() => this.scrollToBottom())
			},
			onVoiceStart() {
				this.isRecording = true
				uni.showToast({ title: '语音功能开发中', icon: 'none' })
			},
			onVoiceEnd() { this.isRecording = false },
			handleTakePhoto() {
				uni.chooseImage({
					count: 1, sourceType: ['camera'], sizeType: ['compressed'],
					success: async (res) => { await this.sendImageFile(res.tempFilePaths[0]) }
				})
			},
			async sendImageFile(filePath) {
				uni.showLoading({ title: '发送中...' })
				try {
					const r = await uploadFile(filePath)
					const url = r.data?.url || r.data
					await sendGroupMessage(this.groupId, { content: url, msgType: 2 })
					this.messages.push({
						id: Date.now(), groupId: this.groupId, senderId: this.myUserId,
						senderAvatar: this.myAvatar, content: url, msgType: 2, sendTime: new Date().toISOString()
					})
					this.$nextTick(() => this.scrollToBottom())
				} catch (e) { uni.showToast({ title: '发送失败', icon: 'none' }) }
				finally { uni.hideLoading() }
			},
			async loadMessages() {
				try {
					const res = await getGroupMessages(this.groupId, 1, 50)
					if (res.data && res.data.list) { this.messages = res.data.list.reverse(); this.hasMore = res.data.list.length >= 50 }
					else if (res.data && Array.isArray(res.data)) { this.messages = res.data.reverse(); this.hasMore = res.data.length >= 50 }
					this.$nextTick(() => this.scrollToBottom())
				} catch (err) {}
			},
			async loadMoreHistory() {
				if (this.loadingMore || !this.hasMore) return
				this.loadingMore = true; this.page++
				try {
					const res = await getGroupMessages(this.groupId, this.page, 50)
					const list = res.data?.list || res.data || []
					if (list.length > 0) { this.messages = [...list.reverse(), ...this.messages]; this.hasMore = list.length >= 50 }
					else this.hasMore = false
				} catch (err) { this.page-- } finally { this.loadingMore = false }
			},
			async handleSend() {
				const c = this.inputText.trim(); if (!c) return; this.inputText = ''
				this.showEmojiPanel = false; this.showExtraPanel = false
				const tmp = {
					id: Date.now(), groupId: this.groupId, senderId: this.myUserId,
					senderAvatar: this.myAvatar, content: c, msgType: 1, sendTime: new Date().toISOString()
				}
				this.messages.push(tmp); this.$nextTick(() => this.scrollToBottom())
				try {
					const res = await sendGroupMessage(this.groupId, { content: c, msgType: 1 })
					const i = this.messages.findIndex(m => m.id === tmp.id)
					if (i > -1 && res.data) this.messages[i] = { ...res.data }
				} catch (err) { uni.showToast({ title: '发送失败', icon: 'none' }) }
			},
			handleChooseImage() {
				this.showExtraPanel = false
				uni.chooseImage({
					count: 9, sizeType: ['compressed'],
					success: async (res) => {
						for (const filePath of res.tempFilePaths) {
							await this.sendImageFile(filePath)
						}
					}
				})
			},
			previewImage(url) {
				uni.previewImage({ current: url, urls: this.messages.filter(m => m.msgType === 2).map(m => m.content) })
			},
			setupWebSocket() {
				this.groupChatHandler = (data) => {
					if (String(data.groupId) === String(this.groupId) && !this.isSelf(data.senderId)) {
						this.messages.push({
							id: Date.now(), groupId: data.groupId, senderId: data.senderId,
							senderName: data.senderName, senderAvatar: data.senderAvatar || '', content: data.content,
							msgType: data.msgType || 1, sendTime: new Date().toISOString()
						})
						this.$nextTick(() => this.scrollToBottom())
					}
				}
				wsClient.on('groupChat', this.groupChatHandler)
			},
			removeWebSocketListeners() { if (this.groupChatHandler) wsClient.off('groupChat', this.groupChatHandler) },
			scrollToBottom() { this.scrollToId = ''; this.$nextTick(() => { this.scrollToId = 'msg-bottom' }) },
			showTimeDivider(i) {
				return i === 0 || new Date(this.messages[i].sendTime).getTime() - new Date(this.messages[i - 1].sendTime).getTime() > 300000
			},
			formatFullTime(t) {
				if (!t) return ''
				const d = new Date(t), n = new Date()
				const h = String(d.getHours()).padStart(2, '0'), m = String(d.getMinutes()).padStart(2, '0')
				if (d.toDateString() === n.toDateString()) return `${h}:${m}`
				const y = new Date(n); y.setDate(y.getDate() - 1)
				if (d.toDateString() === y.toDateString()) return `昨天 ${h}:${m}`
				return `${d.getMonth() + 1}月${d.getDate()}日 ${h}:${m}`
			},
			handleMsgLongPress(msg) {
				uni.showActionSheet({
					itemList: ['复制'],
					success: (r) => { if (r.tapIndex === 0) uni.setClipboardData({ data: msg.content }) }
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.chat-page {
		height: 100vh;
		display: flex;
		flex-direction: column;
		background: #EDEDED;
	}

	.message-list {
		flex: 1;
		overflow: hidden;
		padding: 0 20rpx;
	}

	.loading-wrap {
		text-align: center;
		padding: 20rpx;
		color: #BBB;
		font-size: 22rpx;
	}

	.time-divider {
		text-align: center;
		padding: 24rpx 0 16rpx;

		text {
			font-size: 22rpx;
			color: #B2B2B2;
			background: #DADADA;
			padding: 4rpx 16rpx;
			border-radius: 6rpx;
		}
	}

	.system-msg {
		text-align: center;
		padding: 12rpx 0;

		text {
			font-size: 22rpx;
			color: #B2B2B2;
			background: #DADADA;
			padding: 6rpx 16rpx;
			border-radius: 6rpx;
		}
	}

	.message-row {
		display: flex;
		align-items: flex-start;
		margin-bottom: 30rpx;
		box-sizing: border-box;
		width: 100%;
	}

	.msg-row-other {
		justify-content: flex-start;
	}

	.msg-row-self {
		justify-content: flex-end;
		padding-right: 12rpx;
	}

	.msg-avatar {
		width: 76rpx;
		height: 76rpx;
		border-radius: 8rpx;
		flex-shrink: 0;
		background: #E0E0E0;
	}

	.avatar-text {
		display: flex;
		align-items: center;
		justify-content: center;

		text {
			font-size: 32rpx;
			color: #FFF;
			font-weight: 600;
		}
	}

	.bubble-box {
		position: relative;
		max-width: 60%;
		margin-left: 12rpx;
		flex-shrink: 1;
		min-width: 0;
	}

	.bubble-box-self {
		margin-left: 0;
		margin-right: 12rpx;
	}

	.msg-name {
		font-size: 22rpx;
		color: #999;
		margin-bottom: 6rpx;
		display: block;
	}

	.bubble-inner {
		position: relative;
	}

	.msg-bubble {
		padding: 20rpx 24rpx;
		word-break: break-all;
	}

	.bubble-other {
		background: #FFFFFF;
		border-radius: 8rpx;
	}

	.bubble-self {
		background: #95EC69;
		border-radius: 8rpx;
	}

	.bubble-arrow-left {
		position: absolute;
		left: -16rpx;
		top: 26rpx;
		width: 0;
		height: 0;
		border-style: solid;
		border-width: 12rpx 16rpx 12rpx 0;
		border-color: transparent #FFFFFF transparent transparent;
	}

	.bubble-arrow-right {
		position: absolute;
		right: -16rpx;
		top: 26rpx;
		width: 0;
		height: 0;
		border-style: solid;
		border-width: 12rpx 0 12rpx 16rpx;
		border-color: transparent transparent transparent #95EC69;
	}

	.msg-text {
		font-size: 32rpx;
		line-height: 1.5;
		color: #111111;
	}

	.msg-image {
		max-width: 360rpx;
		min-width: 140rpx;
		border-radius: 8rpx;
		display: block;
	}

	/* 底部输入栏 */
	.input-bar {
		background: #F7F7F7;
		border-top: 1rpx solid #E0E0E0;
	}

	.input-bar-main {
		display: flex;
		align-items: flex-end;
		padding: 16rpx 8rpx;
	}

	.bar-icon-btn {
		width: 72rpx;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.voice-btn {
		flex: 1;
		height: 72rpx;
		background: #FFFFFF;
		border: 1rpx solid #DEDEDE;
		border-radius: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;

		text {
			font-size: 30rpx;
			color: #333;
			font-weight: 500;
		}

		&:active {
			background: #C6C6C6;
		}
	}

	.input-wrap {
		flex: 1;
		min-height: 72rpx;
		max-height: 240rpx;
		background: #FFFFFF;
		border: 1rpx solid #DEDEDE;
		border-radius: 8rpx;
		display: flex;
		align-items: center;
		padding: 12rpx 16rpx;
		box-sizing: border-box;
	}

	.msg-input {
		flex: 1;
		font-size: 30rpx;
		line-height: 1.5;
		color: #333;
		width: 100%;
		min-height: 44rpx;
		max-height: 210rpx;
	}

	.input-placeholder {
		color: #BCBCBC;
	}

	.send-btn-new {
		height: 64rpx;
		padding: 0 28rpx;
		margin: 4rpx 4rpx;
		background: #07C160;
		border-radius: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;

		text {
			font-size: 28rpx;
			color: #FFF;
			font-weight: 600;
		}

		&:active {
			background: #06AD56;
		}
	}

	.emoji-panel {
		border-top: 1rpx solid #E8E8E8;
		background: #F7F7F7;
	}

	.emoji-scroll {
		height: 400rpx;
		padding: 16rpx;
	}

	.emoji-grid {
		display: flex;
		flex-wrap: wrap;
	}

	.emoji-item {
		width: 12.5%;
		height: 80rpx;
		display: flex;
		align-items: center;
		justify-content: center;

		text {
			font-size: 44rpx;
		}

		&:active {
			background: rgba(0, 0, 0, 0.06);
			border-radius: 8rpx;
		}
	}

	.extra-panel {
		border-top: 1rpx solid #E8E8E8;
		background: #F7F7F7;
		padding: 30rpx 20rpx;
	}

	.extra-grid {
		display: flex;
		flex-wrap: wrap;
		gap: 30rpx;
		padding: 0 20rpx;
	}

	.extra-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		width: 120rpx;

		&:active {
			opacity: 0.7;
		}
	}

	.extra-icon {
		width: 104rpx;
		height: 104rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 12rpx;
	}

	.extra-label {
		font-size: 22rpx;
		color: #666;
	}
</style>
