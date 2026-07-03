<template>
  <div class="me-page">
    <el-card class="me-card">
      <h2>欢迎回来</h2>
      <p>你已登录 NavBI Pro。个人功能正在建设中，敬请期待。</p>
      <div class="actions">
        <el-button @click="$router.push('/')">回到导航首页</el-button>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.me-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #eef4fc 0%, #f9f9f7 100%);
}
.me-card {
  width: 420px;
  text-align: center;
}
.me-card h2 {
  margin: 0 0 12px;
}
.me-card p {
  color: #52514e;
  margin: 0 0 20px;
}
.actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
