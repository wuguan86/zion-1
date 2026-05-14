<script>
	import { isLoggedIn } from './utils/auth.js'
	import wsClient from './utils/websocket.js'
	import { fetchCryptoConfig } from './utils/crypto.js'

	export default {
		onLaunch: function() {
			console.log('App Launch')
			// 预加载加密配置
			fetchCryptoConfig()
			if (isLoggedIn()) {
				wsClient.connect()
			} else {
				uni.reLaunch({ url: '/pages/login/index' })
			}
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style lang="scss">
	@import "uview-plus/index.scss";

	page {
		background-color: #EDEDED;
		font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
		font-size: 28rpx;
		color: #1A1A1A;
		line-height: 1.5;
	}

	.safe-area-bottom {
		padding-bottom: env(safe-area-inset-bottom);
	}

	.container {
		padding: 20rpx;
	}

	.avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 8rpx;
		background-color: #E0E0E0;
	}

	.avatar-sm {
		width: 60rpx;
		height: 60rpx;
		border-radius: 6rpx;
	}

	.avatar-lg {
		width: 120rpx;
		height: 120rpx;
		border-radius: 12rpx;
	}

	.avatar-circle {
		border-radius: 50%;
	}

	.divider {
		height: 1rpx;
		background-color: #E5E5E5;
	}

	.card {
		background-color: #FFFFFF;
		border-radius: 12rpx;
		margin: 16rpx;
		padding: 24rpx;
	}

	.btn-primary {
		background: linear-gradient(135deg, #06AD56, #07C160);
		color: #FFFFFF;
		border: none;
		border-radius: 8rpx;
		font-size: 30rpx;
		font-weight: 500;
		padding: 24rpx 0;
		text-align: center;
	}

	.btn-primary:active {
		opacity: 0.85;
	}

	.badge {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 32rpx;
		height: 32rpx;
		padding: 0 8rpx;
		border-radius: 32rpx;
		background-color: #FA5151;
		color: #FFFFFF;
		font-size: 20rpx;
		font-weight: 600;
		line-height: 1;
	}

	.loading-wrap {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 40rpx;
		color: #999999;
		font-size: 26rpx;
	}
</style>
