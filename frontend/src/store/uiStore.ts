import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface UIState {
  viewMode: 'grid' | 'list'
  sidebarOpen: boolean
  searchKeyword: string
  recentSearches: string[]

  setViewMode: (mode: 'grid' | 'list') => void
  setSidebarOpen: (open: boolean) => void
  setSearchKeyword: (keyword: string) => void
  addRecentSearch: (keyword: string) => void
  clearRecentSearches: () => void
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      viewMode: 'grid',
      sidebarOpen: true,
      searchKeyword: '',
      recentSearches: [],

      setViewMode: (mode) => set({ viewMode: mode }),
      setSidebarOpen: (open) => set({ sidebarOpen: open }),
      setSearchKeyword: (keyword) => set({ searchKeyword: keyword }),
      addRecentSearch: (keyword) =>
        set((state) => ({
          recentSearches: [keyword, ...state.recentSearches.filter((s) => s !== keyword)].slice(0, 10),
        })),
      clearRecentSearches: () => set({ recentSearches: [] }),
    }),
    {
      name: 'ui-storage',
    }
  )
)
