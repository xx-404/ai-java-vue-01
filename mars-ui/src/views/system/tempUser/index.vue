<template>
  <div class="page-container">
    <n-card class="user-list-card" size="small">
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="用户名">
            <n-input v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
          </n-form-item>
          <n-form-item label="状态">
            <n-select
              v-model:value="searchForm.status"
              placeholder="请选择状态"
              :options="statusOptions"
              clearable
              style="width: 120px"
            />
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
              <n-button type="warning" @click="handleCheckExpired">
                <template #icon><n-icon><TimerOutline /></n-icon></template>
                检查过期账号
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <div class="table-toolbar">
        <n-space justify="space-between" style="width: 100%">
          <n-space>
            <n-button v-if="hasPermission('sys:tempUser:add')" type="primary" @click="handleAdd">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新增临时账号
            </n-button>
            <n-button 
              v-if="hasPermission('sys:tempUser:delete') && checkedRowKeys.length > 0" 
              type="error" 
              @click="handleBatchDelete"
            >
              <template #icon><n-icon><TrashOutline /></n-icon></template>
              批量删除({{ checkedRowKeys.length }})
            </n-button>
          </n-space>
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
        :row-key="(row: SysUser) => row.id"
        v-model:checked-row-keys="checkedRowKeys"
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

    <n-modal
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      style="width: 700px"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="100"
        class="modal-form"
      >
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="用户名" path="username">
              <n-input v-model:value="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="!formData.id">
            <n-form-item label="密码" path="password">
              <n-input v-model:value="formData.password" type="password" placeholder="留空默认123456" show-password-on="click" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="昵称" path="nickname">
              <n-input v-model:value="formData.nickname" placeholder="请输入昵称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="失效时间" path="expireTime">
              <n-date-picker
                v-model:value="formData.expireTime"
                type="datetime"
                placeholder="请选择失效时间"
                style="width: 100%"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="邮箱" path="email">
              <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="手机号" path="phone">
              <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="性别" path="gender">
              <n-radio-group v-model:value="formData.gender">
                <n-radio :value="1">男</n-radio>
                <n-radio :value="2">女</n-radio>
                <n-radio :value="0">未知</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="状态" path="status">
              <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
                <template #checked>启用</template>
                <template #unchecked>禁用</template>
              </n-switch>
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-divider />
        <n-form-item label="授权菜单">
          <n-transfer
            v-model:value="menuIds"
            :options="menuOptions"
            :titles="['可选菜单', '已授权菜单']"
            :list-style="{
              width: '280px',
              height: '300px'
            }"
          />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="formData.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
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
import { NButton, NTag, NSpace, NDropdown, NPagination, NGrid, NGi, NTransfer, NDatePicker, useMessage, useDialog, type FormInst, type FormRules, type TransferOption } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, ChevronDownOutline, DownloadOutline, TrashOutline, TimerOutline, SettingsOutline } from '@vicons/ionicons5'
import { tempUserApi, menuApi, type SysUser, type SysMenu } from '@/api/system'
import { useUserStore } from '@/stores/user'
import TableColumnSetting from '@/components/TableColumnSetting.vue'
import { useTablePreference, type ColumnDefinition } from '@/composables/useTablePreference'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

const searchForm = reactive({
  username: '',
  status: null as number | null
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const tableData = ref<SysUser[]>([])
const loading = ref(false)
const checkedRowKeys = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const menuOptions = ref<TransferOption[]>([])
const menuIds = ref<number[]>([])

const columnDefs: ColumnDefinition<SysUser>[] = [
  { key: 'selection', title: '选择', column: { type: 'selection' } },
  { key: 'id', title: 'ID', column: { title: 'ID', key: 'id', width: 60 } },
  { key: 'username', title: '用户名', column: { title: '用户名', key: 'username', width: 120 } },
  { key: 'nickname', title: '昵称', column: { title: '昵称', key: 'nickname', width: 120 } },
  { key: 'email', title: '邮箱', column: { title: '邮箱', key: 'email', width: 150, render(row) { return row.email || '-' } } },
  { key: 'phone', title: '手机号', column: { title: '手机号', key: 'phone', width: 120, render(row) { return row.phone || '-' } } },
  {
    key: 'remainingDays',
    title: '剩余天数',
    column: {
      title: '剩余天数',
      key: 'remainingDays',
      width: 100,
      render(row) {
        if (row.remainingDays === undefined || row.remainingDays === null) {
          return '-'
        }
        const type = row.willExpire ? 'error' : row.remainingDays > 7 ? 'success' : 'warning'
        return h(NTag, { type, size: 'small' }, { default: () => {
          if (row.remainingDays <= 0) {
            return '已过期'
          }
          return `${row.remainingDays}天`
        }})
      }
    }
  },
  {
    key: 'expireTime',
    title: '失效时间',
    column: {
      title: '失效时间',
      key: 'expireTime',
      width: 180,
      render(row) {
        return row.expireTime || '-'
      }
    }
  },
  {
    key: 'status',
    title: '状态',
    column: {
      title: '状态',
      key: 'status',
      width: 80,
      render(row) {
        const statusMap: Record<number, { type: 'success' | 'error' | 'warning' | 'info'; label: string }> = {
          0: { type: 'error', label: '禁用' },
          1: { type: 'success', label: '启用' }
        }
        const status = statusMap[row.status] || { type: 'info', label: '未知' }
        return h(NTag, { type: status.type, size: 'small' }, { default: () => status.label })
      }
    }
  },
  { key: 'createTime', title: '创建时间', column: { title: '创建时间', key: 'createTime', width: 170 } },
  {
    key: 'actions',
    title: '操作',
    fixed: 'right',
    column: {
      title: '操作',
      key: 'actions',
      width: 180,
      fixed: 'right',
      render(row) {
        const buttons = []
        if (hasPermission('sys:tempUser:edit')) {
          buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }))
        }
        if (hasPermission('sys:tempUser:delete')) {
          buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
        }
        return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
      }
    }
  }
]

const preference = useTablePreference<SysUser>('system/tempUser', columnDefs, 10)
const tableColumns = computed(() => preference.columns.value)
const columnSettingVisible = ref(false)

pagination.pageSize = preference.pageSize.value

watch(preference.pageSize, (newSize) => {
  pagination.pageSize = newSize
})

const modalVisible = ref(false)
const modalTitle = ref('新增临时账号')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)

const formData = reactive<SysUser>({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  status: 1,
  expireTime: '',
  remark: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  expireTime: [{ required: true, message: '请选择失效时间', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await tempUserApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      username: searchForm.username || undefined,
      status: searchForm.status ?? undefined
    })
    tableData.value = res.list
    pagination.itemCount = Number(res.total)
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

async function loadMenuOptions() {
  try {
    const menus = await menuApi.tree()
    const options: TransferOption[] = []
    function flatten(menu: SysMenu, prefix = '') {
      const label = prefix + menu.name
      options.push({
        label,
        value: menu.id!,
        disabled: menu.type === 3 // 按钮类型不允许单独授权
      })
      if (menu.children) {
        menu.children.forEach(child => flatten(child, label + '/'))
      }
    }
    menus.forEach(flatten)
    menuOptions.value = options
  } catch (error) {
    // 错误已在拦截器处理
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.status = null
  handleSearch()
}

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

function handleAdd() {
  modalTitle.value = '新增临时账号'
  Object.assign(formData, {
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    gender: 0,
    status: 1,
    expireTime: '',
    remark: ''
  })
  menuIds.value = []
  modalVisible.value = true
}

async function handleEdit(row: SysUser) {
  modalTitle.value = '编辑临时账号'
  try {
    const res = await tempUserApi.detail(row.id!)
    Object.assign(formData, res.user)
    menuIds.value = res.menuIds
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    const data = {
      user: { ...formData },
      menuIds: menuIds.value
    }

    if (formData.id) {
      await tempUserApi.update(data)
      message.success('更新成功')
    } else {
      await tempUserApi.create(data)
      message.success('创建成功')
    }

    modalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row: SysUser) {
  dialog.warning({
    title: '提示',
    content: `确定要删除临时账号"${row.username}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await tempUserApi.delete(row.id!)
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) {
    message.warning('请选择要删除的账号')
    return
  }
  dialog.warning({
    title: '提示',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 个临时账号吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        for (const id of checkedRowKeys.value) {
          await tempUserApi.delete(id)
        }
        message.success('删除成功')
        checkedRowKeys.value = []
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

async function handleCheckExpired() {
  try {
    await tempUserApi.checkExpired()
    message.success('已检查并禁用过期账号')
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  }
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
  loadMenuOptions()
})
</script>

<style scoped>
</style>
