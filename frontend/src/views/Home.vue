<template>
  <div class="portal">
    <header class="hero">
      <!-- 右上角用户区 -->
      <div class="topbar">
        <template v-if="auth.token">
          <button class="tb-btn tb-download" type="button" @click="downloadDialogVisible = true">
            下载客户端
          </button>
          <router-link class="tb-link" :to="auth.role === 'ADMIN' ? '/admin/dashboard' : '/me'">
            <span class="tb-avatar">{{ (auth.username || auth.role || 'U')[0].toUpperCase() }}</span>
            <span class="tb-name">{{ auth.username || (auth.role === 'ADMIN' ? '管理员' : '我的') }}</span>
          </router-link>
        </template>
        <template v-else>
          <router-link class="tb-btn tb-ghost" to="/login">登录</router-link>
          <router-link class="tb-btn tb-primary" to="/register">注册</router-link>
        </template>
      </div>

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
            :target="linkTarget(item.url)"
            :rel="linkRel(item.url)"
            @click="handleItemClick(item)"
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

    <footer class="site-footer">
      <p>
        NavBI Pro 精选收录 AI 工具（ChatGPT、Claude、Gemini、Copilot）、开发资源（GitHub、Stack
        Overflow）、设计素材、影音娱乐与科技资讯等常用网站，支持关键词搜索和分类过滤，帮你一站直达目标站点。
      </p>
      <p class="footer-links">
        <router-link to="/login">登录</router-link>
        <span aria-hidden="true">·</span>
        <router-link to="/register">注册</router-link>
      </p>
    </footer>

    <ClientDownloadDialog v-model="downloadDialogVisible" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../api'
import { trackVisit, getSessionId } from '../track'
import { iconSrc, fallbackEmoji } from '../icon'
import { useAuthStore } from '../stores/auth'
import ClientDownloadDialog from '../components/ClientDownloadDialog.vue'

const auth = useAuthStore()

const groups = ref([])
const keyword = ref('')
const activeCategory = ref('全部')
const loading = ref(true)
const downloadDialogVisible = ref(false)

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
  const sessionId = getSessionId()
  const headers = sessionId ? { 'X-Session-Id': sessionId } : {}
  fetch(`/api/nav/click/${item.id}`, { method: 'POST', headers, keepalive: true }).catch(() => {})
}

function handleItemClick(item) {
  onClick(item)
}

const iconFailed = reactive({})

function isExternalUrl(url) {
  try {
    return new URL(url, window.location.origin).origin !== window.location.origin
  } catch {
    return false
  }
}

function linkTarget(url) {
  return isExternalUrl(url) ? '_blank' : '_self'
}

function linkRel(url) {
  return isExternalUrl(url) ? 'noopener' : null
}

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
  position: relative;
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
.site-footer {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
  border-top: 1px solid rgba(11, 11, 11, 0.08);
  color: #898781;
  font-size: 13px;
  text-align: center;
}
.site-footer p {
  margin: 0 0 8px;
}
.footer-links {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.footer-links a {
  color: #898781;
  text-decoration: none;
}
.footer-links a:hover {
  color: #2a78d6;
}

/* ── 右上角 Topbar ── */
.topbar {
  position: absolute;
  top: 18px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 10;
}
.tb-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s;
  white-space: nowrap;
  cursor: pointer;
}
.tb-ghost {
  border: 1px solid rgba(42, 120, 214, 0.4);
  color: #2a78d6;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(6px);
}
.tb-ghost:hover {
  background: rgba(42, 120, 214, 0.08);
  border-color: #2a78d6;
}
.tb-download {
  border: 1px solid rgba(27, 175, 122, 0.35);
  color: #0e7a54;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(6px);
}
.tb-download:hover {
  background: rgba(27, 175, 122, 0.1);
  border-color: #1baf7a;
}
.tb-primary {
  background: linear-gradient(135deg, #2a78d6 0%, #1a5cb8 100%);
  color: #fff;
  border: 1px solid transparent;
  box-shadow: 0 2px 8px rgba(42, 120, 214, 0.3);
}
.tb-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(42, 120, 214, 0.4);
}
.tb-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  text-decoration: none;
  color: #2a78d6;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 12px 5px 5px;
  border-radius: 999px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(42, 120, 214, 0.2);
  transition: all 0.15s;
}
.tb-link:hover {
  background: rgba(42, 120, 214, 0.08);
  border-color: #2a78d6;
}
.tb-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2a78d6, #1a5cb8);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tb-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
