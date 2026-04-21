import { Clock, FileVideo } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { formatDuration, formatDate } from '@/utils/format'
import type { PlayHistory } from '@/types'

export default function History() {
  const mockHistory: PlayHistory[] = [
    {
      id: 1,
      videoId: 1,
      video: {
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
      progress: 1800,
      playCount: 3,
      totalPlayTime: 7200,
      lastPlayedAt: '2024-01-18T14:30:00Z',
      createdAt: '2024-01-15T10:30:00Z',
      updatedAt: '2024-01-18T14:30:00Z',
    },
    {
      id: 2,
      videoId: 2,
      video: {
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
      progress: 3600,
      playCount: 2,
      totalPlayTime: 3600,
      lastPlayedAt: '2024-01-17T10:00:00Z',
      createdAt: '2024-01-14T15:20:00Z',
      updatedAt: '2024-01-17T10:00:00Z',
    },
  ]

  const getProgressPercent = (progress: number, duration: number | null) => {
    if (!duration || duration === 0) return 0
    return (progress / duration) * 100
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Clock className="w-8 h-8 text-blue-500" />
          <h1 className="text-2xl font-bold text-gray-900">播放历史</h1>
        </div>
      </div>

      {mockHistory.length === 0 ? (
        <div className="text-center py-16">
          <Clock className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无播放记录</h3>
          <p className="text-gray-500">开始播放视频，这里会记录你的观看历史</p>
        </div>
      ) : (
        <div className="space-y-4">
          {mockHistory.map((history) => (
            <Card key={history.id} className="overflow-hidden cursor-pointer hover:shadow-md transition-shadow">
              <div className="flex items-center gap-4 p-4">
                <div className="relative w-48 h-28 bg-gray-900 rounded-lg overflow-hidden flex-shrink-0">
                  <div className="absolute inset-0 flex items-center justify-center">
                    <FileVideo className="w-10 h-10 text-gray-600" />
                  </div>
                  <div className="absolute bottom-0 left-0 right-0 h-1 bg-gray-700">
                    <div
                      className="h-full bg-blue-500"
                      style={{
                        width: `${getProgressPercent(history.progress, history.video.duration)}%`,
                      }}
                    />
                  </div>
                  <div className="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-2 py-1 rounded">
                    {formatDuration(history.progress)} / {formatDuration(history.video.duration)}
                  </div>
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium text-gray-900 truncate">{history.video.title}</h3>
                  <p className="text-sm text-gray-500 truncate mt-1">{history.video.description}</p>
                  <div className="flex items-center gap-4 mt-2 text-sm text-gray-400">
                    <span>播放 {history.playCount} 次</span>
                    <span>总观看 {formatDuration(history.totalPlayTime)}</span>
                    <span>最后观看 {formatDate(history.lastPlayedAt)}</span>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
