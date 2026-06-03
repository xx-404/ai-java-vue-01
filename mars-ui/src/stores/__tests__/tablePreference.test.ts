import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTablePreferenceStore, type TablePreferenceConfig } from '@/stores/tablePreference'
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

describe('useTablePreferenceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('loadPreference', () => {
    it('应从后端加载偏好设置并缓存', async () => {
      const mockResponse = {
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 20
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/user')

      expect(result).not.toBeNull()
      expect(result!.columns).toEqual([{ key: 'id', visible: true }])
      expect(result!.pageSize).toBe(20)
      expect(tablePreferenceApi.getPreference).toHaveBeenCalledWith('system/user')
    })

    it('应从缓存返回已加载的偏好设置', async () => {
      const mockResponse = {
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')

      const result = await store.loadPreference('system/user')

      expect(tablePreferenceApi.getPreference).toHaveBeenCalledTimes(1)
      expect(result!.pageSize).toBe(10)
    })

    it('强制刷新时应跳过缓存', async () => {
      const mockResponse1 = { columnsConfig: '[{"key":"id","visible":true}]', pageSize: 10 }
      const mockResponse2 = { columnsConfig: '[{"key":"name","visible":false}]', pageSize: 20 }
      vi.mocked(tablePreferenceApi.getPreference)
        .mockResolvedValueOnce(mockResponse1 as any)
        .mockResolvedValueOnce(mockResponse2 as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')
      const result = await store.loadPreference('system/user', true)

      expect(tablePreferenceApi.getPreference).toHaveBeenCalledTimes(2)
      expect(result!.pageSize).toBe(20)
    })

    it('当后端返回空配置时应返回null', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockRejectedValue(new Error('not found'))

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('nonexistent/page')

      expect(result).toBeNull()
    })

    it('应正确解析列配置JSON', async () => {
      const columns = [
        { key: 'id', visible: true },
        { key: 'name', visible: false },
        { key: 'email', visible: true, width: 200 }
      ]
      const mockResponse = {
        columnsConfig: JSON.stringify(columns),
        pageSize: 15
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/role')

      expect(result!.columns).toEqual(columns)
      expect(result!.pageSize).toBe(15)
    })

    it('当columnsConfig为空字符串时应返回空列数组', async () => {
      const mockResponse = { columnsConfig: '', pageSize: 10 }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/user')

      expect(result!.columns).toEqual([])
    })

    it('当columnsConfig为无效JSON时应返回空列数组', async () => {
      const mockResponse = { columnsConfig: 'invalid json', pageSize: 10 }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/user')

      expect(result!.columns).toEqual([])
    })
  })

  describe('getPreference', () => {
    it('应从内存中获取偏好设置', async () => {
      const mockResponse = {
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      }
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue(mockResponse as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')

      const result = store.getPreference('system/user')
      expect(result).not.toBeNull()
      expect(result!.pageSize).toBe(10)
    })

    it('未加载的页面应返回null', () => {
      const store = useTablePreferenceStore()
      const result = store.getPreference('nonexistent')
      expect(result).toBeNull()
    })
  })

  describe('setPreference', () => {
    it('应保存偏好设置到后端和缓存', async () => {
      vi.mocked(tablePreferenceApi.savePreference).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      const config: TablePreferenceConfig = {
        columns: [{ key: 'id', visible: true }],
        pageSize: 25
      }
      await store.setPreference('system/user', config)

      expect(tablePreferenceApi.savePreference).toHaveBeenCalledWith('system/user', {
        columnsConfig: JSON.stringify(config.columns),
        pageSize: 25
      })
      expect(store.getPreference('system/user')).not.toBeNull()
      expect(store.getPreference('system/user')!.pageSize).toBe(25)
    })

    it('保存后应更新localStorage缓存', async () => {
      vi.mocked(tablePreferenceApi.savePreference).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      await store.setPreference('system/user', {
        columns: [{ key: 'id', visible: true }],
        pageSize: 15
      })

      const cached = localStorage.getItem('mars-table-preferences')
      expect(cached).not.toBeNull()
      const parsed = JSON.parse(cached!)
      expect(parsed['system/user']).toBeDefined()
      expect(parsed['system/user'].pageSize).toBe(15)
    })

    it('后端保存失败时仍应保留本地缓存', async () => {
      vi.mocked(tablePreferenceApi.savePreference).mockRejectedValue(new Error('network error'))

      const store = useTablePreferenceStore()
      await store.setPreference('system/user', {
        columns: [{ key: 'id', visible: true }],
        pageSize: 15
      })

      expect(store.getPreference('system/user')).not.toBeNull()
    })
  })

  describe('setColumns', () => {
    it('应保存列配置到后端', async () => {
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      const columns = [
        { key: 'id', visible: true },
        { key: 'name', visible: false }
      ]
      await store.setColumns('system/user', columns)

      expect(tablePreferenceApi.saveColumnsConfig).toHaveBeenCalledWith(
        'system/user',
        JSON.stringify(columns)
      )
    })

    it('当偏好设置不存在时应创建新的', async () => {
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      const columns = [{ key: 'id', visible: true }]
      await store.setColumns('system/user', columns)

      const result = store.getPreference('system/user')
      expect(result).not.toBeNull()
      expect(result!.columns).toEqual(columns)
      expect(result!.pageSize).toBe(10)
    })

    it('当偏好设置已存在时应仅更新列配置', async () => {
      vi.mocked(tablePreferenceApi.saveColumnsConfig).mockResolvedValue(undefined as any)
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 20
      } as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')

      const newColumns = [{ key: 'name', visible: true }]
      await store.setColumns('system/user', newColumns)

      const result = store.getPreference('system/user')
      expect(result!.columns).toEqual(newColumns)
      expect(result!.pageSize).toBe(20)
    })
  })

  describe('setPageSize', () => {
    it('应保存分页大小到后端', async () => {
      vi.mocked(tablePreferenceApi.savePageSize).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      await store.setPageSize('system/user', 50)

      expect(tablePreferenceApi.savePageSize).toHaveBeenCalledWith('system/user', 50)
    })

    it('当偏好设置不存在时应创建新的', async () => {
      vi.mocked(tablePreferenceApi.savePageSize).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      await store.setPageSize('system/user', 30)

      const result = store.getPreference('system/user')
      expect(result).not.toBeNull()
      expect(result!.pageSize).toBe(30)
      expect(result!.columns).toEqual([])
    })

    it('当偏好设置已存在时应仅更新分页大小', async () => {
      vi.mocked(tablePreferenceApi.savePageSize).mockResolvedValue(undefined as any)
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      } as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')

      await store.setPageSize('system/user', 50)

      const result = store.getPreference('system/user')
      expect(result!.pageSize).toBe(50)
      expect(result!.columns).toEqual([{ key: 'id', visible: true }])
    })
  })

  describe('resetPreference', () => {
    it('应删除本地缓存并调用后端重置', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      } as any)
      vi.mocked(tablePreferenceApi.resetPreference).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')
      expect(store.getPreference('system/user')).not.toBeNull()

      await store.resetPreference('system/user')

      expect(store.getPreference('system/user')).toBeNull()
      expect(tablePreferenceApi.resetPreference).toHaveBeenCalledWith('system/user')
    })

    it('后端重置失败时仍应清除本地缓存', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      } as any)
      vi.mocked(tablePreferenceApi.resetPreference).mockRejectedValue(new Error('network'))

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')

      await store.resetPreference('system/user')

      expect(store.getPreference('system/user')).toBeNull()
    })
  })

  describe('resetAll', () => {
    it('应清除所有本地缓存并调用后端重置', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      } as any)
      vi.mocked(tablePreferenceApi.resetAllPreferences).mockResolvedValue(undefined as any)

      const store = useTablePreferenceStore()
      await store.loadPreference('system/user')
      await store.loadPreference('system/role')

      await store.resetAll()

      expect(store.getPreference('system/user')).toBeNull()
      expect(store.getPreference('system/role')).toBeNull()
      expect(tablePreferenceApi.resetAllPreferences).toHaveBeenCalled()
      expect(localStorage.getItem('mars-table-preferences')).toBeNull()
    })
  })

  describe('多页面隔离', () => {
    it('不同页面的偏好设置应互不影响', async () => {
      vi.mocked(tablePreferenceApi.getPreference).mockImplementation(async (pageKey: string) => {
        if (pageKey === 'system/user') {
          return { columnsConfig: '[{"key":"id","visible":true}]', pageSize: 10 }
        }
        if (pageKey === 'system/role') {
          return { columnsConfig: '[{"key":"name","visible":false}]', pageSize: 20 }
        }
        throw new Error('not found')
      })

      const store = useTablePreferenceStore()
      const userResult = await store.loadPreference('system/user')
      const roleResult = await store.loadPreference('system/role')

      expect(userResult!.pageSize).toBe(10)
      expect(roleResult!.pageSize).toBe(20)
      expect(userResult!.columns[0].key).toBe('id')
      expect(roleResult!.columns[0].key).toBe('name')
    })
  })

  describe('localStorage 缓存', () => {
    it('初始化时应从localStorage加载缓存', async () => {
      const cached = {
        'system/user': {
          columns: [{ key: 'id', visible: true }],
          pageSize: 25
        }
      }
      localStorage.setItem('mars-table-preferences', JSON.stringify(cached))

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/user')

      expect(result!.pageSize).toBe(25)
      expect(tablePreferenceApi.getPreference).not.toHaveBeenCalled()
    })

    it('localStorage缓存损坏时应优雅处理', async () => {
      localStorage.setItem('mars-table-preferences', 'invalid json')
      vi.mocked(tablePreferenceApi.getPreference).mockResolvedValue({
        columnsConfig: '[{"key":"id","visible":true}]',
        pageSize: 10
      } as any)

      const store = useTablePreferenceStore()
      const result = await store.loadPreference('system/user')

      expect(result!.pageSize).toBe(10)
    })
  })
})
