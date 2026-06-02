<template>
  <div class="page-container">
    <n-card class="page-layout">
      <!-- 搜索表单 -->
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="角色名称">
            <n-input v-model:value="searchForm.name" placeholder="请输入角色名称" clearable />
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
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <n-button v-if="hasPermission('sys:role:add')" type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增角色
        </n-button>
      </div>

      <!-- 表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SysRole) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <!-- 新增/编辑弹窗 -->
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
        label-width="80"
        class="modal-form"
      >
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="角色名称" path="name">
              <n-input v-model:value="formData.name" placeholder="请输入角色名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="角色编码" path="code">
              <n-input v-model:value="formData.code" placeholder="请输入角色编码" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="排序" path="sort">
              <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
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
        <n-form-item label="菜单权限" path="menuIds">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="menuTreeData"
              :checked-keys="menuIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleMenuCheck"
              @update:indeterminate-keys="handleIndeterminateChange"
            />
          </div>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
        <n-form-item label="数据权限" path="dataScope">
          <n-select
            v-model:value="formData.dataScope"
            placeholder="请选择数据范围"
            :options="dataScopeOptions"
          />
        </n-form-item>
        <n-form-item v-if="formData.dataScope === 2" label="部门权限">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="deptTreeData"
              :checked-keys="deptIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleDeptCheck"
            />
          </div>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 复制角色弹窗 -->
    <n-modal
      v-model:show="copyModalVisible"
      title="复制角色"
      preset="card"
      style="width: 900px"
      :mask-closable="false"
    >
      <n-form
        ref="copyFormRef"
        :model="copyFormData"
        :rules="copyRules"
        label-placement="left"
        label-width="80"
        class="modal-form"
      >
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="新角色名称" path="name">
              <n-input v-model:value="copyFormData.name" placeholder="请输入新角色名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="新角色编码" path="code">
              <n-input v-model:value="copyFormData.code" placeholder="请输入新角色编码" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <!-- 差异预览 -->
        <n-form-item label="差异预览">
          <n-card size="small" class="diff-card">
            <template #header>
              <n-space>
                <n-icon><GitCompareOutline /></n-icon>
                <span>新旧角色差异对比</span>
              </n-space>
            </template>
            <n-grid :cols="2" :x-gap="12">
              <n-gi>
                <n-alert type="info" title="原角色" style="margin-bottom: 12px">
                  <template #icon>
                    <n-icon><DocumentTextOutline /></n-icon>
                  </template>
                  {{ originalRole?.name }} ({{ originalRole?.code }})
                </n-alert>
              </n-gi>
              <n-gi>
                <n-alert type="success" title="新角色" style="margin-bottom: 12px">
                  <template #icon>
                    <n-icon><AddCircleOutline /></n-icon>
                  </template>
                  {{ copyFormData.name || '未填写' }} ({{ copyFormData.code || '未填写' }})
                </n-alert>
              </n-gi>
            </n-grid>

            <n-divider style="margin: 12px 0" />

            <div class="diff-content">
              <div class="diff-item">
                <span class="diff-label">数据范围：</span>
                <span class="diff-original">{{ getDataScopeLabel(originalRole?.dataScope) }}</span>
                <n-icon class="diff-arrow"><ArrowForwardOutline /></n-icon>
                <span class="diff-new">{{ getDataScopeLabel(copyFormData.dataScope) }}</span>
              </div>
              <div class="diff-item">
                <span class="diff-label">菜单权限：</span>
                <span class="diff-original">{{ originalMenuIds?.length || 0 }} 个菜单</span>
                <n-icon class="diff-arrow"><ArrowForwardOutline /></n-icon>
                <span class="diff-new">{{ copyMenuIds.length + copyHalfCheckedKeys.length }} 个菜单</span>
              </div>
              <div v-if="originalRole?.dataScope === 2 || copyFormData.dataScope === 2" class="diff-item">
                <span class="diff-label">部门权限：</span>
                <span class="diff-original">{{ originalDeptIds?.length || 0 }} 个部门</span>
                <n-icon class="diff-arrow"><ArrowForwardOutline /></n-icon>
                <span class="diff-new">{{ copyDeptIds.length }} 个部门</span>
              </div>
              <div class="diff-item">
                <span class="diff-label">状态：</span>
                <span class="diff-original">{{ originalRole?.status === 1 ? '启用' : '禁用' }}</span>
                <n-icon class="diff-arrow"><ArrowForwardOutline /></n-icon>
                <span class="diff-new">{{ copyFormData.status === 1 ? '启用' : '禁用' }}</span>
              </div>
            </div>
          </n-card>
        </n-form-item>

        <n-form-item label="菜单权限">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="menuTreeData"
              :checked-keys="copyMenuIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleCopyMenuCheck"
              @update:indeterminate-keys="handleCopyIndeterminateChange"
            />
          </div>
        </n-form-item>

        <n-form-item label="数据权限" path="dataScope">
          <n-select
            v-model:value="copyFormData.dataScope"
            placeholder="请选择数据范围"
            :options="dataScopeOptions"
          />
        </n-form-item>
        <n-form-item v-if="copyFormData.dataScope === 2" label="部门权限">
          <div class="menu-tree-wrapper">
            <n-tree
              :data="deptTreeData"
              :checked-keys="copyDeptIds"
              checkable
              cascade
              check-strategy="all"
              selectable
              block-line
              @update:checked-keys="handleCopyDeptCheck"
            />
          </div>
        </n-form-item>

        <n-form-item label="备注" path="remark">
          <n-input v-model:value="copyFormData.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="copyModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="copySubmitLoading" @click="handleCopySubmit">确认复制</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption, NIcon, NAlert, NDivider } from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, GitCompareOutline, DocumentTextOutline, AddCircleOutline, ArrowForwardOutline } from '@vicons/ionicons5'
import { roleApi, menuApi, deptApi, type SysRole, type SysMenu, type SysDept } from '@/api/system'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

// 权限检查
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// 搜索表单
const searchForm = reactive({
  name: '',
  status: null as number | null
})

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 数据范围选项
const dataScopeOptions = [
  { label: '全部数据权限', value: 1 },
  { label: '自定数据权限', value: 2 },
  { label: '本部门数据权限', value: 3 },
  { label: '本部门及以下数据权限', value: 4 },
  { label: '仅本人数据权限', value: 5 }
]

// 表格数据
const tableData = ref<SysRole[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

// 菜单树
const menuTreeData = ref<TreeOption[]>([])
const menuIds = ref<number[]>([])

// 部门树
const deptTreeData = ref<TreeOption[]>([])
const deptIds = ref<number[]>([])

// 表格列
const columns: DataTableColumns<SysRole> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'name', width: 150 },
  { title: '角色编码', key: 'code', width: 150 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small' },
        { default: () => (row.status === 1 ? '启用' : '禁用') }
      )
    }
  },
  { title: '备注', key: 'remark',width: 180, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      const buttons = []
      if (hasPermission('sys:role:edit')) {
        buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPermission('sys:role:copy')) {
        buttons.push(h(NButton, { size: 'small', type: 'info', onClick: () => handleCopy(row) }, { default: () => '复制' }))
      }
      if (hasPermission('sys:role:delete')) {
        buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }))
      }
      return buttons.length > 0 ? h(NSpace, null, { default: () => buttons }) : '-'
    }
  }
]

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增角色')
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)

const formData = reactive<SysRole>({
  id: undefined,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

// 复制弹窗
const copyModalVisible = ref(false)
const copyFormRef = ref<FormInst | null>(null)
const copySubmitLoading = ref(false)
const originalRole = ref<SysRole | null>(null)
const originalMenuIds = ref<number[]>([])
const originalDeptIds = ref<number[]>([])

const copyFormData = reactive<SysRole>({
  id: undefined,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  dataScope: 1,
  remark: ''
})

const copyMenuIds = ref<number[]>([])
const copyDeptIds = ref<number[]>([])
const copyHalfCheckedKeys = ref<number[]>([])

const copyRules: FormRules = {
  name: [{ required: true, message: '请输入新角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入新角色编码', trigger: 'blur' }]
}

// 获取数据范围标签
function getDataScopeLabel(value?: number): string {
  const option = dataScopeOptions.find(opt => opt.value === value)
  return option?.label || '未设置'
}

// 转换菜单为树结构
function convertMenuToTree(menus: SysMenu[]): TreeOption[] {
  return menus.map(menu => ({
    key: menu.id,
    label: menu.name,
    children: menu.children ? convertMenuToTree(menu.children) : undefined
  }))
}

// 转换部门为树结构
function convertDeptToTree(depts: SysDept[]): TreeOption[] {
  return depts.map(dept => ({
    key: dept.id,
    label: dept.deptName,
    children: dept.children ? convertDeptToTree(dept.children) : undefined
  }))
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await roleApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      status: searchForm.status ?? undefined
    })
    tableData.value = res.list
    pagination.itemCount = res.total
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

// 加载菜单树
async function loadMenuTree() {
  try {
    const menus = await menuApi.tree()
    menuTreeData.value = convertMenuToTree(menus)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 加载部门树
async function loadDeptTree() {
  try {
    const depts = await deptApi.tree()
    deptTreeData.value = convertDeptToTree(depts)
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  searchForm.name = ''
  searchForm.status = null
  handleSearch()
}

// 分页
function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

// 菜单选择 - 同时记录半选的父节点
const halfCheckedKeys = ref<number[]>([])

function handleMenuCheck(keys: number[]) {
  menuIds.value = keys
}

function handleIndeterminateChange(keys: number[]) {
  halfCheckedKeys.value = keys
}

// 部门选择
function handleDeptCheck(keys: number[]) {
  deptIds.value = keys
}

// 新增
function handleAdd() {
  modalTitle.value = '新增角色'
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    sort: 0,
    status: 1,
    dataScope: 1,
    remark: ''
  })
  menuIds.value = []
  deptIds.value = []
  modalVisible.value = true
}

// 获取所有叶子节点ID
function getLeafNodeIds(nodes: TreeOption[]): number[] {
  const leafIds: number[] = []
  function traverse(items: TreeOption[]) {
    for (const item of items) {
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      } else {
        leafIds.push(item.key as number)
      }
    }
  }
  traverse(nodes)
  return leafIds
}

// 计算半选的父节点ID（allMenuIds中存在的非叶子节点）
function getIndeterminateNodeIds(checkedLeafIds: number[], allMenuIds: number[], nodes: TreeOption[]): number[] {
  const indeterminateIds: number[] = []
  function traverse(items: TreeOption[]) {
    for (const item of items) {
      if (item.children && item.children.length > 0) {
        // 如果是父节点，并且在allMenuIds中，但不在叶子节点选中列表中
        if (allMenuIds.includes(item.key as number) && !checkedLeafIds.includes(item.key as number)) {
          indeterminateIds.push(item.key as number)
        }
        traverse(item.children)
      }
    }
  }
  traverse(nodes)
  return indeterminateIds
}

// 编辑
async function handleEdit(row: SysRole) {
  modalTitle.value = '编辑角色'
  try {
    const res = await roleApi.detail(row.id!)
    Object.assign(formData, res.role)
    // 只选中叶子节点，父节点会自动计算半选状态
    const leafIds = getLeafNodeIds(menuTreeData.value)
    menuIds.value = res.menuIds.filter((id: number) => leafIds.includes(id))
    // 部门回显
    deptIds.value = res.deptIds || []
    modalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    // 合并选中的节点和半选的父节点
    const allMenuIds = [...new Set([...menuIds.value, ...halfCheckedKeys.value])]

    const data = {
      role: { ...formData },
      menuIds: allMenuIds,
      deptIds: formData.dataScope === 2 ? deptIds.value : []
    }

    if (formData.id) {
      await roleApi.update(data)
      message.success('更新成功')
    } else {
      await roleApi.create(data)
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

// 删除
function handleDelete(row: SysRole) {
  dialog.warning({
    title: '提示',
    content: `确定要删除角色"${row.name}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await roleApi.delete(row.id!)
        message.success('删除成功')
        loadData()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 复制角色
async function handleCopy(row: SysRole) {
  try {
    const res = await roleApi.getCopyDetail(row.id!)
    originalRole.value = res.role
    originalMenuIds.value = res.menuIds
    originalDeptIds.value = res.deptIds || []

    // 初始化复制表单
    Object.assign(copyFormData, {
      id: undefined,
      name: res.role.name + '_副本',
      code: res.role.code + '_copy',
      sort: res.role.sort,
      status: res.role.status,
      dataScope: res.role.dataScope,
      remark: res.role.remark ? res.role.remark + ' (复制)' : '复制自: ' + res.role.name
    })

    // 只选中叶子节点，计算半选的父节点
    const leafIds = getLeafNodeIds(menuTreeData.value)
    const checkedLeafIds = res.menuIds.filter((id: number) => leafIds.includes(id))
    copyMenuIds.value = checkedLeafIds
    copyHalfCheckedKeys.value = getIndeterminateNodeIds(checkedLeafIds, res.menuIds, menuTreeData.value)
    copyDeptIds.value = res.deptIds || []

    copyModalVisible.value = true
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 复制菜单选择
function handleCopyMenuCheck(keys: number[]) {
  copyMenuIds.value = keys
}

function handleCopyIndeterminateChange(keys: number[]) {
  copyHalfCheckedKeys.value = keys
}

// 复制部门选择
function handleCopyDeptCheck(keys: number[]) {
  copyDeptIds.value = keys
}

// 复制提交
async function handleCopySubmit() {
  try {
    await copyFormRef.value?.validate()
    copySubmitLoading.value = true

    // 合并选中的节点和半选的父节点
    const allMenuIds = [...new Set([...copyMenuIds.value, ...copyHalfCheckedKeys.value])]

    const data = {
      role: { ...copyFormData },
      menuIds: allMenuIds,
      deptIds: copyFormData.dataScope === 2 ? copyDeptIds.value : []
    }

    await roleApi.copy(data)
    message.success('复制成功')
    copyModalVisible.value = false
    loadData()
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    copySubmitLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadMenuTree()
  loadDeptTree()
})
</script>

<style lang="scss" scoped>
.menu-tree-wrapper {
  width: 100%;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 12px;
}

.page-layout {
  height: calc(100vh - 160px);
}

.diff-card {
  background-color: #fafafa;
}

.diff-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diff-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.diff-label {
  font-weight: 500;
  min-width: 80px;
}

.diff-original {
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.diff-arrow {
  color: #9ca3af;
}

.diff-new {
  background-color: #dcfce7;
  color: #166534;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
