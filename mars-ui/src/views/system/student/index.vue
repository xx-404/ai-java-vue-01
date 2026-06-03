<template>
  <div class="page-container">
    <n-card>
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="id">
            <n-input v-model:value="searchForm.id" placeholder="请输入id" clearable />
          </n-form-item>
          <n-form-item label="姓名">
            <n-input v-model:value="searchForm.name" placeholder="请输入姓名" clearable />
          </n-form-item>
          <n-form-item label="状态">
            <n-select v-model:value="searchForm.status" placeholder="请选择状态" clearable style="width: 150px" :options="statusOptions" />
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
          <n-space>
            <n-button type="primary" @click="handleAdd">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新增
            </n-button>
            <n-button @click="importModalVisible = true">
              <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
              导入
            </n-button>
            <n-button @click="handleExport">
              <template #icon><n-icon><DownloadOutline /></n-icon></template>
              导出{{ selectedIds.length > 0 ? `(${selectedIds.length})` : '' }}
            </n-button>
            <n-button type="error" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              <template #icon><n-icon><TrashOutline /></n-icon></template>
              删除
            </n-button>
          </n-space>
          <n-button @click="columnSettingVisible = true">
            <template #icon><n-icon><SettingsOutline /></n-icon></template>
            列设置
          </n-button>
        </n-space>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :scroll-x="1200"
        @update:checked-row-keys="handleCheck"
      />
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
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

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" style="width: 800px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-grid :cols="2" :x-gap="24">
        <n-form-item-gi label="学号" path="studentNo">
          <n-input v-model:value="formData.studentNo" placeholder="请输入学号" />
        </n-form-item-gi>
        <n-form-item-gi label="姓名" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入姓名" />
        </n-form-item-gi>
        <n-form-item-gi label="性别" path="gender">
          <n-select v-model:value="formData.gender" placeholder="请选择性别" :options="genderOptions" />
        </n-form-item-gi>
        <n-form-item-gi label="出生日期" path="birthday">
          <n-date-picker v-model:value="formData.birthday" type="datetime" clearable style="width: 100%" />
        </n-form-item-gi>
        <n-form-item-gi label="手机号" path="phone">
          <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </n-form-item-gi>
        <n-form-item-gi label="邮箱" path="email">
          <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </n-form-item-gi>
        <n-form-item-gi label="地址" path="address">
          <ImageUpload v-model="formData.address" />
        </n-form-item-gi>
        <n-form-item-gi label="班级ID" path="classId">
          <n-input v-model:value="formData.classId" placeholder="请输入班级ID" />
        </n-form-item-gi>
        <n-form-item-gi label="状态" path="status">
          <n-select v-model:value="formData.status" placeholder="请选择状态" :options="statusOptions" />
        </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 导入弹窗 -->
    <n-modal v-model:show="importModalVisible" preset="card" title="导入学生表" style="width: 500px">
      <n-space vertical>
        <n-alert type="info">
          <template #header>导入说明</template>
          <ul style="margin: 0; padding-left: 16px; line-height: 1.8">
            <li>请先下载导入模板，按模板格式填写数据</li>
            <li>支持 .xlsx 或 .xls 格式</li>
          </ul>
        </n-alert>
        <n-space>
          <n-button type="primary" @click="handleDownloadTemplate">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            下载模板
          </n-button>
        </n-space>
        <n-upload :max="1" accept=".xlsx,.xls" :show-file-list="true" :custom-request="handleImportUpload">
          <n-upload-dragger>
            <div style="margin-bottom: 12px">
              <n-icon size="48" :depth="3"><CloudUploadOutline /></n-icon>
            </div>
            <n-text style="font-size: 16px">点击或拖拽文件到此处上传</n-text>
            <n-p depth="3" style="margin: 8px 0 0 0">支持 .xlsx 或 .xls 格式</n-p>
          </n-upload-dragger>
        </n-upload>
      </n-space>
      <template #footer>
        <n-button @click="importModalVisible = false">关闭</n-button>
      </template>
    </n-modal>

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
import { NButton, NSpace, NIcon, NUpload, NPagination, useMessage, useDialog, type DataTableColumns, type UploadCustomRequestOptions } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, TrashOutline, CreateOutline, CloudUploadOutline, DownloadOutline, SettingsOutline } from '@vicons/ionicons5'
import { studentApi, type Student } from '@/api/student'
import ImageUpload from '@/components/ImageUpload.vue'
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import { dictDataApi } from '@/api/org'
import { useTablePreference, type ColumnDefinition } from '@/composables/useTablePreference'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()

const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// 搜索表单
const searchForm = reactive({
  id:  null as number | null,
  name: '',
  status:  null as number | null,
})

// 表格数据
const tableData = ref<Student[]>([])
const loading = ref(false)
const selectedIds = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('')
const importModalVisible = ref(false)
const formRef = ref()
const defaultFormData: Student = {
  studentNo: '',
  name: '',
  gender: undefined,
  birthday:  undefined,
  phone: '',
  email: '',
  address: '',
  classId: undefined,
  status: undefined,
}
const formData = reactive<Student>({ ...defaultFormData })

// 字典选项（下拉框/单选框/复选框关联字典时使用）
const genderOptions = ref<{ label: string; value: any }[]>([])
const statusOptions = ref<{ label: string; value: any }[]>([])

// 表单校验规则
const formRules = {
  studentNo: { required: true, message: '请输入学号', trigger: 'blur' },
  name: { required: true, message: '请输入姓名', trigger: 'blur' },
}

const columnDefs: ColumnDefinition<Student>[] = [
  { key: 'selection', title: '选择', column: { type: 'selection' } },
  { key: 'id', title: 'id', column: { title: 'id', key: 'id' } },
  { key: 'studentNo', title: '学号', column: { title: '学号', key: 'studentNo' } },
  { key: 'name', title: '姓名', column: { title: '姓名', key: 'name' } },
  {
    key: 'gender',
    title: '性别',
    column: {
      title: '性别',
      key: 'gender',
      render(row) {
        const val = row.gender
        const opt = genderOptions.value.find(o => o.value === val || String(o.value) === String(val))
        return opt ? opt.label : (val ?? '-')
      }
    }
  },
  { key: 'birthday', title: '出生日期', column: { title: '出生日期', key: 'birthday' } },
  { key: 'phone', title: '手机号', column: { title: '手机号', key: 'phone' } },
  { key: 'email', title: '邮箱', column: { title: '邮箱', key: 'email' } },
  {
    key: 'address',
    title: '地址',
    column: {
      title: '地址',
      key: 'address',
      width: 100,
      render(row) {
        return row.address ? h('img', {
          src: row.address,
          style: { width: '60px', height: '60px', objectFit: 'cover', borderRadius: '4px' }
        }) : '-'
      }
    }
  },
  { key: 'classId', title: '班级ID', column: { title: '班级ID', key: 'classId' } },
  {
    key: 'status',
    title: '状态',
    column: {
      title: '状态',
      key: 'status',
      render(row) {
        const val = row.status
        const opt = statusOptions.value.find(o => o.value === val || String(o.value) === String(val))
        return opt ? opt.label : (val ?? '-')
      }
    }
  },
  {
    key: 'actions',
    title: '操作',
    fixed: 'right',
    column: {
      title: '操作',
      key: 'actions',
      width: 140,
      fixed: 'right',
      render(row) {
        return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'nowrap' } }, [
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) }, {
            default: () => [h(NIcon, null, { default: () => h(CreateOutline) }), ' 编辑']
          }),
          h(NButton, { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row) }, {
            default: () => [h(NIcon, null, { default: () => h(TrashOutline) }), ' 删除']
          })
        ])
      }
    }
  }
]

const preference = useTablePreference<Student>('system/student', columnDefs, 10)
const tableColumns = computed(() => preference.columns.value)
const columnSettingVisible = ref(false)

pagination.pageSize = preference.pageSize.value

watch(preference.pageSize, (newSize) => {
  pagination.pageSize = newSize
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await studentApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      id: searchForm.id || undefined,
      name: searchForm.name || undefined,
      status: searchForm.status || undefined,
    })
    tableData.value = res.list
    pagination.itemCount = res.total
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.id =  null

  searchForm.name = ''

  searchForm.status =  null

  handleSearch()
}

// 分页
function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

async function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  await preference.savePageSize(pageSize)
  loadData()
}

async function handleColumnConfirm(configs: any[]) {
  await preference.updateColumnOrder(configs)
  message.success('列设置已保存')
}

async function handleColumnReset() {
  await preference.resetToDefault()
  message.success('已恢复默认设置')
}

// 选择
function handleCheck(keys: Array<string | number>) {
  selectedIds.value = keys as number[]
}

// 新增
function handleAdd() {
  modalTitle.value = '新增学生表'
  Object.assign(formData, defaultFormData)
  modalVisible.value = true
}

// 编辑
function handleEdit(row: Student) {
  modalTitle.value = '编辑学生表'
  Object.assign(formData, row)
  if (formData.birthday && typeof formData.birthday === 'string') {
    formData.birthday = new Date(formData.birthday + 'T00:00:00').getTime()
  }
  if (formData.createTime && typeof formData.createTime === 'string') {
    formData.createTime = new Date(formData.createTime.replace(' ', 'T')).getTime()
  }
  if (formData.updateTime && typeof formData.updateTime === 'string') {
    formData.updateTime = new Date(formData.updateTime.replace(' ', 'T')).getTime()
  }
  modalVisible.value = true
}

// 提交
async function handleSubmit() {
  await formRef.value?.validate()
  try {
    const submitData = { ...formData } as Student
    if (typeof submitData.birthday === 'number') {
      const d = new Date(submitData.birthday)
      submitData.birthday = d.toISOString().slice(0, 10)
    }
    submitData.address = formData.address ?? ''
    if (typeof submitData.createTime === 'number') {
      submitData.createTime = new Date(submitData.createTime).toISOString().slice(0, 19).replace('T', ' ')
    }
    if (typeof submitData.updateTime === 'number') {
      submitData.updateTime = new Date(submitData.updateTime).toISOString().slice(0, 19).replace('T', ' ')
    }
    if (submitData.id) {
      await studentApi.update(submitData)
      message.success('修改成功')
    } else {
      await studentApi.create(submitData)
      message.success('新增成功')
    }
    modalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 删除
function handleDelete(row: Student) {
  dialog.warning({
    title: '提示',
    content: '确定要删除该记录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await studentApi.delete([row.id!])
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 批量删除
function handleBatchDelete() {
  dialog.warning({
    title: '提示',
    content: `确定要删除选中的 ${selectedIds.value.length} 条记录吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await studentApi.delete(selectedIds.value)
        message.success('删除成功')
        selectedIds.value = []
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 导出
async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (selectedIds.value.length > 0) params.ids = selectedIds.value
    if (searchForm.id != null) params.id = searchForm.id
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.status != null) params.status = searchForm.status
    const blob = await studentApi.export(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生表数据.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 下载导入模板
async function handleDownloadTemplate() {
  try {
    const blob = await studentApi.downloadTemplate()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生表导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 导入上传
async function handleImportUpload({ file }: UploadCustomRequestOptions) {
  if (!file.file) return
  try {
    const result = await studentApi.importData(file.file)
    if (result.fail > 0) {
      dialog.warning({
        title: '导入结果',
        content: `成功: ${result.success} 条，失败: ${result.fail} 条\n错误信息: ${(result.errors || []).join('\n') || '无'}`,
        positiveText: '确定'
      })
    } else {
      message.success(`导入成功，共 ${result.success} 条数据`)
      importModalVisible.value = false
    }
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 加载字典选项
async function loadDictOptions() {
  try {
    const data = await dictDataApi.listByType('gender')
    genderOptions.value = data.map(d => ({ label: d.dictLabel, value: (Number(d.dictValue) || d.dictValue) }))
  } catch {}
  try {
    const data = await dictDataApi.listByType('sys_status')
    statusOptions.value = data.map(d => ({ label: d.dictLabel, value: (Number(d.dictValue) || d.dictValue) }))
  } catch {}
}

onMounted(async () => {
  await preference.init()
  pagination.pageSize = preference.pageSize.value
  loadData()
  loadDictOptions()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}
</style>
