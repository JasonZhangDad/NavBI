<template>
  <div class="code-row">
    <el-input
      :model-value="modelValue"
      maxlength="6"
      placeholder="6 位数字"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <el-button class="send-btn" :disabled="countdown > 0 || sending" :loading="sending" @click="send">
      {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
    </el-button>
  </div>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const props = defineProps({
  modelValue: { type: String, default: '' },
  email: { type: String, default: '' },
  /** 发码接口，如 /auth/register/code */
  endpoint: { type: String, required: true }
})
defineEmits(['update:modelValue'])

const sending = ref(false)
const countdown = ref(0)
let timer = null

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

onUnmounted(() => clearInterval(timer))

async function send() {
  if (!EMAIL_RE.test(props.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  sending.value = true
  try {
    await http.post(props.endpoint, { email: props.email })
    ElMessage.success('验证码已发送，请查收邮箱（含垃圾箱）')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '验证码发送失败')
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.send-btn {
  height: 48px;
  min-width: 104px;
  border-radius: 10px;
}
</style>
