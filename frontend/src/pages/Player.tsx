import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Play, Pause, Volume2, VolumeX, Maximize, SkipBack, SkipForward, Settings } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { usePlayerStore } from '@/store/playerStore'
import { formatDuration } from '@/utils/format'
import type { Video } from '@/types'

export default function Player() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const {
    isPlaying,
    currentTime,
    duration,
    volume,
    isMuted,
    playbackRate,
    setIsPlaying,
    setCurrentTime,
    setVolume,
    setIsMuted,
    setPlaybackRate,
  } = usePlayerStore()

  const mockVideo: Video = {
    id: Number(id) || 1,
    title: 'React 18 新特性详解',
    description: '深入讲解 React 18 的新特性，包括并发特性、Suspense 改进、自动批处理等。',
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
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0

  const togglePlay = () => setIsPlaying(!isPlaying)
  const toggleMute = () => setIsMuted(!isMuted)
  const skipBack = () => setCurrentTime(Math.max(0, currentTime - 10))
  const skipForward = () => setCurrentTime(Math.min(duration, currentTime + 10))

  return (
    <div className="max-w-6xl mx-auto">
      <Button
        variant="ghost"
        className="mb-4"
        onClick={() => navigate(-1)}
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        返回
      </Button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card>
            <div className="relative aspect-video bg-black rounded-t-lg overflow-hidden">
              <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-b from-transparent to-black/30">
                <div className="text-white text-lg">视频播放器区域</div>
              </div>

              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-4">
                <div className="mb-3">
                  <div className="relative h-1 bg-white/30 rounded-full cursor-pointer">
                    <div
                      className="absolute top-0 left-0 h-full bg-blue-500 rounded-full"
                      style={{ width: `${progress}%` }}
                    />
                    <div
                      className="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-white rounded-full shadow"
                      style={{ left: `calc(${progress}% - 6px)` }}
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-white hover:bg-white/20"
                      onClick={skipBack}
                    >
                      <SkipBack className="w-5 h-5" />
                    </Button>

                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-white hover:bg-white/20"
                      onClick={togglePlay}
                    >
                      {isPlaying ? (
                        <Pause className="w-6 h-6" />
                      ) : (
                        <Play className="w-6 h-6" />
                      )}
                    </Button>

                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-white hover:bg-white/20"
                      onClick={skipForward}
                    >
                      <SkipForward className="w-5 h-5" />
                    </Button>

                    <div className="flex items-center gap-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-white hover:bg-white/20"
                        onClick={toggleMute}
                      >
                        {isMuted ? (
                          <VolumeX className="w-5 h-5" />
                        ) : (
                          <Volume2 className="w-5 h-5" />
                        )}
                      </Button>
                      <div className="w-20 h-1 bg-white/30 rounded-full cursor-pointer">
                        <div
                          className="h-full bg-white rounded-full"
                          style={{ width: `${isMuted ? 0 : volume * 100}%` }}
                        />
                      </div>
                    </div>

                    <span className="text-white text-sm">
                      {formatDuration(currentTime)} / {formatDuration(duration || mockVideo.duration)}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-white hover:bg-white/20"
                    >
                      <Settings className="w-5 h-5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-white hover:bg-white/20"
                    >
                      <Maximize className="w-5 h-5" />
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{mockVideo.title}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-gray-600">{mockVideo.description}</p>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">格式</span>
                  <span className="font-medium">{mockVideo.format}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">分辨率</span>
                  <span className="font-medium">{mockVideo.resolution}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">时长</span>
                  <span className="font-medium">{formatDuration(mockVideo.duration)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">来源</span>
                  <span className="font-medium">
                    {mockVideo.sourceType === 'UPLOADED' ? '上传' : '扫描'}
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
