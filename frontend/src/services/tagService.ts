import api from './api'
import { Tag, ApiResponse, TagCreateRequest } from '@/types'

export const tagService = {
  getTags: async (params: {
    keyword?: string
    sortBy?: string
    sortOrder?: string
  } = {}): Promise<Tag[]> => {
    const response = await api.get<ApiResponse<Tag[]>>('/tags', { params })
    return response.data.data
  },

  createTag: async (request: TagCreateRequest): Promise<Tag> => {
    const response = await api.post<ApiResponse<Tag>>('/tags', request)
    return response.data.data
  },

  updateTag: async (id: number, request: Partial<TagCreateRequest>): Promise<Tag> => {
    const response = await api.put<ApiResponse<Tag>>(`/tags/${id}`, request)
    return response.data.data
  },

  deleteTag: async (id: number): Promise<void> => {
    await api.delete(`/tags/${id}`)
  },

  addTagsToVideo: async (videoId: number, tagIds: number[]): Promise<void> => {
    await api.post(`/tags/video/${videoId}`, { tagIds })
  },

  removeTagFromVideo: async (videoId: number, tagId: number): Promise<void> => {
    await api.delete(`/tags/video/${videoId}/${tagId}`)
  },

  addTagsToVideos: async (videoIds: number[], tagIds: number[]): Promise<void> => {
    await api.post('/tags/videos/batch', { videoIds, tagIds })
  },
}
