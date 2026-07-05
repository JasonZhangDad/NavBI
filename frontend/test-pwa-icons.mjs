import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import assert from 'node:assert/strict'

const root = new URL('.', import.meta.url).pathname
const index = readFileSync(join(root, 'index.html'), 'utf8')
const manifestPath = join(root, 'public/manifest.webmanifest')

assert(index.includes('<link rel="manifest" href="/manifest.webmanifest" />'), 'index.html should link the PWA manifest')
assert(index.includes('<link rel="icon" type="image/png" sizes="32x32" href="/icons/navbi-icon-32.png" />'), 'index.html should link the PNG favicon')
assert(index.includes('<link rel="apple-touch-icon" sizes="180x180" href="/icons/navbi-icon-180.png" />'), 'index.html should link the Apple touch icon')
assert(existsSync(manifestPath), 'manifest.webmanifest should exist')

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'))
assert.equal(manifest.name, 'NavBI Pro')
assert.equal(manifest.short_name, 'NavBI')
assert.equal(manifest.display, 'standalone')

function pngSize(path) {
  const bytes = readFileSync(path)
  assert.equal(bytes.toString('ascii', 1, 4), 'PNG', `${path} should be a PNG file`)
  return {
    width: bytes.readUInt32BE(16),
    height: bytes.readUInt32BE(20)
  }
}

for (const [src, size] of [
  ['/icons/navbi-icon-32.png', 32],
  ['/icons/navbi-icon-180.png', 180],
  ['/icons/navbi-icon-192.png', 192],
  ['/icons/navbi-icon-512.png', 512]
]) {
  const filePath = join(root, 'public', src)
  assert(existsSync(filePath), `${src} should exist`)
  assert.deepEqual(pngSize(filePath), { width: size, height: size }, `${src} should be ${size}x${size}`)
}

for (const icon of manifest.icons) {
  const filePath = join(root, 'public', icon.src)
  assert(existsSync(filePath), `${icon.src} referenced by manifest should exist`)
}

assert(manifest.icons.some((icon) => icon.src === '/icons/navbi-icon-192.png' && icon.sizes === '192x192'), 'manifest should expose a 192x192 app icon')
assert(manifest.icons.some((icon) => icon.src === '/icons/navbi-icon-512.png' && icon.sizes === '512x512'), 'manifest should expose a 512x512 app icon')

console.log('pwa_icons_ok')
