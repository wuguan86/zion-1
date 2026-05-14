<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onBeforeMount(() => {
  const { params } = route
  const path = '/' + (Array.isArray(params.path) ? params.path.join('/') : params.path)
  router.replace(path)
})
</script>

<template>
  <div />
</template>
