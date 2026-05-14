<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="个人信息"
    :style="{ width: '500px' }"
    :mask-closable="false"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="80"
      require-mark-placement="right-hanging"
    >
      <n-form-item label="用户名">
        <n-input v-model:value="formData.username" disabled />
      </n-form-item>
      <n-form-item label="昵称" path="nickname">
        <n-input v-model:value="formData.nickname" placeholder="请输入昵称" />
      </n-form-item>
      <n-form-item label="邮箱" path="email">
        <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
      </n-form-item>
      <n-form-item label="手机号" path="mobile">
        <n-input v-model:value="formData.mobile" placeholder="请输入手机号" />
      </n-form-item>
      <n-form-item label="头像">
        <n-input v-model:value="formData.avatar" placeholder="请输入头像URL" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">取消</n-button>
        <n-button type="primary" :loading="loading" @click="handleSubmit">保存</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInst, FormRules } from 'naive-ui'
import { authApi, type ProfileInfo } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const userStore = useUserStore()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const showModal = ref(false)

const formData = ref<Partial<ProfileInfo>>({
  username: '',
  nickname: '',
  email: '',
  mobile: '',
  avatar: ''
})

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

watch(() => props.show, async (val) => {
  showModal.value = val
  if (val) {
    await loadProfile()
  }
})

watch(showModal, (val) => {
  emit('update:show', val)
})

async function loadProfile() {
  try {
    const data = await authApi.getProfile()
    formData.value = {
      username: data.username,
      nickname: data.nickname,
      email: data.email || '',
      mobile: data.mobile || '',
      avatar: data.avatar || ''
    }
  } catch (error) {
    console.error('加载个人信息失败', error)
  }
}

function handleClose() {
  showModal.value = false
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await authApi.updateProfile({
      nickname: formData.value.nickname,
      email: formData.value.email,
      mobile: formData.value.mobile,
      avatar: formData.value.avatar
    })
    window.$message?.success('保存成功')
    // 更新store中的用户信息
    await userStore.getInfo()
    handleClose()
  } catch (error: any) {
    if (error?.message) {
      window.$message?.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>
