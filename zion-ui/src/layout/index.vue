<template>
  <n-layout :has-sider="layoutConfig.siderPosition !== 'top'" class="layout" :class="layoutConfig.siderPosition === 'top' ? 'layout-top' : ''">
    <!-- 侧边栏（左侧/右侧模式） -->
    <n-layout-sider
      v-if="layoutConfig.siderPosition !== 'top'"
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      :position="layoutConfig.siderPosition === 'right' ? 'right' : 'left'"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      class="layout-sider"
      :class="[`theme-${layoutConfig.theme}`, layoutConfig.siderPosition === 'right' ? 'sider-right' : '']"
    >
      <!-- Logo -->
      <div class="logo" :class="{ 'logo-collapsed': collapsed, 'logo-primary': themeStore.headerUsePrimaryColor }" :style="themeStore.headerUsePrimaryColor ? { background: themeStore.primaryColor, borderBottomColor: themeStore.primaryColor } : {}">
        <img v-if="siteLogo" :src="siteLogo" class="logo-img" alt="Logo" />
        <div v-else class="logo-icon" :style="{ background: themeStore.headerUsePrimaryColor ? '#fff' : themeStore.primaryColor, color: themeStore.headerUsePrimaryColor ? themeStore.primaryColor : '#fff' }">{{ siteName.charAt(0) }}</div>
        <transition name="fade">
          <span v-if="!collapsed" class="logo-text">{{ siteName }}</span>
        </transition>
      </div>

      <!-- 菜单 -->
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeMenu"
        @update:value="handleMenuClick"
        class="layout-menu"
      />
    </n-layout-sider>

    <!-- 主内容区 -->
    <n-layout>
      <!-- 顶部导航 -->
      <n-layout-header bordered class="layout-header" :class="[`theme-${layoutConfig.theme}`, { 'header-primary': themeStore.headerUsePrimaryColor }]" :style="headerStyle">
        <!-- 顶部菜单模式下的Logo -->
        <div v-if="layoutConfig.siderPosition === 'top'" class="header-logo">
          <img v-if="siteLogo" :src="siteLogo" class="logo-img" alt="Logo" />
          <div v-else class="logo-icon" :style="{ background: themeStore.headerUsePrimaryColor ? '#fff' : themeStore.primaryColor, color: themeStore.headerUsePrimaryColor ? themeStore.primaryColor : '#fff' }">{{ siteName.charAt(0) }}</div>
          <span class="logo-text">{{ siteName }}</span>
        </div>

        <!-- 顶部菜单模式下的菜单 -->
        <div v-if="layoutConfig.siderPosition === 'top'" class="header-menu">
          <n-menu
            mode="horizontal"
            :options="menuOptions"
            :value="activeMenu"
            @update:value="handleMenuClick"
          />
        </div>

        <div v-else class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </n-breadcrumb-item>
          </n-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 菜单搜索 -->
          <n-popover trigger="click" placement="bottom" :width="400" v-model:show="searchVisible">
            <template #trigger>
              <div class="header-icon" title="搜索菜单">
                <n-icon size="20"><SearchOutline /></n-icon>
              </div>
            </template>
            <div class="search-panel">
              <n-input
                v-model:value="searchKeyword"
                placeholder="搜索菜单..."
                clearable
                autofocus
                @input="handleSearch"
              >
                <template #prefix>
                  <n-icon><SearchOutline /></n-icon>
                </template>
              </n-input>
              <div class="search-results" v-if="searchResults.length > 0">
                <div
                  v-for="item in searchResults"
                  :key="item.key"
                  class="search-result-item"
                  @click="goToMenu(item)"
                >
                  <n-icon size="16" class="result-icon">
                    <component :is="iconMap[item.iconName] || MenuOutline" />
                  </n-icon>
                  <span class="result-label">{{ item.label }}</span>
                  <span class="result-path">{{ item.path }}</span>
                </div>
              </div>
              <n-empty v-else-if="searchKeyword" description="未找到匹配菜单" size="small" style="padding: 20px 0" />
            </div>
          </n-popover>

          <!-- 全屏 -->
          <div class="header-icon" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
            <n-icon size="20">
              <ContractOutline v-if="isFullscreen" />
              <ExpandOutline v-else />
            </n-icon>
          </div>

          <!-- 换肤设置 -->
          <n-popover trigger="click" placement="bottom-end" :width="300">
            <template #trigger>
              <div class="header-icon" title="主题设置">
                <n-icon size="20"><ColorPaletteOutline /></n-icon>
              </div>
            </template>
            <div class="theme-panel">
              <div class="theme-section">
                <div class="theme-title">菜单位置</div>
                <div class="layout-options">
                  <div
                    class="layout-option"
                    :class="{ active: layoutConfig.siderPosition === 'left' }"
                    @click="setLayoutPosition('left')"
                  >
                    <div class="layout-preview layout-left">
                      <div class="preview-sider"></div>
                      <div class="preview-main"></div>
                    </div>
                    <span>左侧菜单</span>
                  </div>
                  <div
                    class="layout-option"
                    :class="{ active: layoutConfig.siderPosition === 'right' }"
                    @click="setLayoutPosition('right')"
                  >
                    <div class="layout-preview layout-right">
                      <div class="preview-main"></div>
                      <div class="preview-sider"></div>
                    </div>
                    <span>右侧菜单</span>
                  </div>
                  <div
                    class="layout-option"
                    :class="{ active: layoutConfig.siderPosition === 'top' }"
                    @click="setLayoutPosition('top')"
                  >
                    <div class="layout-preview layout-top">
                      <div class="preview-header"></div>
                      <div class="preview-content"></div>
                    </div>
                    <span>顶部菜单</span>
                  </div>
                </div>
              </div>
              <div class="theme-section">
                <div class="theme-title">主题风格</div>
                <div class="theme-modes">
                  <div
                    v-for="theme in themeOptions"
                    :key="theme.value"
                    class="theme-mode"
                    :class="{ active: layoutConfig.theme === theme.value }"
                    @click="setTheme(theme.value)"
                  >
                    <div class="theme-mode-preview" :style="{ background: theme.color }">
                      <n-icon v-if="layoutConfig.theme === theme.value" :color="theme.value === 'light' ? '#18a058' : '#fff'"><CheckmarkOutline /></n-icon>
                    </div>
                    <span>{{ theme.label }}</span>
                  </div>
                </div>
              </div>
              <div class="theme-section">
                <div class="theme-title">主题色</div>
                <div class="color-options">
                  <div
                    v-for="item in themeColors"
                    :key="item.color"
                    class="color-option"
                    :class="{ active: themeStore.primaryColor === item.color }"
                    :style="{ backgroundColor: item.color }"
                    :title="item.name"
                    @click="themeStore.setPrimaryColor(item.color)"
                  >
                    <n-icon v-if="themeStore.primaryColor === item.color" color="#fff"><CheckmarkOutline /></n-icon>
                  </div>
                </div>
              </div>
              <div class="theme-section">
                <div class="theme-switch-row">
                  <span class="theme-title" style="margin-bottom: 0">顶栏应用主题色</span>
                  <n-switch :value="themeStore.headerUsePrimaryColor" @update:value="themeStore.setHeaderUsePrimaryColor" size="small" />
                </div>
              </div>
              <div class="theme-section">
                <div class="theme-switch-row">
                  <span class="theme-title" style="margin-bottom: 0">显示页签</span>
                  <n-switch :value="themeStore.showTabs" @update:value="themeStore.setShowTabs" size="small" />
                </div>
              </div>
            </div>
          </n-popover>

          <!-- 消息通知 -->
          <n-popover trigger="click" placement="bottom-end" :width="360" @update:show="handleMessagePopoverShow">
            <template #trigger>
              <n-badge :value="messageStore.totalUnread()" :max="99" :show-zero="false">
                <div class="header-icon" title="消息通知">
                  <n-icon size="20"><NotificationsOutline /></n-icon>
                </div>
              </n-badge>
            </template>
            <div class="message-popover">
              <div class="message-tabs">
                <n-tabs v-model:value="messageTab" type="line" size="small" @update:value="handleTabChange" class="message-tabs-inner">
                  <n-tab-pane name="notice" tab="通知">
                    <template #tab>
                      <n-badge :value="messageStore.noticeCount" :max="99" :show-zero="false" :offset="[8, -2]">
                        <span class="tab-text">通知</span>
                      </n-badge>
                    </template>
                  </n-tab-pane>
                  <n-tab-pane name="chat" tab="消息">
                    <template #tab>
                      <n-badge :value="messageStore.chatCount" :max="99" :show-zero="false" :offset="[8, -2]">
                        <span class="tab-text">消息</span>
                      </n-badge>
                    </template>
                  </n-tab-pane>
                </n-tabs>
              </div>
              <div class="message-list" ref="messageListRef" @scroll="handleMessageScroll">
                <n-spin :show="messageLoading && messagePage === 1">
                  <!-- 通知列表 -->
                  <template v-if="messageTab === 'notice'">
                    <div v-for="item in recentNotices" :key="item.id" class="message-item" @click="handleNoticeClick(item)">
                      <div class="message-item-header">
                        <n-tag :type="item.noticeType === 1 ? 'info' : 'warning'" size="small">
                          {{ item.noticeType === 1 ? '通知' : '公告' }}
                        </n-tag>
                        <span class="message-time">{{ formatMessageTime(item.createTime) }}</span>
                      </div>
                      <div class="message-title">{{ item.title }}</div>
                      <div class="message-content">{{ stripHtml(item.content) }}</div>
                    </div>
                    <n-empty v-if="recentNotices.length === 0 && !messageLoading" description="暂无通知" size="small" style="padding: 30px 0" />
                  </template>
                  <!-- 聊天消息列表 -->
                  <template v-else>
                    <div v-for="item in recentChats" :key="item.senderId || item.id" class="message-item" @click="handleChatClick(item)">
                      <div class="message-item-row">
                        <n-avatar round size="small" :src="item.senderAvatar">
                          {{ item.senderName?.charAt(0) || 'U' }}
                        </n-avatar>
                        <div class="message-item-content">
                          <div class="message-item-header">
                            <span class="message-sender">{{ item.senderName || '用户' }}</span>
                            <span class="message-time">{{ formatMessageTime(item.sendTime) }}</span>
                          </div>
                          <div class="message-content">{{ item.content }}</div>
                        </div>
                      </div>
                    </div>
                    <n-empty v-if="recentChats.length === 0 && !messageLoading" description="暂无消息" size="small" style="padding: 30px 0" />
                  </template>
                </n-spin>
                <!-- 加载更多 -->
                <div v-if="messageLoading && messagePage > 1" class="message-loading">
                  <n-spin size="small" />
                </div>
                <div v-if="!hasMoreMessages && (recentNotices.length > 0 || recentChats.length > 0)" class="message-no-more">
                  没有更多了
                </div>
              </div>
              <div class="message-footer">
                <n-button v-if="messageStore.noticeCount > 0 && messageTab === 'notice'" text size="small" @click="handleMarkAllRead">
                  全部已读
                </n-button>
                <n-button text type="primary" @click="goToMessage">查看全部</n-button>
              </div>
            </div>
          </n-popover>

          <n-dropdown :options="userOptions" @select="handleUserAction">
            <div class="user-info">
              <n-avatar
                round
                size="small"
                :src="userStore.avatar || undefined"
              >
                {{ userStore.nickname?.charAt(0) || 'U' }}
              </n-avatar>
              <span class="user-name">{{ userStore.nickname }}</span>
              <n-icon size="16">
                <ChevronDownOutline />
              </n-icon>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>

      <!-- 页签栏 -->
      <TabBar v-if="themeStore.showTabs" />

      <!-- 内容区 -->
      <n-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>

    <!-- 个人信息弹窗 -->
    <ProfileModal v-model:show="showProfileModal" />

    <!-- 修改密码弹窗 -->
    <PasswordModal v-model:show="showPasswordModal" />

    <!-- 消息通知弹窗 -->
    <MessageNotification />
  </n-layout>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, useMessage, useDialog, type MenuOption } from 'naive-ui'
import {
  HomeOutline,
  SettingsOutline,
  PersonOutline,
  PeopleOutline,
  MenuOutline,
  LogOutOutline,
  KeyOutline,
  ChevronDownOutline,
  BookOutline,
  BusinessOutline,
  GitNetworkOutline,
  IdCardOutline,
  DocumentTextOutline,
  ListOutline,
  LogInOutline,
  PulseOutline,
  PeopleCircleOutline,
  ServerOutline,
  DesktopOutline,
  SettingsSharp,
  FolderOpenOutline,
  DocumentOutline,
  CloudOutline,
  NotificationsOutline,
  ChatbubbleOutline,
  SearchOutline,
  ExpandOutline,
  ContractOutline,
  ColorPaletteOutline,
  CheckmarkOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useSiteStore } from '@/stores/site'
import { useThemeStore, themeColors } from '@/stores/theme'
import ProfileModal from '@/components/ProfileModal.vue'
import PasswordModal from '@/components/PasswordModal.vue'
import MessageNotification from '@/components/MessageNotification.vue'
import TabBar from '@/components/TabBar.vue'
import { noticeApi, chatApi, type SysNotice, type ChatMessage } from '@/api/message'
import { iconMap as externalIconMap } from '@/utils/icons'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const messageStore = useMessageStore()
const siteStore = useSiteStore()
const themeStore = useThemeStore()

// 站点配置
const siteName = computed(() => siteStore.siteName || 'Zion Admin')
const siteLogo = computed(() => siteStore.siteLogo)

// 注册全局message
window.$message = message

const collapsed = ref(false)
const showProfileModal = ref(false)
const showPasswordModal = ref(false)
const messageTab = ref('notice')

// 消息相关
const messageLoading = ref(false)
const recentNotices = ref<SysNotice[]>([])
const recentChats = ref<ChatMessage[]>([])
const messagePage = ref(1)
const hasMoreMessages = ref(true)
const messageListRef = ref<HTMLElement | null>(null)

// 搜索相关
const searchVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref<Array<{ key: string; label: string; path: string; iconName: string }>>([])

// 全屏相关
const isFullscreen = ref(false)

// 布局配置 - 使用 theme store
const layoutConfig = computed(() => ({
  siderPosition: themeStore.siderPosition,
  theme: themeStore.mode
}))

// 主题选项（仅黑白两种）
const themeOptions = [
  { value: 'dark' as const, color: '#001529', label: '暗色主题' },
  { value: 'light' as const, color: '#ffffff', label: '亮色主题' }
]

// 顶栏动态样式
const headerStyle = computed(() => {
  if (themeStore.headerUsePrimaryColor) {
    return {
      background: themeStore.primaryColor,
      borderBottomColor: themeStore.primaryColor
    }
  }
  return {}
})

// 初始化WebSocket和加载未读数
onMounted(() => {
  messageStore.initWebSocket()
  loadUnreadCount()
  // 监听全屏变化
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

// 加载未读数量
async function loadUnreadCount() {
  try {
    const [noticeCount, chatCount] = await Promise.all([
      noticeApi.getUnreadCount(),
      chatApi.getUnreadCount()
    ])
    messageStore.setUnreadCount(noticeCount, chatCount)
  } catch (error) {
    // 忽略错误
  }
}

// 跳转到消息页面
function goToMessage() {
  if (messageTab.value === 'notice') {
    router.push('/message/notice')
  } else {
    router.push('/message/chat')
  }
}

// 消息弹窗显示时加载数据
async function handleMessagePopoverShow(show: boolean) {
  if (show) {
    loadUnreadCount()
    // 重置分页
    messagePage.value = 1
    hasMoreMessages.value = true
    if (messageTab.value === 'notice') {
      recentNotices.value = []
      await loadRecentNotices()
    } else {
      recentChats.value = []
      await loadRecentChats()
    }
  }
}

// Tab 切换时加载数据
async function handleTabChange(tab: string) {
  // 重置分页
  messagePage.value = 1
  hasMoreMessages.value = true
  if (tab === 'notice') {
    recentNotices.value = []
    await loadRecentNotices()
  } else {
    recentChats.value = []
    await loadRecentChats()
  }
}

// 加载最近通知
async function loadRecentNotices(append = false) {
  if (messageLoading.value) return
  try {
    messageLoading.value = true
    const res = await noticeApi.myNotices({ page: messagePage.value, pageSize: 10 })
    const list = res.list || []
    if (append) {
      recentNotices.value = [...recentNotices.value, ...list]
    } else {
      recentNotices.value = list
    }
    hasMoreMessages.value = list.length >= 10
  } catch (error) {
    // 忽略错误
  } finally {
    messageLoading.value = false
  }
}

// 加载最近聊天
async function loadRecentChats(append = false) {
  if (messageLoading.value) return
  try {
    messageLoading.value = true
    const res = await chatApi.getContacts()
    const list = res || []
    if (append) {
      recentChats.value = [...recentChats.value, ...list]
    } else {
      recentChats.value = list
    }
    // 聊天联系人一次性加载，不分页
    hasMoreMessages.value = false
  } catch (error) {
    // 忽略错误
  } finally {
    messageLoading.value = false
  }
}

// 滚动加载更多
function handleMessageScroll(e: Event) {
  const target = e.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
  
  // 距离底部 50px 时加载更多
  if (scrollHeight - scrollTop - clientHeight < 50 && hasMoreMessages.value && !messageLoading.value) {
    messagePage.value++
    if (messageTab.value === 'notice') {
      loadRecentNotices(true)
    }
    // 聊天不做分页
  }
}

// 标记全部已读
async function handleMarkAllRead() {
  try {
    await noticeApi.markAllAsRead()
    messageStore.clearNoticeCount()
    // 刷新列表
    messagePage.value = 1
    await loadRecentNotices()
    message.success('已全部标记为已读')
  } catch (error) {
    // 忽略
  }
}

// 格式化消息时间
function formatMessageTime(time: string | undefined): string {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 去除 HTML 标签
function stripHtml(html: string | undefined): string {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').substring(0, 50)
}

// 点击通知
async function handleNoticeClick(item: SysNotice) {
  // 标记已读
  if (item.id) {
    try {
      await noticeApi.markAsRead(item.id)
      // 刷新未读数量
      loadUnreadCount()
    } catch (error) {
      // 忽略
    }
  }
  router.push({ path: '/message/notice', query: { id: item.id?.toString() } })
}

// 点击聊天
async function handleChatClick(item: ChatMessage) {
  // 标记已读
  if (item.senderId) {
    try {
      await chatApi.markAsRead(item.senderId)
      // 刷新未读数量
      loadUnreadCount()
    } catch (error) {
      // 忽略
    }
  }
  router.push({ path: '/message/chat', query: { userId: item.senderId?.toString() } })
}

// 搜索菜单
function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }

  const keyword = searchKeyword.value.toLowerCase()
  const results: Array<{ key: string; label: string; path: string; iconName: string }> = []

  // 遍历菜单获取所有可搜索项
  function searchMenu(options: any[], parentPath: string = '') {
    for (const item of options) {
      const label = item.label as string
      if (label.toLowerCase().includes(keyword)) {
        if (!item.children) {
          results.push({
            key: item.key,
            label: label,
            path: parentPath ? `${parentPath} / ${label}` : label,
            iconName: getIconName(item.key)
          })
        }
      }
      if (item.children) {
        searchMenu(item.children, parentPath ? `${parentPath} / ${label}` : label)
      }
    }
  }

  searchMenu(menuOptions.value)
  searchResults.value = results.slice(0, 10)
}

// 获取图标名称
function getIconName(key: string): string {
  const iconMapping: Record<string, string> = {
    '/dashboard': 'HomeOutline',
    '/system/user': 'PersonOutline',
    '/system/role': 'PeopleOutline',
    '/system/menu': 'MenuOutline',
    '/system/dict': 'BookOutline',
    '/system/config': 'SettingsSharp',
    '/org/dept': 'GitNetworkOutline',
    '/org/post': 'IdCardOutline',
    '/log/operlog': 'ListOutline',
    '/log/loginlog': 'LogInOutline',
    '/system/file': 'DocumentOutline',
    '/message/notice': 'NotificationsOutline',
    '/message/chat': 'ChatbubbleOutline',
    '/monitor/online': 'PeopleCircleOutline',
    '/monitor/cache': 'ServerOutline',
    '/monitor/server': 'DesktopOutline'
  }
  return iconMapping[key] || 'MenuOutline'
}

// 跳转到菜单
function goToMenu(item: { key: string }) {
  router.push(item.key)
  searchVisible.value = false
  searchKeyword.value = ''
  searchResults.value = []
}

// 切换全屏
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 监听全屏变化
function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// 设置布局位置
function setLayoutPosition(position: 'left' | 'right' | 'top') {
  themeStore.setSiderPosition(position)
}

// 设置主题
function setTheme(theme: 'dark' | 'light') {
  themeStore.setMode(theme)
}

// 图标映射
const iconMap: Record<string, any> = {
  HomeOutline,
  SettingsOutline,
  PersonOutline,
  PeopleOutline,
  MenuOutline,
  BookOutline,
  BusinessOutline,
  GitNetworkOutline,
  IdCardOutline,
  DocumentTextOutline,
  ListOutline,
  LogInOutline,
  PulseOutline,
  PeopleCircleOutline,
  ServerOutline,
  DesktopOutline,
  SettingsSharp,
  FolderOpenOutline,
  DocumentOutline,
  CloudOutline,
  NotificationsOutline,
  ChatbubbleOutline
}

// 合并外部图标映射（已在顶部导入）
Object.assign(iconMap, externalIconMap)

// 渲染图标
function renderIcon(iconName?: string) {
  if (!iconName) return undefined
  const icon = iconMap[iconName]
  if (!icon) return undefined
  return () => h(NIcon, null, { default: () => h(icon) })
}

// 将后台菜单数据转换为 Naive UI 菜单格式
function convertMenus(menus: typeof userStore.menus): MenuOption[] {
  if (!menus || !Array.isArray(menus)) {
    return []
  }
  return menus
    .filter(menu => menu.visible === 1 && menu.type !== 3) // 过滤隐藏菜单和按钮
    .sort((a, b) => a.sort - b.sort) // 按排序字段排序
    .map(menu => {
      // 确保菜单路径是绝对路径（以 / 开头）
      let menuPath = menu.path || `/menu-${menu.id}`
      if (menuPath && !menuPath.startsWith('/') && menu.type === 2) {
        menuPath = '/' + menuPath
      }
      
      // 外链菜单（目录或菜单类型）使用特殊的 key 格式：external:url
      const isExternal = menu.isFrame === 1 && menu.component
      const menuKey = isExternal ? `external:${menu.component}` : menuPath
      
      const option: MenuOption = {
        label: menu.name,
        key: menuKey,
        icon: renderIcon(menu.icon)
      }
      
      // 非外链菜单才处理子菜单
      if (!isExternal && menu.children && menu.children.length > 0) {
        const children = convertMenus(menu.children)
        if (children.length > 0) {
          option.children = children
        }
      }
      return option
    })
}

// 菜单配置 - 从后台动态获取
const menuOptions = computed<MenuOption[]>(() => {
  // 添加首页菜单
  const homeMenu: MenuOption = {
    label: '首页',
    key: '/dashboard',
    icon: renderIcon('HomeOutline')
  }
  
  // 从 userStore 获取动态菜单
  const dynamicMenus = convertMenus(userStore.menus)
  
  return [homeMenu, ...dynamicMenus]
})

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() => {
  const items: Array<{ path: string; title: string }> = []
  if (route.path === '/dashboard') {
    items.push({ path: '/dashboard', title: '首页' })
  } else if (route.path.startsWith('/system')) {
    items.push({ path: '/system', title: '系统管理' })
    if (route.path === '/system/user') {
      items.push({ path: '/system/user', title: '用户管理' })
    } else if (route.path === '/system/role') {
      items.push({ path: '/system/role', title: '角色管理' })
    } else if (route.path === '/system/menu') {
      items.push({ path: '/system/menu', title: '菜单管理' })
    }
  }
  return items
})

// 用户下拉选项
const userOptions = [
  {
    label: '个人中心',
    key: 'profile',
    icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
  },
  {
    label: '修改密码',
    key: 'password',
    icon: () => h(NIcon, null, { default: () => h(KeyOutline) })
  },
  {
    type: 'divider',
    key: 'd1'
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
]

// 菜单点击
function handleMenuClick(key: string) {
  // 判断是否是外链目录
  if (key.startsWith('external:')) {
    const url = key.replace('external:', '')
    window.open(url, '_blank')
    return
  }
  router.push(key)
}

// 用户操作
function handleUserAction(key: string) {
  if (key === 'logout') {
    dialog.warning({
      title: '提示',
      content: '确定要退出登录吗？',
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: () => {
        userStore.logout()
        message.success('已退出登录')
      }
    })
  } else if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'password') {
    showPasswordModal.value = true
  }
}
</script>

<style lang="scss" scoped>
.layout {
  height: 100vh;
}

.layout-sider {
  background: #FFFFFF;
  transition: background-color 0.3s;

  :deep(.n-layout-sider-scroll-container) {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

body.dark-theme .layout-sider {
  background: #18181c;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid #E5E7EB;
  transition: all 0.3s;

  &.logo-collapsed {
    padding: 0 16px;
    justify-content: center;
  }
}

body.dark-theme .logo {
  border-bottom-color: #3f3f46;
}

// 侧边栏logo区域应用主题色时的样式
.logo.logo-primary {
  .logo-text {
    color: #fff;
  }

  .logo-icon {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}

.logo-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFFFFF;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  transition: color 0.3s;
}

body.dark-theme .logo-text {
  color: #ffffffd1;
}

.layout-menu {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #FFFFFF;
  border-bottom: 1px solid #e8e8e8;
  transition: background-color 0.3s;
}

body.dark-theme .layout-header {
  background: #18181c;
  border-bottom: 1px solid #3f3f46;
}

// 顶栏应用主题色时的样式
.layout-header.header-primary {
  background: var(--primary-color) !important;
  border-bottom-color: var(--primary-color) !important;

  .header-icon {
    color: #fff;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
    }
  }

  .user-info {
    &:hover {
      background: rgba(255, 255, 255, 0.15);
    }
  }

  .user-name {
    color: #fff;
  }

  :deep(.n-breadcrumb-item__link),
  :deep(.n-breadcrumb-item__separator) {
    color: rgba(255, 255, 255, 0.85) !important;
  }

  // 顶部菜单模式下的 logo 文字颜色
  .header-logo .logo-text {
    color: #fff;
  }
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #F3F4F6;
  }
}

body.dark-theme .header-icon {
  &:hover {
    background: #3f3f46;
  }
}

.message-popover {
  margin: -12px;
}

.message-tabs {
  padding: 0 12px;
  border-bottom: 1px solid #e8e8e8;
}

.tab-text {
  display: inline-block;
  padding-top: 4px;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 8px 0;
}

.message-list .message-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f5f5;
  }
}

.message-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.message-item-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-item-content {
  flex: 1;
  min-width: 0;
}

.message-sender {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-list .message-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-list .message-content {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

.message-loading {
  padding: 12px;
  text-align: center;
}

.message-no-more {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #999;
}

.message-footer {
  padding: 12px 16px;
  text-align: center;
  border-top: 1px solid #e8e8e8;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: #F3F4F6;
  }
}

body.dark-theme .user-info {
  &:hover {
    background: #3f3f46;
  }
}

.user-name {
  font-size: 14px;
  color: #1F2937;
  font-weight: 500;
}

body.dark-theme .user-name {
  color: #ffffffd1;
}

// 头部固定
.layout-header {
  position: sticky;
  top: 0;
  z-index: 100;
}

// 页签栏固定（紧跟在头部下方）
:deep(.tab-bar) {
  position: sticky;
  top: 60px; // header高度
  z-index: 99;
}

.layout-content {
  background: #F3F4F6;
  transition: background-color 0.3s;
}

body.dark-theme .layout-content {
  background: #101014;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 搜索面板 */
.search-panel {
  margin: -12px;
  padding: 12px;
}

.search-results {
  margin-top: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: #f5f5f5;
  }
}

.result-icon {
  margin-right: 10px;
  color: #666;
}

.result-label {
  font-size: 14px;
  color: #333;
  flex: 1;
}

.result-path {
  font-size: 12px;
  color: #999;
}

/* 主题设置面板 */
.theme-panel {
  margin: -12px;
  padding: 16px;
}

.theme-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.theme-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

body.dark-theme .theme-title {
  color: #ffffffd1;
}

.layout-options {
  display: flex;
  gap: 12px;
}

.layout-option {
  flex: 1;
  text-align: center;
  cursor: pointer;

  span {
    display: block;
    font-size: 12px;
    color: #666;
    margin-top: 8px;
  }

  &.active span {
    color: #18a058;
  }
}

.layout-preview {
  height: 48px;
  border: 2px solid #e8e8e8;
  border-radius: 4px;
  display: flex;
  overflow: hidden;

  .layout-option.active & {
    border-color: #60a5fa;
  }
}

body.dark-theme .layout-preview {
  border-color: #3f3f46;
  
  .layout-option.active & {
    border-color: #60a5fa;
  }
}

.layout-left {
  .preview-sider {
    width: 30%;
    background: #001529;
  }
  .preview-main {
    flex: 1;
    background: #f5f5f5;
  }
}

.layout-right {
  .preview-main {
    flex: 1;
    background: #f5f5f5;
  }
  .preview-sider {
    width: 30%;
    background: #001529;
  }
}

.layout-top {
  flex-direction: column;

  .preview-header {
    height: 30%;
    background: #001529;
  }
  .preview-content {
    flex: 1;
    background: #f5f5f5;
  }
}

.theme-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.theme-modes {
  display: flex;
  gap: 16px;
}

.color-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  border: 2px solid transparent;

  &:hover {
    transform: scale(1.1);
  }

  &.active {
    border-color: #fff;
    box-shadow: 0 0 0 2px currentColor;
  }

  :deep(.n-icon) {
    font-size: 14px;
  }
}

.theme-mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;

  span {
    font-size: 12px;
    color: #666;
    margin-top: 8px;
  }

  &.active span {
    color: #18a058;
    font-weight: 500;
  }
}

.theme-mode-preview {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #e8e8e8;
  transition: all 0.2s;

  .theme-mode:hover & {
    transform: scale(1.05);
  }

  .theme-mode.active & {
    border-color: #60a5fa;
  }
}

body.dark-theme .theme-mode-preview {
  border-color: #3f3f46;
  
  .theme-mode.active & {
    border-color: #60a5fa;
  }
}

body.dark-theme .theme-mode span {
  color: #ffffffa6;
}

body.dark-theme .theme-mode.active span {
  color: #60a5fa;
}

/* 右侧菜单样式 */
.sider-right {
  order: 1;
}

/* 顶部布局样式 */
.layout-top {
  flex-direction: column;

  .layout-header {
    flex-wrap: nowrap;
  }
}

.header-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-right: 24px;

  .logo-icon {
    width: 32px;
    height: 32px;
    background: linear-gradient(135deg, #111827 0%, #374151 100%);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #FFFFFF;
    font-size: 18px;
    font-weight: 700;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    color: #111827;
    white-space: nowrap;
  }
}

.header-menu {
  flex: 1;
  overflow: hidden;

  :deep(.n-menu) {
    background: transparent;
  }
}

/* 主题风格 */
.theme-dark {
  background: #001529 !important;

  .logo-text {
    color: #fff;
  }

  :deep(.n-menu) {
    background: #001529;
    --n-item-text-color: rgba(255, 255, 255, 0.65);
    --n-item-text-color-hover: #fff;
    --n-item-text-color-active: #fff;
    --n-item-icon-color: rgba(255, 255, 255, 0.65);
    --n-item-icon-color-hover: #fff;
    --n-item-icon-color-active: #fff;
    --n-item-color-active: #18a058;
  }
}

.theme-light {
  background: #fff !important;
  border-right: 1px solid #e8e8e8;

  .logo-text {
    color: #333;
  }

  :deep(.n-menu) {
    background: #fff;
  }
}

// 暗色主题下的侧边栏样式覆盖
body.dark-theme .layout-sider {
  background: #18181c !important;
  border-right-color: #3f3f46 !important;
}

body.dark-theme .theme-light {
  background: #18181c !important;
  border-right: 1px solid #3f3f46;

  .logo-text {
    color: #ffffffd1;
  }

  .logo-icon {
    background: linear-gradient(135deg, #3f3f46 0%, #52525b 100%);
    color: #fff;
  }

  :deep(.n-menu) {
    background: #18181c;
    --n-item-text-color: rgba(255, 255, 255, 0.65);
    --n-item-text-color-hover: #fff;
    --n-item-text-color-active: #fff;
    --n-item-icon-color: rgba(255, 255, 255, 0.65);
    --n-item-icon-color-hover: #fff;
    --n-item-icon-color-active: #fff;
    --n-item-color-hover: #27272a;
    --n-item-color-active: #27272a;
    --n-item-color-active-hover: #3f3f46;
    --n-arrow-color: rgba(255, 255, 255, 0.5);
    --n-arrow-color-hover: rgba(255, 255, 255, 0.8);
    --n-arrow-color-active: rgba(255, 255, 255, 0.8);
  }
}

body.dark-theme .theme-dark {
  background: #101014 !important;

  :deep(.n-menu) {
    background: #101014;
  }
}

</style>
