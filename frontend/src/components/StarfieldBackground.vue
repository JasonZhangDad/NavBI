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

function makeLayer(count, size, color, spread) {
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count * 3; i++) {
    positions[i] = (Math.random() - 0.5) * spread
  }
  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const material = new THREE.PointsMaterial({
    color,
    size,
    map: sprite,
    sizeAttenuation: true,
    transparent: true,
    opacity: 0.75,
    blending: THREE.AdditiveBlending,
    depthWrite: false
  })
  const points = new THREE.Points(geometry, material)
  scene.add(points)
  return points
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

function tick() {
  layers.forEach((layer, i) => {
    layer.rotation.y += 0.0003 * (i + 1)
    layer.rotation.x += 0.0001 * (i + 1)
  })
  camera.position.x += (pointer.x * 8 - camera.position.x) * 0.03
  camera.position.y += (-pointer.y * 8 - camera.position.y) * 0.03
  camera.lookAt(scene.position)
  renderer.render(scene, camera)
  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 1, 1000)
  camera.position.z = 140
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: false })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(window.innerWidth, window.innerHeight)
  host.value.appendChild(renderer.domElement)

  sprite = makeSprite()
  layers = [
    makeLayer(900, 2.6, 0x3987e5, 340),
    makeLayer(600, 2.0, 0x199e70, 280),
    makeLayer(280, 3.8, 0x9085e9, 420)
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
    layer.geometry.dispose()
    layer.material.dispose()
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
  z-index: 0;
  pointer-events: none;
}
</style>
