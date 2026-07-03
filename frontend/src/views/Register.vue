<template>
  <div class="register-page">
    <el-card class="register-card">
      <h2>注册 NavBI Pro</h2>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="邮箱">
          <el-input v-model="form.email" autocomplete="email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="form.code" maxlength="6" placeholder="6 位数字" />
            <el-button :disabled="countdown > 0 || sending" :loading="sending" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码（至少 8 位）">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="submit">注册</el-button>
      </el-form>
      <p class="hint">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'

const form = reactive({ email: '', code: '', password: '' })
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const router = useRouter()
let timer = null

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function startCountdown() {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

onUnmounted(() => clearInterval(timer))

function errMsg(e, fallback) {
  return e.response?.data?.message || fallback
}

async function sendCode() {
  if (!EMAIL_RE.test(form.email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  sending.value = true
  try {
    await http.post('/auth/register/code', { email: form.email })
    ElMessage.success('验证码已发送，请查收邮箱（含垃圾箱）')
    startCountdown()
  } catch (e) {
    ElMessage.error(errMsg(e, '验证码发送失败'))
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!EMAIL_RE.test(form.email) || !form.code || form.password.length < 8) {
    ElMessage.warning('请填写邮箱、验证码和至少 8 位的密码')
    return
  }
  loading.value = true
  try {
    await http.post('/auth/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(errMsg(e, '注册失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #eef4fc 0%, #f9f9f7 100%);
}
.register-card {
  width: 380px;
}
.register-card h2 {
  margin: 0 0 20px;
  text-align: center;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.submit {
  width: 100%;
  margin-top: 8px;
}
.hint {
  margin: 16px 0 0;
  text-align: center;
  font-size: 13px;
  color: #898781;
}
.hint a {
  color: #2a78d6;
}
</style>
