<template>
  <div class="portal">
    <header class="hero">
      <h1>NavBI <span>Pro</span></h1>
      <p class="slogan">统一导航入口 · 访问数据大脑</p>
      <el-input
        v-model="keyword"
        class="search"
        size="large"
        placeholder="搜索导航（标题 / 网址）"
        clearable
      />
      <nav class="cats">
        <button
          v-for="cat in categories"
          :key="cat"
          class="cat"
          :class="{ active: cat === activeCategory }"
          @click="activeCategory = cat"
        >
          {{ cat }}
        </button>
      </nav>
    </header>

    <main class="content">
      <el-empty v-if="!loading && displayGroups.length === 0" description="没有匹配的导航" />
      <section v-for="group in displayGroups" :key="group.category" class="group">
        <h2>{{ group.category }}</h2>
        <div class="grid">
          <a
            v-for="item in group.items"
            :key="item.id"
            class="card"
            :href="item.url"
            target="_blank"
            rel="noopener"
            @click="onClick(item)"
          >
            <span class="icon">
              <img
                v-if="!iconFailed[item.id]"
                :src="iconSrc(item)"
                alt=""
                loading="lazy"
                @error="iconFailed[item.id] = true"
              />
              <template v-else>{{ fallbackEmoji(item) }}</template>
            </span>
            <span class="meta">
              <span class="title">{{ item.title }}</span>
              <span class="host">{{ host(item.url) }}</span>
            </span>
          </a>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../api'
import { trackVisit, getSessionId } from '../track'
import { iconSrc, fallbackEmoji } from '../icon'

const groups = ref([])
const keyword = ref('')
const activeCategory = ref('全部')
const loading = ref(true)

/** 分类过滤条：由数据动态生成，后台增删分类自动生效 */
const categories = computed(() => ['全部', ...groups.value.map((g) => g.category)])

const displayGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return groups.value
    .filter((g) => activeCategory.value === '全部' || g.category === activeCategory.value)
    .map((g) => ({
      ...g,
      items: kw
        ? g.items.filter(
            (i) => i.title.toLowerCase().includes(kw) || i.url.toLowerCase().includes(kw)
          )
        : g.items
    }))
    .filter((g) => g.items.length > 0)
})

async function load() {
  loading.value = true
  try {
    const res = await http.get('/nav/list')
    groups.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onClick(item) {
  http.post(`/nav/click/${item.id}`, null, { headers: { 'X-Session-Id': getSessionId() } }).catch(() => {})
}

const iconFailed = reactive({})

function host(url) {
  try {
    return new URL(url).host
  } catch {
    return url
  }
}

onMounted(() => {
  load()
  trackVisit('/')
})
</script>

<style scoped>
.portal {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.hero {
  padding: 64px 24px 40px;
  text-align: center;
  background: linear-gradient(180deg, #eef4fc 0%, #f9f9f7 100%);
}
.hero h1 {
  margin: 0;
  font-size: 40px;
  letter-spacing: 1px;
}
.hero h1 span {
  color: #2a78d6;
}
.slogan {
  margin: 8px 0 28px;
  color: #52514e;
}
.search {
  max-width: 520px;
}
.cats {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.cat {
  border: 1px solid rgba(11, 11, 11, 0.12);
  background: #fcfcfb;
  color: #52514e;
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.cat:hover {
  border-color: #2a78d6;
  color: #2a78d6;
}
.cat.active {
  background: #2a78d6;
  border-color: #2a78d6;
  color: #fff;
}
.content {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
}
.group h2 {
  font-size: 16px;
  color: #52514e;
  margin: 28px 0 12px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
}
.card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fcfcfb;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(11, 11, 11, 0.08);
}
.icon {
  font-size: 24px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
}
.meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.title {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.host {
  font-size: 12px;
  color: #898781;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
