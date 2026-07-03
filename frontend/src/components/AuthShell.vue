<template>
  <div class="auth-page">
    <div class="blob blob-a" aria-hidden="true"></div>
    <div class="blob blob-b" aria-hidden="true"></div>

    <div class="auth-card">
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
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' }
})
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(160deg, #eef4fc 0%, #f9f9f7 60%, #f3eefc 100%);
  overflow: hidden;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
}
.blob-a {
  width: 420px;
  height: 420px;
  background: #8fc0f5;
  top: -120px;
  left: -100px;
}
.blob-b {
  width: 360px;
  height: 360px;
  background: #c9b8f0;
  bottom: -100px;
  right: -80px;
}
.auth-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  box-shadow: 0 24px 64px rgba(42, 120, 214, 0.14), 0 2px 8px rgba(11, 11, 11, 0.05);
  padding: 36px 32px 28px;
  animation: rise 0.35s ease-out;
}
@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: none;
  }
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
  color: #2a78d6;
}
.title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1c1c1a;
}
.subtitle {
  margin: 0 0 24px;
  font-size: 14px;
  color: #898781;
}
.footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(11, 11, 11, 0.06);
  text-align: center;
  font-size: 13px;
  color: #898781;
}
.footer :deep(a) {
  color: #2a78d6;
  text-decoration: none;
  font-weight: 500;
}
.footer :deep(a:hover) {
  text-decoration: underline;
}

/* 表单控件统一风格 */
.auth-card :deep(.el-form-item__label) {
  font-weight: 500;
  color: #52514e;
  padding-bottom: 4px;
}
.auth-card :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px rgba(11, 11, 11, 0.12) inset;
  transition: box-shadow 0.15s;
}
.auth-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2a78d6 inset;
}
.auth-card :deep(.el-input__inner) {
  height: 40px;
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
  box-shadow: 0 6px 16px rgba(42, 120, 214, 0.35);
  transition: transform 0.12s, box-shadow 0.12s;
}
.auth-card :deep(.submit-btn:hover) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(42, 120, 214, 0.45);
}
.auth-card :deep(.submit-btn:active) {
  transform: none;
}
</style>
