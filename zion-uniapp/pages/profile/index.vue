<template>
	<view class="page">
		<!-- 自定义顶部 -->
		<view class="header-wrap">
			<view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
			<view class="nav-bar">
				<text class="nav-title">我的</text>
			</view>

			<!-- 用户头像区域 居中 -->
			<view class="user-center">
				<view class="avatar-wrap" @tap="handleChangeAvatar">
					<image class="user-avatar" :src="userInfo.avatar || '/static/default-avatar.png'"
						mode="aspectFill"></image>
					<view class="avatar-camera">
						<u-icon name="camera-fill" color="#FFF" size="11"></u-icon>
					</view>
				</view>
				<text class="user-nickname">{{ userInfo.nickname || '未设置昵称' }}</text>
				<text class="user-id">ID: {{ userInfo.userId || '--' }}</text>
			</view>
		</view>

		<!-- 菜单区域 -->
		<scroll-view scroll-y class="menu-scroll">
			<view class="menu-group">
				<view class="menu-item" @tap="handleEditProfile">
					<view class="menu-icon-wrap" style="background: rgba(7,193,96,0.1);">
						<u-icon name="account" color="#07C160" size="18"></u-icon>
					</view>
					<text class="menu-text">个人信息</text>
					<u-icon name="arrow-right" color="#CCCCCC" size="14"></u-icon>
				</view>
				<view class="menu-item" @tap="handleChangePassword">
					<view class="menu-icon-wrap" style="background: rgba(24,144,255,0.1);">
						<u-icon name="lock" color="#1890FF" size="18"></u-icon>
					</view>
					<text class="menu-text">修改密码</text>
					<u-icon name="arrow-right" color="#CCCCCC" size="14"></u-icon>
				</view>
			</view>

			<view class="menu-group">
				<view class="menu-item" @tap="handleBlacklist">
					<view class="menu-icon-wrap" style="background: rgba(250,173,20,0.1);">
						<u-icon name="minus-circle" color="#FAAD14" size="18"></u-icon>
					</view>
					<text class="menu-text">黑名单</text>
					<u-icon name="arrow-right" color="#CCCCCC" size="14"></u-icon>
				</view>
				<view class="menu-item" @tap="handleClearCache">
					<view class="menu-icon-wrap" style="background: rgba(153,153,153,0.08);">
						<u-icon name="trash" color="#999" size="18"></u-icon>
					</view>
					<text class="menu-text">清除缓存</text>
					<text class="menu-value">{{ cacheSize }}</text>
					<u-icon name="arrow-right" color="#CCCCCC" size="14"></u-icon>
				</view>
			</view>

			<view class="menu-group">
				<view class="menu-item" @tap="handleAbout">
					<view class="menu-icon-wrap" style="background: rgba(114,46,209,0.08);">
						<u-icon name="info-circle" color="#722ED1" size="18"></u-icon>
					</view>
					<text class="menu-text">关于</text>
					<text class="menu-value">v1.0.0</text>
					<u-icon name="arrow-right" color="#CCCCCC" size="14"></u-icon>
				</view>
			</view>

			<view class="logout-wrap">
				<button class="logout-btn" @tap="handleLogout">退出登录</button>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { logout, getAppProfile, updateAppProfile, uploadAvatar } from '../../utils/api.js'
	import { getUserInfo, setUserInfo, clearAuth } from '../../utils/auth.js'

	export default {
		data() {
			return {
				userInfo: {},
				cacheSize: '0 KB',
				statusBarHeight: 20
			}
		},
		onLoad() {
			const sysInfo = uni.getSystemInfoSync()
			this.statusBarHeight = sysInfo.statusBarHeight || 20
		},
		onShow() {
			this.userInfo = getUserInfo() || {}
			this.loadProfile()
			this.calcCacheSize()
		},
		methods: {
			async loadProfile() {
				try {
					const res = await getAppProfile()
					if (res.data) {
						this.userInfo = { ...this.userInfo, ...res.data }
						setUserInfo(this.userInfo)
					}
				} catch (err) {}
			},
			calcCacheSize() {
				try {
					const info = uni.getStorageInfoSync()
					const s = info.currentSize || 0
					this.cacheSize = s > 1024 ? (s / 1024).toFixed(1) + ' MB' : s + ' KB'
				} catch (e) { this.cacheSize = '0 KB' }
			},
			handleChangeAvatar() {
				uni.chooseImage({
					count: 1,
					sizeType: ['compressed'],
					success: async (res) => {
						uni.showLoading({ title: '上传中...' })
						try {
							const r = await uploadAvatar(res.tempFilePaths[0])
							const url = r.data || ''
							await updateAppProfile({ avatar: url })
							this.userInfo.avatar = url
							setUserInfo(this.userInfo)
							uni.showToast({ title: '头像已更新', icon: 'success' })
						} catch (e) {
							uni.showToast({ title: '上传失败', icon: 'none' })
						} finally { uni.hideLoading() }
					}
				})
			},
		handleEditProfile() {
			uni.navigateTo({ url: '/pages/profile/edit' })
		},
		handleChangePassword() {
			uni.navigateTo({ url: '/pages/profile/password' })
		},
			handleBlacklist() {
				uni.showToast({ title: '功能开发中', icon: 'none' })
			},
			handleClearCache() {
				uni.showModal({
					title: '清除缓存',
					content: `当前缓存 ${this.cacheSize}，确定清除？`,
					success: (res) => {
						if (res.confirm) {
							const t = uni.getStorageSync('token')
							const u = uni.getStorageSync('userInfo')
							uni.clearStorageSync()
							if (t) uni.setStorageSync('token', t)
							if (u) uni.setStorageSync('userInfo', u)
							this.calcCacheSize()
							uni.showToast({ title: '已清除', icon: 'success' })
						}
					}
				})
			},
			handleAbout() {
				uni.showModal({
					title: 'Zion 公共',
					content: 'v1.0.0\n高效沟通，智慧办公',
					showCancel: false
				})
			},
			handleLogout() {
				uni.showModal({
					title: '退出登录',
					content: '确定要退出当前账号吗？',
					confirmColor: '#FA5151',
					success: async (res) => {
						if (res.confirm) {
							try { await logout().catch(() => {}) } catch (e) {}
							clearAuth()
							uni.reLaunch({ url: '/pages/login/index' })
						}
					}
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		background: #F0F0F0;
	}

	/* ---- 顶部区域 ---- */
	.header-wrap {
		background: linear-gradient(160deg, #059B4B 0%, #07C160 45%, #2BD373 100%);
		padding-bottom: 48rpx;
		position: relative;
		overflow: hidden;
	}
	/* 装饰圆 */
	.header-wrap::before {
		content: '';
		position: absolute;
		width: 400rpx;
		height: 400rpx;
		border-radius: 50%;
		background: rgba(255,255,255,0.06);
		top: -160rpx;
		right: -80rpx;
	}
	.header-wrap::after {
		content: '';
		position: absolute;
		width: 260rpx;
		height: 260rpx;
		border-radius: 50%;
		background: rgba(255,255,255,0.04);
		bottom: -40rpx;
		left: -60rpx;
	}

	.status-bar {
		width: 100%;
	}
	.nav-bar {
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		position: relative;
		z-index: 2;
	}
	.nav-title {
		font-size: 34rpx;
		font-weight: 600;
		color: #FFFFFF;
		letter-spacing: 2rpx;
	}

	/* ---- 用户居中区域 ---- */
	.user-center {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 20rpx 0 8rpx;
		position: relative;
		z-index: 2;
	}
	.avatar-wrap {
		position: relative;
		margin-bottom: 18rpx;
	}
	.user-avatar {
		width: 148rpx;
		height: 148rpx;
		border-radius: 50%;
		border: 6rpx solid rgba(255,255,255,0.5);
		background: #E0E0E0;
		box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.15);
	}
	.avatar-camera {
		position: absolute;
		right: 4rpx;
		bottom: 4rpx;
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		background: #07C160;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 4rpx solid #FFFFFF;
		box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.12);
	}
	.user-nickname {
		font-size: 36rpx;
		font-weight: 700;
		color: #FFFFFF;
		letter-spacing: 1rpx;
		text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
	}
	.user-id {
		font-size: 22rpx;
		color: rgba(255,255,255,0.65);
		margin-top: 6rpx;
	}

	/* ---- 菜单区域 ---- */
	.menu-scroll {
		flex: 1;
	}
	.menu-group {
		margin: 20rpx 24rpx 0;
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
	}
	.menu-item {
		display: flex;
		align-items: center;
		padding: 28rpx 28rpx;
		position: relative;

		&:active {
			background: #F7F7F7;
		}

		/* 分割线 - 除最后一个 */
		& + .menu-item::before {
			content: '';
			position: absolute;
			top: 0;
			left: 80rpx;
			right: 28rpx;
			height: 1rpx;
			background: #F2F2F2;
		}
	}
	.menu-icon-wrap {
		width: 52rpx;
		height: 52rpx;
		border-radius: 12rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 20rpx;
		flex-shrink: 0;
	}
	.menu-text {
		flex: 1;
		font-size: 28rpx;
		color: #1A1A1A;
		font-weight: 400;
	}
	.menu-value {
		font-size: 24rpx;
		color: #BBBBBB;
		margin-right: 8rpx;
	}

	/* ---- 退出按钮 ---- */
	.logout-wrap {
		padding: 40rpx 24rpx 80rpx;
	}
	.logout-btn {
		height: 88rpx;
		line-height: 88rpx;
		background: #FFFFFF;
		color: #FA5151;
		font-size: 28rpx;
		font-weight: 500;
		border-radius: 16rpx;
		border: none;
		box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
	}
	.logout-btn::after {
		border: none;
	}
</style>
