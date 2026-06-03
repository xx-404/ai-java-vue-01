import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTablePreference, type ColumnDefinition } from '@/composables/useTablePreference'
import { tablePreferenceApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  tablePreferenceApi: {
    getPreference: vi.fn(),
    savePreference: vi.fn(),
    saveColumnsConfig: vi.fn(),
    savePageSize: vi.fn(),
    resetPreference: vi.fn(),
    resetAllPreferences: vi.fn()
  }
}))

interface TestRow {
  id: number
  name: string
  email: string
  status: number
}

function createColumnDefs(): ColumnDefinition<TestRow>[] {
  return [
    {
      key: 'selection',
      title: '选择',
      fixed: 'left' as const,
      column: { type: 'selection' as const }
    },
    {
      key: 'id',
      title: 'ID',
      column: { title: 'ID', key: 'id' }
    },
    {
      key: 'name',
      title: '名称',
      column: { title: '名称', key: 'name' }
    },
    {
      key: 'email',
      title: '邮箱',
      visible: true,
      column: { title: '邮箱', key: 'email' }
    },
    {
      key: 'status',
      title: '状态',
      column: { title: '状态', key: 'status' }
    },
    {
      key: 'actions',
      title: '操作',
      fixed: 'right' as const,
      column: { title: '操作', key: 'actions' }
    }
  ]
}

describe('useTablePreference', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('初始化', () => {
    it('应使用默认列配置初始化', () => {
      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)

      expect(preference.columnConfigs.value).toHaveLength(6)
      expect(preference.columnConfigs.value[0].key).toBe('selection')
      expect(preference.columnConfigs.value[0].visible).toBe(true)
      expect(preference.pageSize.value).toBe(10)
    })

    it('应正确设置默认pageSize', () => {
      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 20)

      expect(preference.pageSize.value).toBe(20)
    })

    it('初始状态应为未初始化', () => {
      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs)

      expect(preference.initialized.value).toBe(false)
      expect(preference.loading.value).toBe(false)
    })
  })

  describe('init', () => {
    it('应从后端加载偏好设置', async () => {
      const mockResponse = {
        columnsConfig: JSON.stringify([
          { key: 'selection', visible: true },
          { key: 'id', visible: true },
          { key: 'name', visible: false },
          { key: 'email', visible: true },
          { key: 'status', visible: true },
          { key: 'actions', visible: true }
        ]),
        pageSize: 25
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      expect(preference.initialized.value).toBe(true)
      expect(preference.loading.value).toBe(false)
      expect(preference.pageSize.value).toBe(25)
      expect(preference.columnConfigs.value.find(c => c.key === 'name')!.visible).toBe(false)
    })

    it('当没有保存配置时应保持默认', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 15)
      await preference.init()

      expect(preference.pageSize.value).toBe(15)
      expect(preference.columnConfigs.value.every(c => c.visible)).toBe(true)
    })

    it('加载过程中应设置loading状态', async () => {
      let resolvePromise: (value: any) => void
      const promise = new Promise(resolve => { resolvePromise = resolve })
      vi.mocked(tablePreferenceApi.getPreference).mockReturnValue(promise as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs)

      const initPromise = preference.init()
      expect(preference.loading.value).toBe(true)

      resolvePromise!({ columnsConfig: null, pageSize: 10 })
      await initPromise

      expect(preference.loading.value).toBe(false)
    })

    it('当保存的列为空时应保持默认列配置', async () => {
      const mockResponse = {
        columnsConfig: '',
        pageSize: 20
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      expect(preference.pageSize.value).toBe(20)
      expect(preference.columnConfigs.value).toHaveLength(6)
    })
  })

  describe('visibleColumns 计算属性', () => {
    it('应只返回visible为true的列', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      expect(preference.columns.value).toHaveLength(6)

      await preference.toggleColumn('name', false)

      expect(preference.columns.value).toHaveLength(5)
      expect(preference.columns.value.find((c: any) => c.key === 'name')).toBeUndefined()
    })

    it('固定列（left）应始终在最前面', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      const firstCol = preference.columns.value[0] as any
      expect(firstCol.key).toBe('selection')
      expect(firstCol.fixed).toBe('left')
    })

    it('固定列（right）应始终在最后面', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      const lastCol = preference.columns.value[preference.columns.value.length - 1] as any
      expect(lastCol.key).toBe('actions')
      expect(lastCol.fixed).toBe('right')
    })

    it('隐藏固定列后不应在结果中出现', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      await preference.toggleColumn('selection', false)

      expect(preference.columns.value.find((c: any) => c.key === 'selection')).toBeUndefined()
    })
  })

  describe('toggleColumn', () => {
    it('应切换列的可见性', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      expect(preference.columnConfigs.value.find(c => c.key === 'name')!.visible).toBe(true)

      await preference.toggleColumn('name', false)

      expect(preference.columnConfigs.value.find(c => c.key === 'name')!.visible).toBe(false)
      expect(tablePreferenceApi.saveColumnsConfig).toHaveBeenCalled()
    })

    it('切换不存在的列应不做任何操作', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      const initialLength = preference.columnConfigs.value.length
      await preference.toggleColumn('nonexistent', false)

      expect(preference.columnConfigs.value).toHaveLength(initialLength)
    })
  })

  describe('updateColumnOrder', () => {
    it('应更新列的顺序', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      const newOrder = [
        { key: 'selection', visible: true },
        { key: 'status', visible: true },
        { key: 'id', visible: true },
        { key: 'name', visible: true },
        { key: 'email', visible: true },
        { key: 'actions', visible: true }
      ]
      await preference.updateColumnOrder(newOrder)

      expect(preference.columnConfigs.value[1].key).toBe('status')
      expect(preference.columnConfigs.value[2].key).toBe('id')
      expect(tablePreferenceApi.saveColumnsConfig).toHaveBeenCalled()
    })
  })

  describe('moveColumn', () => {
    it('应在可排序列中移动列', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      await preference.moveColumn(0, 2)

      const sortableKeys = preference.sortableKeys
      expect(sortableKeys).toContain('id')
      expect(sortableKeys).toContain('name')
      expect(sortableKeys).toContain('email')
      expect(sortableKeys).toContain('status')
      expect(tablePreferenceApi.saveColumnsConfig).toHaveBeenCalled()
    })

    it('越界索引应不做任何操作', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      const configsBefore = [...preference.columnConfigs.value]
      await preference.moveColumn(-1, 2)
      await preference.moveColumn(0, 999)

      expect(preference.columnConfigs.value).toEqual(configsBefore)
    })

    it('移动后固定列应保持在原位', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      await preference.moveColumn(0, 3)

      const firstConfig = preference.columnConfigs.value[0]
      expect(firstConfig.key).toBe('selection')

      const lastConfig = preference.columnConfigs.value[preference.columnConfigs.value.length - 1]
      expect(lastConfig.key).toBe('actions')
    })
  })

  describe('savePageSize', () => {
    it('应更新分页大小并保存到后端', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))
      vi.mocked(tablePreferenceApi.savePageSize).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      await preference.savePageSize(50)

      expect(preference.pageSize.value).toBe(50)
      expect(tablePreferenceApi.savePageSize).toHaveBeenCalledWith('system/user', 50)
    })
  })

  describe('resetToDefault', () => {
    it('应重置所有列为默认配置', async () => {
      const mockResponse = {
        columnsConfig: JSON.stringify([
          { key: 'selection', visible: true },
          { key: 'id', visible: true },
          { key: 'name', visible: false },
          { key: 'email', visible: true },
          { key: 'status', visible: true },
          { key: 'actions', visible: true }
        ]),
        pageSize: 25
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)
      vi.mocked(tablePreferenceApi.resetPreference).mockResolvedValue(undefined as any)

      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)
      await preference.init()

      expect(preference.pageSize.value).toBe(25)
      expect(preference.columnConfigs.value.find(c => c.key === 'name')!.visible).toBe(false)

      await preference.resetToDefault()

      expect(preference.pageSize.value).toBe(10)
      expect(preference.columnConfigs.value.every(c => c.visible)).toBe(true)
      expect(tablePreferenceApi.resetPreference).toHaveBeenCalledWith('system/user')
    })
  })

  describe('sortableKeys', () => {
    it('应只包含非固定列的key', () => {
      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)

      expect(preference.sortableKeys).toContain('id')
      expect(preference.sortableKeys).toContain('name')
      expect(preference.sortableKeys).toContain('email')
      expect(preference.sortableKeys).toContain('status')
      expect(preference.sortableKeys).not.toContain('selection')
      expect(preference.sortableKeys).not.toContain('actions')
    })
  })

  describe('columnDefs', () => {
    it('应返回原始列定义', () => {
      const columnDefs = createColumnDefs()
      const preference = useTablePreference('system/user', columnDefs, 10)

      expect(preference.columnDefs).toBe(columnDefs)
    })
  })

  describe('不同pageKey隔离', () => {
    it('不同pageKey的偏好应互不影响', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockImplementation(async (pageKey: string) => {
        if (pageKey === 'system/user') {
          return { columnsConfig: null, pageSize: 15 }
        }
        if (pageKey === 'system/role') {
          return { columnsConfig: null, pageSize: 30 }
        }
        throw new Error('not found')
      })

      const columnDefs = createColumnDefs()
      const userPreference = useTablePreference('system/user', columnDefs, 10)
      const rolePreference = useTablePreference('system/role', columnDefs, 10)

      await userPreference.init()
      await rolePreference.init()

      expect(userPreference.pageSize.value).toBe(15)
      expect(rolePreference.pageSize.value).toBe(30)
    })
  })
})
