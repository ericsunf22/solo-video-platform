import { useEffect, useState } from 'react'
import { Heart, FileVideo } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { formatDuration, formatFileSize, formatDate } from '@/utils/format'
import { videoService } from '@/services'
import type { Video } from '@/types'

export default function Favorites() {
  const [favorites, setFavorites] = useState<Video[]>([])
  const [loading, setLoading] = useState(false)

  const loadFavorites = async () => {
    setLoading(true)
    try {
      const response = await videoService.getFavorites({ page: 1, size: 50 })
      setFavorites(response.list || [])
    } catch (error) {
      console.error('Failed to load favorites:', error)
      setFavorites([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadFavorites()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Heart className="w-8 h-8 text-red-500" />
          <h1 className="text-2xl font-bold text-gray-900">我的收藏</h1>
          <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
            {favorites.length} 个视频
          </span>
        </div>
      </div>

      {favorites.length === 0 ? (
        <div className="text-center py-16">
          <Heart className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无收藏</h3>
          <p className="text-gray-500">收藏喜欢的视频，方便以后快速找到</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {favorites.map((video) => (
            <Card key={video.id} className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group">
              <div className="relative aspect-video bg-gray-900">
                <div className="absolute inset-0 flex items-center justify-center">
                  <FileVideo className="w-16 h-16 text-gray-600" />
                </div>
                <div className="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
                  {formatDuration(video.duration)}
                </div>
                <div className="absolute top-2 right-2">
                  <Heart className="w-5 h-5 text-red-500 fill-red-500" />
                </div>
              </div>
              <CardContent className="p-4">
                <h3 className="font-medium text-gray-900 truncate mb-1">{video.title}</h3>
                <div className="flex items-center gap-4 text-xs text-gray-500">
                  <span>{formatFileSize(video.fileSize)}</span>
                  <span>{formatDate(video.createdAt)}</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
