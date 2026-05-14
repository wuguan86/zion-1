<template>
  <div class="iframe-container">
    <n-spin :show="loading" description="加载中...">
      <iframe
        ref="iframeRef"
        :src="frameSrc"
        class="iframe-content"
        frameborder="0"
        allowfullscreen
        @load="handleLoad"
      />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const iframeRef = ref<HTMLIFrameElement | null>(null)
const loading = ref(true)

// 从路由 meta 中获取外链地址
const frameSrc = computed(() => {
  return (route.meta.frameSrc as string) || ''
})

function handleLoad() {
  loading.value = false
}

onMounted(() => {
  // 如果 5 秒后还在加载，强制关闭 loading（防止跨域问题导致 load 事件不触发）
  setTimeout(() => {
    loading.value = false
  }, 5000)
})
</script>

<style lang="scss" scoped>
.iframe-container {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 120px);
  position: relative;
  
  :deep(.n-spin-container) {
    height: 100%;
  }
  
  :deep(.n-spin-content) {
    height: 100%;
  }
}

.iframe-content {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 120px);
  border: none;
}
</style>
