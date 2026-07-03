<template>
  <AuthShell title="修改密码" subtitle="通过邮箱验证码设置新密码">
    <el-form :model="form" label-position="top" size="large" @submit.prevent="submit">
      <el-form-item label="邮箱">
        <el-input v-model="form.email" autocomplete="email" placeholder="注册时使用的邮箱" />
      </el-form-item>
      <el-form-item label="验证码">
        <CodeInput v-model="form.code" :email="form.email" endpoint="/auth/password/code" />
      </el-form-item>
      <el-form-item label="新密码（至少 8 位）">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="设置新密码"
        />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">确认修改</el-button>
    </el-form>

    <template #footer>
      想起密码了？<router-link to="/login">去登录</router-link>
    </template>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import { useAuthStore } from '../stores/auth'
import AuthShell from '../components/AuthShell.vue'
import CodeInput from '../components/CodeInput.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({ email: route.query.email || '', code: '', password: '' })
const loading = ref(false)

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

async function submit() {
  if (!EMAIL_RE.test(form.email) || !form.code || form.password.length < 8) {
    ElMessage.warning('请填写邮箱、验证码和至少 8 位的新密码')
    return
  }
  loading.value = true
  try {
    await http.post('/auth/password/reset', form)
    ElMessage.success('密码已修改，请重新登录')
    auth.logout()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>
