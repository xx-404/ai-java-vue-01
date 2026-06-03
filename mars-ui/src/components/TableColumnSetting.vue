<template>
  <n-modal
    v-model:show="innerShow"
    title="表格列设置"
    preset="card"
    style="width: 420px"
    :mask-closable="false"
  >
    <div class="column-setting">
      <div class="setting-tips">
        <n-text depth="3">勾选需要显示的列，拖拽可调整顺序</n-text>
      </div>

      <div class="column-list">
        <div
          v-for="(col, index) in displayColumns"
          :key="col.key"
          class="column-item"
          :class="{
            'is-fixed': col.isFixed,
            'is-dragging': dragIndex === index,
            'is-over': dragOverIndex === index && dragIndex !== index
          }"
          :draggable="!col.isFixed && !col.isFixedRight"
          @dragstart="handleDragStart($event, index)"
          @dragend="handleDragEnd"
          @dragover.prevent="handleDragOver($event, index)"
          @dragleave="handleDragLeave"
          @drop="handleDrop($event, index)"
        >
          <div class="column-handle" v-if="!col.isFixed && !col.isFixedRight">
            <n-icon size="16" color="#999">
              <ReorderThreeOutline />
            </n-icon>
          </div>
          <div class="column-handle-placeholder" v-else></div>

          <n-checkbox
            :checked="col.visible"
            :disabled="col.isFixed || col.isFixedRight"
            @update:checked="(val) => handleToggle(col.key, val)"
          >
            {{ col.title }}
          </n-checkbox>

          <n-tag v-if="col.isFixed" size="small" type="info">固定</n-tag>
          <n-tag v-if="col.isFixedRight" size="small" type="info">固定</n-tag>
        </div>
      </div>

      <div class="setting-footer">
        <n-space>
          <n-button size="small" @click="handleSelectAll">全选</n-button>
          <n-button size="small" @click="handleSelectNone">全不选</n-button>
          <n-button size="small" @click="handleReset">恢复默认</n-button>
        </n-space>
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="innerShow = false">取消</n-button>
        <n-button type="primary" @click="handleConfirm">确定</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NCheckbox, NTag, NSpace, NButton, NIcon, useMessage } from 'naive-ui'
import { ReorderThreeOutline } from '@vicons/ionicons5'
import type { ColumnDefinition } from '@/composables/useTablePreference'
import type { TableColumnConfig } from '@/stores/tablePreference'

const props = defineProps<{
  show: boolean
  columnDefs: ColumnDefinition[]
  columnConfigs: TableColumnConfig[]
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  confirm: [configs: TableColumnConfig[]]
  reset: []
}>()

const message = useMessage()

const innerShow = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const localConfigs = ref<TableColumnConfig[]>([])

watch(() => props.columnConfigs, (configs) => {
  if (configs && configs.length > 0) {
    localConfigs.value = JSON.parse(JSON.stringify(configs))
  } else {
    localConfigs.value = props.columnDefs.map(col => ({
      key: col.key,
      visible: col.visible !== false,
      width: col.width
    }))
  }
}, { immediate: true, deep: true })

interface DisplayColumn {
  key: string
  title: string
  visible: boolean
  isFixed: boolean
  isFixedRight: boolean
}

const displayColumns = computed<DisplayColumn[]>(() => {
  const fixedLeft: DisplayColumn[] = []
  const fixedRight: DisplayColumn[] = []

  const configMap = new Map(localConfigs.value.map(c => [c.key, c]))

  props.columnDefs.forEach(colDef => {
    const config = configMap.get(colDef.key)
    const col: DisplayColumn = {
      key: colDef.key,
      title: colDef.title,
      visible: config?.visible ?? true,
      isFixed: colDef.fixed === 'left',
      isFixedRight: colDef.fixed === 'right'
    }

    if (col.isFixed) {
      fixedLeft.push(col)
    } else if (col.isFixedRight) {
      fixedRight.push(col)
    }
  })

  const sortableOrdered: DisplayColumn[] = []
  const hasConfigs = localConfigs.value.length > 0

  if (hasConfigs) {
    localConfigs.value.forEach(config => {
      const colDef = props.columnDefs.find(c => c.key === config.key)
      if (colDef && !colDef.fixed) {
        sortableOrdered.push({
          key: colDef.key,
          title: colDef.title,
          visible: config.visible,
          isFixed: false,
          isFixedRight: false
        })
      }
    })
  } else {
    props.columnDefs.forEach(colDef => {
      if (!colDef.fixed && colDef.fixed !== 'right') {
        sortableOrdered.push({
          key: colDef.key,
          title: colDef.title,
          visible: true,
          isFixed: false,
          isFixedRight: false
        })
      }
    })
  }

  return [...fixedLeft, ...sortableOrdered, ...fixedRight]
})

const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

function handleDragStart(e: DragEvent, index: number) {
  dragIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', index.toString())
  }
}

function handleDragEnd() {
  dragIndex.value = null
  dragOverIndex.value = null
}

function handleDragOver(e: DragEvent, index: number) {
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'move'
  }
  const col = displayColumns.value[index]
  if (!col.isFixed && !col.isFixedRight) {
    dragOverIndex.value = index
  }
}

function handleDragLeave() {
  dragOverIndex.value = null
}

function handleDrop(e: DragEvent, toIndex: number) {
  e.preventDefault()
  if (dragIndex.value === null || dragIndex.value === toIndex) {
    return
  }

  const fromCol = displayColumns.value[dragIndex.value]
  const toCol = displayColumns.value[toIndex]

  if (fromCol.isFixed || fromCol.isFixedRight || toCol.isFixed || toCol.isFixedRight) {
    return
  }

  const sortableConfigs = localConfigs.value.filter(c => {
    const colDef = props.columnDefs.find(d => d.key === c.key)
    return !colDef?.fixed
  })

  const fixedLeftConfigs = localConfigs.value.filter(c => {
    const colDef = props.columnDefs.find(d => d.key === c.key)
    return colDef?.fixed === 'left'
  })
  const fixedRightConfigs = localConfigs.value.filter(c => {
    const colDef = props.columnDefs.find(d => d.key === c.key)
    return colDef?.fixed === 'right'
  })

  const sortableFromIndex = sortableConfigs.findIndex(c => c.key === fromCol.key)
  const sortableToIndex = sortableConfigs.findIndex(c => c.key === toCol.key)

  if (sortableFromIndex !== -1 && sortableToIndex !== -1) {
    const [moved] = sortableConfigs.splice(sortableFromIndex, 1)
    sortableConfigs.splice(sortableToIndex, 0, moved)
    localConfigs.value = [...fixedLeftConfigs, ...sortableConfigs, ...fixedRightConfigs]
  }

  dragIndex.value = null
  dragOverIndex.value = null
}

function handleToggle(key: string, visible: boolean) {
  const config = localConfigs.value.find(c => c.key === key)
  if (config) {
    config.visible = visible
  }
}

function handleSelectAll() {
  localConfigs.value.forEach(config => {
    config.visible = true
  })
}

function handleSelectNone() {
  localConfigs.value.forEach(config => {
    const colDef = props.columnDefs.find(c => c.key === config.key)
    if (!colDef?.fixed) {
      config.visible = false
    }
  })
}

function handleReset() {
  emit('reset')
  message.success('已恢复默认设置')
}

function handleConfirm() {
  const hasVisible = localConfigs.value.some(c => c.visible)
  if (!hasVisible) {
    message.warning('至少需要显示一列')
    return
  }
  emit('confirm', JSON.parse(JSON.stringify(localConfigs.value)))
  innerShow.value = false
}
</script>

<style lang="scss" scoped>
.column-setting {
  max-height: 500px;
  display: flex;
  flex-direction: column;
}

.setting-tips {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: var(--n-color-fill-tertiary);
  border-radius: 6px;
}

.column-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid var(--n-border-color);
  border-radius: 6px;
  padding: 4px;
  min-height: 200px;
}

.column-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
  margin-bottom: 2px;

  &:hover {
    background: var(--n-color-hover);
  }

  &.is-fixed,
  &.is-fixed-right {
    cursor: not-allowed;
    background: var(--n-color-fill-tertiary);
  }

  &.is-dragging {
    opacity: 0.5;
    background: var(--n-color-primary-supple);
  }

  &.is-over {
    border: 1px dashed var(--n-primary-color);
    background: var(--n-color-primary-supple);
  }
}

.column-handle {
  display: flex;
  align-items: center;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

.column-handle-placeholder {
  width: 16px;
}

.setting-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--n-border-color);
}
</style>
