import { useEffect, useState, ChangeEvent, MouseEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Heart } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { usePlayerStore } from '@/store/playerStore'
import { formatDuration } from '@/utils/format'
import { cn } from '@/utils/cn'
import { videoService } from '@/services'
import type { Video } from '@/types'

export default function Player() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [video, setVideo] = useState<Video | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const {
    volume,
    isMuted,
    playbackRate,
    setIsPlaying,
    setCurrentTime,
    setDuration,
    setVolume,
    setIsMuted,
    setPlaybackRate,
    setCurrentVideo,
  } = usePlayerStore()

  const loadVideo = async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    try {
      const videoData = await videoService.getVideoById(Number(id))
      setVideo(videoData)
      setCurrentVideo(videoData)
      if (videoData.duration) {
        setCurrentTime(0)
      }
      
      await videoService.incrementPlayCount(Number(id))
    } catch (err) {
      console.error('Failed to load video:', err)
      setError('视频加载失败，请检查视频是否存在')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadVideo()
    return () => {
      setCurrentVideo(null)
    }
  }, [id])

  const toggleFavorite = async () => {
    if (!video) return
    try {
      const newIsFavorite = await videoService.toggleFavorite(video.id)
      setVideo({ ...video, isFavorite: newIsFavorite })
    } catch (err) {
      console.error('Failed to toggle favorite:', err)
    }
  }

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto">
        <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          返回
        </Button>
        <div className="flex items-center justify-center h-96">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </div>
    )
  }

  if (error || !video) {
    return (
      <div className="max-w-6xl mx-auto">
        <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          返回
        </Button>
        <div className="text-center py-16">
          <h3 className="text-lg font-medium text-gray-900 mb-2">{error || '视频不存在'}</h3>
          <p className="text-gray-500">请检查视频是否已被删除</p>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto">
      <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回
      </Button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card>
            <div className="relative aspect-video bg-black rounded-t-lg overflow-hidden">
              <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-b from-transparent to-black/30">
                <video
                  className="w-full h-full object-contain"
                  controls
                  src={videoService.getVideoStream(video.id)}
                  onPlay={() => setIsPlaying(true)}
                  onPause={() => setIsPlaying(false)}
                  onTimeUpdate={(e) => setCurrentTime(e.currentTarget.currentTime)}
                  onDurationChange={(e) => setDuration(e.currentTarget.duration)}
                >
                  您的浏览器不支持视频播放
                </video>
              </div>
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-lg">{video.title}</CardTitle>
              <Button variant="ghost" size="icon" onClick={toggleFavorite}>
                <Heart
                  className={cn(
                    'w-5 h-5',
                    video.isFavorite ? 'text-red-500 fill-red-500' : 'text-gray-400'
                  )}
                />
              </Button>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-gray-600">{video.description || '暂无描述'}</p>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">格式</span>
                  <span className="font-medium">{video.format || '-'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">分辨率</span>
                  <span className="font-medium">{video.resolution || '-'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">时长</span>
                  <span className="font-medium">{formatDuration(video.duration)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">来源</span>
                  <span className="font-medium">
                    {video.sourceType === 'UPLOADED' ? '上传' : '扫描'}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">播放控制</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">播放速度</span>
                <div className="flex gap-2">
                  {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => (
                    <Button
                      key={rate}
                      variant={playbackRate === rate ? 'default' : 'outline'}
                      size="sm"
                      onClick={() => setPlaybackRate(rate)}
                    >
                      {rate}x
                    </Button>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">音量</span>
                <div className="flex items-center gap-2 flex-1 max-w-48 ml-4">
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.01"
                    value={isMuted ? 0 : volume}
                    onChange={(e) => {
                      setVolume(parseFloat(e.target.value))
                      if (parseFloat(e.target.value) > 0) setIsMuted(false)
                    }}
                    className="w-full"
                  />
                  <span className="text-sm w-10 text-right">{Math.round((isMuted ? 0 : volume) * 100)}%</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
