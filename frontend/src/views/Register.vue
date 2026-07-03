<template>
  <AuthShell title="创建账号" subtitle="使用邮箱验证码完成注册">
    <el-form :model="form" label-position="top" size="large" @submit.prevent="submit">
      <el-form-item label="邮箱">
        <el-input v-model="form.email" autocomplete="email" placeholder="you@example.com" />
      </el-form-item>
      <el-form-item label="验证码">
        <CodeInput v-model="form.code" :email="form.email" endpoint="/auth/register/code" />
      </el-form-item>
      <el-form-item label="密码（至少 8 位）">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="设置登录密码"
        />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">注 册</el-button>
    </el-form>

    <template #footer>
      已有账号？<router-link to="/login">去登录</router-link>
    </template>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import AuthShell from '../components/AuthShell.vue'
import CodeInput from '../components/CodeInput.vue'

const form = reactive({ email: '', code: '', password: '' })
const loading = ref(false)
const router = useRouter()

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

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
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
