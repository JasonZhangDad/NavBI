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
      <el-table-column label="今日下载" width="100">
        <template #default="{ row }">
          {{ row.downloadsUsedToday || 0 }}/{{ row.dailyDownloadLimit || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="每日额度" width="130">
        <template #default="{ row }">
          <el-input-number
            v-model="row.dailyDownloadLimit"
            size="small"
            :min="0"
            :controls="false"
            class="limit-input"
          />
        </template>
      </el-table-column>
      <el-table-column label="今日剩余" width="90">
        <template #default="{ row }">{{ remainingToday(row) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="actions">
            <el-button size="small" type="primary" plain @click="saveDownloadLimit(row)">保存额度</el-button>
            <el-button
              size="small"
              :type="row.enabled ? 'danger' : 'success'"
              plain
              @click="toggleEnabled(row)"
            >{{ row.enabled ? '封禁' : '解封' }}</el-button>
          </div>
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

async function saveDownloadLimit(row) {
  const dailyDownloadLimit = Number(row.dailyDownloadLimit)
  if (!Number.isInteger(dailyDownloadLimit) || dailyDownloadLimit < 0) {
    ElMessage.error('每日额度必须是非负整数')
    return
  }
  await http.patch(`/admin/users/${row.id}/download-limit`, { dailyDownloadLimit })
  ElMessage.success('额度已保存')
  load()
}

function remainingToday(row) {
  return Math.max(Number(row.dailyDownloadLimit || 0) - Number(row.downloadsUsedToday || 0), 0)
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
.limit-input {
  width: 92px;
}
.actions {
  display: flex;
  gap: 8px;
}
.actions .el-button {
  margin-left: 0;
}
</style>
