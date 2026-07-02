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
        <h4>访问趋势</h4>
        <el-radio-group v-model="trendDim" size="small" @change="loadTrend">
          <el-radio-button value="hour">近 24 小时</el-radio-button>
          <el-radio-button value="day">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="trendEl" class="chart chart-lg"></div>
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
        <h4>热门页面 Top10</h4>
        <div ref="pagesEl" class="chart chart-lg"></div>
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
import http from '../../api'

/* dataviz 参考调色板：固定槽位顺序，不循环生成 */
const SERIES = ['#2a78d6', '#1baf7a', '#eda100', '#008300', '#4a3aa7', '#e34948']
const INK = { primary: '#0b0b0b', secondary: '#52514e', muted: '#898781', grid: '#e1e0d9', baseline: '#c3c2b7' }

const summary = reactive({ todayPv: 0, todayUv: 0, yesterdayPv: 0, totalPv: 0, totalUv: 0, ipCount: 0 })
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
const pagesEl = ref()
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

const baseAxis = {
  axisLabel: { color: INK.muted, fontSize: 11 },
  axisLine: { lineStyle: { color: INK.baseline } },
  axisTick: { show: false }
}
const baseTooltip = {
  backgroundColor: '#fcfcfb',
  borderColor: 'rgba(11,11,11,0.1)',
  textStyle: { color: INK.primary, fontSize: 12 }
}

function trendOption(points) {
  const labels = points.map((p) =>
    trendDim.value === 'hour' ? p.bucket.slice(11, 16) : p.bucket.slice(5, 10)
  )
  return {
    color: SERIES,
    tooltip: { ...baseTooltip, trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: INK.baseline } } },
    legend: { right: 0, top: 0, textStyle: { color: INK.secondary } },
    grid: { left: 44, right: 16, top: 36, bottom: 28 },
    xAxis: { type: 'category', boundaryGap: false, data: labels, ...baseAxis },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: INK.muted, fontSize: 11 },
      splitLine: { lineStyle: { color: INK.grid } }
    },
    series: [
      { name: 'PV', type: 'line', data: points.map((p) => p.pv), lineStyle: { width: 2 }, showSymbol: false },
      { name: 'UV', type: 'line', data: points.map((p) => p.uv), lineStyle: { width: 2 }, showSymbol: false }
    ]
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
      itemStyle: { borderColor: '#fcfcfb', borderWidth: 2, borderRadius: 4 },
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

function onLogPageSizeChange() {
  logPage.value = 1
  loadLogs()
}

async function loadAll() {
  loading.value = true
  try {
    const [sum, device, browser, pages] = await Promise.all([
      http.get('/bi/summary'),
      http.get('/bi/device'),
      http.get('/bi/browser'),
      http.get('/bi/top-pages')
    ])
    Object.assign(summary, sum.data)
    charts.device.setOption(donutOption(device.data || []), true)
    charts.browser.setOption(donutOption(browser.data || []), true)
    charts.pages.setOption(hBarOption(pages.data || []), true)
    await Promise.all([loadTrend(), loadGeo(), loadLogs()])
  } finally {
    loading.value = false
  }
}

function onResize() {
  Object.values(charts).forEach((c) => c.resize())
}

onMounted(() => {
  initChart('trend', trendEl)
  initChart('device', deviceEl)
  initChart('browser', browserEl)
  initChart('geo', geoEl)
  initChart('pages', pagesEl)
  window.addEventListener('resize', onResize)
  loadAll()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach((c) => c.dispose())
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.toolbar h3 {
  margin: 0;
}
.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}
.tile {
  background: #fcfcfb;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tile .label {
  font-size: 13px;
  color: #52514e;
}
.tile .value {
  font-size: 30px;
  font-weight: 700;
}
.tile .sub {
  font-size: 12px;
  color: #898781;
}
.panel {
  background: #fcfcfb;
  border: 1px solid rgba(11, 11, 11, 0.1);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.panel h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #52514e;
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
</style>
