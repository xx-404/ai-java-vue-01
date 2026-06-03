import { ref, computed, watch, onMounted, nextTick } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { useTablePreferenceStore, type TableColumnConfig } from '@/stores/tablePreference'

export interface ColumnDefinition<T = any> {
  key: string
  title: string
  visible?: boolean
  fixed?: 'left' | 'right' | boolean
  width?: number
  column: DataTableColumns<T>[number]
}

export function useTablePreference<T = any>(pageKey: string, columnDefs: ColumnDefinition<T>[], defaultPageSize = 10) {
  const preferenceStore = useTablePreferenceStore()

  const initialized = ref(false)
  const loading = ref(false)

  const columnConfigs = ref<TableColumnConfig[]>(
    columnDefs.map(col => ({
      key: col.key,
      visible: col.visible !== false,
      width: col.width
    }))
  )

  const pageSize = ref(defaultPageSize)

  async function init() {
    loading.value = true
    try {
      const savedConfig = await preferenceStore.loadPreference(pageKey)
      if (savedConfig) {
        if (savedConfig.columns && savedConfig.columns.length > 0) {
          columnConfigs.value = savedConfig.columns
        }
        if (savedConfig.pageSize) {
          pageSize.value = savedConfig.pageSize
        }
      }
    } catch (e) {
      console.error('Failed to init table preference:', pageKey, e)
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  const fixedColumns = columnDefs.filter(col => col.fixed === 'left' || col.fixed === 'right')
  const sortableKeys = columnDefs.filter(col => !col.fixed).map(col => col.key)

  const visibleColumns = computed(() => {
    const configMap = new Map(columnConfigs.value.map(c => [c.key, c]))

    const result: DataTableColumns<T>[number][] = []

    const leftFixed = fixedColumns.filter(c => c.fixed === 'left')
    leftFixed.forEach(col => {
      const config = configMap.get(col.key)
      if (config?.visible !== false) {
        result.push({ ...col.column, key: col.key, fixed: 'left' })
      }
    })

    columnConfigs.value.forEach(config => {
      if (sortableKeys.includes(config.key) && config.visible) {
        const colDef = columnDefs.find(c => c.key === config.key)
        if (colDef) {
          result.push({ ...colDef.column, key: colDef.key })
        }
      }
    })

    const rightFixed = fixedColumns.filter(c => c.fixed === 'right')
    rightFixed.forEach(col => {
      const config = configMap.get(col.key)
      if (config?.visible !== false) {
        result.push({ ...col.column, key: col.key, fixed: 'right' })
      }
    })

    return result as DataTableColumns<T>
  })

  async function saveColumnConfigs() {
    await preferenceStore.setColumns(pageKey, columnConfigs.value)
  }

  async function savePageSize(size: number) {
    pageSize.value = size
    await preferenceStore.setPageSize(pageKey, size)
  }

  async function resetToDefault() {
    await preferenceStore.resetPreference(pageKey)
    columnConfigs.value = columnDefs.map(col => ({
      key: col.key,
      visible: col.visible !== false,
      width: col.width
    }))
    pageSize.value = defaultPageSize
  }

  async function updateColumnOrder(newOrder: TableColumnConfig[]) {
    columnConfigs.value = newOrder
    await saveColumnConfigs()
  }

  async function toggleColumn(key: string, visible: boolean) {
    const config = columnConfigs.value.find(c => c.key === key)
    if (config) {
      config.visible = visible
      await saveColumnConfigs()
    }
  }

  async function moveColumn(fromIndex: number, toIndex: number) {
    const sortableConfigs = columnConfigs.value.filter(c => sortableKeys.includes(c.key))
    if (fromIndex < 0 || fromIndex >= sortableConfigs.length ||
        toIndex < 0 || toIndex >= sortableConfigs.length) {
      return
    }

    const [moved] = sortableConfigs.splice(fromIndex, 1)
    sortableConfigs.splice(toIndex, 0, moved)

    const fixedLeftConfigs = columnConfigs.value.filter(c => {
      const colDef = columnDefs.find(d => d.key === c.key)
      return colDef?.fixed === 'left'
    })
    const fixedRightConfigs = columnConfigs.value.filter(c => {
      const colDef = columnDefs.find(d => d.key === c.key)
      return colDef?.fixed === 'right'
    })

    columnConfigs.value = [...fixedLeftConfigs, ...sortableConfigs, ...fixedRightConfigs]
    await saveColumnConfigs()
  }

  return {
    columns: visibleColumns,
    pageSize,
    columnConfigs,
    columnDefs,
    sortableKeys,
    loading,
    initialized,
    init,
    savePageSize,
    resetToDefault,
    updateColumnOrder,
    toggleColumn,
    moveColumn,
    saveColumnConfigs
  }
}
