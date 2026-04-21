import api from './api'
import { Video, PageResponse, ApiResponse } from '@/types'

export const favoriteService = {
  toggleFavorite: async (videoId: number): Promise<{ isFavorite: boolean }> => {
    const response = await api.post<ApiResponse<{ isFavorite: boolean }>>(`/favorites/toggle/${videoId}`)
    return response.data.data
  },

  getFavorites: async (params: {
    page?: number
    size?: number
    keyword?: string
    sortBy?: string
    sortOrder?: string
  } = {}): Promise<PageResponse<Video>> => {
    const response = await api.get<ApiResponse<PageResponse<Video>>>('/favorites', { params })
    return response.data.data
  },

  addFavorites: async (videoIds: number[]): Promise<void> => {
    await api.post('/favorites/batch', { videoIds })
  },

  removeFavorites: async (videoIds: number[]): Promise<void> => {
    await api.delete('/favorites/batch', { data: { videoIds } })
  },
}
