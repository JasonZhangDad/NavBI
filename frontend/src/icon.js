const TWO_PART_TLD = new Set(['com.cn', 'net.cn', 'org.cn', 'gov.cn', 'edu.cn', 'co.uk', 'com.hk', 'com.tw'])

function rootDomain(host) {
  const parts = host.split('.')
  if (parts.length <= 2) {
    return host
  }
  const lastTwo = parts.slice(-2).join('.')
  return TWO_PART_TLD.has(lastTwo) ? parts.slice(-3).join('.') : lastTwo
}

/**
 * 图标候选链：手动图片 URL > DDG(完整域名) > DDG(根域名) > 站点自身 favicon。
 * 全部失败后由组件回退到 emoji。
 */
export function iconCandidates(item) {
  const list = []
  if (item.icon && /^https?:\/\//.test(item.icon)) {
    list.push(item.icon)
  }
  try {
    const host = new URL(item.url).host
    list.push(`https://icons.duckduckgo.com/ip3/${host}.ico`)
    const root = rootDomain(host)
    if (root !== host) {
      list.push(`https://icons.duckduckgo.com/ip3/${root}.ico`)
    }
    list.push(`https://${host}/favicon.ico`)
  } catch {
    // URL 非法：仅用手动图标或 emoji
  }
  return list
}

/** emoji 兜底：icon 字段是图片 URL 时用默认符号 */
export function fallbackEmoji(item) {
  return item.icon && !/^https?:\/\//.test(item.icon) ? item.icon : '🔗'
}
