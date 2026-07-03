<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>登录 NavBI Pro</h2>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名 / 邮箱">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" class="submit">登录</el-button>
      </el-form>
      <p class="hint">
        没有账号？<router-link to="/register">邮箱注册</router-link>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'
import { useAuthStore } from '../stores/auth'

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
    auth.setAuth(res.data.token, res.data.role)
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #eef4fc 0%, #f9f9f7 100%);
}
.login-card {
  width: 360px;
}
.login-card h2 {
  margin: 0 0 20px;
  text-align: center;
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
