<template>
  <div v-if="visible" class="watermark-container" ref="containerRef"></div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useSiteStore } from '@/stores/site'
import { useUserStore } from '@/stores/user'

const siteStore = useSiteStore()
const userStore = useUserStore()
const containerRef = ref<HTMLElement | null>(null)

// 是否显示水印
const visible = computed(() => siteStore.watermarkEnabled && userStore.isLogin)

// 获取水印文本
const watermarkText = computed(() => {
  const type = siteStore.watermarkType
  // 优先使用 nickname，其次使用 username
  const username = userStore.user?.nickname || userStore.user?.username || userStore.nickname || '用户'
  
  switch (type) {
    case 'username':
      return username
    case 'username_time':
      const now = new Date()
      const dateStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      return `${username} ${dateStr}`
    case 'sitename':
      return siteStore.siteName || 'Zion Admin'
    case 'custom':
      return siteStore.watermarkCustomText || siteStore.siteName || 'Zion Admin'
    default:
      return username
  }
})

// 创建水印
function createWatermark() {
  if (!containerRef.value) return
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // 设置画布大小
  const width = 250
  const height = 180
  canvas.width = width
  canvas.height = height
  
  // 设置水印样式 - 透明度范围 0.05 ~ 0.5，配置值需要放大
  const configOpacity = siteStore.watermarkOpacity || 0.1
  const opacity = Math.max(0.08, Math.min(0.5, configOpacity * 1.5))
  ctx.font = '16px Arial, sans-serif'
  ctx.fillStyle = `rgba(100, 100, 100, ${opacity})`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  // 旋转画布
  ctx.translate(width / 2, height / 2)
  ctx.rotate(-25 * Math.PI / 180)
  
  // 绘制水印文字
  ctx.fillText(watermarkText.value, 0, 0)
  
  // 设置背景
  containerRef.value.style.backgroundImage = `url(${canvas.toDataURL('image/png')})`
}

// 监听变化重新绘制水印
watch(
  [
    visible, 
    watermarkText, 
    () => siteStore.watermarkOpacity,
    () => siteStore.watermarkType,
    () => siteStore.watermarkCustomText,
    () => siteStore.watermarkEnabled,
    () => siteStore.siteName,
    () => userStore.user
  ], 
  () => {
    nextTick(() => {
      if (visible.value && containerRef.value) {
        createWatermark()
      }
    })
  }, 
  { immediate: true, deep: true }
)

onMounted(() => {
  nextTick(() => {
    if (visible.value) {
      createWatermark()
    }
  })
})
</script>

<style scoped>
.watermark-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 9999;
  background-repeat: repeat;
}
</style>
