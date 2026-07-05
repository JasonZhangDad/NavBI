import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = dirname(fileURLToPath(import.meta.url))

function read(path) {
  return readFileSync(resolve(root, path), 'utf8')
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

const starfield = read('src/components/StarfieldBackground.vue')
const home = read('src/views/Home.vue')
const authShell = read('src/components/AuthShell.vue')
const adminLayout = read('src/views/admin/AdminLayout.vue')

assert(starfield.includes('z-index: 1'), 'Starfield canvas should sit above page backgrounds')
assert(starfield.includes('transform: translateZ(0)'), 'Starfield canvas should have its own compositing layer')
assert(starfield.includes('CAMERA_Z = 110'), 'Camera should be close enough for visible depth')
assert(starfield.includes('moveLayerTowardCamera'), 'Three.js stars should move toward the camera')
assert(starfield.includes('position.needsUpdate = true'), 'Moving stars should flush geometry updates')
assert(starfield.includes('.starfield::before'), 'Starfield should have a visible CSS fallback layer')
assert(starfield.includes('@keyframes starfield-drift'), 'Starfield fallback should animate when motion is allowed')

for (const [name, source] of [
  ['Home', home],
  ['AuthShell', authShell],
  ['AdminLayout', adminLayout]
]) {
  assert(source.includes('z-index: 2'), `${name} content should sit above the starfield layer`)
}

console.log('starfield_visibility_ok')
