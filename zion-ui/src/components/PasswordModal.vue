<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="修改密码"
    :style="{ width: '450px' }"
    :mask-closable="false"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="100"
      require-mark-placement="right-hanging"
    >
      <n-form-item label="当前密码" path="oldPassword">
        <n-input
          v-model:value="formData.oldPassword"
          type="password"
          show-password-on="click"
          placeholder="请输入当前密码"
        />
      </n-form-item>
      <n-form-item label="新密码" path="newPassword">
        <n-input
          v-model:value="formData.newPassword"
          type="password"
          show-password-on="click"
          placeholder="请输入新密码"
        />
      </n-form-item>
      <n-form-item label="确认新密码" path="confirmPassword">
        <n-input
          v-model:value="formData.confirmPassword"
          type="password"
          show-password-on="click"
          placeholder="请再次输入新密码"
        />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">取消</n-button>
        <n-button type="primary" :loading="loading" @click="handleSubmit">确定</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FormInst, FormRules, FormItemRule } from 'naive-ui'
import { authApi } from '@/api/auth'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const showModal = ref(false)

const formData = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: FormItemRule, value: string): boolean | Error => {
  if (value !== formData.value.newPassword) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const rules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

watch(() => props.show, (val) => {
  showModal.value = val
  if (val) {
    resetForm()
  }
})

watch(showModal, (val) => {
  emit('update:show', val)
})

function resetForm() {
  formData.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

function handleClose() {
  showModal.value = false
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await authApi.updatePassword({
      oldPassword: formData.value.oldPassword,
      newPassword: formData.value.newPassword
    })
    window.$message?.success('密码修改成功，请重新登录')
    handleClose()
    // 可选：修改密码后自动退出登录
    // setTimeout(() => {
    //   userStore.logout()
    // }, 1500)
  } catch (error: any) {
    if (error?.message) {
      window.$message?.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>
