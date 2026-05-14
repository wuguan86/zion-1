<template>
	<view class="login-page">
		<!-- 顶部区域 -->
		<view class="login-header">
			<view class="header-bg"></view>
			<view class="logo-area">
				<image class="logo" src="/static/logo.png" mode="aspectFit"></image>
				<text class="app-name">ZionAdmin</text>
				<text class="app-desc">简洁高效的管理平台</text>
			</view>
		</view>

		<!-- 主体区域 -->
		<view class="login-body">
			<view class="login-card">
				<button class="wx-login-btn" @tap="handleWxLogin" :loading="loading">
					<u-icon name="weixin-fill" color="#FFFFFF" size="20"></u-icon>
					<text class="wx-btn-text">微信授权登录</text>
				</button>

				<view class="divider-line">
					<view class="line"></view>
					<text class="divider-text">或</text>
					<view class="line"></view>
				</view>

				<view class="toggle-login" @tap="showPhoneLogin = !showPhoneLogin">
					<text>{{ showPhoneLogin ? '返回微信登录' : '手机号登录' }}</text>
				</view>

				<view class="phone-form" v-if="showPhoneLogin">
					<view class="input-group">
						<view class="input-wrap">
							<u-icon name="phone" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
							<input class="input-field" type="number" maxlength="11"
								v-model="phone" placeholder="请输入手机号" placeholder-class="placeholder" />
						</view>
					</view>
					<view class="input-group">
						<view class="input-wrap">
							<u-icon name="lock" color="#BBB" size="18" style="margin-right: 16rpx;"></u-icon>
							<input class="input-field code-field" type="number" maxlength="6"
								v-model="smsCode" placeholder="请输入验证码" placeholder-class="placeholder" />
							<view class="code-btn" :class="{ 'code-disabled': codeCountdown > 0 }" @tap="handleSendCode">
								<text>{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</text>
							</view>
						</view>
					</view>
					<button class="phone-login-btn" @tap="handlePhoneLogin" :loading="loading">登录</button>
				</view>
			</view>

			<view class="agreement">
				<text class="agree-text">登录即表示同意</text>
				<text class="agree-link">《用户协议》</text>
				<text class="agree-text">和</text>
				<text class="agree-link">《隐私政策》</text>
			</view>
		</view>

		<!-- 底部弹出：选择头像和昵称 -->
		<view class="popup-mask" v-if="showProfilePopup" @tap="showProfilePopup = false"></view>
		<view class="popup-sheet" :class="{ 'popup-show': showProfilePopup }">
			<view class="popup-header">
				<text class="popup-title">完善个人信息</text>
				<view class="popup-close" @tap="showProfilePopup = false">
					<u-icon name="close" color="#999" size="18"></u-icon>
				</view>
			</view>

			<view class="popup-body">
				<!-- 头像选择 -->
				<view class="profile-avatar-area">
					<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
						<image class="chosen-avatar" :src="chosenAvatar || '/static/default-avatar.png'" mode="aspectFill"></image>
						<view class="avatar-edit-badge">
							<u-icon name="camera-fill" color="#FFF" size="12"></u-icon>
						</view>
					</button>
					<text class="avatar-hint">点击选择头像</text>
				</view>

				<!-- 昵称输入 -->
				<view class="nickname-area">
					<text class="nickname-label">昵称</text>
					<input class="nickname-input" type="nickname" v-model="chosenNickname"
						placeholder="请输入昵称" placeholder-class="placeholder" />
				</view>
			</view>

			<view class="popup-footer">
				<button class="confirm-login-btn" @tap="confirmLogin" :loading="confirming">
					确认并登录
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { wxLogin, sendSmsCode, updateAppProfile, uploadAvatar } from '../../utils/api.js'
	import { BASE_URL } from '../../utils/request.js'
	import { setToken, setUserInfo } from '../../utils/auth.js'
	import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				loading: false, confirming: false,
				showPhoneLogin: false, showProfilePopup: false,
				phone: '', smsCode: '', codeCountdown: 0, codeTimer: null,
				chosenAvatar: '', chosenNickname: '',
				pendingLoginData: null
			}
		},
		onUnload() { if (this.codeTimer) clearInterval(this.codeTimer) },
		methods: {
			async handleWxLogin() {
				if (this.loading) return
				this.loading = true
				try {
					// #ifdef MP-WEIXIN
					const loginRes = await new Promise((resolve, reject) => {
						uni.login({ provider: 'weixin', success: resolve, fail: reject })
					})
					const res = await wxLogin({ wxCode: loginRes.code, loginType: 'MINIPROGRAM' })
					// 登录成功，弹出选择头像昵称
					this.pendingLoginData = res.data
					this.chosenNickname = res.data.nickname || ''
					this.chosenAvatar = res.data.avatar || ''
					this.showProfilePopup = true
					// #endif
					// #ifndef MP-WEIXIN
					uni.showToast({ title: '请在微信小程序中使用', icon: 'none' })
					// #endif
				} catch (err) {
					console.error('登录失败:', err)
					uni.showToast({ title: '登录失败，请重试', icon: 'none' })
				} finally { this.loading = false }
			},

			onChooseAvatar(e) {
				this.chosenAvatar = e.detail.avatarUrl
			},

			async confirmLogin() {
				if (this.confirming) return
				if (!this.chosenNickname.trim()) {
					uni.showToast({ title: '请输入昵称', icon: 'none' }); return
				}
				this.confirming = true
				try {
					const data = this.pendingLoginData
					// 先设置 token，后续 API 调用需要认证
					setToken(data.token)

					const nickname = this.chosenNickname.trim()
					let avatarUrl = data.avatar || ''

					// 处理头像：优先直接使用 URL（微信CDN链接等），只有本地临时文件才上传
					if (this.chosenAvatar && this.chosenAvatar !== data.avatar) {
						if (this.chosenAvatar.startsWith('https://') || this.chosenAvatar.startsWith('http://thirdwx.') || this.chosenAvatar.startsWith('https://thirdwx.')) {
							// 微信CDN头像链接，直接使用，无需上传
							avatarUrl = this.chosenAvatar
						} else if (this.chosenAvatar.startsWith('http://tmp/') || this.chosenAvatar.startsWith('wxfile://')) {
							// 本地临时文件，需要上传
							try {
								const uploadRes = await uploadAvatar(this.chosenAvatar)
								// 返回的可能是相对路径，拼接完整URL
								const url = uploadRes.data || ''
								avatarUrl = url.startsWith('http') ? url : (url ? BASE_URL + url : avatarUrl)
							} catch (e) {
								console.warn('头像上传失败，使用选择的头像:', e)
								avatarUrl = this.chosenAvatar
							}
						} else {
							// 其他情况（可能是完整URL），直接使用
							avatarUrl = this.chosenAvatar
						}
					}

					// 将昵称、用户名、头像同步更新到数据库
					try {
						await updateAppProfile({ nickname, avatar: avatarUrl })
					} catch (e) {
						console.warn('更新个人资料失败:', e)
					}

					setUserInfo({
						userId: data.userId,
						username: nickname,
						nickname: nickname,
						avatar: avatarUrl
					})
					wsClient.connect()
					this.showProfilePopup = false
					uni.showToast({ title: '登录成功', icon: 'success' })
					setTimeout(() => { uni.switchTab({ url: '/pages/index/index' }) }, 500)
				} catch (err) {
					console.error('确认登录失败:', err)
				} finally { this.confirming = false }
			},

			async handleSendCode() {
				if (this.codeCountdown > 0) return
				if (!this.phone || !/^1[3-9]\d{9}$/.test(this.phone)) {
					uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
				}
				try {
					await sendSmsCode({ phone: this.phone })
					uni.showToast({ title: '验证码已发送', icon: 'success' })
					this.codeCountdown = 60
					this.codeTimer = setInterval(() => {
						this.codeCountdown--
						if (this.codeCountdown <= 0) clearInterval(this.codeTimer)
					}, 1000)
				} catch (err) { console.error('发送验证码失败:', err) }
			},

			async handlePhoneLogin() {
				if (this.loading) return
				if (!this.phone || !/^1[3-9]\d{9}$/.test(this.phone)) {
					uni.showToast({ title: '请输入正确的手机号', icon: 'none' }); return
				}
				if (!this.smsCode || this.smsCode.length < 4) {
					uni.showToast({ title: '请输入验证码', icon: 'none' }); return
				}
				this.loading = true
				try {
					const res = await wxLogin({ phone: this.phone, smsCode: this.smsCode, loginType: 'SMS' })
					this.pendingLoginData = res.data
					this.chosenNickname = res.data.nickname || ''
					this.chosenAvatar = res.data.avatar || ''
					this.showProfilePopup = true
				} catch (err) { console.error('登录失败:', err) }
				finally { this.loading = false }
			}
		}
	}
</script>

<style lang="scss" scoped>
	.login-page { min-height: 100vh; background: #F5F5F5; position: relative; }

	.login-header { position: relative; height: 600rpx; }
	.header-bg {
		position: absolute; top: 0; left: 0; right: 0; height: 520rpx;
		background: linear-gradient(160deg, #06AD56 0%, #07C160 50%, #2BD373 100%);
		border-radius: 0 0 60rpx 60rpx;
	}
	.logo-area {
		position: relative; z-index: 1;
		display: flex; flex-direction: column; align-items: center; padding-top: 200rpx;
	}
	.logo {
		width: 130rpx; height: 130rpx; border-radius: 28rpx;
		background: #FFF; box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.15);
	}
	.app-name { font-size: 44rpx; font-weight: 700; color: #FFF; margin-top: 24rpx; letter-spacing: 4rpx; }
	.app-desc { font-size: 24rpx; color: rgba(255,255,255,0.7); margin-top: 10rpx; letter-spacing: 2rpx; }

	.login-body { padding: 0 40rpx; margin-top: -40rpx; }
	.login-card {
		background: #FFF; border-radius: 20rpx; padding: 48rpx 36rpx;
		box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.06);
	}

	.wx-login-btn {
		display: flex; align-items: center; justify-content: center;
		height: 88rpx; background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 44rpx; border: none;
	}
	.wx-login-btn::after { border: none; }
	.wx-btn-text { margin-left: 10rpx; }

	.divider-line { display: flex; align-items: center; margin: 32rpx 0 20rpx; }
	.divider-line .line { flex: 1; height: 1rpx; background: #EBEBEB; }
	.divider-text { padding: 0 20rpx; font-size: 22rpx; color: #BBB; }

	.toggle-login {
		text-align: center; padding: 8rpx 0;
		text { font-size: 26rpx; color: #07C160; }
	}

	.phone-form {
		margin-top: 24rpx;
		.input-group { margin-bottom: 20rpx; }
		.input-wrap {
			display: flex; align-items: center;
			height: 84rpx; background: #F8F8F8; border-radius: 42rpx;
			padding: 0 28rpx; border: 1rpx solid #EBEBEB;
		}
		.input-field { flex: 1; height: 84rpx; font-size: 28rpx; color: #333; }
		.code-field { flex: 1; }
		.code-btn {
			padding: 0 20rpx; height: 52rpx; line-height: 52rpx;
			border-left: 1rpx solid #E0E0E0; margin-left: 16rpx; padding-left: 20rpx;
			text { font-size: 24rpx; color: #07C160; }
		}
		.code-disabled text { color: #BBB; }
	}

	.phone-login-btn {
		height: 88rpx; background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 44rpx; border: none; margin-top: 8rpx;
	}
	.phone-login-btn::after { border: none; }

	.agreement {
		display: flex; align-items: center; justify-content: center;
		flex-wrap: wrap; padding: 32rpx 0;
		.agree-text { font-size: 22rpx; color: #BBB; }
		.agree-link { font-size: 22rpx; color: #07C160; }
	}

	.placeholder { color: #CCC; }

	/* 底部弹出层 */
	.popup-mask {
		position: fixed; top: 0; left: 0; right: 0; bottom: 0;
		background: rgba(0,0,0,0.45); z-index: 200;
	}
	.popup-sheet {
		position: fixed; left: 0; right: 0; bottom: 0; z-index: 201;
		background: #FFF; border-radius: 24rpx 24rpx 0 0;
		padding: 0 40rpx; padding-bottom: env(safe-area-inset-bottom);
		transform: translateY(100%); transition: transform 0.3s ease;
	}
	.popup-show { transform: translateY(0); }

	.popup-header {
		display: flex; align-items: center; justify-content: space-between;
		padding: 32rpx 0 16rpx; border-bottom: 1rpx solid #F0F0F0;
	}
	.popup-title { font-size: 32rpx; font-weight: 600; color: #1A1A1A; }
	.popup-close {
		width: 56rpx; height: 56rpx; display: flex;
		align-items: center; justify-content: center;
	}

	.popup-body { padding: 40rpx 0; }

	.profile-avatar-area {
		display: flex; flex-direction: column; align-items: center; margin-bottom: 40rpx;
	}
	.avatar-btn {
		position: relative; width: 140rpx; height: 140rpx;
		padding: 0; margin: 0; background: none; border: none;
		line-height: normal;
	}
	.avatar-btn::after { border: none; }
	.chosen-avatar {
		width: 140rpx; height: 140rpx; border-radius: 50%;
		background: #EDEDED; border: 4rpx solid #FFF;
		box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.1);
	}
	.avatar-edit-badge {
		position: absolute; right: 2rpx; bottom: 2rpx;
		width: 40rpx; height: 40rpx; border-radius: 50%;
		background: #07C160; display: flex;
		align-items: center; justify-content: center;
		border: 3rpx solid #FFF;
	}
	.avatar-hint { font-size: 22rpx; color: #BBB; margin-top: 12rpx; }

	.nickname-area {
		display: flex; align-items: center;
		padding: 24rpx 0; border-bottom: 1rpx solid #F0F0F0;
	}
	.nickname-label {
		font-size: 28rpx; color: #1A1A1A; font-weight: 500;
		width: 100rpx; flex-shrink: 0;
	}
	.nickname-input { flex: 1; font-size: 28rpx; color: #333; height: 60rpx; }

	.popup-footer { padding: 24rpx 0 32rpx; }
	.confirm-login-btn {
		height: 88rpx; background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFF; font-size: 30rpx; font-weight: 500;
		border-radius: 44rpx; border: none;
	}
	.confirm-login-btn::after { border: none; }
</style>
