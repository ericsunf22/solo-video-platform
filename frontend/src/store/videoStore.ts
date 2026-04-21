import { create } from 'zustand'
import { Video, PageResponse } from '@/types'

interface VideoState {
  videos: Video[]
  currentVideo: Video | null
  loading: boolean
  pagination: PageResponse<Video> | null
  selectedVideoIds: number[]

  setVideos: (videos: Video[]) => void
  setCurrentVideo: (video: Video | null) => void
  setLoading: (loading: boolean) => void
  setPagination: (pagination: PageResponse<Video> | null) => void
  setSelectedVideoIds: (ids: number[]) => void
  toggleVideoSelection: (id: number) => void
  clearSelection: () => void
}

export const useVideoStore = create<VideoState>()((set) => ({
  videos: [],
  currentVideo: null,
  loading: false,
  pagination: null,
  selectedVideoIds: [],

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
}))
