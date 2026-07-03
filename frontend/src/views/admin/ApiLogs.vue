<template>
  <div>
    <div class="toolbar">
      <h3>接口日志</h3>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="records" v-loading="loading" stripe empty-text="暂无日志">
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status < 400 ? 'success' : row.status === 401 || row.status === 403 ? 'warning' : 'danger'" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时(ms)" width="90" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="username" label="用户" width="140" show-overflow-tooltip />
      <el-table-column prop="userAgent" label="User-Agent" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api'

const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/bi/api-logs', { params: { page: page.value, size: size.value } })
    records.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.toolbar h3 {
  margin: 0;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
