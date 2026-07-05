<template>
  <div class="auth-page">
    <StarfieldBackground />
    <div class="blob blob-a" aria-hidden="true"></div>
    <div class="blob blob-b" aria-hidden="true"></div>

    <div ref="cardEl" class="auth-card">
      <router-link to="/" class="brand">
        <span class="brand-mark">🧭</span>
        <span class="brand-name">NavBI <em>Pro</em></span>
      </router-link>
      <h1 class="title">{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>

      <slot />

      <div v-if="$slots.footer" class="footer">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { animate, stagger } from 'animejs'
import StarfieldBackground from './StarfieldBackground.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})

const cardEl = ref()

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animate(cardEl.value, {
    opacity: [0, 1],
    translateY: [28, 0],
    duration: 550,
    ease: 'outCubic'
  })
  animate(cardEl.value.children, {
    opacity: [0, 1],
    translateY: [14, 0],
    duration: 500,
    delay: stagger(70, { start: 120 }),
    ease: 'outCubic'
  })
})
</script>

<style scoped>
.auth-page {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(900px 480px at 20% -10%, rgba(57, 135, 229, 0.16), transparent 60%),
    radial-gradient(720px 420px at 90% 110%, rgba(144, 133, 233, 0.14), transparent 60%),
    #0a0e1a;
  overflow: hidden;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.18;
  pointer-events: none;
}
.blob-a {
  width: 420px;
  height: 420px;
  background: #2361b8;
  top: -120px;
  left: -100px;
}
.blob-b {
  width: 360px;
  height: 360px;
  background: #6a5bd8;
  bottom: -100px;
  right: -80px;
}
.auth-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
  background: rgba(20, 24, 38, 0.75);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.5), 0 0 40px rgba(57, 135, 229, 0.12);
  padding: 36px 32px 28px;
  color: #e8eaf2;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: inherit;
  margin-bottom: 20px;
}
.brand-mark {
  font-size: 22px;
}
.brand-name {
  font-weight: 700;
  font-size: 17px;
  letter-spacing: 0.5px;
}
.brand-name em {
  font-style: normal;
  background: linear-gradient(135deg, #5aa2f0 0%, #9085e9 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
}
.subtitle {
  margin: 0 0 24px;
  font-size: 14px;
  color: #9aa3b8;
}
.footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
  font-size: 13px;
  color: #9aa3b8;
}
.footer :deep(a) {
  color: #6ea8f0;
  text-decoration: none;
  font-weight: 500;
}
.footer :deep(a:hover) {
  text-decoration: underline;
}

/* 表单控件统一风格 */
.auth-card :deep(.el-form-item__label) {
  font-weight: 500;
  color: #c3c2b7;
  padding-bottom: 4px;
}
.auth-card :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 14px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14) inset;
  transition: box-shadow 0.15s;
}
.auth-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #3987e5 inset;
}
.auth-card :deep(.el-input__inner) {
  height: 40px;
  color: #e8eaf2;
}
.auth-card :deep(.el-input__inner::placeholder) {
  color: #7d879e;
}
.auth-card :deep(.el-input__icon) {
  color: #7d879e;
}
.auth-card :deep(.submit-btn) {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b8ae6 0%, #2a78d6 60%, #2361b8 100%);
  box-shadow: 0 6px 20px rgba(57, 135, 229, 0.4);
  transition: transform 0.12s, box-shadow 0.12s;
}
.auth-card :deep(.submit-btn:hover) {
  transform: translateY(-1px);
  box-shadow: 0 8px 26px rgba(57, 135, 229, 0.55);
}
.auth-card :deep(.submit-btn:active) {
  transform: none;
}
</style>
