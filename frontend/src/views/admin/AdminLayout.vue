<template>
  <el-container class="admin">
    <StarfieldBackground />
    <el-aside width="200px" class="aside">
      <div class="brand">NavBI <em>Pro</em></div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/admin/dashboard">📊 数据大屏</el-menu-item>
        <el-menu-item index="/admin/nav">🧭 导航管理</el-menu-item>
        <el-menu-item index="/admin/categories">🗂️ 分类设置</el-menu-item>
        <el-menu-item index="/admin/users">👥 用户管理</el-menu-item>
        <el-menu-item index="/admin/api-logs">📜 接口日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="body">
      <el-header class="header">
        <router-link to="/" class="link">← 返回导航站</router-link>
        <div class="header-actions">
          <button class="download-link" type="button" @click="downloadDialogVisible = true">
            下载客户端
          </button>
          <el-button text @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <ClientDownloadDialog v-model="downloadDialogVisible" />
  </el-container>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { animate, stagger } from 'animejs'
import { useAuthStore } from '../../stores/auth'
import ClientDownloadDialog from '../../components/ClientDownloadDialog.vue'
import StarfieldBackground from '../../components/StarfieldBackground.vue'

const router = useRouter()
const auth = useAuthStore()
const downloadDialogVisible = ref(false)

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  // 启用 Element Plus 暗色变量（弹窗/表格/表单等自动变暗），离开后台时移除
  document.documentElement.classList.add('dark')
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    animate('.menu .el-menu-item', {
      opacity: [0, 1],
      translateX: [-14, 0],
      duration: 450,
      delay: stagger(60),
      ease: 'outCubic'
    })
  }
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('dark')
})
</script>

<style scoped>
.admin {
  min-height: 100vh;
  background:
    radial-gradient(1000px 420px at 70% -10%, rgba(57, 135, 229, 0.12), transparent 60%),
    #0d1120;
  color: #e8eaf2;
}
.aside,
.body {
  position: relative;
  z-index: 1;
}
.aside {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}
.brand {
  padding: 20px 16px;
  font-weight: 700;
  font-size: 18px;
  color: #ffffff;
}
.brand em {
  font-style: normal;
  background: linear-gradient(135deg, #5aa2f0 0%, #9085e9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.menu {
  border-right: none;
  background: transparent;
}
.menu :deep(.el-menu-item) {
  color: #b6bdd0;
  border-radius: 8px;
  margin: 2px 8px;
}
.menu :deep(.el-menu-item:hover) {
  background: rgba(57, 135, 229, 0.12);
  color: #6ea8f0;
}
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(57, 135, 229, 0.35), rgba(35, 97, 184, 0.35));
  color: #ffffff;
  box-shadow: 0 0 14px rgba(57, 135, 229, 0.25);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.link {
  color: #9aa3b8;
  text-decoration: none;
  font-size: 14px;
}
.link:hover {
  color: #6ea8f0;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.download-link {
  color: #2fce96;
  font-size: 14px;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
}
.download-link:hover {
  color: #1baf7a;
}
</style>
