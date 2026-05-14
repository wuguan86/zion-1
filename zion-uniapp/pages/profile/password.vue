<template>
	<view class="page">
		<!-- 提示信息 -->
		<view class="tip-bar">
			<u-icon name="info-circle" color="#1890FF" size="15"></u-icon>
			<text class="tip-text">为了账号安全，请设置一个不易被猜到的密码</text>
		</view>

		<!-- 表单区域 -->
		<view class="form-section">
			<view class="form-item">
				<text class="form-label">原密码</text>
				<view class="input-wrap">
					<input
						class="form-input"
						:type="showOld ? 'text' : 'password'"
						v-model="form.oldPassword"
						placeholder="请输入原密码"
						placeholder-class="placeholder"
						maxlength="32"
					/>
					<view class="eye-btn" @tap="showOld = !showOld">
						<u-icon :name="showOld ? 'eye' : 'eye-off'" color="#999" size="18"></u-icon>
					</view>
				</view>
			</view>

			<view class="form-item">
				<text class="form-label">新密码</text>
				<view class="input-wrap">
					<input
						class="form-input"
						:type="showNew ? 'text' : 'password'"
						v-model="form.newPassword"
						placeholder="请输入新密码"
						placeholder-class="placeholder"
						maxlength="32"
					/>
					<view class="eye-btn" @tap="showNew = !showNew">
						<u-icon :name="showNew ? 'eye' : 'eye-off'" color="#999" size="18"></u-icon>
					</view>
				</view>
			</view>

			<view class="form-item">
				<text class="form-label">确认密码</text>
				<view class="input-wrap">
					<input
						class="form-input"
						:type="showConfirm ? 'text' : 'password'"
						v-model="form.confirmPassword"
						placeholder="请再次输入新密码"
						placeholder-class="placeholder"
						maxlength="32"
					/>
					<view class="eye-btn" @tap="showConfirm = !showConfirm">
						<u-icon :name="showConfirm ? 'eye' : 'eye-off'" color="#999" size="18"></u-icon>
					</view>
				</view>
			</view>
		</view>

		<!-- 密码强度指示 -->
		<view class="strength-section" v-if="form.newPassword">
			<text class="strength-label">密码强度：</text>
			<view class="strength-bars">
				<view class="strength-bar" :class="{ active: passwordStrength >= 1, weak: passwordStrength === 1 }"></view>
				<view class="strength-bar" :class="{ active: passwordStrength >= 2, medium: passwordStrength === 2 }"></view>
				<view class="strength-bar" :class="{ active: passwordStrength >= 3, strong: passwordStrength >= 3 }"></view>
			</view>
			<text class="strength-text" :class="strengthClass">{{ strengthText }}</text>
		</view>

		<!-- 提交按钮 -->
		<view class="btn-wrap">
			<button class="submit-btn" :disabled="submitting" @tap="handleSubmit">
				{{ submitting ? '提交中...' : '确认修改' }}
			</button>
		</view>
	</view>
</template>

<script>
	import { changeAppPassword } from '../../utils/api.js'
	import { clearAuth } from '../../utils/auth.js'
	import wsClient from '../../utils/websocket.js'

	export default {
		data() {
			return {
				form: {
					oldPassword: '',
					newPassword: '',
					confirmPassword: ''
				},
				showOld: false,
				showNew: false,
				showConfirm: false,
				submitting: false
			}
		},
		computed: {
			passwordStrength() {
				const pwd = this.form.newPassword
				if (!pwd) return 0
				let strength = 0
				if (pwd.length >= 6) strength++
				if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) strength++
				if (/\d/.test(pwd) && /[^A-Za-z0-9]/.test(pwd)) strength++
				return strength
			},
			strengthText() {
				const map = { 0: '', 1: '弱', 2: '中', 3: '强' }
				return map[this.passwordStrength] || ''
			},
			strengthClass() {
				const map = { 1: 'text-weak', 2: 'text-medium', 3: 'text-strong' }
				return map[this.passwordStrength] || ''
			}
		},
		methods: {
			validate() {
				if (!this.form.oldPassword) {
					uni.showToast({ title: '请输入原密码', icon: 'none' })
					return false
				}
				if (!this.form.newPassword) {
					uni.showToast({ title: '请输入新密码', icon: 'none' })
					return false
				}
				if (this.form.newPassword.length < 6) {
					uni.showToast({ title: '新密码至少6位', icon: 'none' })
					return false
				}
				if (this.form.newPassword === this.form.oldPassword) {
					uni.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
					return false
				}
				if (this.form.newPassword !== this.form.confirmPassword) {
					uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
					return false
				}
				return true
			},

			async handleSubmit() {
				if (!this.validate()) return
				this.submitting = true
				try {
					await changeAppPassword({
						oldPassword: this.form.oldPassword,
						newPassword: this.form.newPassword
					})
					uni.showModal({
						title: '修改成功',
						content: '密码已修改，请重新登录',
						showCancel: false,
						success: () => {
							// 密码修改成功后退出登录
							wsClient.close()
							clearAuth()
							uni.reLaunch({ url: '/pages/login/index' })
						}
					})
				} catch (e) {
					uni.showToast({ title: e.message || '修改失败', icon: 'none' })
				} finally {
					this.submitting = false
				}
			}
		}
	}
</script>

<style lang="scss" scoped>
	.page {
		min-height: 100vh;
		background: #F0F0F0;
	}

	/* 提示条 */
	.tip-bar {
		display: flex;
		align-items: center;
		padding: 20rpx 32rpx;
		background: rgba(24, 144, 255, 0.06);
		margin: 0 0 20rpx;
	}

	.tip-text {
		font-size: 24rpx;
		color: #1890FF;
		margin-left: 10rpx;
	}

	/* 表单区域 */
	.form-section {
		margin: 0 24rpx;
		background: #FFFFFF;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
	}

	.form-item {
		padding: 0 28rpx;
		position: relative;

		& + .form-item::before {
			content: '';
			position: absolute;
			top: 0;
			left: 28rpx;
			right: 28rpx;
			height: 1rpx;
			background: #F2F2F2;
		}
	}

	.form-label {
		display: block;
		font-size: 24rpx;
		color: #999999;
		padding-top: 24rpx;
	}

	.input-wrap {
		display: flex;
		align-items: center;
		padding: 12rpx 0 24rpx;
	}

	.form-input {
		flex: 1;
		font-size: 30rpx;
		color: #1A1A1A;
		height: 56rpx;
	}

	.placeholder {
		color: #CCCCCC;
		font-size: 28rpx;
	}

	.eye-btn {
		padding: 12rpx;
		flex-shrink: 0;
	}

	/* 密码强度 */
	.strength-section {
		display: flex;
		align-items: center;
		padding: 24rpx 52rpx;
	}

	.strength-label {
		font-size: 24rpx;
		color: #999999;
		flex-shrink: 0;
	}

	.strength-bars {
		display: flex;
		gap: 8rpx;
		margin: 0 12rpx;
	}

	.strength-bar {
		width: 60rpx;
		height: 8rpx;
		border-radius: 4rpx;
		background: #E8E8E8;
		transition: background 0.3s;

		&.active.weak {
			background: #FF4D4F;
		}

		&.active.medium {
			background: #FAAD14;
		}

		&.active.strong {
			background: #52C41A;
		}
	}

	.strength-text {
		font-size: 24rpx;
	}

	.text-weak {
		color: #FF4D4F;
	}

	.text-medium {
		color: #FAAD14;
	}

	.text-strong {
		color: #52C41A;
	}

	/* 提交按钮 */
	.btn-wrap {
		padding: 60rpx 24rpx;
	}

	.submit-btn {
		height: 88rpx;
		line-height: 88rpx;
		background: linear-gradient(135deg, #07C160 0%, #059B4B 100%);
		color: #FFFFFF;
		font-size: 30rpx;
		font-weight: 500;
		border-radius: 16rpx;
		border: none;
		box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);

		&[disabled] {
			opacity: 0.6;
		}
	}

	.submit-btn::after {
		border: none;
	}
</style>
