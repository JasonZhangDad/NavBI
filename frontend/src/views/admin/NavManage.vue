<template>
  <div>
    <div class="toolbar">
      <h3>导航管理</h3>
      <div class="toolbar-actions">
        <el-select v-model="categoryFilter" placeholder="全部分类" clearable class="category-filter">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.name"
          />
        </el-select>
        <el-button type="primary" @click="openDialog()">新增导航</el-button>
      </div>
    </div>

    <el-table :data="displayItems" v-loading="loading" stripe empty-text="没有匹配的导航">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图标" width="70">
        <template #default="{ row }">
          <img
            v-if="!iconFailed[row.id]"
            :src="iconSrc(row)"
            class="row-icon"
            alt=""
            @error="iconFailed[row.id] = true"
          />
          <span v-else>{{ fallbackEmoji(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="url" label="网址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="clickCount" label="点击 PV" width="90" />
      <el-table-column prop="uvCount" label="点击 UV" width="90" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v) => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑导航' : '新增导航'" width="480px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="网址" required>
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" filterable placeholder="请选择分类" class="full">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="留空自动取网站 logo；可填图片 URL 覆盖，emoji 作加载失败兜底" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api'
import { iconSrc, fallbackEmoji } from '../../icon'

const items = ref([])
const categories = ref([])
const categoryFilter = ref('')
const iconFailed = reactive({})
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const emptyForm = { id: null, title: '', url: '', category: '默认', icon: '', sort: 0, enabled: true }
const form = reactive({ ...emptyForm })

const displayItems = computed(() => {
  if (!categoryFilter.value) {
    return items.value
  }
  return items.value.filter((item) => item.category === categoryFilter.value)
})

async function load() {
  loading.value = true
  try {
    const [navRes, categoryRes] = await Promise.all([http.get('/nav/all'), http.get('/nav/categories')])
    items.value = navRes.data || []
    categories.value = categoryRes.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm, row || {})
  if (!form.category && categories.value.length > 0) {
    form.category = categories.value[0].name
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.title || !form.url || !form.category) {
    ElMessage.warning('标题、网址和分类必填')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await http.put('/nav/update', form)
    } else {
      await http.post('/nav/add', form)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, enabled) {
  await http.put('/nav/update', { id: row.id, enabled })
  row.enabled = enabled
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？`, '提示', { type: 'warning' })
  await http.delete(`/nav/delete/${row.id}`)
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
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar h3 {
  margin: 0;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.category-filter {
  width: 180px;
}
.row-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
}
.full {
  width: 100%;
}
</style>
