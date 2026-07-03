<template>
  <div>
    <div class="toolbar">
      <h3>用户管理</h3>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe empty-text="暂无注册用户">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="role" label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '正常' : '已封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="代理权限" width="100">
        <template #default="{ row }">
          <el-tag :type="row.proxyEnabled ? 'warning' : ''" size="small">
            {{ row.proxyEnabled ? '已开启' : '未开启' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :type="row.enabled ? 'danger' : 'success'"
            plain
            @click="toggleEnabled(row)"
          >{{ row.enabled ? '封禁' : '解封' }}</el-button>
          <el-button
            size="small"
            :type="row.proxyEnabled ? 'warning' : 'primary'"
            plain
            @click="toggleProxy(row)"
          >{{ row.proxyEnabled ? '关闭代理' : '开启代理' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await http.get('/admin/users', { params: { page: page.value, size: size.value } })
    list.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function toggleEnabled(row) {
  const action = row.enabled ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定要${action}用户 ${row.email} 吗？`, '确认操作', { type: 'warning' })
  await http.patch(`/admin/users/${row.id}/enabled`, { enabled: !row.enabled })
  ElMessage.success(`${action}成功`)
  load()
}

async function toggleProxy(row) {
  const action = row.proxyEnabled ? '关闭' : '开启'
  await ElMessageBox.confirm(`确定要${action}用户 ${row.email} 的代理权限吗？`, '确认操作', { type: 'warning' })
  await http.patch(`/admin/users/${row.id}/proxy`, { proxyEnabled: !row.proxyEnabled })
  ElMessage.success(`代理权限已${action}`)
  load()
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar h3 {
  margin: 0;
  font-size: 16px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
