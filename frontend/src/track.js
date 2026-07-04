const KEY = 'navbi_session_id'

export function getSessionId() {
  return localStorage.getItem(KEY) || ''
}

/** 页面访问埋点：首次由服务端派发 sessionId 并持久化到 localStorage。 */
export async function trackVisit(url) {
  try {
    const response = await fetch('/api/track', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url,
        referer: document.referrer || null,
        sessionId: getSessionId() || null
      }),
      keepalive: true
    })
    const res = await response.json()
    if (res.data?.sessionId) {
      localStorage.setItem(KEY, res.data.sessionId)
    }
  } catch {
    // 埋点失败不影响页面
  }
}
