import { create } from 'zustand'
import { Video, PageResponse } from '@/types'

export type SortBy = 'createdAt' | 'title' | 'duration' | 'fileSize'
export type SortOrder = 'asc' | 'desc'

interface VideoState {
  videos: Video[]
  currentVideo: Video | null
  loading: boolean
  pagination: PageResponse<Video> | null
  currentPage: number
  pageSize: number
  sortBy: SortBy
  sortOrder: SortOrder
  selectedVideoIds: number[]
  needRefresh: boolean

  setVideos: (videos: Video[]) => void
  setCurrentVideo: (video: Video | null) => void
  setLoading: (loading: boolean) => void
  setPagination: (pagination: PageResponse<Video> | null) => void
  setCurrentPage: (page: number) => void
  setPageSize: (size: number) => void
  setSortBy: (sortBy: SortBy) => void
  setSortOrder: (sortOrder: SortOrder) => void
  setSelectedVideoIds: (ids: number[]) => void
  toggleVideoSelection: (id: number) => void
  clearSelection: () => void
  triggerRefresh: () => void
  markRefreshed: () => void
}

export const useVideoStore = create<VideoState>()((set) => ({
  videos: [],
  currentVideo: null,
  loading: false,
  pagination: null,
  currentPage: 1,
  pageSize: 20,
  sortBy: 'title',
  sortOrder: 'asc',
  selectedVideoIds: [],
  needRefresh: false,

  setVideos: (videos) => set({ videos }),
  setCurrentVideo: (video) => set({ currentVideo: video }),
  setLoading: (loading) => set({ loading }),
  setPagination: (pagination) => set({ pagination }),
  setCurrentPage: (page) => set({ currentPage: page }),
  setPageSize: (size) => set({ pageSize: size }),
  setSortBy: (sortBy) => set({ sortBy, currentPage: 1 }),
  setSortOrder: (sortOrder) => set({ sortOrder, currentPage: 1 }),
  setSelectedVideoIds: (ids) => set({ selectedVideoIds: ids }),
  toggleVideoSelection: (id) =>
    set((state) => ({
      selectedVideoIds: state.selectedVideoIds.includes(id)
        ? state.selectedVideoIds.filter((i) => i !== id)
        : [...state.selectedVideoIds, id],
    })),
  clearSelection: () => set({ selectedVideoIds: [] }),
  triggerRefresh: () => set({ needRefresh: true, currentPage: 1 }),
  markRefreshed: () => set({ needRefresh: false }),
}))
