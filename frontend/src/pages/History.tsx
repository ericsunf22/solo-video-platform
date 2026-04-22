import { useEffect, useState } from 'react'
import { Clock, FileVideo, Trash2 } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { formatDuration, formatDate } from '@/utils/format'
import { videoService } from '@/services'
import { useToastStore } from '@/store'
import type { PlayHistory } from '@/types'

export default function History() {
  const [historyList, setHistoryList] = useState<PlayHistory[]>([])
  const [loading, setLoading] = useState(false)
  const { error, success } = useToastStore()

  const loadHistory = async () => {
    setLoading(true)
    try {
      const response = await videoService.getPlayHistory({ page: 1, size: 50 })
      setHistoryList(response.list || [])
    } catch (_err) {
      console.error('Failed to load play history:', _err)
      setHistoryList([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadHistory()
  }, [])

  const handleClearHistory = async () => {
    if (!confirm('确定要清空所有播放历史吗？')) return
    try {
      await videoService.clearPlayHistory()
      setHistoryList([])
      success('播放历史已清空')
    } catch (err) {
      console.error('Failed to clear play history:', err)
      error('清空播放历史失败')
    }
  }

  const getProgressPercent = (progress: number, duration: number | null) => {
    if (!duration || duration === 0) return 0
    return (progress / duration) * 100
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
        <div className="flex items-center gap-3">
          <Clock className="w-8 h-8 text-blue-500" />
          <h1 className="text-2xl font-bold text-gray-900">播放历史</h1>
          <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
            {historyList.length} 条记录
          </span>
        </div>
        {historyList.length > 0 && (
          <Button variant="destructive" size="sm" onClick={handleClearHistory}>
            <Trash2 className="w-4 h-4 mr-2" />
            清空历史
          </Button>
        )}
      </div>

      {historyList.length === 0 ? (
        <div className="text-center py-16">
          <Clock className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无播放记录</h3>
          <p className="text-gray-500">开始播放视频，这里会记录你的观看历史</p>
        </div>
      ) : (
        <div className="space-y-4">
          {historyList.map((history) => (
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
