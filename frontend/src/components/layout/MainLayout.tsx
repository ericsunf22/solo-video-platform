import { useState, useEffect, FormEvent, useCallback, useRef } from 'react'
import { Link, useLocation, Outlet } from 'react-router-dom'
import { Home, Heart, Clock, Tag, Settings, Search, Menu, Upload, FolderOpen, X, CheckCircle, RefreshCw, FileVideo, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useUIStore, useVideoStore, useToastStore } from '@/store'
import { videoService } from '@/services/videoService'
import { cn } from '@/utils/cn'

const navItems = [
  { path: '/', label: '视频库', icon: Home },
  { path: '/favorites', label: '收藏', icon: Heart },
  { path: '/history', label: '播放历史', icon: Clock },
  { path: '/tags', label: '标签管理', icon: Tag },
  { path: '/settings', label: '设置', icon: Settings },
]

export default function MainLayout() {
  const location = useLocation()
  const { sidebarOpen, setSidebarOpen, searchKeyword, setSearchKeyword, addRecentSearch } = useUIStore()
  const { triggerRefresh } = useVideoStore()
  const { warning, success, error } = useToastStore()
  const [showUploadDialog, setShowUploadDialog] = useState(false)
  const [showScanDialog, setShowScanDialog] = useState(false)
  const [scanFolderPath, setScanFolderPath] = useState('')
  const [scanRecursive, setScanRecursive] = useState(true)
  const [scanUpdateExisting, setScanUpdateExisting] = useState(true)
  const [isScanning, setIsScanning] = useState(false)
  const [scanProgress, setScanProgress] = useState(0)
  const [currentScanningFile, setCurrentScanningFile] = useState('')
  const [scanStats, setScanStats] = useState({
    newVideos: 0,
    updatedVideos: 0,
    skippedVideos: 0,
  })
  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const fetchScanProgress = useCallback(async () => {
    try {
      const progress = await videoService.getScanProgress()
      setScanProgress(progress.progress)
      setCurrentScanningFile(progress.currentScanningFile)
      setScanStats({
        newVideos: progress.newVideos,
        updatedVideos: progress.updatedVideos,
        skippedVideos: progress.skippedVideos,
      })
      return progress.isScanning
    } catch (err) {
      console.error('获取扫描进度失败:', err)
      return false
    }
  }, [])

  const loadScanFolderPath = async () => {
    try {
      const savedPath = await videoService.getSetting('scanFolderPath', '')
      if (savedPath) {
        setScanFolderPath(savedPath)
      } else {
        const localPath = localStorage.getItem('scanFolderPath')
        if (localPath) {
          setScanFolderPath(localPath)
        }
      }
    } catch (_err) {
      console.error('Failed to load scan folder path:', _err)
      const localPath = localStorage.getItem('scanFolderPath')
      if (localPath) {
        setScanFolderPath(localPath)
      }
    }
  }

  const saveScanFolderPath = async (path: string) => {
    localStorage.setItem('scanFolderPath', path)
    try {
      await videoService.setSetting('scanFolderPath', path, '扫描文件夹的默认路径')
    } catch (_err) {
      console.error('Failed to save scan folder path:', _err)
    }
  }

  useEffect(() => {
    loadScanFolderPath()
  }, [])

  useEffect(() => {
    if (isScanning) {
      pollIntervalRef.current = setInterval(fetchScanProgress, 500)
    }
    
    return () => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current)
        pollIntervalRef.current = null
      }
    }
  }, [isScanning, fetchScanProgress])

  const handleSearch = (e: FormEvent<HTMLFormElement>): void => {
    e.preventDefault()
    if (searchKeyword.trim()) {
      addRecentSearch(searchKeyword)
    }
  }

  const handleCancelScan = async () => {
    try {
      await videoService.cancelScan()
      warning('扫描已取消')
    } catch (err) {
      console.error('取消扫描失败:', err)
    }
  }

  const handleScanFolder = async () => {
    if (!scanFolderPath.trim()) {
      warning('请输入文件夹路径')
      return
    }

    setIsScanning(true)
    setScanProgress(0)
    setCurrentScanningFile('')
    setScanStats({ newVideos: 0, updatedVideos: 0, skippedVideos: 0 })

    try {
      await saveScanFolderPath(scanFolderPath)

      const result = await videoService.scanFolder({
        folderPath: scanFolderPath,
        recursive: scanRecursive,
        updateExisting: scanUpdateExisting,
      })

      setScanStats({
        newVideos: result.newVideos,
        updatedVideos: result.updatedVideos,
        skippedVideos: result.skippedVideos,
      })

      success(`扫描完成！新增视频: ${result.newVideos}, 更新视频: ${result.updatedVideos}, 跳过视频: ${result.skippedVideos}, 总视频数: ${result.totalVideos}`)
      
      if (result.newVideos > 0 || result.updatedVideos > 0) {
        triggerRefresh()
      }
      
      setTimeout(() => {
        setShowScanDialog(false)
      }, 1500)
    } catch (err) {
      console.error('扫描失败:', err)
      error('扫描失败: ' + (err instanceof Error ? err.message : '未知错误'))
    } finally {
      setIsScanning(false)
      setScanProgress(100)
    }
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <aside
        className={cn(
          'bg-white border-r border-gray-200 transition-all duration-300 flex flex-col',
          sidebarOpen ? 'w-64' : 'w-20'
        )}
      >
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
              <Home className="w-6 h-6 text-white" />
            </div>
            {sidebarOpen && <span className="font-bold text-lg">视频平台</span>}
          </div>
        </div>

        <nav className="flex-1 p-4" aria-label="主导航">
          <ul className="space-y-2">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname === item.path
              return (
                <li key={item.path}>
                  <Link
                    to={item.path}
                    className={cn(
                      'flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
                      isActive
                        ? 'bg-blue-50 text-blue-600'
                        : 'text-gray-600 hover:bg-gray-100'
                    )}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <Icon className="w-5 h-5" aria-hidden="true" />
                    {sidebarOpen && <span>{item.label}</span>}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="p-4 border-t border-gray-200">
          <Button
            variant="outline"
            className="w-full justify-start gap-2 mb-2"
            onClick={() => setShowUploadDialog(true)}
          >
            <Upload className="w-5 h-5" />
            {sidebarOpen && '上传视频'}
          </Button>
          <Button
            variant="outline"
            className="w-full justify-start gap-2"
            onClick={() => setShowScanDialog(true)}
          >
            <FolderOpen className="w-5 h-5" />
            {sidebarOpen && '扫描文件夹'}
          </Button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="bg-white border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setSidebarOpen(!sidebarOpen)}
              >
                <Menu className="w-5 h-5" />
              </Button>
              <form onSubmit={handleSearch} className="w-96">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <Input
                    placeholder="搜索视频..."
                    className="pl-10"
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                  />
                </div>
              </form>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-500">今天是 {new Date().toLocaleDateString('zh-CN')}</span>
            </div>
          </div>
        </header>

        <div className="flex-1 overflow-auto p-6">
          <Outlet />
        </div>
      </main>

      {showUploadDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">上传视频</h2>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
              <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-500 mb-2">拖拽视频文件到这里</p>
              <p className="text-gray-400 text-sm">或者点击选择文件</p>
              <input
                type="file"
                multiple
                accept="video/*"
                className="hidden"
                id="video-upload"
              />
              <label htmlFor="video-upload">
                <Button className="mt-4 cursor-pointer">选择文件</Button>
              </label>
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Button variant="outline" onClick={() => setShowUploadDialog(false)}>
                取消
              </Button>
              <Button onClick={() => setShowUploadDialog(false)}>确认上传</Button>
            </div>
          </div>
        </div>
      )}

      {showScanDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-lg shadow-2xl transition-all duration-300">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className={cn(
                  'w-10 h-10 rounded-lg flex items-center justify-center',
                  isScanning ? 'bg-blue-500' : 'bg-gray-100'
                )}>
                  {isScanning ? (
                    <Loader2 className="w-5 h-5 text-white animate-spin" />
                  ) : (
                    <FolderOpen className="w-5 h-5 text-gray-600" />
                  )}
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-900">扫描本地文件夹</h2>
                  {isScanning && (
                    <p className="text-sm text-blue-600">正在扫描中，请稍候...</p>
                  )}
                </div>
              </div>
              {!isScanning && (
                <button
                  onClick={() => setShowScanDialog(false)}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <X className="w-5 h-5 text-gray-500" />
                </button>
              )}
            </div>

            {!isScanning ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    文件夹路径
                  </label>
                  <Input
                    placeholder="请输入文件夹路径，例如：/mnt/hdd/videos"
                    value={scanFolderPath}
                    onChange={(e) => setScanFolderPath(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div className="flex items-center gap-6 py-2">
                  <label className="flex items-center gap-2 cursor-pointer group">
                    <div className="relative">
                      <input
                        type="checkbox"
                        checked={scanRecursive}
                        onChange={(e) => setScanRecursive(e.target.checked)}
                        className="sr-only peer"
                      />
                      <div className="w-5 h-5 border-2 border-gray-300 rounded peer-checked:bg-blue-500 peer-checked:border-blue-500 transition-all flex items-center justify-center">
                        {scanRecursive && (
                          <CheckCircle className="w-3 h-3 text-white" />
                        )}
                      </div>
                    </div>
                    <span className="text-sm text-gray-700 group-hover:text-gray-900">递归扫描子文件夹</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer group">
                    <div className="relative">
                      <input
                        type="checkbox"
                        checked={scanUpdateExisting}
                        onChange={(e) => setScanUpdateExisting(e.target.checked)}
                        className="sr-only peer"
                      />
                      <div className="w-5 h-5 border-2 border-gray-300 rounded peer-checked:bg-blue-500 peer-checked:border-blue-500 transition-all flex items-center justify-center">
                        {scanUpdateExisting && (
                          <CheckCircle className="w-3 h-3 text-white" />
                        )}
                      </div>
                    </div>
                    <span className="text-sm text-gray-700 group-hover:text-gray-900">更新已存在的视频</span>
                  </label>
                </div>
                <div className="bg-blue-50 rounded-lg p-3">
                  <p className="text-xs text-blue-700">
                    <span className="font-medium">提示：</span>
                    勾选「更新已存在的视频」将重新提取所有视频的时长和封面图。
                  </p>
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="relative">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700">扫描进度</span>
                    <span className="text-sm font-medium text-blue-600">
                      {scanProgress > 0 && scanProgress < 100 ? `${scanProgress}%` : '处理中...'}
                    </span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-blue-500 to-blue-600 rounded-full transition-all duration-300 ease-out relative"
                      style={{ width: `${scanProgress > 0 && scanProgress < 100 ? scanProgress : 5}%` }}
                    >
                      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse" />
                    </div>
                  </div>
                </div>

                {currentScanningFile && (
                  <div className="bg-gray-50 rounded-lg p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <FileVideo className="w-4 h-4 text-gray-500 flex-shrink-0" />
                      <span className="text-xs font-medium text-gray-600">当前处理</span>
                    </div>
                    <p className="text-xs text-gray-700 truncate font-mono">
                      {currentScanningFile}
                    </p>
                  </div>
                )}

                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-green-50 rounded-lg p-3 text-center">
                    <div className="flex items-center justify-center mb-1">
                      <FileVideo className="w-4 h-4 text-green-600" />
                    </div>
                    <p className="text-2xl font-bold text-green-600">{scanStats.newVideos}</p>
                    <p className="text-xs text-green-700">新增视频</p>
                  </div>
                  <div className="bg-blue-50 rounded-lg p-3 text-center">
                    <div className="flex items-center justify-center mb-1">
                      <RefreshCw className="w-4 h-4 text-blue-600" />
                    </div>
                    <p className="text-2xl font-bold text-blue-600">{scanStats.updatedVideos}</p>
                    <p className="text-xs text-blue-700">更新视频</p>
                  </div>
                  <div className="bg-gray-50 rounded-lg p-3 text-center">
                    <div className="flex items-center justify-center mb-1">
                      <X className="w-4 h-4 text-gray-500" />
                    </div>
                    <p className="text-2xl font-bold text-gray-600">{scanStats.skippedVideos}</p>
                    <p className="text-xs text-gray-700">跳过视频</p>
                  </div>
                </div>

                <div className="flex items-center justify-center gap-2 text-sm text-gray-500">
                  <div className="flex gap-1">
                    <span className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                  <span>正在提取视频元数据...</span>
                </div>
              </div>
            )}

            <div className={cn(
              'flex justify-end gap-2 mt-6',
              isScanning && 'justify-center'
            )}>
              {!isScanning ? (
                <>
                  <Button
                    variant="outline"
                    onClick={() => setShowScanDialog(false)}
                  >
                    取消
                  </Button>
                  <Button onClick={handleScanFolder}>
                    开始扫描
                  </Button>
                </>
              ) : (
                <Button
                  variant="outline"
                  onClick={handleCancelScan}
                  className="gap-2"
                >
                  <X className="w-4 h-4" />
                  取消扫描
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
