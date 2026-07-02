/**
 * 图标一律走后端代理接口（服务端抓取+缓存，规避浏览器网络环境差异），
 * 加载失败由组件回退到 emoji。
 */
export function iconSrc(item) {
  return `/api/nav/icon/${item.id}`
}

/** emoji 兜底：icon 字段是图片 URL 时用默认符号 */
export function fallbackEmoji(item) {
  return item.icon && !/^https?:\/\//.test(item.icon) ? item.icon : '🔗'
}
