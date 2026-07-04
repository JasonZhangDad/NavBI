import { useAuthStore } from './stores/auth'

export const CLIENT_DOWNLOADS = [
  {
    platform: 'windows',
    label: 'Windows 安装包 (.msi)',
    filename: 'Cloudflare_WARP_2026.6.822.0.msi'
  },
  {
    platform: 'macos',
    label: 'macOS 安装包 (.pkg)',
    filename: 'Cloudflare_WARP_2026.6.822.0.pkg'
  }
]

export async function downloadClientInstaller(platform) {
  const item = CLIENT_DOWNLOADS.find((entry) => entry.platform === platform)
  if (!item) {
    throw new Error('不支持的下载类型')
  }

  const { token } = useAuthStore()
  if (!token) {
    throw new Error('请先登录')
  }

  const response = await fetch(`/api/downloads/client/${platform}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!response.ok) {
    const message = await readErrorMessage(response)
    throw new Error(message)
  }

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = item.filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function readErrorMessage(response) {
  try {
    const body = await response.json()
    return body.message || '下载失败'
  } catch {
    return '下载失败'
  }
}
