import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tablePreferenceApi, type TableColumnConfig } from '@/api/system'

export { type TableColumnConfig } from '@/api/system'

export interface TablePreferenceConfig {
  columns: TableColumnConfig[]
  pageSize: number
}

export const useTablePreferenceStore = defineStore('tablePreference', () => {
  const preferences = ref<Record<string, TablePreferenceConfig>>({})
  const loadingPages = ref<Set<string>>(new Set())

  function loadFromCache() {
    try {
      const saved = localStorage.getItem('mars-table-preferences')
      if (saved) {
        preferences.value = JSON.parse(saved)
      }
    } catch (e) {
      console.error('Failed to load table preferences from cache:', e)
    }
  }

  function saveToCache() {
    try {
      localStorage.setItem('mars-table-preferences', JSON.stringify(preferences.value))
    } catch (e) {
      console.error('Failed to save table preferences to cache:', e)
    }
  }

  function parseColumnsConfig(configStr: string | undefined): TableColumnConfig[] {
    if (!configStr) return []
    try {
      return JSON.parse(configStr)
    } catch (e) {
      console.error('Failed to parse columns config:', e)
      return []
    }
  }

  async function loadPreference(pageKey: string, forceRefresh = false): Promise<TablePreferenceConfig | null> {
    if (!forceRefresh && preferences.value[pageKey]) {
      return preferences.value[pageKey]
    }

    if (loadingPages.value.has(pageKey)) {
      return preferences.value[pageKey] || null
    }

    loadingPages.value.add(pageKey)
    try {
      const res = await tablePreferenceApi.getPreference(pageKey)
      const config: TablePreferenceConfig = {
        columns: parseColumnsConfig(res.columnsConfig),
        pageSize: res.pageSize || 10
      }
      preferences.value[pageKey] = config
      saveToCache()
      return config
    } catch (e) {
      console.error('Failed to load table preference:', pageKey, e)
      return preferences.value[pageKey] || null
    } finally {
      loadingPages.value.delete(pageKey)
    }
  }

  function getPreference(pageKey: string): TablePreferenceConfig | null {
    return preferences.value[pageKey] || null
  }

  async function setPreference(pageKey: string, config: TablePreferenceConfig) {
    preferences.value[pageKey] = config
    saveToCache()
    try {
      await tablePreferenceApi.savePreference(pageKey, {
        columnsConfig: JSON.stringify(config.columns),
        pageSize: config.pageSize
      })
    } catch (e) {
      console.error('Failed to save table preference:', pageKey, e)
    }
  }

  async function setColumns(pageKey: string, columns: TableColumnConfig[]) {
    if (!preferences.value[pageKey]) {
      preferences.value[pageKey] = { columns, pageSize: 10 }
    } else {
      preferences.value[pageKey].columns = columns
    }
    saveToCache()
    try {
      await tablePreferenceApi.saveColumnsConfig(pageKey, JSON.stringify(columns))
    } catch (e) {
      console.error('Failed to save table columns:', pageKey, e)
    }
  }

  async function setPageSize(pageKey: string, pageSize: number) {
    if (!preferences.value[pageKey]) {
      preferences.value[pageKey] = { columns: [], pageSize }
    } else {
      preferences.value[pageKey].pageSize = pageSize
    }
    saveToCache()
    try {
      await tablePreferenceApi.savePageSize(pageKey, pageSize)
    } catch (e) {
      console.error('Failed to save table page size:', pageKey, e)
    }
  }

  async function resetPreference(pageKey: string) {
    delete preferences.value[pageKey]
    saveToCache()
    try {
      await tablePreferenceApi.resetPreference(pageKey)
    } catch (e) {
      console.error('Failed to reset table preference:', pageKey, e)
    }
  }

  async function resetAll() {
    preferences.value = {}
    localStorage.removeItem('mars-table-preferences')
    try {
      await tablePreferenceApi.resetAllPreferences()
    } catch (e) {
      console.error('Failed to reset all table preferences:', e)
    }
  }

  loadFromCache()

  return {
    preferences,
    loadingPages,
    loadPreference,
    getPreference,
    setPreference,
    setColumns,
    setPageSize,
    resetPreference,
    resetAll
  }
}, {
  persist: false
})
