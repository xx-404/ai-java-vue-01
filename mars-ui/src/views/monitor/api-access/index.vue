<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 统计卡片 -->
      <div class="stats-section">
        <div class="stats-cards">
          <n-card class="stat-card" size="small" :bordered="false" content-style="background: transparent">
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalCount }}</span>
              <span class="stat-label">请求总数</span>
            </div>
          </n-card>
          <n-card class="stat-card success" size="small" :bordered="false" content-style="background: transparent">
            <div class="stat-item">
              <span class="stat-value">{{ stats.successCount }}</span>
              <span class="stat-label">成功</span>
            </div>
          </n-card>
          <n-card class="stat-card fail" size="small" :bordered="false" content-style="background: transparent">
            <div class="stat-item">
              <span class="stat-value">{{ stats.failCount }}</span>
              <span class="stat-label">失败</span>
            </div>
          </n-card>
        </div>

        <n-grid :cols="2" :x-gap="16" class="stats-charts">
          <n-gi>
            <n-card title="请求方法分布" size="small" :bordered="false" content-style="background: transparent">
              <div ref="methodChartRef" class="chart-box"></div>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card title="Top10 API 路径" size="small" :bordered="false" content-style="background: transparent">
              <div ref="pathChartRef" class="chart-box"></div>
            </n-card>
          </n-gi>
          <n-gi :span="2">
            <n-card title="每日请求趋势" size="small" :bordered="false" content-style="background: transparent">
              <div ref="lineChartRef" class="chart-box"></div>
            </n-card>
          </n-gi>
        </n-grid>
      </div>

      <!-- 筛选与表格 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="API 路径">
            <n-input v-model:value="searchForm.apiPath" placeholder="请输入路径" clearable style="width: 200px" />
          </n-form-item>
          <n-form-item label="请求方法">
            <n-select v-model:value="searchForm.method" placeholder="请选择" :options="methodOptions" clearable style="width: 120px" />
          </n-form-item>
          <n-form-item label="状态">
            <n-select v-model:value="searchForm.success" placeholder="请选择" :options="successOptions" clearable style="width: 100px" />
          </n-form-item>
          <n-form-item label="日期范围">
            <n-date-picker v-model:value="dateRange" type="datetimerange" clearable />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                搜索
              </n-button>
              <n-button @click="handleReset">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                重置
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <n-space justify="space-between" style="width: 100%">
          <span></span>
          <n-button @click="columnSettingVisible = true">
            <template #icon><n-icon><SettingsOutline /></n-icon></template>
            列设置
          </n-button>
        </n-space>
      </div>

      <n-data-table :columns="tableColumns" :data="tableData" :loading="loading" :row-key="(row: ApiAccessLog) => row.id" />

      <div class="pagination-container">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="preference.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        >
          <template #prefix>共 {{ pagination.itemCount }} 条</template>
        </n-pagination>
      </div>
    </n-card>

    <TableColumnSetting
      v-model:show="columnSettingVisible"
      :column-defs="preference.columnDefs"
      :column-configs="preference.columnConfigs"
      @confirm="handleColumnConfirm"
      @reset="handleColumnReset"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { NCard, NGrid, NGi, NForm, NFormItem, NInput, NSelect, NDatePicker, NButton, NSpace, NIcon, NDataTable, NPagination, type DataTableColumns, useMessage } from 'naive-ui'
import { SearchOutline, RefreshOutline, SettingsOutline } from '@vicons/ionicons5'
import { apiAccessApi, type ApiAccessLog, type ApiAccessStatistics } from '@/api/monitor'
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import { useTablePreference, type ColumnDefinition } from '@/composables/useTablePreference'

const message = useMessage()

const stats = reactive<ApiAccessStatistics>({
  totalCount: 0,
  successCount: 0,
  failCount: 0,
  dailyStats: {},
  topPaths: [],
  methodCount: {}
})

const methodChartRef = ref<HTMLElement | null>(null)
const pathChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
let methodChart: any = null
let pathChart: any = null
let lineChart: any = null

const searchForm = reactive({
  apiPath: '',
  method: null as string | null,
  success: null as number | null
})

const dateRange = ref<[number, number] | null>(null)
const tableData = ref<ApiAccessLog[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, itemCount: 0 })

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

const successOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const columnDefs: ColumnDefinition<ApiAccessLog>[] = [
  { key: 'id', title: 'ID', column: { title: 'ID', key: 'id', width: 70 } },
  { key: 'apiPath', title: 'API 路径', column: { title: 'API 路径', key: 'apiPath', ellipsis: { tooltip: true }, minWidth: 200 } },
  { key: 'method', title: '方法', column: { title: '方法', key: 'method', width: 80 } },
  { key: 'statusCode', title: '状态码', column: { title: '状态码', key: 'statusCode', width: 90 } },
  {
    key: 'success',
    title: '成功',
    column: {
      title: '成功',
      key: 'success',
      width: 70,
      render: (row) => row.success === 1 ? '是' : '否'
    }
  },
  { key: 'costTime', title: '耗时(ms)', column: { title: '耗时(ms)', key: 'costTime', width: 90 } },
  { key: 'ip', title: 'IP', column: { title: 'IP', key: 'ip', width: 120 } },
  { key: 'userId', title: '用户ID', column: { title: '用户ID', key: 'userId', width: 90 } },
  { key: 'startTime', title: '请求时间', column: { title: '请求时间', key: 'startTime', width: 180 } }
]

const preference = useTablePreference<ApiAccessLog>('monitor/api-access', columnDefs, 20)
const tableColumns = computed(() => preference.columns.value)
const columnSettingVisible = ref(false)

pagination.pageSize = preference.pageSize.value

watch(preference.pageSize, (newSize) => {
  pagination.pageSize = newSize
})

const startDate = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() - 6)
  return d.toISOString().slice(0, 10)
})
const endDate = computed(() => new Date().toISOString().slice(0, 10))

async function loadStatistics() {
  try {
    const res = await apiAccessApi.statistics({ startDate: startDate.value, endDate: endDate.value })
    Object.assign(stats, res)
    updateCharts()
  } catch { /* ignore */ }
}

function updateCharts() {
  import('echarts').then((echarts) => {
    if (methodChartRef.value && stats.methodCount) {
      if (!methodChart) methodChart = echarts.init(methodChartRef.value)
      const data = Object.entries(stats.methodCount).map(([name, value]) => ({ name, value }))
      methodChart.setOption({
        tooltip: { trigger: 'item' },
        series: [{ type: 'pie', radius: '60%', data }]
      })
    }
    if (pathChartRef.value && stats.topPaths?.length) {
      if (!pathChart) pathChart = echarts.init(pathChartRef.value)
      const xData = stats.topPaths.map((p) => p.apiPath.length > 25 ? p.apiPath.slice(0, 22) + '...' : p.apiPath)
      const yData = stats.topPaths.map((p) => p.count)
      pathChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 80, right: 20, bottom: 60 },
        xAxis: { type: 'category', data: xData, axisLabel: { rotate: 30 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: yData, itemStyle: { color: '#18a058' } }]
      })
    }
    if (lineChartRef.value && stats.dailyStats) {
      if (!lineChart) lineChart = echarts.init(lineChartRef.value)
      const keys = Object.keys(stats.dailyStats).sort()
      const totalData = keys.map((k) => stats.dailyStats[k]?.total ?? 0)
      const successData = keys.map((k) => stats.dailyStats[k]?.success ?? 0)
      const failData = keys.map((k) => stats.dailyStats[k]?.fail ?? 0)
      lineChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['总数', '成功', '失败'] },
        xAxis: { type: 'category', data: keys },
        yAxis: { type: 'value' },
        series: [
          { name: '总数', type: 'line', smooth: true, data: totalData, itemStyle: { color: '#18a058' } },
          { name: '成功', type: 'line', smooth: true, data: successData, itemStyle: { color: '#2080f0' } },
          { name: '失败', type: 'line', smooth: true, data: failData, itemStyle: { color: '#d03050' } }
        ]
      })
    }
  }).catch(() => {})
}

async function loadPage() {
  loading.value = true
  try {
    const formatDt = (ms: number) => {
      const d = new Date(ms)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
    }
    const [startTime, endTime] = dateRange.value
      ? [formatDt(dateRange.value[0] as number), formatDt(dateRange.value[1] as number)]
      : [null, null]
    const res = await apiAccessApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      apiPath: searchForm.apiPath || undefined,
      method: searchForm.method || undefined,
      success: searchForm.success ?? undefined,
      startTime: startTime as any,
      endTime: endTime as any
    })
    tableData.value = res.list || []
    pagination.itemCount = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.page = page
  loadPage()
}

async function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  await preference.savePageSize(pageSize)
  loadPage()
}

async function handleColumnConfirm(configs: any[]) {
  await preference.updateColumnOrder(configs)
  message.success('列设置已保存')
}

async function handleColumnReset() {
  await preference.resetToDefault()
  message.success('已恢复默认设置')
}

function handleSearch() {
  pagination.page = 1
  loadPage()
}

function handleReset() {
  searchForm.apiPath = ''
  searchForm.method = null
  searchForm.success = null
  dateRange.value = null
  pagination.page = 1
  loadPage()
}

let timer: ReturnType<typeof setInterval> | null = null
onMounted(async () => {
  await preference.init()
  pagination.pageSize = preference.pageSize.value
  loadStatistics()
  loadPage()
  timer = setInterval(loadStatistics, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  methodChart?.dispose()
  pathChart?.dispose()
  lineChart?.dispose()
})
</script>

<style lang="scss" scoped>
.page-layout {
  .stats-section {
    margin-bottom: 20px;
  }
  .stats-cards {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;
  }
  .stat-card {
    flex: 1;
    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: #111827;
      }
      .stat-label {
        font-size: 13px;
        color: #6b7280;
      }
    }
    &.success .stat-value { color: #18a058; }
    &.fail .stat-value { color: #d03050; }
  }
  .stats-charts {
    margin-bottom: 16px;
  }
  .chart-box {
    height: 260px;
  }
  .search-form {
    margin-bottom: 16px;
  }
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
  }
}
</style>
