import { create } from 'zustand'
import { Video, PageResponse } from '@/types'

interface VideoState {
  videos: Video[]
  currentVideo: Video | null
  loading: boolean
  pagination: PageResponse<Video> | null
  selectedVideoIds: number[]
  needRefresh: boolean

  setVideos: (videos: Video[]) => void
  setCurrentVideo: (video: Video | null) => void
  setLoading: (loading: boolean) => void
  setPagination: (pagination: PageResponse<Video> | null) => void
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
  selectedVideoIds: [],
  needRefresh: false,

  setVideos: (videos) => set({ videos }),
  setCurrentVideo: (video) => set({ currentVideo: video }),
  setLoading: (loading) => set({ loading }),
  setPagination: (pagination) => set({ pagination }),
  setSelectedVideoIds: (ids) => set({ selectedVideoIds: ids }),
  toggleVideoSelection: (id) =>
    set((state) => ({
      selectedVideoIds: state.selectedVideoIds.includes(id)
        ? state.selectedVideoIds.filter((i) => i !== id)
        : [...state.selectedVideoIds, id],
    })),
  clearSelection: () => set({ selectedVideoIds: [] }),
  triggerRefresh: () => set({ needRefresh: true }),
  markRefreshed: () => set({ needRefresh: false }),
}))
