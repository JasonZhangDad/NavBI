<template>
  <div>
    <div class="toolbar">
      <h3>分类设置</h3>
      <el-button type="primary" @click="openDialog()">新增分类</el-button>
    </div>

    <el-table :data="pagedCategories" v-loading="loading" stripe empty-text="暂无分类">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" min-width="180" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="categories.length"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const pageSize = ref(20)
const emptyForm = { id: null, name: '', sort: 0 }
const form = reactive({ ...emptyForm })

const pagedCategories = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return categories.value.slice(start, start + pageSize.value)
})

watch([categories, pageSize], () => {
  const maxPage = Math.max(1, Math.ceil(categories.value.length / pageSize.value))
  if (page.value > maxPage) {
    page.value = maxPage
  }
})

async function load() {
  loading.value = true
  try {
    const res = await http.get('/nav/categories')
    categories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm, row || {})
  dialogVisible.value = true
}

async function save() {
  form.name = form.name.trim()
  if (!form.name) {
    ElMessage.warning('分类名称必填')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await http.put('/nav/categories', form)
    } else {
      await http.post('/nav/categories', form)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
  await http.delete(`/nav/categories/${row.id}`)
  ElMessage.success('已删除')
  load()
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
