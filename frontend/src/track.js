import http from './api'

const KEY = 'navbi_session_id'

export function getSessionId() {
  return localStorage.getItem(KEY) || ''
}

/** 页面访问埋点：首次由服务端派发 sessionId 并持久化到 localStorage。 */
export async function trackVisit(url) {
  try {
    const res = await http.post('/track', {
      url,
      referer: document.referrer || null,
      sessionId: getSessionId() || null
    })
    if (res.data?.sessionId) {
      localStorage.setItem(KEY, res.data.sessionId)
    }
  } catch {
    // 埋点失败不影响页面
  }
}
