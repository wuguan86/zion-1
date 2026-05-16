<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { normalizeFileUrl } from '@/utils/fileUrl'

type AvatarSize = 'small' | 'medium' | 'large' | number

const props = withDefaults(defineProps<{
  src?: string | null
  name?: string | null
  size?: AvatarSize
}>(), {
  src: '',
  name: '',
  size: 'medium'
})

const loadFailed = ref(false)

const avatarSrc = computed(() => normalizeFileUrl(props.src))
const avatarText = computed(() => props.name?.trim().charAt(0) || 'U')
const avatarSize = computed(() => {
  if (typeof props.size === 'number') {
    return `${props.size}px`
  }

  const sizeMap: Record<Exclude<AvatarSize, number>, string> = {
    small: '28px',
    medium: '40px',
    large: '56px'
  }
  return sizeMap[props.size]
})

const avatarFontSize = computed(() => {
  const parsedSize = Number.parseInt(avatarSize.value, 10)
  return `${Math.max(12, Math.round(parsedSize * 0.36))}px`
})

watch(avatarSrc, () => {
  loadFailed.value = false
})

function handleError() {
  loadFailed.value = true
}
</script>

<template>
  <span
    class="user-avatar"
    :style="{
      width: avatarSize,
      height: avatarSize,
      fontSize: avatarFontSize
    }"
  >
    <img
      v-if="avatarSrc && !loadFailed"
      :src="avatarSrc"
      :alt="name || '用户头像'"
      class="user-avatar-img"
      @error="handleError"
    />
    <span v-else class="user-avatar-text">{{ avatarText }}</span>
  </span>
</template>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 50%;
  background: #c9c9c9;
  color: #fff;
  font-weight: 500;
  line-height: 1;
}

.user-avatar-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
</style>
