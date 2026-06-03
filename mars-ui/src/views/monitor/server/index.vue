<template>
  <div class="page-container">
    <n-grid :cols="24" :x-gap="16" :y-gap="16">
      <n-gi :span="6">
        <n-card size="small" :class="['metric-card', cpuAlert ? 'metric-card--alert' : '']">
          <div class="metric-header">
            <span class="metric-title">CPU 使用率</span>
            <n-tag v-if="cpuAlert" type="error" size="small" :bordered="false">告警</n-tag>
            <n-tag v-else-if="cpuPercent > 0" type="success" size="small" :bordered="false">正常</n-tag>
          </div>
          <div class="metric-value" :style="{ color: cpuAlert ? '#d03050' : cpuPercent > 70 ? '#f0a020' : '#18a058' }">
            {{ cpuPercent.toFixed(1) }}%
          </div>
          <n-progress
            type="line"
            :percentage="cpuPercent"
            :color="cpuAlert ? '#d03050' : cpuPercent > 70 ? '#f0a020' : '#18a058'"
            rail-color="#f0f0f0"
            :height="8"
            :border-radius="4"
            indicator-placement="inside"
            :processing="cpuAlert"
          />
          <div class="metric-threshold" v-if="thresholds.cpu">
            阈值: {{ thresholds.cpu }}%
          </div>
          <n-descriptions :column="1" label-placement="left" size="small" style="margin-top: 8px">
            <n-descriptions-item label="系统负载">{{ serverInfo.cpu?.systemLoadAverage?.toFixed(2) }}</n-descriptions-item>
            <n-descriptions-item label="核心数">{{ serverInfo.cpu?.availableProcessors }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>

      <n-gi :span="6">
        <n-card size="small" :class="['metric-card', memAlert ? 'metric-card--alert' : '']">
          <div class="metric-header">
            <span class="metric-title">内存使用率</span>
            <n-tag v-if="memAlert" type="error" size="small" :bordered="false">告警</n-tag>
            <n-tag v-else-if="memPercent > 0" type="success" size="small" :bordered="false">正常</n-tag>
          </div>
          <div class="metric-value" :style="{ color: memAlert ? '#d03050' : memPercent > 70 ? '#f0a020' : '#18a058' }">
            {{ memPercent.toFixed(1) }}%
          </div>
          <n-progress
            type="line"
            :percentage="memPercent"
            :color="memAlert ? '#d03050' : memPercent > 70 ? '#f0a020' : '#18a058'"
            rail-color="#f0f0f0"
            :height="8"
            :border-radius="4"
            indicator-placement="inside"
            :processing="memAlert"
          />
          <div class="metric-threshold" v-if="thresholds.memory">
            阈值: {{ thresholds.memory }}%
          </div>
          <n-descriptions :column="1" label-placement="left" size="small" style="margin-top: 8px">
            <n-descriptions-item label="已用">{{ serverInfo.memory?.heapUsed }}</n-descriptions-item>
            <n-descriptions-item label="最大">{{ serverInfo.memory?.heapMax }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>

      <n-gi :span="6">
        <n-card size="small" :class="['metric-card', diskAlert ? 'metric-card--alert' : '']">
          <div class="metric-header">
            <span class="metric-title">磁盘使用率</span>
            <n-tag v-if="diskAlert" type="error" size="small" :bordered="false">告警</n-tag>
            <n-tag v-else-if="maxDiskPercent > 0" type="success" size="small" :bordered="false">正常</n-tag>
          </div>
          <div class="metric-value" :style="{ color: diskAlert ? '#d03050' : maxDiskPercent > 70 ? '#f0a020' : '#18a058' }">
            {{ maxDiskPercent.toFixed(1) }}%
          </div>
          <n-progress
            type="line"
            :percentage="maxDiskPercent"
            :color="diskAlert ? '#d03050' : maxDiskPercent > 70 ? '#f0a020' : '#18a058'"
            rail-color="#f0f0f0"
            :height="8"
            :border-radius="4"
            indicator-placement="inside"
            :processing="diskAlert"
          />
          <div class="metric-threshold" v-if="thresholds.disk">
            阈值: {{ thresholds.disk }}%
          </div>
          <n-descriptions :column="1" label-placement="left" size="small" style="margin-top: 8px">
            <n-descriptions-item label="盘符数">{{ (serverInfo.disks || []).length }}</n-descriptions-item>
            <n-descriptions-item label="最高使用盘">{{ maxDiskPath || '-' }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>

      <n-gi :span="6">
        <n-card size="small" class="metric-card">
          <div class="metric-header">
            <span class="metric-title">JVM 信息</span>
          </div>
          <n-descriptions :column="1" label-placement="left" size="small">
            <n-descriptions-item label="名称">{{ serverInfo.jvm?.name }}</n-descriptions-item>
            <n-descriptions-item label="版本">{{ serverInfo.jvm?.version }}</n-descriptions-item>
            <n-descriptions-item label="启动时间">{{ serverInfo.jvm?.startTime }}</n-descriptions-item>
            <n-descriptions-item label="运行时长">{{ serverInfo.jvm?.uptime }}</n-descriptions-item>
          </n-descriptions>
          <n-descriptions :column="1" label-placement="left" size="small" style="margin-top: 4px">
            <n-descriptions-item label="主机名">{{ serverInfo.sys?.hostName }}</n-descriptions-item>
            <n-descriptions-item label="IP">{{ serverInfo.sys?.hostAddress }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="磁盘详情" style="margin-top: 16px">
      <n-data-table :columns="diskColumns" :data="serverInfo.disks || []" :row-key="(row: any) => row.path" />
    </n-card>

    <n-grid :cols="2" :x-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="CPU 负载趋势">
          <div ref="cpuChartRef" style="height: 260px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="内存使用率趋势">
          <div ref="memoryChartRef" style="height: 260px"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card style="margin-top: 16px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <n-tabs v-model:value="alertTab" type="line" size="small">
            <n-tab name="active">
              当前告警
              <n-badge v-if="activeAlerts.length" :value="activeAlerts.length" type="error" style="margin-left: 6px" />
            </n-tab>
            <n-tab name="history">历史告警</n-tab>
          </n-tabs>
          <div style="display: flex; gap: 8px;">
            <n-button size="small" @click="showThresholdModal = true">阈值设置</n-button>
            <n-button v-if="alertTab === 'history'" size="small" type="warning" @click="handleCleanHistory">清空历史</n-button>
          </div>
        </div>
      </template>

      <div v-if="alertTab === 'active'">
        <n-data-table :columns="alertColumns" :data="activeAlerts" :row-key="(row: any) => row.id" size="small" />
        <n-empty v-if="!activeAlerts.length" description="暂无活跃告警" style="padding: 24px 0" />
      </div>

      <div v-else>
        <n-data-table
          :columns="historyColumns"
          :data="historyData"
          :row-key="(row: any) => row.id"
          size="small"
          :pagination="historyPagination"
          :remote="true"
          @update:page="loadHistoryAlerts"
        />
      </div>
    </n-card>

    <n-modal v-model:show="showThresholdModal" preset="card" title="告警阈值设置" style="width: 520px">
      <n-form label-placement="left" label-width="100">
        <n-form-item v-for="config in alertConfigs" :key="config.id" :label="getAlertTypeName(config.alertType)">
          <div style="display: flex; align-items: center; gap: 12px; width: 100%;">
            <n-input-number
              v-model:value="config.threshold"
              :min="1"
              :max="100"
              :step="1"
              style="width: 140px;"
            >
              <template #suffix>%</template>
            </n-input-number>
            <n-switch v-model:value="config.enabled" :unchecked-value="0" :checked-value="1">
              <template #checked>启用</template>
              <template #unchecked>禁用</template>
            </n-switch>
          </div>
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showThresholdModal = false">取消</n-button>
        <n-button type="primary" @click="saveThresholds" style="margin-left: 12px">保存</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import { NProgress, NTag, NButton, useMessage, type DataTableColumns } from 'naive-ui'
import { serverApi, alertApi, type SysAlertConfig, type SysAlertRecord } from '@/api/monitor'

const message = useMessage()
const serverInfo = ref<any>({})
const cpuChartRef = ref<HTMLElement | null>(null)
const memoryChartRef = ref<HTMLElement | null>(null)

let timer: ReturnType<typeof setInterval> | null = null
let cpuChart: any = null
let memoryChart: any = null
const cpuData: number[] = []
const memoryData: number[] = []
const timeLabels: string[] = []

const alertTab = ref('active')
const activeAlerts = ref<SysAlertRecord[]>([])
const historyData = ref<SysAlertRecord[]>([])
const historyPagination = ref({ page: 1, pageSize: 10, itemCount: 0 })
const alertConfigs = ref<SysAlertConfig[]>([])
const showThresholdModal = ref(false)

const thresholds = computed(() => {
  const result: Record<string, number> = {}
  for (const config of alertConfigs.value) {
    if (config.enabled && config.alertType !== 'disk') {
      result[config.alertType] = config.threshold
    }
    if (config.alertType === 'disk' && config.enabled) {
      result.disk = config.threshold
    }
  }
  return result
})

const cpuPercent = computed(() => {
  const load = serverInfo.value.cpu?.systemLoadAverage || 0
  const processors = serverInfo.value.cpu?.availableProcessors || 1
  return Math.min(Math.max(load / processors * 100, 0), 100)
})

const memPercent = computed(() => {
  const heapUsed = parseMemoryBytes(serverInfo.value.memory?.heapUsed || '0')
  const heapMax = parseMemoryBytes(serverInfo.value.memory?.heapMax || '1')
  return heapMax > 0 ? (heapUsed / heapMax) * 100 : 0
})

const maxDiskPercent = computed(() => {
  const disks = serverInfo.value.disks || []
  if (!disks.length) return 0
  return Math.max(...disks.map((d: any) => parseFloat(d.usedPercent) || 0))
})

const maxDiskPath = computed(() => {
  const disks = serverInfo.value.disks || []
  if (!disks.length) return ''
  let max = 0, path = ''
  for (const d of disks) {
    const pct = parseFloat(d.usedPercent) || 0
    if (pct > max) { max = pct; path = d.path }
  }
  return path
})

const cpuAlert = computed(() => {
  const t = thresholds.value.cpu
  return t !== undefined && cpuPercent.value >= t
})

const memAlert = computed(() => {
  const t = thresholds.value.memory
  return t !== undefined && memPercent.value >= t
})

const diskAlert = computed(() => {
  const t = thresholds.value.disk
  return t !== undefined && maxDiskPercent.value >= t
})

function parseMemoryBytes(text: string): number {
  if (!text) return 0
  text = text.trim()
  if (text.endsWith(' GB')) return parseFloat(text.replace(' GB', '')) * 1024 * 1024 * 1024
  if (text.endsWith(' MB')) return parseFloat(text.replace(' MB', '')) * 1024 * 1024
  if (text.endsWith(' KB')) return parseFloat(text.replace(' KB', '')) * 1024
  if (text.endsWith(' B')) return parseFloat(text.replace(' B', ''))
  return parseFloat(text.replace(/[^\d.]/g, '')) || 0
}

const diskColumns: DataTableColumns<any> = [
  { title: '盘符路径', key: 'path', width: 100 },
  { title: '总大小', key: 'total', width: 120 },
  { title: '可用大小', key: 'free', width: 120 },
  { title: '已用大小', key: 'usable', width: 120 },
  {
    title: '使用率', key: 'usedPercent', width: 200,
    render(row) {
      const percent = parseFloat(row.usedPercent) || 0
      const diskThreshold = thresholds.value.disk
      const isAlert = diskThreshold !== undefined && percent >= diskThreshold
      return h('div', { style: 'display:flex;align-items:center;gap:8px' }, [
        h(NProgress, {
          type: 'line',
          percentage: percent,
          indicatorPlacement: 'inside',
          color: isAlert ? '#d03050' : percent > 70 ? '#f0a020' : '#18a058',
          processing: isAlert,
          style: 'flex:1'
        }),
        isAlert ? h(NTag, { type: 'error', size: 'small', bordered: false }, () => '告警') : null
      ].filter(Boolean))
    }
  }
]

const alertTypeMap: Record<string, string> = { cpu: 'CPU', memory: '内存', disk: '磁盘' }

function getAlertTypeName(type: string) {
  return alertTypeMap[type] || type
}

const alertColumns: DataTableColumns<SysAlertRecord> = [
  {
    title: '告警类型', key: 'alertType', width: 100,
    render(row) {
      return h(NTag, { type: 'error', size: 'small', bordered: false }, () => getAlertTypeName(row.alertType))
    }
  },
  { title: '指标', key: 'metricKey', width: 120, render(row) { return row.metricKey || '-' } },
  {
    title: '当前值', key: 'currentValue', width: 100,
    render(row) { return `${row.currentValue?.toFixed(1)}%` }
  },
  {
    title: '阈值', key: 'threshold', width: 100,
    render(row) { return `${row.threshold?.toFixed(1)}%` }
  },
  { title: '触发时间', key: 'triggerTime', width: 180 }
]

const historyColumns: DataTableColumns<SysAlertRecord> = [
  ...alertColumns,
  {
    title: '状态', key: 'status', width: 80,
    render(row) {
      return h(NTag, { type: 'success', size: 'small', bordered: false }, () => '已恢复')
    }
  },
  { title: '恢复时间', key: 'recoverTime', width: 180, render(row) { return row.recoverTime || '-' } },
  {
    title: '持续时间', key: 'duration', width: 120,
    render(row) {
      if (!row.recoverTime || !row.triggerTime) return '-'
      const ms = new Date(row.recoverTime).getTime() - new Date(row.triggerTime).getTime()
      const mins = Math.floor(ms / 60000)
      if (mins < 60) return `${mins}分钟`
      const hours = Math.floor(mins / 60)
      return `${hours}小时${mins % 60}分`
    }
  },
  {
    title: '操作', key: 'actions', width: 80,
    render(row) {
      return h(NButton, {
        size: 'small', type: 'error', quaternary: true,
        onClick: () => handleDeleteRecord(row.id!)
      }, () => '删除')
    }
  }
]

async function loadServerInfo() {
  try {
    serverInfo.value = await serverApi.info()
    if (serverInfo.value.activeAlerts) {
      activeAlerts.value = serverInfo.value.activeAlerts
    }
    updateCharts()
  } catch { /* ignore */ }
}

async function loadAlertConfigs() {
  try {
    alertConfigs.value = await alertApi.configList()
  } catch { /* ignore */ }
}

async function loadActiveAlerts() {
  try {
    activeAlerts.value = await alertApi.activeList()
  } catch { /* ignore */ }
}

async function loadHistoryAlerts(page?: number) {
  if (page) historyPagination.value.page = page
  try {
    const res = await alertApi.historyPage({
      page: historyPagination.value.page,
      pageSize: historyPagination.value.pageSize
    })
    historyData.value = res.records || []
    historyPagination.value.itemCount = res.total || 0
  } catch { /* ignore */ }
}

async function saveThresholds() {
  try {
    await alertApi.updateConfigBatch(alertConfigs.value)
    message.success('阈值设置已保存')
    showThresholdModal.value = false
    loadAlertConfigs()
    await loadServerInfo()
    await loadActiveAlerts()
  } catch {
    message.error('保存失败')
  }
}

async function handleDeleteRecord(id: number) {
  try {
    await alertApi.deleteRecord(id)
    message.success('已删除')
    loadHistoryAlerts()
  } catch {
    message.error('删除失败')
  }
}

async function handleCleanHistory() {
  try {
    await alertApi.cleanHistory()
    message.success('历史告警已清空')
    loadHistoryAlerts()
  } catch {
    message.error('清空失败')
  }
}

function updateCharts() {
  if (typeof window === 'undefined') return

  const now = new Date().toLocaleTimeString()
  timeLabels.push(now)
  if (timeLabels.length > 10) timeLabels.shift()

  cpuData.push(cpuPercent.value)
  if (cpuData.length > 10) cpuData.shift()

  memoryData.push(memPercent.value)
  if (memoryData.length > 10) memoryData.shift()

  if (!cpuChart && cpuChartRef.value) {
    import('echarts').then((echarts) => {
      cpuChart = echarts.init(cpuChartRef.value!)
      memoryChart = echarts.init(memoryChartRef.value!)
      renderCharts()
    }).catch(() => {})
  } else {
    renderCharts()
  }
}

function renderCharts() {
  if (!cpuChart || !memoryChart) return

  const cpuThreshold = thresholds.value.cpu
  const memThreshold = thresholds.value.memory

  cpuChart.setOption({
    title: { text: 'CPU 负载 (%)', left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 25 },
    xAxis: { type: 'category', data: timeLabels, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 10 } },
    series: [
      {
        data: cpuData, type: 'line', smooth: true,
        areaStyle: { color: cpuAlert.value ? 'rgba(208,48,80,0.15)' : 'rgba(24,160,88,0.15)' },
        lineStyle: { color: cpuAlert.value ? '#d03050' : '#18a058', width: 2 },
        itemStyle: { color: cpuAlert.value ? '#d03050' : '#18a058' },
        ...(cpuThreshold ? {
          markLine: {
            silent: true,
            data: [{ yAxis: cpuThreshold, lineStyle: { color: '#d03050', type: 'dashed' }, label: { formatter: `阈值 ${cpuThreshold}%`, fontSize: 10 } }]
          }
        } : {})
      }
    ]
  })

  memoryChart.setOption({
    title: { text: '内存使用率 (%)', left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 25 },
    xAxis: { type: 'category', data: timeLabels, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 10 } },
    series: [
      {
        data: memoryData, type: 'line', smooth: true,
        areaStyle: { color: memAlert.value ? 'rgba(208,48,80,0.15)' : 'rgba(24,160,88,0.15)' },
        lineStyle: { color: memAlert.value ? '#d03050' : '#18a058', width: 2 },
        itemStyle: { color: memAlert.value ? '#d03050' : '#18a058' },
        ...(memThreshold ? {
          markLine: {
            silent: true,
            data: [{ yAxis: memThreshold, lineStyle: { color: '#d03050', type: 'dashed' }, label: { formatter: `阈值 ${memThreshold}%`, fontSize: 10 } }]
          }
        } : {})
      }
    ]
  })
}

onMounted(() => {
  loadServerInfo()
  loadAlertConfigs()
  loadActiveAlerts()
  loadHistoryAlerts()
  timer = setInterval(() => {
    loadServerInfo()
    loadActiveAlerts()
  }, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  cpuChart?.dispose()
  memoryChart?.dispose()
})
</script>

<style scoped>
.metric-card {
  transition: box-shadow 0.3s;
}
.metric-card--alert {
  box-shadow: 0 0 0 2px #d03050, 0 2px 8px rgba(208, 48, 80, 0.2);
  animation: alert-pulse 2s ease-in-out infinite;
}
@keyframes alert-pulse {
  0%, 100% { box-shadow: 0 0 0 2px #d03050, 0 2px 8px rgba(208, 48, 80, 0.2); }
  50% { box-shadow: 0 0 0 2px #d03050, 0 2px 16px rgba(208, 48, 80, 0.4); }
}
.metric-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.metric-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.metric-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  line-height: 1.2;
}
.metric-threshold {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
