<template>
  <div class="image-upload">
    <n-upload
      :action="uploadUrl"
      :headers="headers"
      :max="1"
      list-type="image-card"
      v-model:file-list="fileList"
      @finish="handleFinish"
      @remove="handleRemove"
      accept="image/*"
    >
      上传图片
    </n-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { type UploadFileInfo } from 'naive-ui'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const userStore = useUserStore()

const uploadUrl = '/api/sys/file/upload/image'
const headers = computed(() => ({
  'Authorization': userStore.token || ''
}))

// 文件列表
const fileList = ref<UploadFileInfo[]>([])

// 监听 modelValue 变化，回显图片
watch(() => props.modelValue, (val) => {
  if (val && fileList.value.length === 0) {
    fileList.value = [{
      id: 'default',
      name: 'image',
      status: 'finished',
      url: val
    }]
  } else if (!val) {
    fileList.value = []
  }
}, { immediate: true })

// 上传完成
function handleFinish({ file, event }: { file: UploadFileInfo, event?: ProgressEvent }) {
  try {
    const response = JSON.parse((event?.target as XMLHttpRequest).response)
    console.log('上传响应:', response)
    if ((response.code === 0 || response.code === 200) && response.data) {
      // 返回的是 SysFile 对象，优先使用 url
      const data = response.data as { url?: string; filePath?: string } | string
      const url = typeof data === 'string' ? data : (data.url || data.filePath)
      if (url) {
        console.log('图片URL:', url)
        emit('update:modelValue', url)
        file.url = url
        file.status = 'finished'
      }
    }
  } catch (e) {
    console.error('上传失败', e)
  }
  return file
}

// 移除图片
function handleRemove() {
  emit('update:modelValue', undefined)
  return true
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
  gap: 4px;
}

:deep(.n-upload-file-list .n-upload-file.n-upload-file--image-card-type) {
  width: 100px;
  height: 100px;
}

:deep(.n-upload-trigger.n-upload-trigger--image-card) {
  width: 100px;
  height: 100px;
}
</style>
