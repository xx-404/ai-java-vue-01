<template>
  <div class="page-container">
    <n-card class="page-layout">
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="用户名">
            <n-input v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
          </n-form-item>
          <n-form-item label="状态">
            <n-select v-model:value="searchForm.status" placeholder="请选择状态" :options="statusOptions" clearable style="width: 120px" />
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

      <div class="table-toolbar">
        <n-space justify="space-between" style="width: 100%">
          <n-button v-if="hasPermission('sys:loginlog:delete')" type="error" @click="handleClean">
            <template #icon><n-icon><TrashOutline /></n-icon></template>
            清空日志
          </n-button>
          <n-button @click="columnSettingVisible = true">
            <template #icon><n-icon><SettingsOutline /></n-icon></template>
            列设置
          </n-button>
        </n-space>
      </div>

      <n-data-table
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: SysLoginLog) => row.id"
        remote
      />

      <div class="pagination-container" style="display: flex; justify-content: flex-end; margin-top: 12px">
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
          <template #prefix>
            共 {{ pagination.itemCount }} 条
          </template>
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
import { ref, reactive, h, onMounted, computed, watch } from 'vue'
import { NButton, NTag, NSpace, NPagination, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { SearchOutline, RefreshOutline, TrashOutline, SettingsOutline } from '@vicons/ionicons5'
import { loginLogApi, type SysLoginLog } from '@/api/monitor'
import { useUserStore } from '@/stores/user'
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import { useTablePreference, type ColumnDefinition } from '@/composables/useTablePreference'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

const searchForm = reactive({ username: '', status: null as number | null })
const statusOptions = [{ label: '成功', value: 0 }, { label: '失败', value: 1 }]
const tableData = ref<SysLoginLog[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const pageCount = computed(() => Math.ceil(pagination.itemCount / pagination.pageSize))

const columnDefs: ColumnDefinition<SysLoginLog>[] = [
  { key: 'id', title: 'ID', column: { title: 'ID', key: 'id', width: 80 } },
  { key: 'username', title: '用户名', column: { title: '用户名', key: 'username', width: 120 } },
  { key: 'ipaddr', title: 'IP地址', column: { title: 'IP地址', key: 'ipaddr', width: 140 } },
  { key: 'loginLocation', title: '登录地点', column: { title: '登录地点', key: 'loginLocation', width: 150 } },
  { key: 'browser', title: '浏览器', column: { title: '浏览器', key: 'browser', width: 200 } },
  { key: 'os', title: '操作系统', column: { title: '操作系统', key: 'os', width: 300 } },
  {
    key: 'status',
    title: '状态',
    column: {
      title: '状态',
      key: 'status',
      width: 80,
      render(row) {
        return h(NTag, { type: row.status === 0 ? 'success' : 'error', size: 'small' }, { default: () => row.status === 0 ? '成功' : '失败' })
      }
    }
  },
  { key: 'msg', title: '提示信息', column: { title: '提示信息', key: 'msg', ellipsis: { tooltip: true } } },
  { key: 'loginTime', title: '登录时间', column: { title: '登录时间', key: 'loginTime', width: 180 } },
  {
    key: 'actions',
    title: '操作',
    fixed: 'right',
    column: {
      title: '操作',
      key: 'actions',
      width: 80,
      fixed: 'right',
      render(row) {
        return hasPermission('sys:loginlog:delete')
          ? h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' })
          : '-'
      }
    }
  }
]

const preference = useTablePreference<SysLoginLog>('log/loginlog', columnDefs, 10)
const tableColumns = computed(() => preference.columns.value)
const columnSettingVisible = ref(false)

pagination.pageSize = preference.pageSize.value

watch(preference.pageSize, (newSize) => {
  pagination.pageSize = newSize
})

async function loadData() {
  loading.value = true
  try {
    const res = await loginLogApi.page({ page: pagination.page, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
  } finally { loading.value = false }
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() { searchForm.username = ''; searchForm.status = null; handleSearch() }
function handlePageChange(page: number) { pagination.page = page; loadData() }
async function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  await preference.savePageSize(pageSize)
  loadData()
}

function handleDelete(row: SysLoginLog) {
  dialog.warning({
    title: '提示', content: '确定要删除该日志吗？', positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await loginLogApi.delete(row.id!); message.success('删除成功'); loadData() }
  })
}

function handleClean() {
  dialog.warning({
    title: '提示', content: '确定要清空所有登录日志吗？', positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => { await loginLogApi.clean(); message.success('清空成功'); loadData() }
  })
}

async function handleColumnConfirm(configs: any[]) {
  await preference.updateColumnOrder(configs)
  message.success('列设置已保存')
}

async function handleColumnReset() {
  await preference.resetToDefault()
  message.success('已恢复默认设置')
}

onMounted(async () => {
  await preference.init()
  pagination.pageSize = preference.pageSize.value
  loadData()
})
</script>


