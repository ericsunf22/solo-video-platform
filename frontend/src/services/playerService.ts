import api from './api'
import { PlayHistory, PageResponse, ApiResponse, PlayProgressRequest } from '@/types'

export const playerService = {
  saveProgress: async (request: PlayProgressRequest): Promise<void> => {
    await api.post('/player/progress', request)
  },

  getProgress: async (videoId: number): Promise<{ videoId: number; progress: number; lastPlayedAt: string }> => {
    const response = await api.get<ApiResponse<{ videoId: number; progress: number; lastPlayedAt: string }>>(
      `/player/progress/${videoId}`
    )
    return response.data.data
  },

  getHistory: async (params: { page?: number; size?: number } = {}): Promise<PageResponse<PlayHistory>> => {
    const response = await api.get<ApiResponse<PageResponse<PlayHistory>>>('/player/history', { params })
    return response.data.data
  },

  clearHistory: async (): Promise<void> => {
    await api.delete('/player/history')
  },
}
