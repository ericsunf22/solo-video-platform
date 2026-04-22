import { useState, useEffect } from 'react'
import { Link, useLocation, Outlet } from 'react-router-dom'
import { Home, Heart, Clock, Tag, Settings, Search, Menu, Upload, FolderOpen } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useUIStore, useVideoStore, useToastStore } from '@/store'
import { videoService } from '@/services/videoService'

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
  const [scanUpdateExisting, setScanUpdateExisting] = useState(false)
  const [isScanning, setIsScanning] = useState(false)

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

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchKeyword.trim()) {
      addRecentSearch(searchKeyword)
    }
  }

  const handleScanFolder = async () => {
    if (!scanFolderPath.trim()) {
      warning('请输入文件夹路径')
      return
    }

    setIsScanning(true)
    try {
      await saveScanFolderPath(scanFolderPath)

      const result = await videoService.scanFolder({
        folderPath: scanFolderPath,
        recursive: scanRecursive,
        updateExisting: scanUpdateExisting,
      })

      success(`扫描完成！新增视频: ${result.newVideos}, 更新视频: ${result.updatedVideos}, 跳过视频: ${result.skippedVideos}, 总视频数: ${result.totalVideos}`)
      
      if (result.newVideos > 0 || result.updatedVideos > 0) {
        triggerRefresh()
      }
      
      setShowScanDialog(false)
    } catch (err) {
      console.error('扫描失败:', err)
      error('扫描失败: ' + (err instanceof Error ? err.message : '未知错误'))
    } finally {
      setIsScanning(false)
    }
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <aside
        className={`${
          sidebarOpen ? 'w-64' : 'w-20'
        } bg-white border-r border-gray-200 transition-all duration-300 flex flex-col`}
      >
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
              <Home className="w-6 h-6 text-white" />
            </div>
            {sidebarOpen && <span className="font-bold text-lg">视频平台</span>}
          </div>
        </div>

        <nav className="flex-1 p-4">
          <ul className="space-y-2">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname === item.path
              return (
                <li key={item.path}>
                  <Link
                    to={item.path}
                    className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                      isActive
                        ? 'bg-blue-50 text-blue-600'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    <Icon className="w-5 h-5" />
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
          <div className="bg-white rounded-xl p-6 w-full max-w-md">
            <h2 className="text-xl font-bold mb-4">扫描本地文件夹</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  文件夹路径
                </label>
                <Input
                  placeholder="请输入文件夹路径"
                  value={scanFolderPath}
                  onChange={(e) => setScanFolderPath(e.target.value)}
                  disabled={isScanning}
                />
              </div>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={scanRecursive}
                    onChange={(e) => setScanRecursive(e.target.checked)}
                    disabled={isScanning}
                    className="rounded"
                  />
                  <span className="text-sm">递归扫描子文件夹</span>
                </label>
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={scanUpdateExisting}
                    onChange={(e) => setScanUpdateExisting(e.target.checked)}
                    disabled={isScanning}
                    className="rounded"
                  />
                  <span className="text-sm">更新已存在的视频</span>
                </label>
              </div>
            </div>
            <div className="flex justify-end gap-2 mt-6">
              <Button
                variant="outline"
                onClick={() => setShowScanDialog(false)}
                disabled={isScanning}
              >
                取消
              </Button>
              <Button onClick={handleScanFolder} disabled={isScanning}>
                {isScanning ? '扫描中...' : '开始扫描'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
