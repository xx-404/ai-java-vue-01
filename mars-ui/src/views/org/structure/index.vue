<template>
  <div class="page-container">
    <div class="org-layout">
      <n-card class="org-tree-card" size="small">
        <template #header>
          <div class="org-tree-header">
            <span>组织架构</span>
            <n-button quaternary circle size="small" @click="loadTree" :loading="treeLoading">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
            </n-button>
          </div>
        </template>
        <div class="org-search">
          <n-input v-model:value="searchKeyword" placeholder="搜索部门/岗位/人员" clearable size="small">
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
        </div>
        <n-spin :show="treeLoading">
          <div class="org-tree-wrapper">
            <n-tree
              :data="treeData"
              :pattern="searchKeyword"
              :selected-keys="selectedKeys"
              :expanded-keys="expandedKeys"
              key-field="key"
              label-field="label"
              children-field="children"
              selectable
              block-line
              :render-prefix="renderPrefix"
              @update:selected-keys="handleSelect"
              @update:expanded-keys="handleExpand"
            />
          </div>
        </n-spin>
      </n-card>

      <n-card class="org-detail-card" size="small">
        <template #header>
          <div class="detail-header">
            <span>{{ detailTitle }}</span>
            <n-space v-if="selectedNode && selectedNode.nodeType !== 'virtual'">
              <template v-if="selectedNode.nodeType === 'dept'">
                <n-button v-if="hasPermission('sys:dept:edit')" size="small" @click="handleEditDept">编辑</n-button>
                <n-button v-if="hasPermission('sys:dept:add')" type="primary" size="small" @click="handleAddDept">新增子部门</n-button>
                <n-button v-if="hasPermission('sys:dept:delete')" type="error" size="small" @click="handleDeleteDept">删除</n-button>
              </template>
              <template v-if="selectedNode.nodeType === 'post'">
                <n-button v-if="hasPermission('sys:post:edit')" size="small" @click="handleEditPost">编辑</n-button>
                <n-button v-if="hasPermission('sys:post:add')" type="primary" size="small" @click="handleAddPost">新增子岗位</n-button>
                <n-button v-if="hasPermission('sys:post:delete')" type="error" size="small" @click="handleDeletePost">删除</n-button>
              </template>
              <template v-if="selectedNode.nodeType === 'user'">
                <n-button v-if="hasPermission('sys:user:edit')" size="small" @click="handleEditUser">编辑</n-button>
                <n-button v-if="hasPermission('sys:user:edit')" size="small" @click="handleToggleQuit">
                  {{ selectedNode.extra?.isQuit === 1 ? '取消离职' : '标记离职' }}
                </n-button>
                <n-button v-if="hasPermission('sys:user:edit')" size="small" @click="handleResetPwd">重置密码</n-button>
                <n-button v-if="hasPermission('sys:user:delete')" type="error" size="small" @click="handleDeleteUser">删除</n-button>
              </template>
            </n-space>
          </div>
        </template>

        <div class="detail-content" v-if="selectedNode">
          <template v-if="selectedNode.nodeType === 'virtual'">
            <n-empty :description="selectedNode.label === '部门' ? '请选择左侧部门节点查看详情' : '请选择左侧岗位节点查看详情'" />
          </template>

          <template v-if="selectedNode.nodeType === 'dept'">
            <n-descriptions bordered :column="2" label-placement="left" size="small">
              <n-descriptions-item label="部门名称">{{ selectedNode.extra?.deptName || selectedNode.label }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag :type="selectedNode.extra?.status === 1 ? 'success' : 'error'" size="small">
                  {{ selectedNode.extra?.status === 1 ? '正常' : '停用' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="负责人">{{ selectedNode.extra?.leader || '-' }}</n-descriptions-item>
              <n-descriptions-item label="联系电话">{{ selectedNode.extra?.phone || '-' }}</n-descriptions-item>
              <n-descriptions-item label="邮箱">{{ selectedNode.extra?.email || '-' }}</n-descriptions-item>
              <n-descriptions-item label="排序">{{ selectedNode.extra?.sort ?? '-' }}</n-descriptions-item>
              <n-descriptions-item label="创建时间" :span="2">{{ selectedNode.extra?.createTime || '-' }}</n-descriptions-item>
            </n-descriptions>
          </template>

          <template v-if="selectedNode.nodeType === 'post'">
            <n-descriptions bordered :column="2" label-placement="left" size="small">
              <n-descriptions-item label="岗位名称">{{ selectedNode.extra?.postName || selectedNode.label }}</n-descriptions-item>
              <n-descriptions-item label="岗位编码">{{ selectedNode.extra?.postCode || '-' }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag :type="selectedNode.extra?.status === 1 ? 'success' : 'error'" size="small">
                  {{ selectedNode.extra?.status === 1 ? '正常' : '停用' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="排序">{{ selectedNode.extra?.sort ?? '-' }}</n-descriptions-item>
              <n-descriptions-item label="备注" :span="2">{{ selectedNode.extra?.remark || '-' }}</n-descriptions-item>
              <n-descriptions-item label="创建时间" :span="2">{{ selectedNode.extra?.createTime || '-' }}</n-descriptions-item>
            </n-descriptions>
          </template>

          <template v-if="selectedNode.nodeType === 'user'">
            <n-descriptions bordered :column="2" label-placement="left" size="small">
              <n-descriptions-item label="用户名">{{ selectedNode.extra?.username || '-' }}</n-descriptions-item>
              <n-descriptions-item label="昵称">{{ selectedNode.extra?.nickname || '-' }}</n-descriptions-item>
              <n-descriptions-item label="部门">{{ selectedNode.extra?.deptName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="岗位">{{ selectedNode.extra?.postNames || '-' }}</n-descriptions-item>
              <n-descriptions-item label="用户类型">
                <n-tag :type="userTypeTag(selectedNode.extra?.userType)" size="small">
                  {{ userTypeLabel(selectedNode.extra?.userType) }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag :type="statusTagType(selectedNode.extra?.status)" size="small">
                  {{ statusLabel(selectedNode.extra?.status) }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="手机号">{{ selectedNode.extra?.phone || '-' }}</n-descriptions-item>
              <n-descriptions-item label="邮箱">{{ selectedNode.extra?.email || '-' }}</n-descriptions-item>
              <n-descriptions-item label="性别">{{ genderLabel(selectedNode.extra?.gender) }}</n-descriptions-item>
              <n-descriptions-item label="离职">
                <n-tag :type="selectedNode.extra?.isQuit === 1 ? 'error' : 'success'" size="small">
                  {{ selectedNode.extra?.isQuit === 1 ? '是' : '否' }}
                </n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="创建时间" :span="2">{{ selectedNode.extra?.createTime || '-' }}</n-descriptions-item>
            </n-descriptions>
          </template>
        </div>

        <div class="detail-empty" v-else>
          <n-empty description="请选择左侧节点查看详情" />
        </div>
      </n-card>
    </div>

    <n-modal v-model:show="deptModalVisible" :title="deptModalTitle" preset="card" style="width: 550px" :mask-closable="false">
      <n-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-placement="left" label-width="90">
        <n-form-item label="上级部门" path="parentId">
          <n-tree-select
            v-model:value="deptForm.parentId"
            :options="deptTreeOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="请选择上级部门"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="部门名称" path="deptName">
          <n-input v-model:value="deptForm.deptName" placeholder="请输入部门名称" />
        </n-form-item>
        <n-form-item label="负责人" path="leader">
          <n-input v-model:value="deptForm.leader" placeholder="请输入负责人" />
        </n-form-item>
        <n-form-item label="联系电话" path="phone">
          <n-input v-model:value="deptForm.phone" placeholder="请输入联系电话" />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="deptForm.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="显示排序" path="sort">
          <n-input-number v-model:value="deptForm.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="deptForm.status" :checked-value="1" :unchecked-value="0">
            <template #checked>正常</template>
            <template #unchecked>停用</template>
          </n-switch>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="deptModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmitDept">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="postModalVisible" :title="postModalTitle" preset="card" style="width: 500px" :mask-closable="false">
      <n-form ref="postFormRef" :model="postForm" :rules="postRules" label-placement="left" label-width="80">
        <n-form-item label="上级岗位" path="parentId">
          <n-tree-select
            v-model:value="postForm.parentId"
            :options="postTreeOptions"
            key-field="id"
            label-field="postName"
            children-field="children"
            placeholder="请选择上级岗位"
            clearable
          />
        </n-form-item>
        <n-form-item label="岗位编码" path="postCode">
          <n-input v-model:value="postForm.postCode" placeholder="请输入岗位编码" />
        </n-form-item>
        <n-form-item label="岗位名称" path="postName">
          <n-input v-model:value="postForm.postName" placeholder="请输入岗位名称" />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="postForm.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="postForm.status" :checked-value="1" :unchecked-value="0">
            <template #checked>正常</template>
            <template #unchecked>停用</template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="postForm.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="postModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmitPost">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, useMessage, useDialog, type TreeOption, type FormInst, type FormRules } from 'naive-ui'
import {
  SearchOutline,
  RefreshOutline,
  BusinessOutline,
  IdCardOutline,
  PersonOutline,
  FolderOutline
} from '@vicons/ionicons5'
import { orgStructureApi, deptApi, postApi, type OrgTreeNode, type SysDept, type SysPost } from '@/api/org'
import { userApi } from '@/api/system'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()
const userStore = useUserStore()
const hasPermission = (p: string) => userStore.hasPermission(p)

const searchKeyword = ref('')
const treeData = ref<OrgTreeNode[]>([])
const treeLoading = ref(false)
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const selectedNode = ref<OrgTreeNode | null>(null)
const usersLoaded = ref(new Set<string>())

const detailTitle = computed(() => {
  if (!selectedNode.value) return '组织架构详情'
  const n = selectedNode.value
  if (n.nodeType === 'virtual') return n.label
  if (n.nodeType === 'dept') return `部门 - ${n.label}`
  if (n.nodeType === 'post') return `岗位 - ${n.label}`
  if (n.nodeType === 'user') return `用户 - ${n.label}`
  return '组织架构详情'
})

function renderPrefix({ option }: { option: TreeOption }) {
  const nodeType = (option as any).nodeType as string
  let icon: any = FolderOutline
  if (nodeType === 'virtual') icon = FolderOutline
  else if (nodeType === 'dept') icon = BusinessOutline
  else if (nodeType === 'post') icon = IdCardOutline
  else if (nodeType === 'user') icon = PersonOutline
  return h(NIcon, { size: 16, style: { marginRight: '4px' } }, { default: () => h(icon) })
}

async function loadTree() {
  treeLoading.value = true
  try {
    treeData.value = await orgStructureApi.tree()
    usersLoaded.value.clear()
  } catch (e) {
    console.error('加载组织架构树失败:', e)
  } finally {
    treeLoading.value = false
  }
}

async function loadUsersForNode(node: OrgTreeNode) {
  if (!node.nodeId) return
  if (usersLoaded.value.has(node.key)) return
  if (node.nodeType !== 'dept' && node.nodeType !== 'post') return

  try {
    const userNodes = await orgStructureApi.users(node.nodeType, node.nodeId)
    if (!node.children) node.children = []

    const structuralChildren = node.children.filter(c => c.nodeType !== 'user')
    node.children = [...structuralChildren, ...userNodes]
    usersLoaded.value.add(node.key)
  } catch (e) {
    console.error('加载人员数据失败:', e)
  }
}

async function handleExpand(keys: string[]) {
  const addedKeys = keys.filter(k => !expandedKeys.value.includes(k))
  expandedKeys.value = keys

  for (const key of addedKeys) {
    const node = findNodeByKey(treeData.value, key)
    if (node && (node.nodeType === 'dept' || node.nodeType === 'post')) {
      await loadUsersForNode(node)
    }
  }
}

function handleSelect(keys: string[]) {
  selectedKeys.value = keys
  if (keys.length === 0) {
    selectedNode.value = null
    return
  }
  const node = findNodeByKey(treeData.value, keys[0])
  selectedNode.value = node
}

function findNodeByKey(nodes: OrgTreeNode[], key: string): OrgTreeNode | null {
  for (const node of nodes) {
    if (node.key === key) return node
    if (node.children) {
      const found = findNodeByKey(node.children, key)
      if (found) return found
    }
  }
  return null
}

function userTypeTag(t?: string) {
  const m: Record<string, 'info' | 'success' | 'warning'> = { admin: 'info', pc: 'success', app: 'warning' }
  return m[t || 'admin'] || 'info'
}
function userTypeLabel(t?: string) {
  const m: Record<string, string> = { admin: '后台管理员', pc: 'PC前台', app: 'App/小程序' }
  return m[t || 'admin'] || t || '未知'
}
function statusTagType(s?: number) {
  const m: Record<number, 'success' | 'error' | 'warning' | 'info'> = { 0: 'error', 1: 'success', 2: 'warning', 3: 'error' }
  return m[s ?? -1] || 'info'
}
function statusLabel(s?: number) {
  const m: Record<number, string> = { 0: '禁用', 1: '启用', 2: '待审核', 3: '审核拒绝' }
  return m[s ?? -1] || '未知'
}
function genderLabel(g?: number) {
  const m: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
  return m[g ?? 0] || '未知'
}

// ==================== 部门维护 ====================
const deptModalVisible = ref(false)
const deptModalTitle = ref('')
const deptFormRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const deptForm = reactive<SysDept>({ id: undefined, parentId: 0, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 })
const deptRules: FormRules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const deptTreeOptions = computed(() => [{ id: 0, deptName: '主目录', children: collectDeptList(treeData.value) }])

function collectDeptList(nodes: OrgTreeNode[]): any[] {
  const result: any[] = []
  for (const node of nodes) {
    if (node.nodeType === 'dept') {
      const item: any = { id: node.nodeId, deptName: node.label }
      if (node.children) {
        const deptChildren = node.children.filter(c => c.nodeType === 'dept')
        if (deptChildren.length > 0) item.children = collectDeptList(deptChildren)
      }
      result.push(item)
    }
    if (node.nodeType === 'virtual' && node.children) {
      result.push(...collectDeptList(node.children))
    }
  }
  return result
}

function handleAddDept() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'dept') return
  deptModalTitle.value = '新增子部门'
  Object.assign(deptForm, { id: undefined, parentId: selectedNode.value.nodeId, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 })
  deptModalVisible.value = true
}

async function handleEditDept() {
  if (!selectedNode.value?.nodeId) return
  try {
    const dept = await deptApi.detail(selectedNode.value.nodeId)
    deptModalTitle.value = '编辑部门'
    Object.assign(deptForm, dept)
    deptModalVisible.value = true
  } catch (e) { console.error(e) }
}

function handleDeleteDept() {
  if (!selectedNode.value?.nodeId) return
  dialog.warning({
    title: '提示',
    content: `确定要删除部门"${selectedNode.value.label}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deptApi.delete(selectedNode.value!.nodeId!)
        message.success('删除成功')
        selectedNode.value = null
        selectedKeys.value = []
        loadTree()
      } catch (e) { console.error(e) }
    }
  })
}

async function handleSubmitDept() {
  try {
    await deptFormRef.value?.validate()
    submitLoading.value = true
    if (deptForm.id) {
      await deptApi.update(deptForm)
      message.success('更新成功')
    } else {
      await deptApi.create(deptForm)
      message.success('创建成功')
    }
    deptModalVisible.value = false
    loadTree()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

// ==================== 岗位维护 ====================
const postModalVisible = ref(false)
const postModalTitle = ref('')
const postFormRef = ref<FormInst | null>(null)
const postForm = reactive<SysPost>({ id: undefined, parentId: 0, postCode: '', postName: '', sort: 0, status: 1, remark: '' })
const postRules: FormRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }]
}

const postTreeOptions = computed(() => [{ id: 0, postName: '顶级岗位', children: collectPostList(treeData.value) }])

function collectPostList(nodes: OrgTreeNode[]): any[] {
  const result: any[] = []
  for (const node of nodes) {
    if (node.nodeType === 'post') {
      const item: any = { id: node.nodeId, postName: node.label }
      if (node.children) {
        const postChildren = node.children.filter(c => c.nodeType === 'post')
        if (postChildren.length > 0) item.children = collectPostList(postChildren)
      }
      result.push(item)
    }
    if (node.nodeType === 'virtual' && node.children) {
      result.push(...collectPostList(node.children))
    }
  }
  return result
}

function handleAddPost() {
  if (!selectedNode.value || selectedNode.value.nodeType !== 'post') return
  postModalTitle.value = '新增子岗位'
  Object.assign(postForm, { id: undefined, parentId: selectedNode.value.nodeId, postCode: '', postName: '', sort: 0, status: 1, remark: '' })
  postModalVisible.value = true
}

async function handleEditPost() {
  if (!selectedNode.value?.nodeId) return
  try {
    const post = await postApi.detail(selectedNode.value.nodeId)
    postModalTitle.value = '编辑岗位'
    Object.assign(postForm, post)
    postModalVisible.value = true
  } catch (e) { console.error(e) }
}

function handleDeletePost() {
  if (!selectedNode.value?.nodeId) return
  dialog.warning({
    title: '提示',
    content: `确定要删除岗位"${selectedNode.value.label}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await postApi.delete(selectedNode.value!.nodeId!)
        message.success('删除成功')
        selectedNode.value = null
        selectedKeys.value = []
        loadTree()
      } catch (e) { console.error(e) }
    }
  })
}

async function handleSubmitPost() {
  try {
    await postFormRef.value?.validate()
    submitLoading.value = true
    if (postForm.id) {
      await postApi.update(postForm)
      message.success('更新成功')
    } else {
      await postApi.create(postForm)
      message.success('创建成功')
    }
    postModalVisible.value = false
    loadTree()
  } catch (e) { console.error(e) } finally { submitLoading.value = false }
}

// ==================== 用户操作 ====================
function handleEditUser() {
  if (!selectedNode.value?.nodeId) return
  router.push({ path: '/system/user', query: { id: String(selectedNode.value.nodeId) } })
}

function handleToggleQuit() {
  if (!selectedNode.value?.nodeId) return
  dialog.warning({
    title: '提示',
    content: `确定要${selectedNode.value!.extra?.isQuit === 1 ? '取消' : '标记'}离职吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.toggleQuit(selectedNode.value!.nodeId!)
        message.success('操作成功')
        loadTree()
      } catch (e) { console.error(e) }
    }
  })
}

function handleResetPwd() {
  if (!selectedNode.value?.nodeId) return
  dialog.warning({
    title: '提示',
    content: '确定要重置该用户密码吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.resetPassword(selectedNode.value!.nodeId!)
        message.success('密码已重置')
      } catch (e) { console.error(e) }
    }
  })
}

function handleDeleteUser() {
  if (!selectedNode.value?.nodeId) return
  dialog.warning({
    title: '提示',
    content: `确定要删除用户"${selectedNode.value.label}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.delete(selectedNode.value!.nodeId!)
        message.success('删除成功')
        selectedNode.value = null
        selectedKeys.value = []
        loadTree()
      } catch (e) { console.error(e) }
    }
  })
}

onMounted(() => { loadTree() })
</script>

<style scoped>
.org-layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.org-tree-card {
  width: 300px;
  flex-shrink: 0;
}

.org-tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}

.org-search {
  margin-bottom: 10px;
}

.org-tree-wrapper {
  height: calc(100vh - 260px);
  overflow-y: auto;
}

.org-detail-card {
  flex: 1;
  min-width: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-content {
  padding: 4px 0;
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}
</style>
