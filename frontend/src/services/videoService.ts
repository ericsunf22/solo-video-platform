import api from './api'
import { Video, PageResponse, ApiResponse, VideoUploadRequest, VideoUpdateRequest, ScanResultResponse, FolderScanRequest } from '@/types'

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
    const response = await api.get<ApiResponse<PageResponse<Video>>>('/videos', { params })
    return response.data.data
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

  getScanProgress: async (): Promise<{ progress: number; status: string }> => {
    const response = await api.get<ApiResponse<{ progress: number; status: string }>>('/scan/progress')
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
}
