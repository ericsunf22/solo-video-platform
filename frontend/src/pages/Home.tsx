import { useEffect } from 'react'
import { Grid, List, Eye, Clock, FileVideo } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useVideoStore, useUIStore } from '@/store'
import { videoService } from '@/services'
import { formatDuration, formatFileSize, formatDate } from '@/utils/format'
import type { Video } from '@/types'

export default function Home() {
  const { videos, loading, setVideos, setLoading, setPagination } = useVideoStore()
  const { viewMode, setViewMode } = useUIStore()

  useEffect(() => {
    loadVideos()
  }, [])

  const loadVideos = async () => {
    setLoading(true)
    try {
      const response = await videoService.getVideos({ page: 1, size: 20 })
      setVideos(response.list)
      setPagination(response)
    } catch (error) {
      console.error('Failed to load videos:', error)
      setVideos(mockVideos)
    } finally {
      setLoading(false)
    }
  }

  const mockVideos: Video[] = [
    {
      id: 1,
      title: 'React 18 新特性详解',
      description: '深入讲解 React 18 的新特性',
      filePath: '/storage/videos/react18.mp4',
      fileName: 'react18.mp4',
      fileSize: 1024 * 1024 * 256,
      duration: 3600,
      format: 'mp4',
      resolution: '1920x1080',
      coverPath: null,
      sourceType: 'UPLOADED',
      isFavorite: true,
      createdAt: '2024-01-15T10:30:00Z',
      updatedAt: '2024-01-15T10:30:00Z',
      tags: [],
    },
    {
      id: 2,
      title: 'TypeScript 高级类型技巧',
      description: 'TypeScript 类型系统深度解析',
      filePath: '/storage/videos/typescript.mp4',
      fileName: 'typescript.mp4',
      fileSize: 1024 * 1024 * 512,
      duration: 7200,
      format: 'mp4',
      resolution: '1920x1080',
      coverPath: null,
      sourceType: 'SCANNED',
      isFavorite: false,
      createdAt: '2024-01-14T15:20:00Z',
      updatedAt: '2024-01-14T15:20:00Z',
      tags: [],
    },
    {
      id: 3,
      title: 'Node.js 性能优化实战',
      description: 'Node.js 应用性能调优指南',
      filePath: '/storage/videos/nodejs.mp4',
      fileName: 'nodejs.mp4',
      fileSize: 1024 * 1024 * 384,
      duration: 5400,
      format: 'avi',
      resolution: '1280x720',
      coverPath: null,
      sourceType: 'UPLOADED',
      isFavorite: true,
      createdAt: '2024-01-13T09:00:00Z',
      updatedAt: '2024-01-13T09:00:00Z',
      tags: [],
    },
  ]

  const VideoCard = ({ video }: { video: Video }) => (
    <Card className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group">
      <div className="relative aspect-video bg-gray-900">
        <div className="absolute inset-0 flex items-center justify-center">
          <FileVideo className="w-16 h-16 text-gray-600" />
        </div>
        <div className="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
          {formatDuration(video.duration)}
        </div>
        <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
          <Play className="w-16 h-16 text-white" />
        </div>
      </div>
      <CardContent className="p-4">
        <h3 className="font-medium text-gray-900 truncate mb-1">{video.title}</h3>
        <div className="flex items-center gap-4 text-xs text-gray-500">
          <span className="flex items-center gap-1">
            <Eye className="w-3 h-3" />
            {formatFileSize(video.fileSize)}
          </span>
          <span className="flex items-center gap-1">
            <Clock className="w-3 h-3" />
            {formatDate(video.createdAt)}
          </span>
        </div>
      </CardContent>
    </Card>
  )

  const VideoRow = ({ video }: { video: Video }) => (
    <div className="flex items-center gap-4 p-4 bg-white rounded-lg border hover:border-blue-300 transition-colors cursor-pointer">
      <div className="relative w-40 h-24 bg-gray-900 rounded overflow-hidden flex-shrink-0">
        <div className="absolute inset-0 flex items-center justify-center">
          <FileVideo className="w-8 h-8 text-gray-600" />
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
          >
            <Grid className="w-4 h-4" />
          </Button>
          <Button
            variant={viewMode === 'list' ? 'default' : 'outline'}
            size="icon"
            onClick={() => setViewMode('list')}
          >
            <List className="w-4 h-4" />
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
