<template>
  <AuthShell title="个人中心" :subtitle="auth.username ? `已登录：${auth.username}` : '你已登录 NavBI Pro'">
    <p class="tip">个人功能正在建设中，敬请期待。</p>
    <div class="actions">
      <el-button size="large" type="primary" @click="downloadDialogVisible = true">下载客户端</el-button>
      <el-button size="large" @click="$router.push('/')">回到导航首页</el-button>
      <el-button size="large" @click="changePassword">修改密码</el-button>
      <el-button size="large" type="danger" plain @click="logout">退出登录</el-button>
    </div>
    <ClientDownloadDialog v-model="downloadDialogVisible" />
  </AuthShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthShell from '../components/AuthShell.vue'
import ClientDownloadDialog from '../components/ClientDownloadDialog.vue'

const router = useRouter()
const auth = useAuthStore()
const downloadDialogVisible = ref(false)

function changePassword() {
  router.push({ path: '/reset-password', query: auth.username ? { email: auth.username } : {} })
}

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.tip {
  margin: 0 0 20px;
  color: #52514e;
  font-size: 14px;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.actions .el-button {
  width: 100%;
  margin: 0;
  border-radius: 10px;
}
</style>
