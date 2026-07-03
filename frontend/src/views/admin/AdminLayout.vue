<template>
  <el-container class="admin">
    <el-aside width="200px" class="aside">
      <div class="brand">NavBI Pro</div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/admin/dashboard">📊 数据大屏</el-menu-item>
        <el-menu-item index="/admin/nav">🧭 导航管理</el-menu-item>
        <el-menu-item index="/admin/categories">🗂️ 分类设置</el-menu-item>
        <el-menu-item index="/admin/api-logs">📜 接口日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <router-link to="/" class="link">← 返回导航站</router-link>
        <el-button text @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin {
  min-height: 100vh;
}
.aside {
  background: #fcfcfb;
  border-right: 1px solid rgba(11, 11, 11, 0.1);
}
.brand {
  padding: 20px 16px;
  font-weight: 700;
  font-size: 18px;
  color: #2a78d6;
}
.menu {
  border-right: none;
  background: transparent;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fcfcfb;
  border-bottom: 1px solid rgba(11, 11, 11, 0.1);
}
.link {
  color: #52514e;
  text-decoration: none;
  font-size: 14px;
}
.link:hover {
  color: #2a78d6;
}
</style>
