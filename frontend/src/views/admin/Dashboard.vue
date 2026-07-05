<template>
  <div class="dash">
    <div class="toolbar">
      <h3>数据大屏</h3>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <div class="tiles">
      <div class="tile">
        <span class="label">今日 PV</span>
        <span class="value">{{ fmt(summary.todayPv) }}</span>
        <span class="sub">昨日 {{ fmt(summary.yesterdayPv) }}</span>
      </div>
      <div class="tile">
        <span class="label">今日 UV</span>
        <span class="value">{{ fmt(summary.todayUv) }}</span>
        <span class="sub">独立访客</span>
      </div>
      <div class="tile">
        <span class="label">访问人数</span>
        <span class="value">{{ fmt(summary.todayIpCount) }}</span>
        <span class="sub">今日去重 IP</span>
      </div>
      <div class="tile">
        <span class="label">今日注册</span>
        <span class="value">{{ fmt(summary.todayRegisterCount) }}</span>
        <span class="sub">当天新增账号</span>
      </div>
      <div class="tile">
        <span class="label">总访问量</span>
        <span class="value">{{ fmt(summary.totalPv) }}</span>
        <span class="sub">累计 UV {{ fmt(summary.totalUv) }}</span>
      </div>
      <div class="tile">
        <span class="label">独立 IP</span>
        <span class="value">{{ fmt(summary.ipCount) }}</span>
        <span class="sub">全部时间</span>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h4>访问趋势（3D，可拖拽旋转）</h4>
        <el-radio-group v-model="trendDim" size="small" @change="loadTrend">
          <el-radio-button value="hour">近 24 小时</el-radio-button>
          <el-radio-button value="day">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendEl" class="chart chart-lg"></div>
    </div>

    <div class="row row-2">
      <div class="panel">
        <h4>注册趋势</h4>
        <div ref="registerTrendEl" class="chart"></div>
      </div>
      <div class="panel">
        <h4>注册地区</h4>
        <div ref="registerGeoEl" class="chart"></div>
      </div>
    </div>

    <div class="row">
      <div class="panel">
        <h4>设备占比</h4>
        <div ref="deviceEl" class="chart"></div>
      </div>
      <div class="panel">
        <h4>浏览器占比</h4>
        <div ref="browserEl" class="chart"></div>
      </div>
      <div class="panel">
        <div class="panel-head">
          <h4>地域分布</h4>
          <el-radio-group v-model="geoLevel" size="small" @change="loadGeo">
            <el-radio-button value="country">国家</el-radio-button>
            <el-radio-button value="province">省份</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="geoEl" class="chart"></div>
      </div>
    </div>

    <div class="row row-2">
      <div class="panel">
        <h4>热门导航 Top10</h4>
        <div ref="navsEl" class="chart chart-lg"></div>
      </div>
      <div class="panel">
        <h4>最近访问</h4>
        <el-table :data="logs" size="small" height="320">
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column label="地区" width="140">
            <template #default="{ row }">{{ region(row) }}</template>
          </el-table-column>
          <el-table-column prop="device" label="设备" width="70" />
          <el-table-column prop="browser" label="浏览器" width="90" />
          <el-table-column prop="url" label="页面" show-overflow-tooltip />
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="logPage"
            v-model:page-size="logPageSize"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            :total="logTotal"
            @current-change="loadLogs"
            @size-change="onLogPageSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import 'echarts-gl'
import { animate, utils } from 'animejs'
import http from '../../api'

/* dataviz 参考调色板深色档：固定槽位顺序，不循环生成（已按面板底色 #151826 校验通过） */
const SERIES = ['#3987e5', '#199e70', '#c98500', '#008300', '#9085e9', '#e66767']
const INK = { primary: '#ffffff', secondary: '#c3c2b7', muted: '#898781', grid: '#2a3046', baseline: '#3a4160' }
const PANEL_BG = '#151826'

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

const summary = reactive({
  todayPv: 0,
  todayUv: 0,
  yesterdayPv: 0,
  totalPv: 0,
  totalUv: 0,
  todayIpCount: 0,
  ipCount: 0,
  todayRegisterCount: 0
})
const logs = ref([])
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const trendDim = ref('hour')
const geoLevel = ref('country')
const loading = ref(false)

const trendEl = ref()
const deviceEl = ref()
const browserEl = ref()
const geoEl = ref()
const navsEl = ref()
const registerTrendEl = ref()
const registerGeoEl = ref()
const charts = {}

function fmt(n) {
  return (n ?? 0).toLocaleString()
}
function fmtTime(t) {
  return (t || '').replace('T', ' ').slice(0, 19)
}
function region(row) {
  return [row.country, row.province, row.city].filter((v) => v && v !== '未知').join(' / ') || '未知'
}

function initChart(key, el) {
  charts[key] = echarts.init(el.value)
  return charts[key]
}

/** 指标数字滚动：anime.js 逐帧写 reactive 对象驱动视图 */
function applySummary(data) {
  const target = {}
  Object.keys(summary).forEach((k) => (target[k] = data?.[k] ?? 0))
  if (reducedMotion) {
    Object.assign(summary, target)
    return
  }
  animate(summary, { ...target, duration: 900, ease: 'outCubic', modifier: utils.round(0) })
}

const baseAxis = {
  axisLabel: { color: INK.muted, fontSize: 11 },
  axisLine: { lineStyle: { color: INK.baseline } },
  axisTick: { show: false }
}
const baseTooltip = {
  backgroundColor: '#1c2233',
  borderColor: 'rgba(255,255,255,0.12)',
  textStyle: { color: '#e8eaf2', fontSize: 12 }
}

/** 访问趋势：echarts-gl bar3D，PV/UV 两排柱，支持拖拽旋转 */
function trendOption(points) {
  const labels = points.map((p) =>
    trendDim.value === 'hour' ? p.bucket.slice(11, 16) : p.bucket.slice(5, 10)
  )
  const axis3dLabel = { color: INK.muted, fontSize: 10 }
  const axis3dLine = { lineStyle: { color: INK.baseline } }
  return {
    tooltip: {
      ...baseTooltip,
      formatter: (p) => `${labels[p.value[0]]}<br/>${p.marker}${p.seriesName}：${p.value[2]}`
    },
    legend: { right: 0, top: 0, textStyle: { color: INK.secondary } },
    xAxis3D: { type: 'category', data: labels, axisLabel: axis3dLabel, axisLine: axis3dLine },
    yAxis3D: { type: 'category', data: ['PV', 'UV'], axisLabel: { ...axis3dLabel, fontSize: 11 }, axisLine: axis3dLine },
    zAxis3D: {
      type: 'value',
      minInterval: 1,
      axisLabel: axis3dLabel,
      axisLine: axis3dLine,
      splitLine: { lineStyle: { color: INK.grid } }
    },
    grid3D: {
      boxWidth: 190,
      boxDepth: 28,
      boxHeight: 55,
      top: -10,
      light: { main: { intensity: 1.2, shadow: false }, ambient: { intensity: 0.45 } },
      viewControl: { alpha: 24, beta: 6, distance: 175, minDistance: 100, maxDistance: 420 }
    },
    series: [
      {
        name: 'PV',
        type: 'bar3D',
        shading: 'lambert',
        data: points.map((p, i) => [i, 0, p.pv]),
        itemStyle: { color: SERIES[0] },
        emphasis: { itemStyle: { color: '#6ea8f0' } }
      },
      {
        name: 'UV',
        type: 'bar3D',
        shading: 'lambert',
        data: points.map((p, i) => [i, 1, p.uv]),
        itemStyle: { color: SERIES[1] },
        emphasis: { itemStyle: { color: '#2fce96' } }
      }
    ]
  }
}

function registerTrendOption(points) {
  return {
    color: [SERIES[1]],
    tooltip: { ...baseTooltip, trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: INK.baseline } } },
    grid: { left: 44, right: 16, top: 18, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((p) => p.bucket.slice(5, 10)),
      ...baseAxis
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: INK.muted, fontSize: 11 },
      splitLine: { lineStyle: { color: INK.grid } }
    },
    series: [{
      name: '注册量',
      type: 'line',
      data: points.map((p) => p.value),
      lineStyle: { width: 2 },
      areaStyle: { color: 'rgba(25, 158, 112, 0.18)' },
      showSymbol: false
    }]
  }
}

function donutOption(data) {
  return {
    color: SERIES,
    tooltip: { ...baseTooltip, trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '52%'],
      itemStyle: { borderColor: PANEL_BG, borderWidth: 2, borderRadius: 4 },
      label: { color: INK.secondary, fontSize: 11, formatter: '{b} {c}' },
      labelLine: { lineStyle: { color: INK.baseline } },
      data: data.map((d) => ({ name: d.name, value: d.value }))
    }]
  }
}

function hBarOption(data) {
  const items = [...data].reverse()
  return {
    tooltip: { ...baseTooltip, trigger: 'item' },
    grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: INK.muted, fontSize: 11 },
      splitLine: { lineStyle: { color: INK.grid } }
    },
    yAxis: {
      type: 'category',
      data: items.map((d) => (d.name.length > 18 ? d.name.slice(0, 18) + '…' : d.name)),
      ...baseAxis
    },
    series: [{
      type: 'bar',
      barWidth: 14,
      itemStyle: { color: SERIES[0], borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', color: INK.secondary, fontSize: 11 },
      data: items.map((d) => d.value)
    }]
  }
}

async function loadTrend() {
  const res = await http.get('/bi/trend', { params: { dimension: trendDim.value } })
  charts.trend.setOption(trendOption(res.data || []), true)
}

async function loadGeo() {
  const res = await http.get('/bi/geo', { params: { level: geoLevel.value } })
  charts.geo.setOption(hBarOption((res.data || []).slice(0, 8)), true)
}

async function loadLogs() {
  const res = await http.get('/bi/logs', { params: { page: logPage.value, size: logPageSize.value } })
  logs.value = res.data?.records || []
  logTotal.value = res.data?.total || 0
}

async function loadRegisterStats() {
  const [trend, geo] = await Promise.all([
    http.get('/bi/register-trend'),
    http.get('/bi/register-geo')
  ])
  charts.registerTrend.setOption(registerTrendOption(trend.data || []), true)
  charts.registerGeo.setOption(hBarOption((geo.data || []).slice(0, 8)), true)
}

function onLogPageSizeChange() {
  logPage.value = 1
  loadLogs()
}

async function loadAll() {
  loading.value = true
  try {
    const [sum, device, browser, navs] = await Promise.all([
      http.get('/bi/summary'),
      http.get('/bi/device'),
      http.get('/bi/browser'),
      http.get('/nav/all')
    ])
    applySummary(sum.data)
    charts.device.setOption(donutOption(device.data || []), true)
    charts.browser.setOption(donutOption(browser.data || []), true)
    charts.navs.setOption(hBarOption(topNavs(navs.data || [])), true)
    await Promise.all([loadTrend(), loadGeo(), loadRegisterStats(), loadLogs()])
  } finally {
    loading.value = false
  }
}

function topNavs(items) {
  return items
    .map((item) => ({ name: item.title, value: item.clickCount || 0 }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 10)
}

function onResize() {
  Object.values(charts).forEach((c) => c.resize())
}

onMounted(() => {
  initChart('trend', trendEl)
  initChart('device', deviceEl)
  initChart('browser', browserEl)
  initChart('geo', geoEl)
  initChart('navs', navsEl)
  initChart('registerTrend', registerTrendEl)
  initChart('registerGeo', registerGeoEl)
  window.addEventListener('resize', onResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach((c) => c.dispose())
})
</script>

<style scoped>
.dash {
  min-height: calc(100vh - 100px);
  color: #e8eaf2;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.toolbar h3 {
  margin: 0;
  color: #ffffff;
}
.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}
.tile {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tile .label {
  font-size: 13px;
  color: #9aa3b8;
}
.tile .value {
  font-size: 30px;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 0 18px rgba(57, 135, 229, 0.35);
}
.tile .sub {
  font-size: 12px;
  color: #7d879e;
}
.panel {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.panel h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #b6bdd0;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.panel-head h4 {
  margin: 0;
}
.row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
}
.row .panel {
  margin-bottom: 14px;
}
.row-2 {
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
}
.chart {
  height: 260px;
}
.chart-lg {
  height: 320px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* ── Element Plus 深色适配 ── */
.dash :deep(.el-button) {
  --el-button-bg-color: rgba(255, 255, 255, 0.06);
  --el-button-border-color: rgba(255, 255, 255, 0.16);
  --el-button-text-color: #c3c2b7;
  --el-button-hover-bg-color: rgba(57, 135, 229, 0.15);
  --el-button-hover-border-color: #3987e5;
  --el-button-hover-text-color: #6ea8f0;
}
.dash :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.16);
  color: #b6bdd0;
  box-shadow: none;
}
.dash :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3987e5, #2361b8);
  border-color: #3987e5;
  color: #fff;
  box-shadow: 0 0 12px rgba(57, 135, 229, 0.4);
}
.dash :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-header-text-color: #b6bdd0;
  --el-table-text-color: #c3c2b7;
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-row-hover-bg-color: rgba(57, 135, 229, 0.08);
}
.dash :deep(.el-table__empty-text) {
  color: #7d879e;
}
.dash :deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.06);
  --el-pagination-text-color: #c3c2b7;
  --el-pagination-button-color: #c3c2b7;
  color: #c3c2b7;
}
.dash :deep(.el-pagination .el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.14) inset;
}
.dash :deep(.el-pagination .el-input__inner) {
  color: #c3c2b7;
}
.dash :deep(.el-pagination.is-background .el-pager li) {
  background: rgba(255, 255, 255, 0.06);
  color: #c3c2b7;
}
.dash :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: #3987e5;
  color: #fff;
}
.dash :deep(.el-pagination.is-background .btn-prev),
.dash :deep(.el-pagination.is-background .btn-next) {
  background: rgba(255, 255, 255, 0.06);
  color: #c3c2b7;
}
</style>
