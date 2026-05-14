<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { LogOut, Person } from '@vicons/ionicons5'

const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
}
</script>

<template>
  <div class="home-page">
    <header class="home-header">
      <h1 class="app-name">Zion</h1>
      <n-button text @click="handleLogout">
        <template #icon>
          <n-icon><LogOut /></n-icon>
        </template>
        退出登录
      </n-button>
    </header>

    <main class="home-main">
      <div class="welcome-card">
        <n-avatar
          :src="userStore.avatar"
          :size="80"
          round
          fallback-src=""
        >
          <n-icon size="40"><Person /></n-icon>
        </n-avatar>
        <h2>欢迎回来，{{ userStore.nickname }}</h2>
        <p class="username">@{{ userStore.user?.username }}</p>
        <n-divider />
        <p class="placeholder-hint">更多功能即将上线，敬请期待</p>
      </div>
    </main>
  </div>
</template>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e5e7eb;

  .app-name {
    font-size: 20px;
    font-weight: 700;
    color: #1a1a2e;
  }
}

.home-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.welcome-card {
  text-align: center;
  background: #fff;
  padding: 48px 64px;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.06);

  h2 {
    margin-top: 20px;
    font-size: 22px;
    font-weight: 600;
    color: #1a1a2e;
  }

  .username {
    margin-top: 6px;
    font-size: 14px;
    color: #9ca3af;
  }

  .placeholder-hint {
    font-size: 14px;
    color: #9ca3af;
    margin-top: 8px;
  }
}
</style>
