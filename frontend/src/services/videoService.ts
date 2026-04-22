import api from './api'
import { Video, PageResponse, ApiResponse, VideoUploadRequest, VideoUpdateRequest, ScanResultResponse, FolderScanRequest, PlayHistory, PlayProgressRequest } from '@/types'

export const videoService = {
  getVideos: async (params: {
    page?: number
    size?: number
    keyword?: string
    tagIds?: number[]
    isFavorite?: boolean
    sortBy?: string
    sortOrder?: string
  } = {}): Promise<PageResponse<Video>> => {
    const response = await api.get<ApiResponse<{ content: Video[]; totalElements: number; number: number; size: number; totalPages: number }>>('/videos', { params })
    const data = response.data.data
    return {
      list: data.content,
      total: data.totalElements,
      page: data.number + 1,
      size: data.size,
      totalPages: data.totalPages,
    }
  },

  getVideoById: async (id: number): Promise<Video> => {
    const response = await api.get<ApiResponse<Video>>(`/videos/${id}`)
    return response.data.data
  },

  uploadVideo: async (request: VideoUploadRequest): Promise<Video> => {
    const formData = new FormData()
    formData.append('file', request.file)
    if (request.title) formData.append('title', request.title)
    if (request.description) formData.append('description', request.description)

    const response = await api.post<ApiResponse<Video>>('/videos/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data.data
  },

  uploadVideos: async (files: File[]): Promise<Video[]> => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))

    const response = await api.post<ApiResponse<Video[]>>('/videos/upload/batch', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data.data
  },

  updateVideo: async (id: number, request: VideoUpdateRequest): Promise<Video> => {
    const response = await api.put<ApiResponse<Video>>(`/videos/${id}`, request)
    return response.data.data
  },

  deleteVideo: async (id: number): Promise<void> => {
    await api.delete(`/videos/${id}`)
  },

  deleteVideos: async (ids: number[]): Promise<void> => {
    await api.delete('/videos/batch', { data: { ids } })
  },

  scanFolder: async (request: FolderScanRequest): Promise<ScanResultResponse> => {
    const response = await api.post<ApiResponse<ScanResultResponse>>('/scan/folder', request)
    return response.data.data
  },

  getScanProgress: async (): Promise<{
    progress: number
    isScanning: boolean
    currentScanningFile: string
    newVideos: number
    updatedVideos: number
    skippedVideos: number
  }> => {
    const response = await api.get<ApiResponse<{
      progress: number
      isScanning: boolean
      currentScanningFile: string
      newVideos: number
      updatedVideos: number
      skippedVideos: number
    }>>('/scan/progress')
    return response.data.data
  },

  cancelScan: async (): Promise<void> => {
    await api.post('/scan/cancel')
  },

  getVideoCover: (id: number): string => {
    return `/api/videos/${id}/cover`
  },

  getVideoStream: (id: number): string => {
    return `/api/stream/${id}`
  },

  getFavorites: async (params: {
    page?: number
    size?: number
    keyword?: string
    sortBy?: string
    sortOrder?: string
  } = {}): Promise<PageResponse<Video>> => {
    const response = await api.get<ApiResponse<{ content: Video[]; totalElements: number; number: number; size: number; totalPages: number }>>('/favorites', { params })
    const data = response.data.data
    return {
      list: data.content,
      total: data.totalElements,
      page: data.number + 1,
      size: data.size,
      totalPages: data.totalPages,
    }
  },

  toggleFavorite: async (videoId: number): Promise<boolean> => {
    const response = await api.post<ApiResponse<{ isFavorite: boolean }>>(`/favorites/toggle/${videoId}`)
    return response.data.data.isFavorite
  },

  addToFavorites: async (videoIds: number[]): Promise<void> => {
    await api.post('/favorites/batch', { videoIds })
  },

  removeFromFavorites: async (videoIds: number[]): Promise<void> => {
    await api.delete('/favorites/batch', { data: { videoIds } })
  },

  getPlayHistory: async (params: {
    page?: number
    size?: number
  } = {}): Promise<PageResponse<PlayHistory>> => {
    const response = await api.get<ApiResponse<{ content: PlayHistory[]; totalElements: number; number: number; size: number; totalPages: number }>>('/player/history', { params })
    const data = response.data.data
    return {
      list: data.content,
      total: data.totalElements,
      page: data.number + 1,
      size: data.size,
      totalPages: data.totalPages,
    }
  },

  savePlayProgress: async (request: PlayProgressRequest): Promise<void> => {
    await api.post('/player/progress', request)
  },

  getPlayProgress: async (videoId: number): Promise<PlayHistory | null> => {
    try {
      const response = await api.get<ApiResponse<PlayHistory>>(`/player/progress/${videoId}`)
      return response.data.data
    } catch (_error) {
      return null
    }
  },

  incrementPlayCount: async (videoId: number): Promise<void> => {
    await api.post(`/player/play/${videoId}`)
  },

  clearPlayHistory: async (): Promise<void> => {
    await api.delete('/player/history')
  },

  getAllSettings: async (): Promise<Map<string, string>> => {
    const response = await api.get<ApiResponse<Map<string, string>>>('/settings')
    return response.data.data
  },

  getSetting: async (key: string, defaultValue?: string): Promise<string> => {
    const params: Record<string, string> = {}
    if (defaultValue !== undefined) {
      params.defaultValue = defaultValue
    }
    const response = await api.get<ApiResponse<string>>(`/settings/${key}`, { params })
    return response.data.data
  },

  setSetting: async (key: string, value: string, description?: string): Promise<void> => {
    const body: Record<string, string> = { value }
    if (description) {
      body.description = description
    }
    await api.put(`/settings/${key}`, body)
  },

  deleteSetting: async (key: string): Promise<void> => {
    await api.delete(`/settings/${key}`)
  },
}
