import { useEffect, useCallback } from 'react'
import { Grid, List, Eye, Clock, FileVideo } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useVideoStore, useUIStore } from '@/store'
import { videoService } from '@/services'
import { formatDuration, formatFileSize, formatDate } from '@/utils/format'
import type { Video } from '@/types'

function Play({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <polygon points="6 3 20 12 6 21 6 3" />
    </svg>
  )
}

interface VideoCardProps {
  video: Video
  onClick?: () => void
}

const VideoCard = ({ video, onClick }: VideoCardProps) => (
  <Card 
    className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group"
    onClick={onClick}
    role="button"
    tabIndex={0}
    aria-label={`播放视频: ${video.title}`}
  >
    <div className="relative aspect-video bg-gray-900">
      <div className="absolute inset-0 flex items-center justify-center">
        <FileVideo className="w-16 h-16 text-gray-600" aria-hidden="true" />
      </div>
      <div className="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
        {formatDuration(video.duration)}
      </div>
      <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
        <Play className="w-16 h-16 text-white" aria-hidden="true" />
      </div>
    </div>
    <CardContent className="p-4">
      <h3 className="font-medium text-gray-900 truncate mb-1">{video.title}</h3>
      <div className="flex items-center gap-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <Eye className="w-3 h-3" aria-hidden="true" />
          {formatFileSize(video.fileSize)}
        </span>
        <span className="flex items-center gap-1">
          <Clock className="w-3 h-3" aria-hidden="true" />
          {formatDate(video.createdAt)}
        </span>
      </div>
    </CardContent>
  </Card>
)

interface VideoRowProps {
  video: Video
  onClick?: () => void
}

const VideoRow = ({ video, onClick }: VideoRowProps) => (
  <div 
    className="flex items-center gap-4 p-4 bg-white rounded-lg border hover:border-blue-300 transition-colors cursor-pointer"
    onClick={onClick}
    role="button"
    tabIndex={0}
    aria-label={`播放视频: ${video.title}`}
  >
    <div className="relative w-40 h-24 bg-gray-900 rounded overflow-hidden flex-shrink-0">
      <div className="absolute inset-0 flex items-center justify-center">
        <FileVideo className="w-8 h-8 text-gray-600" aria-hidden="true" />
      </div>
      <div className="absolute bottom-1 right-1 bg-black/70 text-white text-xs px-1 py-0.5 rounded">
        {formatDuration(video.duration)}
      </div>
    </div>
    <div className="flex-1 min-w-0">
      <h3 className="font-medium text-gray-900 truncate">{video.title}</h3>
      <p className="text-sm text-gray-500 truncate mt-1">{video.description}</p>
      <div className="flex items-center gap-4 mt-2 text-xs text-gray-400">
        <span>{formatFileSize(video.fileSize)}</span>
        <span>{video.resolution}</span>
        <span>{formatDate(video.createdAt)}</span>
      </div>
    </div>
  </div>
)

export default function Home() {
  const { videos = [], loading, setVideos, setLoading, setPagination, needRefresh, markRefreshed } = useVideoStore()
  const { viewMode, setViewMode } = useUIStore()

  const loadVideos = useCallback(async () => {
    setLoading(true)
    try {
      const response = await videoService.getVideos({ page: 1, size: 20 })
      setVideos(response.list)
      setPagination(response)
    } catch (error) {
      console.error('Failed to load videos:', error)
      setVideos([])
    } finally {
      setLoading(false)
    }
  }, [setVideos, setLoading, setPagination])

  useEffect(() => {
    loadVideos()
  }, [loadVideos])

  useEffect(() => {
    if (needRefresh) {
      loadVideos()
      markRefreshed()
    }
  }, [needRefresh, loadVideos, markRefreshed])

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
        <h1 className="text-2xl font-bold text-gray-900">视频库</h1>
        <div className="flex items-center gap-2">
          <Button
            variant={viewMode === 'grid' ? 'default' : 'outline'}
            size="icon"
            onClick={() => setViewMode('grid')}
            aria-label="网格视图"
          >
            <Grid className="w-4 h-4" aria-hidden="true" />
          </Button>
          <Button
            variant={viewMode === 'list' ? 'default' : 'outline'}
            size="icon"
            onClick={() => setViewMode('list')}
            aria-label="列表视图"
          >
            <List className="w-4 h-4" aria-hidden="true" />
          </Button>
        </div>
      </div>

      {videos.length === 0 ? (
        <div className="text-center py-16">
          <FileVideo className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无视频</h3>
          <p className="text-gray-500">上传视频或扫描本地文件夹开始使用</p>
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {videos.map((video) => (
            <VideoCard key={video.id} video={video} />
          ))}
        </div>
      ) : (
        <div className="space-y-3">
          {videos.map((video) => (
            <VideoRow key={video.id} video={video} />
          ))}
        </div>
      )}
    </div>
  )
}
