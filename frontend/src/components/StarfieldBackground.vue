<template>
  <div ref="host" class="starfield" aria-hidden="true"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

const host = ref()

let renderer = null
let scene = null
let camera = null
let rafId = 0
let layers = []
let sprite = null
const pointer = { x: 0, y: 0 }
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const CAMERA_Z = 110
const NEAR_Z = 108

/** 圆形发光贴图：PointsMaterial 默认渲染方形点，必须贴图才有星光观感 */
function makeSprite() {
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 64
  const ctx = canvas.getContext('2d')
  const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
  gradient.addColorStop(0, 'rgba(255,255,255,1)')
  gradient.addColorStop(0.35, 'rgba(255,255,255,0.55)')
  gradient.addColorStop(1, 'rgba(255,255,255,0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 64, 64)
  return new THREE.CanvasTexture(canvas)
}

function resetStar(positions, index, spread, depth) {
  const radius = Math.pow(Math.random(), 0.55) * spread
  const angle = Math.random() * Math.PI * 2
  positions[index] = Math.cos(angle) * radius
  positions[index + 1] = Math.sin(angle) * radius * 0.62
  positions[index + 2] = -80 - Math.random() * depth
}

function makeLayer(count, size, color, spread, depth, speed, trail) {
  const positions = new Float32Array(count * 3)
  const trails = new Float32Array(count * 6)
  for (let i = 0; i < count; i++) {
    resetStar(positions, i * 3, spread, depth)
  }
  const pointGeometry = new THREE.BufferGeometry()
  pointGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const material = new THREE.PointsMaterial({
    color,
    size,
    map: sprite,
    sizeAttenuation: true,
    transparent: true,
    opacity: 0.9,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  })
  const trailGeometry = new THREE.BufferGeometry()
  trailGeometry.setAttribute('position', new THREE.BufferAttribute(trails, 3))
  const trailMaterial = new THREE.LineBasicMaterial({
    color,
    transparent: true,
    opacity: 0.46,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  })
  const group = new THREE.Group()
  group.add(new THREE.LineSegments(trailGeometry, trailMaterial))
  group.add(new THREE.Points(pointGeometry, material))
  group.userData = {
    spread,
    depth,
    speed,
    trail,
    pointPosition: pointGeometry.attributes.position,
    trailPosition: trailGeometry.attributes.position
  }
  scene.add(group)
  updateTrailSegments(group)
  return group
}

function onPointerMove(e) {
  pointer.x = (e.clientX / window.innerWidth) * 2 - 1
  pointer.y = (e.clientY / window.innerHeight) * 2 - 1
}

function onResize() {
  camera.aspect = window.innerWidth / window.innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(window.innerWidth, window.innerHeight)
}

function updateTrailSegments(layer) {
  const { pointPosition, trailPosition, trail } = layer.userData
  const positions = pointPosition.array
  const trails = trailPosition.array
  for (let i = 0, j = 0; i < positions.length; i += 3, j += 6) {
    const x = positions[i]
    const y = positions[i + 1]
    const z = positions[i + 2]
    trails[j] = x
    trails[j + 1] = y
    trails[j + 2] = z
    trails[j + 3] = x * 0.82
    trails[j + 4] = y * 0.82
    trails[j + 5] = z - trail
  }
  trailPosition.needsUpdate = true
}

function moveLayerTowardCamera(layer) {
  const position = layer.userData.pointPosition
  const positions = position.array
  const { spread, depth, speed } = layer.userData
  for (let i = 2; i < positions.length; i += 3) {
    positions[i] += speed
    if (positions[i] > NEAR_Z) {
      resetStar(positions, i - 2, spread, depth)
    }
  }
  position.needsUpdate = true
  updateTrailSegments(layer)
}

function tick() {
  layers.forEach((layer, i) => {
    moveLayerTowardCamera(layer)
    layer.rotation.z += 0.0011 * (i + 1)
  })
  camera.position.x += (pointer.x * 14 - camera.position.x) * 0.035
  camera.position.y += (-pointer.y * 10 - camera.position.y) * 0.035
  camera.lookAt(scene.position)
  renderer.render(scene, camera)
  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 1, 1000)
  camera.position.z = CAMERA_Z
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: false })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearAlpha(0)
  renderer.setSize(window.innerWidth, window.innerHeight)
  host.value.appendChild(renderer.domElement)

  sprite = makeSprite()
  layers = [
    makeLayer(820, 7.4, 0x5aa2f0, 260, 360, 4.6, 34),
    makeLayer(560, 5.8, 0x2fce96, 220, 300, 3.2, 24),
    makeLayer(320, 9.2, 0xb0a8ff, 320, 430, 6.4, 46)
  ]

  window.addEventListener('resize', onResize)
  if (reducedMotion) {
    renderer.render(scene, camera)
  } else {
    window.addEventListener('pointermove', onPointerMove)
    tick()
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointermove', onPointerMove)
  layers.forEach((layer) => {
    layer.children.forEach((child) => {
      child.geometry.dispose()
      child.material.dispose()
    })
  })
  sprite.dispose()
  renderer.dispose()
  renderer.domElement.remove()
})
</script>

<style scoped>
.starfield {
  position: fixed;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  pointer-events: none;
  transform: translateZ(0);
}
.starfield::before,
.starfield::after {
  content: '';
  position: absolute;
  inset: -12%;
  z-index: 0;
  background-image:
    radial-gradient(circle at 24px 36px, rgba(255, 255, 255, 0.98) 0 1.4px, transparent 2.4px),
    radial-gradient(circle at 116px 72px, rgba(90, 162, 240, 0.9) 0 1.7px, transparent 2.7px),
    radial-gradient(circle at 198px 26px, rgba(47, 206, 150, 0.82) 0 1.4px, transparent 2.5px),
    radial-gradient(circle at 68px 148px, rgba(176, 168, 255, 0.84) 0 1.8px, transparent 2.9px),
    radial-gradient(circle at 222px 128px, rgba(255, 255, 255, 0.88) 0 1.4px, transparent 2.4px);
  background-size: 260px 190px;
  opacity: 0.58;
  animation: starfield-drift 8s linear infinite;
}
.starfield::after {
  --scale: 1.55;
  --drift-x: 13%;
  --drift-y: -9%;
  background-size: 340px 260px;
  opacity: 0.4;
  animation-duration: 13s;
}
.starfield canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
}
@keyframes starfield-drift {
  from {
    transform: translate3d(0, 0, 0) scale(var(--scale, 1));
  }
  to {
    transform: translate3d(var(--drift-x, -10%), var(--drift-y, 7%), 0) scale(var(--scale, 1));
  }
}
@media (prefers-reduced-motion: reduce) {
  .starfield::before,
  .starfield::after {
    animation: none;
  }
}
</style>
