<template>
  <div class="portal">
    <StarfieldBackground />

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
            @pointermove="onCardMove"
            @pointerleave="onCardLeave"
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
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { animate, stagger } from 'animejs'
import http from '../api'
import { getSessionId } from '../track'
import { iconSrc, fallbackEmoji } from '../icon'
import { useAuthStore } from '../stores/auth'
import ClientDownloadDialog from '../components/ClientDownloadDialog.vue'
import StarfieldBackground from '../components/StarfieldBackground.vue'

const auth = useAuthStore()

const groups = ref([])
const keyword = ref('')
const activeCategory = ref('全部')
const loading = ref(true)
const downloadDialogVisible = ref(false)

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

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

/** 卡片交错浮入（首次加载与切换分类时播放；搜索输入不触发，避免打字闪动） */
function animateCards() {
  if (reducedMotion) return
  animate('.portal .card', {
    opacity: [0, 1],
    translateY: [24, 0],
    duration: 550,
    delay: stagger(30),
    ease: 'outCubic'
  })
}

async function load() {
  loading.value = true
  try {
    const res = await http.get('/nav/list')
    groups.value = res.data || []
    await nextTick()
    animateCards()
  } finally {
    loading.value = false
  }
}

watch(activeCategory, async () => {
  await nextTick()
  animateCards()
})

function onClick(item) {
  const sessionId = getSessionId()
  const headers = sessionId ? { 'X-Session-Id': sessionId } : {}
  fetch(`/api/nav/click/${item.id}`, { method: 'POST', headers, keepalive: true }).catch(() => {})
}

function handleItemClick(item) {
  onClick(item)
}

/** 鼠标悬停 3D 倾斜：按光标在卡片内的位置绕 X/Y 轴旋转 */
function onCardMove(e) {
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  const px = (e.clientX - rect.left) / rect.width - 0.5
  const py = (e.clientY - rect.top) / rect.height - 0.5
  el.style.transform = `perspective(700px) rotateX(${(-py * 8).toFixed(2)}deg) rotateY(${(px * 10).toFixed(2)}deg) translateY(-2px)`
}

function onCardLeave(e) {
  e.currentTarget.style.transform = ''
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
  if (!reducedMotion) {
    animate('.hero > *:not(.topbar)', {
      opacity: [0, 1],
      translateY: [16, 0],
      duration: 600,
      delay: stagger(80),
      ease: 'outCubic'
    })
  }
})
</script>

<style scoped>
.portal {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(1100px 520px at 50% -10%, rgba(57, 135, 229, 0.18), transparent 60%),
    radial-gradient(820px 420px at 85% 15%, rgba(144, 133, 233, 0.12), transparent 60%),
    #0a0e1a;
  color: #e8eaf2;
}
.hero {
  position: relative;
  z-index: 2;
  padding: 64px 24px 40px;
  text-align: center;
}
.hero h1 {
  margin: 0;
  font-size: clamp(30px, 8vw, 40px);
  letter-spacing: 1px;
  color: #fff;
  text-shadow: 0 0 28px rgba(57, 135, 229, 0.4);
}
.hero h1 span {
  background: linear-gradient(135deg, #5aa2f0 0%, #9085e9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.slogan {
  margin: 8px 0 28px;
  color: #9aa3b8;
}
.search {
  max-width: 520px;
}
.search :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
  backdrop-filter: blur(10px);
  border-radius: 12px;
}
.search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #3987e5 inset, 0 0 20px rgba(57, 135, 229, 0.25);
}
.search :deep(.el-input__inner) {
  color: #e8eaf2;
}
.search :deep(.el-input__inner::placeholder) {
  color: #7d879e;
}
.cats {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.cat {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.04);
  color: #b6bdd0;
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.cat:hover {
  border-color: #3987e5;
  color: #6ea8f0;
}
.cat.active {
  background: linear-gradient(135deg, #3987e5 0%, #2361b8 100%);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 0 16px rgba(57, 135, 229, 0.45);
}
.content {
  position: relative;
  z-index: 2;
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
}
.content :deep(.el-empty__description p) {
  color: #7d879e;
}
.group h2 {
  font-size: 16px;
  color: #b6bdd0;
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
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  transition: transform 0.12s ease-out, box-shadow 0.2s, border-color 0.2s;
  will-change: transform;
}
.card:hover {
  border-color: rgba(57, 135, 229, 0.55);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45), 0 0 24px rgba(57, 135, 229, 0.18);
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
  color: #7d879e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.site-footer {
  position: relative;
  z-index: 2;
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #7d879e;
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
  color: #7d879e;
  text-decoration: none;
}
.footer-links a:hover {
  color: #6ea8f0;
}

/* ── 右上角 Topbar ── */
.topbar {
  position: absolute;
  top: 18px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 3;
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
  border: 1px solid rgba(110, 168, 240, 0.45);
  color: #6ea8f0;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(6px);
}
.tb-ghost:hover {
  background: rgba(57, 135, 229, 0.15);
  border-color: #3987e5;
}
.tb-download {
  border: 1px solid rgba(27, 175, 122, 0.45);
  color: #2fce96;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(6px);
}
.tb-download:hover {
  background: rgba(27, 175, 122, 0.15);
  border-color: #1baf7a;
}
.tb-primary {
  background: linear-gradient(135deg, #3987e5 0%, #2361b8 100%);
  color: #fff;
  border: 1px solid transparent;
  box-shadow: 0 2px 12px rgba(57, 135, 229, 0.45);
}
.tb-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 18px rgba(57, 135, 229, 0.6);
}
.tb-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  text-decoration: none;
  color: #6ea8f0;
  font-size: 13px;
  font-weight: 500;
  padding: 5px 12px 5px 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(110, 168, 240, 0.3);
  transition: all 0.15s;
}
.tb-link:hover {
  background: rgba(57, 135, 229, 0.15);
  border-color: #3987e5;
}
.tb-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3987e5, #2361b8);
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
