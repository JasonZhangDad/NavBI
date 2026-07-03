<template>
  <AuthShell title="欢迎回来" subtitle="登录后可使用个人功能与管理后台">
    <el-form :model="form" label-position="top" size="large" @submit.prevent="submit">
      <el-form-item label="用户名 / 邮箱">
        <el-input v-model="form.username" autocomplete="username" placeholder="admin 或 you@example.com" />
      </el-form-item>
      <el-form-item label="密码">
        <div class="pwd-field">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="输入密码"
          />
          <router-link class="forgot" to="/reset-password">忘记密码？</router-link>
        </div>
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">登 录</el-button>
    </el-form>

    <template #footer>
      没有账号？<router-link to="/register">邮箱注册</router-link>
    </template>
  </AuthShell>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import { useAuthStore } from '../stores/auth'
import AuthShell from '../components/AuthShell.vue'

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    auth.setAuth(res.data.token, res.data.role, form.username)
    router.push(res.data.role === 'ADMIN' ? '/admin/dashboard' : '/me')
  } catch (e) {
    if (e.response?.status === 401) {
      ElMessage.error('用户名或密码错误')
    } else if (e.response?.status === 429) {
      ElMessage.error(e.response.data?.message || '尝试过于频繁，请稍后再试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.pwd-field {
  width: 100%;
}
.forgot {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #898781;
  text-decoration: none;
  text-align: right;
  width: 100%;
}
.forgot:hover {
  color: #2a78d6;
}
</style>
