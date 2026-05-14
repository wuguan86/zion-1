<template>
  <div class="page-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <!-- 左侧欢迎信息 -->
      <div class="welcome-info">
        <div class="welcome-header">
          <n-avatar round :size="56" :src="userStore.avatar || undefined">
            {{ userStore.nickname?.charAt(0) || 'U' }}
          </n-avatar>
          <div class="welcome-text">
            <h1 class="welcome-title">
              {{ getGreeting() }}，{{ userStore.nickname }} 👋
            </h1>
            <p class="welcome-desc">
              这是您的管理控制台，您可以在这里管理系统的各项功能
            </p>
          </div>
        </div>
        <div class="welcome-time">
          <div class="time-display">{{ currentTime }}</div>
          <div class="date-display">{{ currentDate }}</div>
        </div>
      </div>
      <!-- 右侧轮播Banner -->
      <div class="welcome-banner">
        <n-carousel autoplay :interval="5000" dot-type="line" show-arrow="hover" class="banner-carousel">
          <div v-for="(banner, index) in banners" :key="index" class="banner-item"
               :style="{ background: banner.bgColor }">
            <div class="banner-content">
              <div class="banner-text">
                <h3 class="banner-title">{{ banner.title }}</h3>
                <p class="banner-subtitle">{{ banner.subtitle }}</p>
              </div>
              <div class="banner-icon">
                <n-icon :size="64" :color="banner.iconColor">
                  <component :is="banner.icon"/>
                </n-icon>
              </div>
            </div>
          </div>
        </n-carousel>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <n-card v-for="stat in stats" :key="stat.title" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ background: stat.bgColor }">
            <n-icon size="24" :color="stat.color">
              <component :is="stat.icon"/>
            </n-icon>
          </div>
          <div class="stat-info">
            <n-skeleton v-if="loading" :width="60" :height="28"/>
            <div v-else class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </n-card>
    </div>

    <!-- 中间区域：快捷入口 + 更新日志 -->
    <n-grid :x-gap="20" :cols="2" class="middle-section">
      <!-- 快捷入口 -->
      <n-gi>
        <n-card title="快捷入口" class="shortcuts-card">
          <div class="shortcuts-grid">
            <div
                v-for="shortcut in shortcuts"
                :key="shortcut.path"
                class="shortcut-item"
                @click="router.push(shortcut.path)"
            >
              <div class="shortcut-icon" :style="{ background: shortcut.bgColor }">
                <n-icon size="24" :color="shortcut.color">
                  <component :is="shortcut.icon"/>
                </n-icon>
              </div>
              <div class="shortcut-name">{{ shortcut.name }}</div>
            </div>
          </div>
        </n-card>
      </n-gi>

      <!-- 更新日志 -->
      <n-gi>
        <n-card title="更新日志" class="changelog-card">
          <n-timeline>
            <n-timeline-item
                v-for="log in changelog"
                :key="log.version"
                :type="log.type"
                :title="log.version"
                :time="log.date"
            >
              <ul class="changelog-list">
                <li v-for="(item, idx) in log.changes" :key="idx">{{ item }}</li>
              </ul>
            </n-timeline-item>
          </n-timeline>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 底部区域：系统信息 + 作者介绍 -->
    <n-grid :x-gap="20" :cols="2" class="bottom-section">
      <!-- 系统信息 -->
      <n-gi>
        <n-card title="系统信息" class="system-card">
          <n-descriptions :column="1" label-placement="left">
            <n-descriptions-item label="系统名称">Zion Admin</n-descriptions-item>
            <n-descriptions-item label="系统版本">v1.0.7</n-descriptions-item>
            <n-descriptions-item label="前端框架">Vue 3.4 + Naive UI</n-descriptions-item>
            <n-descriptions-item label="后端框架">Spring Boot 4.0</n-descriptions-item>
            <n-descriptions-item label="数据库">PgSQL 17.0</n-descriptions-item>
            <n-descriptions-item label="缓存">Redis 8.0</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>

      <!-- 作者介绍 -->
      <n-gi>
        <n-card title="关于作者" class="author-card">
          <div class="author-content">

            <div class="author-info">
              <h3 class="author-name">Zion</h3>
              <p class="author-desc">全栈开发，专注于后台管理系统的开发与优化。</p>
              <div class="author-links">

              </div>
            </div>
          </div>
          <n-divider/>
          <div class="project-info">
            <p class="project-desc">
              Zion Admin 是一个基于 Spring Boot 4 + Vue 3 的现代化后台管理系统，
              采用最新的技术栈，提供完整的权限管理、系统监控等功能。
            </p>
            <div class="project-stats">
              <div class="project-stat-item">
                <n-icon size="18" color="#F59E0B">
                  <Star/>
                </n-icon>
                <span>开源免费</span>
              </div>
              <div class="project-stat-item">
                <n-icon size="18" color="#10B981">
                  <Refresh/>
                </n-icon>
                <span>持续更新</span>
              </div>
              <div class="project-stat-item">
                <n-icon size="18" color="#3B82F6">
                  <DocumentText/>
                </n-icon>
                <span>文档完善</span>
              </div>
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, onUnmounted, markRaw} from 'vue'
import {useRouter} from 'vue-router'
import {
  PersonOutline,
  PeopleOutline,
  MenuOutline,
  ShieldCheckmarkOutline,
  LogoGithub,
  LogoGitlab,
  LogoWechat,
  Globe,
  Mail,
  Star,
  Refresh,
  DocumentText,
  SettingsOutline,
  TimerOutline,
  ServerOutline,
  RocketOutline,
  SparklesOutline,
  CodeSlashOutline,
  CloudOutline,
  ChatbubbleOutline
} from '@vicons/ionicons5'
import {useUserStore} from '@/stores/user'
import {dashboardApi} from '@/api/system'

const router = useRouter()
const userStore = useUserStore()

const currentTime = ref('')
const currentDate = ref('')
const loading = ref(true)

// 获取问候语
function getGreeting() {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
}

// 轮播Banner数据
const banners = [
  {
    title: 'Zion Admin',
    subtitle: '现代化后台管理系统',
    bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: markRaw(RocketOutline),
    iconColor: 'rgba(255,255,255,0.3)'
  },
  {
    title: '技术栈',
    subtitle: 'Spring Boot 4 + Vue 3',
    bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    icon: markRaw(CodeSlashOutline),
    iconColor: 'rgba(255,255,255,0.3)'
  },
  {
    title: '功能全面',
    subtitle: '持续更新 · 文档完善',
    bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    icon: markRaw(SparklesOutline),
    iconColor: 'rgba(255,255,255,0.3)'
  },
  {
    title: '云端部署',
    subtitle: '支持 Docker 一键部署',
    bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    icon: markRaw(CloudOutline),
    iconColor: 'rgba(255,255,255,0.3)'
  }
]

// 统计数据
const stats = ref([
  {
    title: '用户总数',
    value: 0,
    icon: markRaw(PersonOutline),
    color: '#111827',
    bgColor: '#F3F4F6'
  },
  {
    title: '角色数量',
    value: 0,
    icon: markRaw(PeopleOutline),
    color: '#059669',
    bgColor: '#D1FAE5'
  },
  {
    title: '菜单数量',
    value: 0,
    icon: markRaw(MenuOutline),
    color: '#2563EB',
    bgColor: '#DBEAFE'
  },
  {
    title: '权限数量',
    value: 0,
    icon: markRaw(ShieldCheckmarkOutline),
    color: '#D97706',
    bgColor: '#FEF3C7'
  }
])

// 快捷入口
const shortcuts = [
  {
    name: '用户管理',
    path: '/system/user',
    icon: markRaw(PersonOutline),
    color: '#111827',
    bgColor: '#F3F4F6'
  },
  {
    name: '角色管理',
    path: '/system/role',
    icon: markRaw(PeopleOutline),
    color: '#059669',
    bgColor: '#D1FAE5'
  },
  {
    name: '菜单管理',
    path: '/system/menu',
    icon: markRaw(MenuOutline),
    color: '#2563EB',
    bgColor: '#DBEAFE'
  },
  {
    name: '系统配置',
    path: '/system/config',
    icon: markRaw(SettingsOutline),
    color: '#7C3AED',
    bgColor: '#EDE9FE'
  },
  {
    name: '定时任务',
    path: '/monitor/job',
    icon: markRaw(TimerOutline),
    color: '#DC2626',
    bgColor: '#FEE2E2'
  },
  {
    name: '服务监控',
    path: '/monitor/server',
    icon: markRaw(ServerOutline),
    color: '#0891B2',
    bgColor: '#CFFAFE'
  }
]

// 更新日志
const changelog = [
  {
    version: 'v1.0.7',
    date: '2026-03-01',
    type: 'success' as const,
    changes: [
      '代码生成：新增表单布局配置，支持「一行两列」和「从上到下」两种布局',
      '代码生成：编辑配置弹窗新增「布局配置」标签页',
      '代码生成：修复 LocalDate/LocalDateTime 字段 JSON 解析错误',
      '代码生成：日期时间字段提交格式与 Jackson 配置（yyyy-MM-dd HH:mm:ss）兼容',
      '代码生成：编辑时日期字符串正确转换为时间戳供日期选择器使用'
    ]
  },
  {
    version: 'v1.0.6',
    date: '2026-03-01',
    type: 'success' as const,
    changes: [
      '系统通知：移除短信渠道，Webhook 拆分为飞书/钉钉/企业微信分别选择',
      '钉钉推送：支持加签密钥（SEC），确保安全校验',
      'zion-push 模块重构：支持文本和图片，统一 Webhook 发送逻辑',
      '新增通知记录：可查看各渠道推送触达情况及成功/失败状态',
      '推送失败支持重试：通知记录中失败渠道可一键重试'
    ]
  },
  {
    version: 'v1.0.5',
    date: '2026-02-28',
    type: 'success' as const,
    changes: [
      '数据库连接池升级为 HikariCP',
      '定时任务新增 Cron 表达式常用预设选择',
      '定时任务新增调度日志查看功能',
      '定时任务新增调度统计图表（执行数、成功/失败比例）',
      '缓存监控页面新增 ECharts 统计图（内存、QPS、命中率、连接数）',
      '修复通知类型表单校验问题',
      '优化统计卡片样式（透明背景）'
    ]
  },
  {
    version: 'v1.0.4',
    date: '2026-02-24',
    type: 'success' as const,
    changes: [
      '新增用户批量导入导出功能（EasyExcel）',
      '导入模板支持角色和岗位字段',
      '新增用户多选导出功能',
      '新增用户批量删除功能',
      '优化文件下载认证处理'
    ]
  },
  {
    version: 'v1.0.3',
    date: '2026-02-24',
    type: 'success' as const,
    changes: [
      '新增前端反调试控制（安全配置开关）',
      '优化系统配置页面按钮布局',
      '优化弹窗按钮主题色适配',
      '新增多种主题颜色选择',
      '修复操作日志耗时统计问题'
    ]
  },
  {
    version: 'v1.0.2',
    date: '2026-02-13',
    type: 'success' as const,
    changes: [
      '新增 RustFS 对象存储支持',
      '新增腾讯云 COS 存储支持',
      '优化存储配置页面布局，访问域名按存储类型分组',
      '修复 Office 文档预览样式问题',
      '修复 PDF 预览需要登录的问题',
      '支持大文件上传（最大 500MB）',
      '优化文件列表全选效果',
      '文件管理新增拖拽上传提示'
    ]
  },
  {
    version: 'v1.0.1',
    date: '2026-01-31',
    type: 'success' as const,
    changes: [
      '新增暗黑主题模式，支持一键切换',
      '优化首页布局，新增轮播 Banner',
      '新增邮件配置及测试发送功能',
      '新增接口加密功能（全局/部分加密）',
      '新增 RSA 密钥自动生成功能',
      '优化即时聊天页面暗黑模式适配'
    ]
  },
  {
    version: 'v1.0.0',
    date: '2026-01-29',
    type: 'info' as const,
    changes: [
      '新增文件存储策略工厂（本地/MinIO/OSS/COS）',
      '新增推送服务策略工厂（极光/友盟/个推）',
      '新增短信/支付服务策略工厂',
      '优化登录页面（三种样式+滑块验证码）',
      '完善系统配置分组管理'
    ]
  },
  {
    version: 'v0.9.0',
    date: '2026-01-25',
    type: 'info' as const,
    changes: [
      '新增即时通讯功能（WebSocket私聊/群聊）',
      '完成字典管理和系统配置功能',
      '实现部门和岗位管理',
      '完成定时任务管理功能'
    ]
  },
  {
    version: 'v0.8.0',
    date: '2026-01-20',
    type: 'default' as const,
    changes: [
      '搭建项目基础框架',
      '集成 Sa-Token 实现认证授权',
      '完成基础权限管理（用户、角色、菜单）',
      '实现登录日志和操作日志记录',
      '添加系统监控功能'
    ]
  }
]

// 加载统计数据
async function loadStats() {
  try {
    loading.value = true
    const data = await dashboardApi.getStats()
    stats.value[0].value = data.userCount
    stats.value[1].value = data.roleCount
    stats.value[2].value = data.menuCount
    stats.value[3].value = data.permissionCount
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

// 更新时间
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {hour12: false})
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

let timer: number
onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
  loadStats()
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style lang="scss" scoped>
// 欢迎区域
.welcome-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.welcome-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
}

.welcome-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 6px 0;
}

.welcome-desc {
  font-size: 14px;
  color: #6B7280;
  margin: 0;
}

.welcome-time {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-top: 16px;
}

.time-display {
  font-size: 36px;
  font-weight: 700;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.date-display {
  font-size: 14px;
  color: #6B7280;
}

// 轮播Banner
.welcome-banner {
  width: 380px;
  flex-shrink: 0;
}

.banner-carousel {
  height: 100%;
  border-radius: 12px;
  overflow: hidden;

  :deep(.n-carousel__slides) {
    height: 100%;
  }

  :deep(.n-carousel__slide) {
    height: 100%;
  }

  :deep(.n-carousel__dots) {
    bottom: 12px;
  }

  :deep(.n-carousel__dot) {
    background: rgba(255, 255, 255, 0.5);

    &.n-carousel__dot--active {
      background: #fff;
    }
  }

  :deep(.n-carousel__arrow) {
    background: rgba(255, 255, 255, 0.2);
    color: #fff;

    &:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  }
}

.banner-item {
  height: 100%;
  min-height: 140px;
  padding: 24px;
  display: flex;
  align-items: center;
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.banner-text {
  flex: 1;
}

.banner-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px 0;
}

.banner-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.banner-icon {
  opacity: 0.6;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  :deep(.n-card__content) {
    padding: 20px;
  }
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
}

.stat-title {
  font-size: 14px;
  color: #6B7280;
  margin-top: 4px;
}

.middle-section {
  margin-bottom: 20px;
}

.shortcuts-card {
  height: 100%;
}

.shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #F3F4F6;
  }
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.shortcut-name {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}

.changelog-card {
  height: 100%;

  :deep(.n-card__content) {
    max-height: 280px;
    overflow-y: auto;
  }
}

.changelog-list {
  margin: 0;
  padding-left: 16px;
  font-size: 13px;
  color: #6B7280;

  li {
    margin-bottom: 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.bottom-section {
  margin-bottom: 20px;
}

.system-card {
  height: 100%;

  :deep(.n-descriptions) {
    --n-th-padding: 10px 12px;
    --n-td-padding: 10px 12px;
  }
}

.author-card {
  height: 100%;
}

.author-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.author-avatar {
  flex-shrink: 0;
}

.author-info {
  flex: 1;
}

.author-name {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.author-desc {
  font-size: 14px;
  color: #6B7280;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.author-links {
  margin-top: 8px;
}

.author-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #e5e7eb;
    color: #111827;
  }
}

.wechat-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}

.project-info {
  margin-top: 4px;
}

.project-desc {
  font-size: 13px;
  color: #6B7280;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.project-stats {
  display: flex;
  gap: 24px;
}

.project-stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #374151;
}

@media (max-width: 1200px) {
  .welcome-section {
    flex-direction: column;
  }

  .welcome-banner {
    width: 100%;
  }

  .banner-item {
    min-height: 120px;
  }

  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .middle-section,
  .bottom-section {
    :deep(.n-grid) {
      display: block;
    }

    :deep(.n-gi) {
      margin-bottom: 20px;
    }
  }
}

@media (max-width: 768px) {
  .welcome-header {
    flex-direction: column;
    text-align: center;
  }

  .welcome-time {
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }

  .stat-cards {
    grid-template-columns: 1fr;
  }

  .shortcuts-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .author-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .project-stats {
    justify-content: center;
    flex-wrap: wrap;
  }
}

</style>
