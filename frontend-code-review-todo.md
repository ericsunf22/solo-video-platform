# 前端代码审查待办事项 (Code Review Todo List)

> 审查日期：2026-04-22
> 技术栈：React 18 + TypeScript + Vite + Zustand + ShadCN UI
> 审查维度：React 18 最佳实践、TypeScript 严格模式、Vite 配置、ShadCN UI 模式、React Query 模式、可访问性、安全性与代码质量

---

## 🔴 Critical 严重问题 (必须立即修复)

### 1. 缺少 React Query 集成，使用手动数据获取

**问题描述：**
项目中所有数据获取都使用 `useState` + `useEffect` + 手动 Axios 调用，没有使用 React Query 这样的专业数据获取库。

**影响：**
- 缺少缓存机制，每次重新渲染都会重新请求
- 没有自动重新获取（如窗口聚焦时）
- 没有错误重试机制
- 代码冗余，每个组件都要手动处理 loading、error 状态
- 用户体验差，没有后台刷新功能

**涉及文件：**
- `frontend/src/pages/Home.tsx` (第 14-37 行)
- `frontend/src/pages/Player.tsx` (第 30-56 行)
- 所有包含数据获取的组件

**修复建议：**
1. 安装 `@tanstack/react-query` 或 `@tanstack/react-query-devtools`
2. 创建 Query Client 并在 App 组件中提供
3. 定义查询键工厂（Query Key Factory）
4. 使用 `useQuery` 替代手动的 `useEffect` + `useState`
5. 使用 `useMutation` 处理所有写操作（POST/PUT/DELETE）

**代码示例：**
```typescript
// 1. 创建 queryClient.ts
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 分钟内数据视为新鲜
      gcTime: 10 * 60 * 1000, // 10 分钟后垃圾回收
      retry: 1, // 失败重试 1 次
    },
  },
})

// 2. 在 App.tsx 中提供
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './lib/queryClient'

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      {/* 原有内容 */}
    </QueryClientProvider>
  )
}

// 3. 定义查询键工厂
const videoKeys = {
  all: ['videos'] as const,
  lists: (page: number, size: number) => [...videoKeys.all, 'list', page, size] as const,
  detail: (id: number) => [...videoKeys.all, 'detail', id] as const,
}

// 4. 使用 useQuery
const { data, isLoading, isError, error } = useQuery({
  queryKey: videoKeys.lists(1, 20),
  queryFn: () => videoService.getVideos({ page: 1, size: 20 }),
})
```

---

### 2. 可访问性问题严重

**问题描述：**
整个项目没有遵循可访问性最佳实践，缺少语义化 HTML、ARIA 属性和键盘导航支持。

**影响：**
- 不符合 WCAG 2.2 A/AA 标准
- 屏幕阅读器无法正确解析页面结构
- 键盘用户无法使用导航和交互元素
- 可能面临法律合规风险

**涉及文件：**
- 整个项目，特别是：
  - `frontend/src/components/layout/MainLayout.tsx`
  - `frontend/src/pages/Home.tsx`
  - 所有自定义组件

**具体问题点：**

#### 2.1 缺少语义化 HTML 元素
- 使用 `<div>` 代替 `<nav>`, `<main>`, `<section>`, `<article>` 等语义化元素
- 导航没有使用 `<nav>` 包裹
- 主要内容区域没有使用 `<main>` 标签

#### 2.2 缺少 ARIA 属性
- 按钮没有 `aria-label` 或 `aria-labelledby`
- 图标按钮没有可访问名称
- 表单输入没有 `aria-describedby` 关联错误信息
- 动态内容更新没有使用 `aria-live` 区域

#### 2.3 缺少键盘导航支持
- 自定义对话框没有焦点陷阱
- 下拉菜单没有键盘导航（上下箭头、Enter、Escape）
- 标签页没有键盘导航（左右箭头）
- 没有 "跳过导航" 链接

#### 2.4 表单可访问性
- 输入框没有关联的 `<label>` 元素
- 占位符文本不是可访问的标签
- 错误信息没有与输入框关联

**修复建议：**

1. **使用语义化 HTML**
   ```tsx
   // 之前
   <div className="sidebar">...</div>
   
   // 之后
   <nav className="sidebar" aria-label="主导航">...</nav>
   ```

2. **为图标按钮添加 aria-label**
   ```tsx
   // 之前
   <Button variant="ghost" size="icon" onClick={toggleFavorite}>
     <Heart className="w-5 h-5" />
   </Button>
   
   // 之后
   <Button 
     variant="ghost" 
     size="icon" 
     onClick={toggleFavorite}
     aria-label={video.isFavorite ? "取消收藏" : "添加收藏"}
   >
     <Heart className="w-5 h-5" aria-hidden="true" />
   </Button>
   ```

3. **为表单输入添加正确的标签**
   ```tsx
   // 之前
   <Input placeholder="搜索视频..." />
   
   // 之后
   <div className="relative">
     <Label htmlFor="search" className="sr-only">搜索视频</Label>
     <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" aria-hidden="true" />
     <Input
       id="search"
       type="search"
       placeholder="搜索视频..."
       className="pl-10"
       aria-label="搜索视频"
     />
   </div>
   ```

4. **使用 Radix UI 组件处理复杂交互**
   Radix UI 组件已经内置了可访问性支持，包括：
   - 正确的 ARIA 属性
   - 键盘导航
   - 焦点管理
   - 屏幕阅读器支持

5. **添加跳过导航链接**
   ```tsx
   <a href="#main-content" className="sr-only focus:not-sr-only">
     跳到主要内容
   </a>
   <main id="main-content">...</main>
   ```

---

## 🟠 Major 主要问题 (应该尽快修复)

### 3. 组件内部定义子组件，导致不必要的重新渲染

**问题描述：**
在 `Home.tsx` 中，`VideoCard`、`VideoRow` 和 `Play` 组件都定义在 `Home` 组件内部。

**影响：**
- 每次 `Home` 组件重新渲染时，这些子组件都会被重新创建
- 即使 props 没有变化，子组件也会重新渲染
- 性能浪费，特别是在视频列表较长时

**涉及文件：**
- `frontend/src/pages/Home.tsx` (第 39-107 行)

**问题代码：**
```tsx
export default function Home() {
  // ... 状态和逻辑
  
  // ❌ 在组件内部定义子组件
  const VideoCard = ({ video }: { video: Video }) => (
    <Card className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group">
      {/* ... */}
    </Card>
  )
  
  const VideoRow = ({ video }: { video: Video }) => (
    <div className="flex items-center gap-4 p-4 bg-white rounded-lg border hover:border-blue-300 transition-colors cursor-pointer">
      {/* ... */}
    </div>
  )
  
  function Play({ className }: { className?: string }) {
    return <svg ... />
  }
  
  return (
    <div>
      {/* 使用这些内部组件 */}
      {videos.map((video) => (
        <VideoCard key={video.id} video={video} />
      ))}
    </div>
  )
}
```

**修复建议：**
将这些子组件移到 `Home` 组件外部，或者使用 `React.memo` 包装（如果确实需要在内部定义）。

**修复后的代码：**
```tsx
// ✅ 移到组件外部
import { FileVideo, Eye, Clock } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
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
  // Home 组件的实现...
  return (
    <div>
      {videos.map((video) => (
        viewMode === 'grid' 
          ? <VideoCard key={video.id} video={video} />
          : <VideoRow key={video.id} video={video} />
      ))}
    </div>
  )
}
```

---

### 4. 缺少代码分割和懒加载

**问题描述：**
所有路由组件都直接导入，没有使用 `React.lazy()` 和 `<Suspense>` 进行代码分割。

**影响：**
- 首屏加载时间过长
- 初始 bundle 体积过大
- 用户等待时间增加，可能导致用户流失

**涉及文件：**
- `frontend/src/router.tsx` (第 3-8 行)

**问题代码：**
```tsx
import { Routes, Route } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'
// ❌ 全部直接导入，没有代码分割
import Home from '@/pages/Home'
import Player from '@/pages/Player'
import Favorites from '@/pages/Favorites'
import History from '@/pages/History'
import Tags from '@/pages/Tags'
import Settings from '@/pages/Settings'
```

**修复建议：**
使用 `React.lazy()` 和 `<Suspense>` 对路由组件进行懒加载，并提供加载骨架屏。

**修复后的代码：**
```tsx
import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'

// ✅ 使用 React.lazy 懒加载路由组件
const Home = lazy(() => import('@/pages/Home'))
const Player = lazy(() => import('@/pages/Player'))
const Favorites = lazy(() => import('@/pages/Favorites'))
const History = lazy(() => import('@/pages/History'))
const Tags = lazy(() => import('@/pages/Tags'))
const Settings = lazy(() => import('@/pages/Settings'))

// 加载骨架屏组件
function PageSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="aspect-video bg-gray-200 rounded-lg"></div>
        ))}
      </div>
    </div>
  )
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={
          <Suspense fallback={<PageSkeleton />}>
            <Home />
          </Suspense>
        } />
        <Route path="player/:id" element={
          <Suspense fallback={<PageSkeleton />}>
            <Player />
          </Suspense>
        } />
        <Route path="favorites" element={
          <Suspense fallback={<PageSkeleton />}>
            <Favorites />
          </Suspense>
        } />
        <Route path="history" element={
          <Suspense fallback={<PageSkeleton />}>
            <History />
          </Suspense>
        } />
        <Route path="tags" element={
          <Suspense fallback={<PageSkeleton />}>
            <Tags />
          </Suspense>
        } />
        <Route path="settings" element={
          <Suspense fallback={<PageSkeleton />}>
            <Settings />
          </Suspense>
        } />
      </Route>
    </Routes>
  )
}
```

**进一步优化建议：**

1. **基于路由的代码分割**：上面已经实现

2. **基于功能的代码分割**：对于大型组件库（如图表、视频编辑器等），使用动态导入
   ```tsx
   // 示例：懒加载图表组件
   const Chart = lazy(() => import('@/components/Chart'))
   ```

3. **配置 Vite 手动分块**：
   ```typescript
   // vite.config.ts
   import { defineConfig } from 'vite'
   
   export default defineConfig({
     build: {
       rollupOptions: {
         output: {
           manualChunks: {
             // 将 React 相关库单独打包
             'react-vendor': ['react', 'react-dom', 'react-router-dom'],
             // 将状态管理单独打包
             'state-vendor': ['zustand'],
             // 将 UI 组件库单独打包
             'ui-vendor': ['@radix-ui/react-dialog', '@radix-ui/react-dropdown-menu', 'lucide-react'],
           },
         },
       },
     },
   })
   ```

4. **使用预加载**：对于用户可能点击的下一个页面，使用 `React.lazy` + `import()` 预加载
   ```tsx
   // 示例：在用户悬停在导航链接上时预加载页面
   const preloadPlayer = () => {
     import('@/pages/Player')
   }
   
   <Link 
     to="/player/1" 
     onMouseEnter={preloadPlayer}
     onFocus={preloadPlayer}
   >
     播放视频
   </Link>
   ```

---

### 5. MainLayout 组件过于庞大，职责过多

**问题描述：**
`MainLayout` 组件包含了以下多个功能：
- 侧边栏导航
- 顶部导航栏
- 搜索功能
- 上传对话框
- 扫描文件夹对话框
- 扫描路径的加载和保存

**影响：**
- 组件难以维护，超过 270 行代码
- 测试困难，一个测试用例需要覆盖多个功能
- 不符合单一职责原则（SRP）
- 代码复用性差，对话框逻辑无法在其他地方使用
- 状态和逻辑耦合在一起

**涉及文件：**
- `frontend/src/components/layout/MainLayout.tsx` (整个文件)

**问题分析：**

```tsx
// MainLayout 组件包含：
export default function MainLayout() {
  // 1. 导航相关状态
  const location = useLocation()
  const { sidebarOpen, setSidebarOpen, searchKeyword, setSearchKeyword, addRecentSearch } = useUIStore()
  
  // 2. 对话框状态
  const [showUploadDialog, setShowUploadDialog] = useState(false)
  const [showScanDialog, setShowScanDialog] = useState(false)
  
  // 3. 扫描功能状态
  const [scanFolderPath, setScanFolderPath] = useState('')
  const [scanRecursive, setScanRecursive] = useState(true)
  const [scanUpdateExisting, setScanUpdateExisting] = useState(false)
  const [isScanning, setIsScanning] = useState(false)
  
  // 4. 扫描路径加载逻辑
  const loadScanFolderPath = async () => { /* ... */ }
  
  // 5. 扫描路径保存逻辑
  const saveScanFolderPath = async (path: string) => { /* ... */ }
  
  // 6. 搜索处理逻辑
  const handleSearch = (e: React.FormEvent) => { /* ... */ }
  
  // 7. 扫描文件夹处理逻辑
  const handleScanFolder = async () => { /* ... */ }
  
  // 8. 渲染：侧边栏 + 顶部栏 + 两个对话框
  return (
    <div className="flex h-screen bg-gray-50">
      {/* 侧边栏 */}
      <aside className="...">...</aside>
      
      {/* 主内容区 */}
      <main className="...">...</main>
      
      {/* 上传对话框 */}
      {showUploadDialog && <div className="...">...</div>}
      
      {/* 扫描对话框 */}
      {showScanDialog && <div className="...">...</div>}
    </div>
  )
}
```

**修复建议：**

将 `MainLayout` 组件拆分为以下几个独立的组件和自定义 hook：

#### 5.1 拆分对话框组件

**创建 `UploadDialog.tsx`：**
```tsx
// frontend/src/components/dialogs/UploadDialog.tsx
import { useState } from 'react'
import { Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useToastStore } from '@/store'

interface UploadDialogProps {
  isOpen: boolean
  onClose: () => void
  onUpload?: (files: FileList) => void
}

export function UploadDialog({ isOpen, onClose, onUpload }: UploadDialogProps) {
  const { success, error } = useToastStore()
  const [selectedFiles, setSelectedFiles] = useState<FileList | null>(null)
  
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFiles(e.target.files)
  }
  
  const handleUpload = async () => {
    if (!selectedFiles || selectedFiles.length === 0) {
      error('请选择要上传的视频文件')
      return
    }
    
    try {
      onUpload?.(selectedFiles)
      success(`已选择 ${selectedFiles.length} 个文件`)
      onClose()
    } catch (err) {
      error('上传失败: ' + (err instanceof Error ? err.message : '未知错误'))
    }
  }
  
  if (!isOpen) return null
  
  return (
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
            onChange={handleFileChange}
          />
          <label htmlFor="video-upload">
            <Button className="mt-4 cursor-pointer">选择文件</Button>
          </label>
          {selectedFiles && selectedFiles.length > 0 && (
            <p className="mt-2 text-sm text-gray-600">
              已选择 {selectedFiles.length} 个文件
            </p>
          )}
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <Button variant="outline" onClick={onClose}>
            取消
          </Button>
          <Button onClick={handleUpload}>确认上传</Button>
        </div>
      </div>
    </div>
  )
}
```

**创建 `ScanDialog.tsx`：**
```tsx
// frontend/src/components/dialogs/ScanDialog.tsx
import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useToastStore, useVideoStore } from '@/store'
import { videoService } from '@/services'

interface ScanDialogProps {
  isOpen: boolean
  onClose: () => void
}

export function ScanDialog({ isOpen, onClose }: ScanDialogProps) {
  const { success, error, warning } = useToastStore()
  const { triggerRefresh } = useVideoStore()
  
  const [scanFolderPath, setScanFolderPath] = useState('')
  const [scanRecursive, setScanRecursive] = useState(true)
  const [scanUpdateExisting, setScanUpdateExisting] = useState(false)
  const [isScanning, setIsScanning] = useState(false)
  
  // 加载保存的扫描路径
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
  
  // 保存扫描路径
  const saveScanFolderPath = async (path: string) => {
    localStorage.setItem('scanFolderPath', path)
    try {
      await videoService.setSetting('scanFolderPath', path, '扫描文件夹的默认路径')
    } catch (_err) {
      console.error('Failed to save scan folder path:', _err)
    }
  }
  
  // 处理扫描
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
      
      onClose()
    } catch (err) {
      console.error('扫描失败:', err)
      error('扫描失败: ' + (err instanceof Error ? err.message : '未知错误'))
    } finally {
      setIsScanning(false)
    }
  }
  
  // 打开对话框时加载路径
  useEffect(() => {
    if (isOpen) {
      loadScanFolderPath()
    }
  }, [isOpen])
  
  if (!isOpen) return null
  
  return (
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
            onClick={onClose}
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
  )
}
```

#### 5.2 拆分导航组件

**创建 `Sidebar.tsx`：**
```tsx
// frontend/src/components/layout/Sidebar.tsx
import { Link, useLocation } from 'react-router-dom'
import { Home, Heart, Clock, Tag, Settings, Upload, FolderOpen } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface SidebarProps {
  isOpen: boolean
  onUploadClick: () => void
  onScanClick: () => void
}

const navItems = [
  { path: '/', label: '视频库', icon: Home },
  { path: '/favorites', label: '收藏', icon: Heart },
  { path: '/history', label: '播放历史', icon: Clock },
  { path: '/tags', label: '标签管理', icon: Tag },
  { path: '/settings', label: '设置', icon: Settings },
]

export function Sidebar({ isOpen, onUploadClick, onScanClick }: SidebarProps) {
  const location = useLocation()
  
  return (
    <aside
      className={`${
        isOpen ? 'w-64' : 'w-20'
      } bg-white border-r border-gray-200 transition-all duration-300 flex flex-col`}
    >
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
            <Home className="w-6 h-6 text-white" />
          </div>
          {isOpen && <span className="font-bold text-lg">视频平台</span>}
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
                  className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-blue-600'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon className="w-5 h-5" aria-hidden="true" />
                  {isOpen && <span>{item.label}</span>}
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
          onClick={onUploadClick}
        >
          <Upload className="w-5 h-5" aria-hidden="true" />
          {isOpen && '上传视频'}
        </Button>
        <Button
          variant="outline"
          className="w-full justify-start gap-2"
          onClick={onScanClick}
        >
          <FolderOpen className="w-5 h-5" aria-hidden="true" />
          {isOpen && '扫描文件夹'}
        </Button>
      </div>
    </aside>
  )
}
```

**创建 `Header.tsx`：**
```tsx
// frontend/src/components/layout/Header.tsx
import { FormEvent } from 'react'
import { Search, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useUIStore } from '@/store'

interface HeaderProps {
  onMenuClick: () => void
}

export function Header({ onMenuClick }: HeaderProps) {
  const { searchKeyword, setSearchKeyword, addRecentSearch } = useUIStore()
  
  const handleSearch = (e: FormEvent) => {
    e.preventDefault()
    if (searchKeyword.trim()) {
      addRecentSearch(searchKeyword)
    }
  }
  
  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={onMenuClick}
            aria-label="切换侧边栏"
          >
            <Menu className="w-5 h-5" aria-hidden="true" />
          </Button>
          <form onSubmit={handleSearch} className="w-96">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" aria-hidden="true" />
              <Input
                type="search"
                placeholder="搜索视频..."
                className="pl-10"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                aria-label="搜索视频"
              />
            </div>
          </form>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-500">今天是 {new Date().toLocaleDateString('zh-CN')}</span>
        </div>
      </div>
    </header>
  )
}
```

#### 5.3 重构后的 MainLayout

```tsx
// frontend/src/components/layout/MainLayout.tsx
import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useUIStore } from '@/store'
import { Sidebar } from './Sidebar'
import { Header } from './Header'
import { UploadDialog } from '@/components/dialogs/UploadDialog'
import { ScanDialog } from '@/components/dialogs/ScanDialog'

export default function MainLayout() {
  const { sidebarOpen, setSidebarOpen } = useUIStore()
  
  // 对话框状态（最小化）
  const [showUploadDialog, setShowUploadDialog] = useState(false)
  const [showScanDialog, setShowScanDialog] = useState(false)
  
  return (
    <div className="flex h-screen bg-gray-50">
      {/* 跳过导航链接（可访问性） */}
      <a 
        href="#main-content" 
        className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:bg-blue-600 focus:text-white focus:px-4 focus:py-2 focus:rounded"
      >
        跳到主要内容
      </a>
      
      {/* 侧边栏 */}
      <Sidebar
        isOpen={sidebarOpen}
        onUploadClick={() => setShowUploadDialog(true)}
        onScanClick={() => setShowScanDialog(true)}
      />

      {/* 主内容区 */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />

        <div id="main-content" className="flex-1 overflow-auto p-6">
          <Outlet />
        </div>
      </main>

      {/* 对话框 */}
      <UploadDialog
        isOpen={showUploadDialog}
        onClose={() => setShowUploadDialog(false)}
      />
      
      <ScanDialog
        isOpen={showScanDialog}
        onClose={() => setShowScanDialog(false)}
      />
    </div>
  )
}
```

**拆分后的收益：**
1. **MainLayout 从 270+ 行减少到约 50 行**
2. **每个组件职责单一**：
   - `MainLayout`: 只负责布局组合和状态协调
   - `Sidebar`: 负责侧边栏导航
   - `Header`: 负责顶部导航和搜索
   - `UploadDialog`: 负责上传对话框
   - `ScanDialog`: 负责扫描对话框
3. **更好的可测试性**：每个组件可以独立测试
4. **更好的可复用性**：对话框组件可以在其他地方使用
5. **更好的可维护性**：修改一个功能不会影响其他功能

---

### 6. 缺少环境变量类型验证

**问题描述：**
没有对 `import.meta.env` 进行类型定义和验证，可能导致运行时错误。

**影响：**
- 环境变量使用不安全，缺少类型检查
- 访问不存在的环境变量时，TypeScript 不会报错
- 运行时可能出现 `undefined.trim()` 等错误
- 难以追踪环境变量的来源和用途

**涉及文件：**
- 整个项目，特别是：
  - `frontend/src/services/api.ts` (API 基础 URL)
  - 任何使用 `import.meta.env` 的地方

**问题示例：**
```typescript
// ❌ 没有类型检查
const apiUrl = import.meta.env.VITE_API_URL
// 如果 VITE_API_URL 未定义，apiUrl 是 undefined
// 后续使用时可能导致运行时错误
const fullUrl = apiUrl.trim() // ❌ 运行时错误：undefined.trim()
```

**修复建议：**

#### 6.1 创建环境变量类型定义

**创建 `src/vite-env.d.ts` 或 `src/env.d.ts`：**
```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  // API 配置
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TIMEOUT: string
  
  // 应用配置
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_DEFAULT_PAGE_SIZE: string
  
  // 其他环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

#### 6.2 创建环境变量验证工具

**创建 `src/lib/env.ts`：**
```typescript
/**
 * 环境变量验证和管理
 * 确保所有必需的环境变量都已定义，并提供类型安全的访问
 */

// 定义环境变量的类型
interface EnvConfig {
  // API 配置
  apiBaseUrl: string
  apiTimeout: number
  
  // 应用配置
  appTitle: string
  appDefaultPageSize: number
  
  // 开发模式标志
  isDevelopment: boolean
  isProduction: boolean
}

// 验证并获取环境变量
function getEnv(): EnvConfig {
  // 检查是否在浏览器环境
  const isBrowser = typeof import.meta !== 'undefined'
  
  if (!isBrowser) {
    throw new Error('环境变量只能在浏览器环境中访问')
  }
  
  const env = import.meta.env
  
  // 验证必需的环境变量
  const requiredVars = [
    'VITE_API_BASE_URL',
  ] as const
  
  const missingVars = requiredVars.filter((key) => !env[key])
  
  if (missingVars.length > 0) {
    throw new Error(
      `缺少必需的环境变量: ${missingVars.join(', ')}\n` +
      `请检查 .env 文件或环境变量配置`
    )
  }
  
  // 解析和验证环境变量
  const config: EnvConfig = {
    // API 配置
    apiBaseUrl: env.VITE_API_BASE_URL,
    apiTimeout: parseInt(env.VITE_API_TIMEOUT || '30000', 10),
    
    // 应用配置
    appTitle: env.VITE_APP_TITLE || '视频平台',
    appDefaultPageSize: parseInt(env.VITE_APP_DEFAULT_PAGE_SIZE || '20', 10),
    
    // 环境标志
    isDevelopment: env.MODE === 'development',
    isProduction: env.MODE === 'production',
  }
  
  // 额外的验证
  if (isNaN(config.apiTimeout) || config.apiTimeout <= 0) {
    console.warn(`无效的 API_TIMEOUT: ${env.VITE_API_TIMEOUT}, 使用默认值 30000`)
    config.apiTimeout = 30000
  }
  
  if (isNaN(config.appDefaultPageSize) || config.appDefaultPageSize <= 0) {
    console.warn(`无效的 DEFAULT_PAGE_SIZE: ${env.VITE_APP_DEFAULT_PAGE_SIZE}, 使用默认值 20`)
    config.appDefaultPageSize = 20
  }
  
  return config
}

// 单例模式：只验证一次
let envConfig: EnvConfig | null = null

export function getEnvConfig(): EnvConfig {
  if (!envConfig) {
    envConfig = getEnv()
  }
  return envConfig
}

// 导出环境变量的便捷访问
export const env = getEnvConfig()

// 导出单独的变量（便于解构使用）
export const {
  apiBaseUrl,
  apiTimeout,
  appTitle,
  appDefaultPageSize,
  isDevelopment,
  isProduction,
} = env
```

#### 6.3 更新 API 配置使用验证后的环境变量

**修改 `frontend/src/services/api.ts`：**
```typescript
import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ApiResponse } from '@/types'
import { env } from '@/lib/env'

const api: AxiosInstance = axios.create({
  baseURL: env.apiBaseUrl, // 使用验证后的环境变量
  timeout: env.apiTimeout,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    return response
  },
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default api
```

#### 6.4 创建 .env.example 模板文件

**创建 `frontend/.env.example`：**
```env
# API 配置
VITE_API_BASE_URL=/api
VITE_API_TIMEOUT=30000

# 应用配置
VITE_APP_TITLE=视频平台
VITE_APP_DEFAULT_PAGE_SIZE=20

# 说明：
# - 所有客户端可访问的环境变量必须以 VITE_ 前缀开头
# - 敏感信息（如数据库密码）不应该放在这里
# - 复制此文件为 .env.local 并根据实际情况修改
```

**创建 `frontend/.env.local`（实际使用的文件，应添加到 .gitignore）：**
```env
# API 配置
VITE_API_BASE_URL=/api
VITE_API_TIMEOUT=30000

# 应用配置
VITE_APP_TITLE=我的视频平台
VITE_APP_DEFAULT_PAGE_SIZE=20
```

#### 6.5 更新 .gitignore

确保以下内容已添加到 `frontend/.gitignore`：
```
# 环境变量文件
.env.local
.env.*.local

# 保留示例文件
!.env.example
```

**修复后的收益：**
1. **类型安全**：TypeScript 会检查环境变量的存在性和类型
2. **运行时验证**：应用启动时验证必需的环境变量，早期发现问题
3. **更好的可维护性**：所有环境变量集中管理，有文档说明
4. **更好的开发体验**：IDE 自动补全环境变量名
5. **避免常见错误**：防止 `undefined.trim()` 等运行时错误

---

## 🟡 Minor 次要问题 (建议修复)

### 7. 事件处理函数缺少明确类型

**问题描述：**
一些事件处理函数没有明确的类型定义，依赖 TypeScript 的类型推断。

**影响：**
- 类型推断可能不准确，特别是在复杂场景下
- 代码可读性降低，不熟悉代码的开发者难以理解参数类型
- 重构时容易引入类型错误

**涉及文件：**
- `frontend/src/components/layout/MainLayout.tsx` (第 62-67 行)
- `frontend/src/pages/Player.tsx` (第 187-191 行)
- 其他事件处理函数

**问题代码示例：**
```tsx
// ❌ 事件类型不明确
const handleSearch = (e) => {
  e.preventDefault()
  // ...
}

// ❌ 内联事件处理，类型不明确
onChange={(e) => {
  setVolume(parseFloat(e.target.value))
  if (parseFloat(e.target.value) > 0) setIsMuted(false)
}}
```

**修复建议：**

为所有事件处理函数添加明确的类型定义。

#### 7.1 常见事件类型

以下是 React 中常见的事件类型：

| 事件类型 | 类型定义 | 说明 |
|---------|---------|------|
| 点击事件 | `React.MouseEvent<HTMLButtonElement>` | 按钮点击 |
| 表单提交 | `React.FormEvent<HTMLFormElement>` | 表单提交 |
| 输入变化 | `React.ChangeEvent<HTMLInputElement>` | 输入框值变化 |
| 选择变化 | `React.ChangeEvent<HTMLSelectElement>` | 下拉选择变化 |
| 键盘事件 | `React.KeyboardEvent<HTMLInputElement>` | 键盘按键 |
| 聚焦事件 | `React.FocusEvent<HTMLInputElement>` | 元素聚焦/失焦 |
| 拖拽事件 | `React.DragEvent<HTMLDivElement>` | 拖拽操作 |
| 剪贴板事件 | `React.ClipboardEvent<HTMLInputElement>` | 复制/粘贴/剪切 |
| 合成事件 | `React.CompositionEvent<HTMLInputElement>` | 输入法输入 |

#### 7.2 修复后的代码示例

**修复 `handleSearch`：**
```tsx
// ✅ 明确的事件类型
const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault()
  if (searchKeyword.trim()) {
    addRecentSearch(searchKeyword)
  }
}
```

**修复音量滑块的 `onChange`：**
```tsx
// ✅ 明确的事件类型
const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  const newVolume = parseFloat(e.target.value)
  setVolume(newVolume)
  if (newVolume > 0) {
    setIsMuted(false)
  }
}

// 使用
<input
  type="range"
  min="0"
  max="1"
  step="0.01"
  value={isMuted ? 0 : volume}
  onChange={handleVolumeChange}
  className="w-full"
  aria-label="音量"
/>
```

**修复复选框的 `onChange`：**
```tsx
// ✅ 明确的事件类型
const handleRecursiveChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  setScanRecursive(e.target.checked)
}

// 使用
<label className="flex items-center gap-2">
  <input
    type="checkbox"
    checked={scanRecursive}
    onChange={handleRecursiveChange}
    disabled={isScanning}
    className="rounded"
    aria-label="递归扫描子文件夹"
  />
  <span className="text-sm">递归扫描子文件夹</span>
</label>
```

#### 7.3 最佳实践

1. **优先使用具名函数**：
   ```tsx
   // ✅ 具名函数，便于调试和测试
   const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
     console.log('Button clicked', e.currentTarget)
   }
   
   <button onClick={handleClick}>点击</button>
   ```

2. **提取复杂的事件处理逻辑**：
   ```tsx
   // ✅ 提取到自定义 hook 或单独的函数
   function useFormSubmit<T>(onSubmit: (data: T) => void) {
     return (e: React.FormEvent<HTMLFormElement>) => {
       e.preventDefault()
       const formData = new FormData(e.currentTarget)
       const data = Object.fromEntries(formData) as T
       onSubmit(data)
     }
   }
   ```

3. **为自定义事件处理器定义类型**：
   ```tsx
   // ✅ 定义自定义事件类型
   type VideoSelectHandler = (
     videoId: number,
     event: React.MouseEvent<HTMLDivElement>
   ) => void
   
   interface VideoCardProps {
     video: Video
     onSelect: VideoSelectHandler
   }
   ```

4. **使用类型守卫处理联合类型事件**：
   ```tsx
   // ✅ 使用类型守卫
   function isInputEvent(
     e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
   ): e is React.ChangeEvent<HTMLInputElement> {
     return e.currentTarget.tagName === 'INPUT'
   }
   
   const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
     if (isInputEvent(e)) {
       // 这里 e 被类型推断为 HTMLInputElement
       console.log('Input value:', e.target.value)
     } else {
       // 这里 e 被类型推断为 HTMLSelectElement
       console.log('Select value:', e.target.value)
     }
   }
   ```

**修复后的收益：**
1. **更好的类型安全**：TypeScript 会在编译时检查事件类型
2. **更好的代码可读性**：其他开发者可以立即理解参数类型
3. **更好的 IDE 支持**：自动补全和类型提示更准确
4. **更少的运行时错误**：类型错误在编译时被捕获
5. **更容易重构**：修改事件处理逻辑时，类型系统提供保护

---

### 8. 没有使用 `cn()` 函数合并类名

**问题描述：**
虽然项目中安装了 `clsx` 和 `tailwind-merge`，但没有使用 `cn()` 函数来合并条件类名，而是使用字符串模板或直接的逻辑表达式。

**影响：**
- 类名合并可能不正确，特别是当有冲突的 Tailwind 类时
- 代码可读性差，条件类名逻辑分散
- 容易出现重复的类名
- 没有利用 `tailwind-merge` 的智能合并功能

**涉及文件：**
- 整个项目，特别是：
  - `frontend/src/pages/Home.tsx`
  - `frontend/src/components/layout/MainLayout.tsx`
  - 所有使用条件类名的组件

**问题代码示例：**
```tsx
// ❌ 使用字符串模板，类名可能冲突
<Button className={`bg-blue-500 ${isActive ? 'opacity-100' : 'opacity-50'}`} />

// ❌ 使用逻辑或，可能产生 'undefined' 字符串
<div className={isActive && 'active'} /> // 当 isActive 为 false 时，className 为 false

// ❌ 多个类名条件，难以维护
<Link
  className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
    isActive
      ? 'bg-blue-50 text-blue-600'
      : 'text-gray-600 hover:bg-gray-100'
  }`}
/>
```

**修复建议：**

#### 8.1 创建或完善 `cn()` 函数

**检查 `frontend/src/utils/cn.ts`：**
```typescript
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * 合并类名，自动处理 Tailwind CSS 类名冲突
 * 结合了 clsx 的条件类名和 tailwind-merge 的智能合并
 * 
 * @example
 * cn('bg-red-500', isActive && 'bg-blue-500')
 * // 当 isActive 为 true 时，返回 'bg-blue-500'（自动处理冲突）
 * 
 * @example
 * cn('px-2 py-1', { 'px-4': isLarge }, className)
 * // 支持对象语法和可选的 className prop
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

**确保在 `tsconfig.json` 中配置了路径别名：**
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

#### 8.2 使用 `cn()` 函数重构代码

**修复后的代码示例：**

```tsx
import { cn } from '@/utils/cn'

// ✅ 使用 cn() 合并类名
<Button className={cn('bg-blue-500', isActive ? 'opacity-100' : 'opacity-50')} />

// ✅ 条件类名更清晰
<div className={cn(
  'flex items-center gap-3 px-3 py-2 rounded-lg transition-colors',
  isActive 
    ? 'bg-blue-50 text-blue-600' 
    : 'text-gray-600 hover:bg-gray-100'
)}>
  内容
</div>

// ✅ 支持对象语法
<button
  className={cn(
    'px-4 py-2 rounded',
    {
      'bg-blue-600 text-white': variant === 'primary',
      'bg-gray-200 text-gray-800': variant === 'secondary',
      'opacity-50 cursor-not-allowed': disabled,
    },
    className // 支持传入额外的 className
  )}
  disabled={disabled}
>
  {children}
</button>

// ✅ 处理数组和 undefined
<div className={cn(
  'base-class',
  isActive && 'active-class', // 条件为 false 时自动忽略
  isLarge ? 'large' : 'small',
  customClassName // 可能为 undefined，自动忽略
)} />
```

#### 8.3 重构具体组件

**重构 `Home.tsx` 中的 `VideoCard`：**
```tsx
// 之前
const VideoCard = ({ video }: { video: Video }) => (
  <Card className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group">
    {/* ... */}
  </Card>
)

// 之后
interface VideoCardProps {
  video: Video
  className?: string
  onClick?: () => void
}

const VideoCard = ({ video, className, onClick }: VideoCardProps) => (
  <Card 
    className={cn(
      'overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group',
      className // 支持传入额外的 className
    )}
    onClick={onClick}
    role="button"
    tabIndex={0}
  >
    <div className="relative aspect-video bg-gray-900">
      <div className="absolute inset-0 flex items-center justify-center">
        <FileVideo className="w-16 h-16 text-gray-600" />
      </div>
      <div className={cn(
        'absolute inset-0 bg-black/50 transition-opacity flex items-center justify-center',
        'opacity-0 group-hover:opacity-100' // 条件类名
      )}>
        <Play className="w-16 h-16 text-white" />
      </div>
    </div>
    {/* ... */}
  </Card>
)
```

**重构 `MainLayout.tsx` 中的导航链接：**
```tsx
// 之前
<Link
  to={item.path}
  className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${
    isActive
      ? 'bg-blue-50 text-blue-600'
      : 'text-gray-600 hover:bg-gray-100'
  }`}
>
  {/* ... */}
</Link>

// 之后
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
```

#### 8.4 `cn()` 函数的优势

1. **自动处理 Tailwind 类名冲突**：
   ```tsx
   // 传统方式，bg-blue-500 和 bg-red-500 都会保留
   <div className={`bg-blue-500 ${isError && 'bg-red-500'}`} />
   
   // 使用 cn()，自动保留最后一个有效的类
   <div className={cn('bg-blue-500', isError && 'bg-red-500')} />
   // 当 isError 为 true 时，只保留 'bg-red-500'
   ```

2. **支持多种输入格式**：
   ```tsx
   cn('foo', 'bar') // => 'foo bar'
   cn('foo', { bar: true, baz: false }) // => 'foo bar'
   cn('foo', undefined, false, 'bar') // => 'foo bar'（自动忽略 falsy 值）
   cn(['foo', 'bar']) // => 'foo bar'（支持数组）
   ```

3. **更好的类型安全**：
   ```tsx
   // cn() 接受 ClassValue 类型，包括：
   // - string
   // - number
   // - boolean
   // - undefined
   // - null
   // - ClassValue[]
   // - { [key: string]: boolean | undefined | null }
   ```

**修复后的收益：**
1. **正确的类名合并**：自动处理 Tailwind 类名冲突
2. **更好的代码可读性**：条件类名逻辑更清晰
3. **更少的错误**：自动忽略 falsy 值，避免 'false' 或 'undefined' 字符串
4. **更好的可维护性**：统一的类名合并方式
5. **支持组件扩展**：方便传入额外的 className prop

---

### 9. 缺少错误边界

**问题描述：**
没有使用 `<ErrorBoundary>` 来捕获组件树中的错误，可能导致整个应用崩溃。

**影响：**
- 单个组件的错误会导致整个应用白屏
- 用户体验差，没有友好的错误提示
- 难以调试和定位错误
- 不符合现代前端应用的健壮性要求

**涉及文件：**
- `frontend/src/App.tsx` (整个应用)
- 所有路由组件

**问题分析：**

在 React 中，如果一个组件在渲染过程中抛出错误，且没有被错误边界捕获，会导致：
1. 整个组件树被卸载
2. 用户看到白屏
3. 没有任何错误提示
4. 应用需要刷新才能恢复

**修复建议：**

#### 9.1 安装 `react-error-boundary` 库（推荐）

```bash
cd frontend
npm install react-error-boundary
```

#### 9.2 创建错误边界组件

**创建 `frontend/src/components/ErrorBoundary.tsx`：**
```tsx
import { Component, ReactNode } from 'react'
import { ErrorBoundary as ReactErrorBoundary, FallbackProps } from 'react-error-boundary'
import { AlertTriangle, RefreshCw, Home } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

// ============================================================================
// 错误回退组件（Error Fallback Component）
// 当子组件抛出错误时显示
// ============================================================================

interface ErrorFallbackProps extends FallbackProps {
  /** 错误标题 */
  title?: string
  /** 是否显示重置按钮 */
  showReset?: boolean
  /** 是否显示返回首页按钮 */
  showHome?: boolean
}

/**
 * 通用错误回退组件
 * 显示友好的错误信息和操作按钮
 */
export function ErrorFallback({
  error,
  resetErrorBoundary,
  title = '出现了一些问题',
  showReset = true,
  showHome = true,
}: ErrorFallbackProps) {
  // 开发环境显示详细错误信息
  const isDevelopment = import.meta.env.MODE === 'development'
  
  // 处理重置操作
  const handleReset = () => {
    resetErrorBoundary?.()
  }
  
  // 处理返回首页
  const handleGoHome = () => {
    window.location.href = '/'
  }
  
  return (
    <div className="min-h-[400px] flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mb-4">
            <AlertTriangle className="w-8 h-8 text-red-600" aria-hidden="true" />
          </div>
          <CardTitle className="text-xl font-bold text-gray-900">
            {title}
          </CardTitle>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {/* 错误描述 */}
          <p className="text-center text-gray-600">
            {error?.message || '页面加载时出现意外错误'}
          </p>
          
          {/* 开发环境显示详细错误 */}
          {isDevelopment && error?.stack && (
            <details className="mt-4">
              <summary className="cursor-pointer text-sm text-gray-500 hover:text-gray-700">
                查看详细错误信息
              </summary>
              <pre className="mt-2 p-4 bg-gray-100 rounded-lg text-xs overflow-auto max-h-64 text-gray-800">
                {error.stack}
              </pre>
            </details>
          )}
          
          {/* 操作按钮 */}
          <div className="flex gap-2 justify-center pt-4">
            {showReset && (
              <Button
                variant="default"
                onClick={handleReset}
                className="gap-2"
              >
                <RefreshCw className="w-4 h-4" aria-hidden="true" />
                重试
              </Button>
            )}
            
            {showHome && (
              <Button
                variant="outline"
                onClick={handleGoHome}
                className="gap-2"
              >
                <Home className="w-4 h-4" aria-hidden="true" />
                返回首页
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

// ============================================================================
// 页面级别错误边界（Page Error Boundary）
// 用于包裹路由组件
// ============================================================================

interface PageErrorBoundaryProps {
  children: ReactNode
  /** 页面名称，用于错误日志 */
  pageName?: string
}

/**
 * 页面级别错误边界
 * 当整个页面出错时显示友好的错误信息
 */
export function PageErrorBoundary({ children, pageName }: PageErrorBoundaryProps) {
  // 记录错误到控制台（可以扩展为发送到监控服务）
  const handleError = (error: Error, info: { componentStack: string }) => {
    console.error(`[Page Error] ${pageName || 'Unknown page'}:`, error)
    console.error('Component stack:', info.componentStack)
    
    // 可以在这里添加错误监控上报
    // errorTracking.captureException(error, {
    //   extra: { pageName, componentStack: info.componentStack }
    // })
  }
  
  return (
    <ReactErrorBoundary
      FallbackComponent={ErrorFallback}
      onError={handleError}
    >
      {children}
    </ReactErrorBoundary>
  )
}

// ============================================================================
// 组件级别错误边界（Component Error Boundary）
// 用于包裹特定组件，防止单个组件错误影响整个页面
// ============================================================================

interface ComponentErrorBoundaryProps {
  children: ReactNode
  /** 组件名称，用于错误日志 */
  componentName?: string
  /** 自定义回退渲染 */
  fallback?: ReactNode | ((props: FallbackProps) => ReactNode)
}

/**
 * 组件级别错误边界
 * 用于包裹可能出错的组件，提供更细粒度的错误处理
 */
export function ComponentErrorBoundary({
  children,
  componentName,
  fallback,
}: ComponentErrorBoundaryProps) {
  const handleError = (error: Error, info: { componentStack: string }) => {
    console.error(`[Component Error] ${componentName || 'Unknown component'}:`, error)
  }
  
  // 简单的回退组件
  const SimpleFallback = ({ resetErrorBoundary }: FallbackProps) => (
    <div className="p-4 border border-red-200 bg-red-50 rounded-lg">
      <p className="text-sm text-red-600">
        组件加载失败
        <button
          onClick={resetErrorBoundary}
          className="ml-2 underline hover:text-red-800"
        >
          重试
        </button>
      </p>
    </div>
  )
  
  return (
    <ReactErrorBoundary
      FallbackComponent={typeof fallback === 'function' ? fallback as any : SimpleFallback}
      onError={handleError}
    >
      {children}
    </ReactErrorBoundary>
  )
}

// ============================================================================
// 类组件错误边界（Class Component Error Boundary）
// 用于需要完全控制错误边界行为的场景
// ============================================================================

interface ClassErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

/**
 * 类组件实现的错误边界
 * 可以更细粒度地控制错误处理逻辑
 */
export class ClassErrorBoundary extends Component<
  { children: ReactNode; fallback?: ReactNode },
  ClassErrorBoundaryState
> {
  constructor(props: { children: ReactNode; fallback?: ReactNode }) {
    super(props)
    this.state = { hasError: false, error: null }
  }
  
  static getDerivedStateFromError(error: Error): ClassErrorBoundaryState {
    // 更新 state 使下一次渲染显示回退 UI
    return { hasError: true, error }
  }
  
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // 可以在这里记录错误日志
    console.error('Error caught by boundary:', error)
    console.error('Component stack:', errorInfo.componentStack)
  }
  
  render() {
    if (this.state.hasError) {
      // 可以渲染任何自定义的回退 UI
      return this.props.fallback || (
        <div className="p-4 text-center text-gray-500">
          <p>出现了一些问题</p>
        </div>
      )
    }
    
    return this.props.children
  }
}
```

#### 9.3 在 App 组件中使用错误边界

**修改 `frontend/src/App.tsx`：**
```tsx
import { BrowserRouter } from 'react-router-dom'
import { AppRouter } from './router'
import { ToastContainer } from '@/components/ui/toast-container'
import { PageErrorBoundary } from '@/components/ErrorBoundary'

function App() {
  return (
    <BrowserRouter>
      {/* 全局错误边界，包裹整个应用 */}
      <PageErrorBoundary pageName="App">
        <AppRouter />
        <ToastContainer />
      </PageErrorBoundary>
    </BrowserRouter>
  )
}

export default App
```

#### 9.4 在路由级别使用错误边界

**修改 `frontend/src/router.tsx`：**
```tsx
import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'
import { PageErrorBoundary } from '@/components/ErrorBoundary'

// 懒加载路由组件
const Home = lazy(() => import('@/pages/Home'))
const Player = lazy(() => import('@/pages/Player'))
const Favorites = lazy(() => import('@/pages/Favorites'))
const History = lazy(() => import('@/pages/History'))
const Tags = lazy(() => import('@/pages/Tags'))
const Settings = lazy(() => import('@/pages/Settings'))

// 页面加载骨架屏
function PageSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="aspect-video bg-gray-200 rounded-lg"></div>
        ))}
      </div>
    </div>
  )
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        {/* 每个路由都有自己的错误边界 */}
        <Route index element={
          <PageErrorBoundary pageName="Home">
            <Suspense fallback={<PageSkeleton />}>
              <Home />
            </Suspense>
          </PageErrorBoundary>
        } />
        
        <Route path="player/:id" element={
          <PageErrorBoundary pageName="Player">
            <Suspense fallback={<PageSkeleton />}>
              <Player />
            </Suspense>
          </PageErrorBoundary>
        } />
        
        <Route path="favorites" element={
          <PageErrorBoundary pageName="Favorites">
            <Suspense fallback={<PageSkeleton />}>
              <Favorites />
            </Suspense>
          </PageErrorBoundary>
        } />
        
        <Route path="history" element={
          <PageErrorBoundary pageName="History">
            <Suspense fallback={<PageSkeleton />}>
              <History />
            </Suspense>
          </PageErrorBoundary>
        } />
        
        <Route path="tags" element={
          <PageErrorBoundary pageName="Tags">
            <Suspense fallback={<PageSkeleton />}>
              <Tags />
            </Suspense>
          </PageErrorBoundary>
        } />
        
        <Route path="settings" element={
          <PageErrorBoundary pageName="Settings">
            <Suspense fallback={<PageSkeleton />}>
              <Settings />
            </Suspense>
          </PageErrorBoundary>
        } />
      </Route>
    </Routes>
  )
}
```

#### 9.5 在组件级别使用错误边界

**示例：在可能出错的组件外包裹错误边界**
```tsx
import { ComponentErrorBoundary } from '@/components/ErrorBoundary'
import { VideoChart } from './VideoChart'

function VideoAnalytics() {
  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold">视频数据分析</h2>
      
      {/* 使用 ComponentErrorBoundary 包裹可能出错的图表组件 */}
      <ComponentErrorBoundary componentName="VideoChart">
        <VideoChart />
      </ComponentErrorBoundary>
      
      {/* 即使图表组件出错，其他内容仍然可以正常显示 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 bg-white rounded-lg border">
          <h3 className="font-medium">总播放量</h3>
          <p className="text-2xl font-bold text-blue-600">1,234</p>
        </div>
        {/* 其他统计卡片 */}
      </div>
    </div>
  )
}
```

#### 9.6 错误边界的最佳实践

1. **分层使用错误边界**：
   - **全局级别**：`App.tsx` 中的错误边界，作为最后的防线
   - **页面级别**：每个路由组件的错误边界
   - **组件级别**：特定高风险组件的错误边界

2. **错误边界不能捕获的错误**：
   - 事件处理器中的错误（使用 `try-catch`）
   - 异步代码中的错误（使用 `try-catch` 或 Promise.catch）
   - 服务端渲染期间的错误
   - 错误边界组件自身抛出的错误

3. **配合其他错误处理机制**：
   ```tsx
   // 事件处理器中的错误使用 try-catch
   const handleClick = async () => {
     try {
       await fetchData()
     } catch (error) {
       console.error('操作失败:', error)
       // 显示错误提示
       toast.error('操作失败，请重试')
     }
   }
   
   // 异步组件中的错误使用 error boundary + try-catch
   function AsyncComponent() {
     const [data, setData] = useState(null)
     const [error, setError] = useState<Error | null>(null)
     
     useEffect(() => {
       fetch('/api/data')
         .then(res => res.json())
         .then(setData)
         .catch(setError) // 捕获异步错误
     }, [])
     
     if (error) {
       throw error // 抛出错误让 ErrorBoundary 捕获
     }
     
     return <div>{data?.value}</div>
   }
   ```

4. **错误恢复策略**：
   - 提供"重试"按钮（`resetErrorBoundary`）
   - 提供"返回首页"按钮
   - 记录错误日志以便分析

**修复后的收益：**
1. **更好的用户体验**：单个组件错误不会导致整个应用白屏
2. **友好的错误提示**：用户知道发生了什么，可以采取行动
3. **更容易调试**：错误被捕获并记录，可以定位问题
4. **更健壮的应用**：多层错误边界提供保护
5. **更好的可维护性**：错误处理逻辑集中管理

---

### 10. API 响应处理不够健壮

**问题描述：**
API 响应拦截器只是简单地打印错误并拒绝，没有统一的错误处理逻辑。

**影响：**
- 错误处理分散在各个组件中
- 没有统一的错误提示
- 没有处理常见的 HTTP 错误状态码
- 没有错误重试机制
- 难以维护和扩展

**涉及文件：**
- `frontend/src/services/api.ts` (第 12-20 行)
- 所有调用 API 的组件

**问题代码：**
```typescript
// frontend/src/services/api.ts
api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    return response
  },
  (error) => {
    // ❌ 只是简单地打印错误并拒绝
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)
```

**组件中的问题：**
```tsx
// 在组件中手动处理错误，代码重复
try {
  const response = await videoService.getVideos({ page: 1, size: 20 })
  setVideos(response.list)
} catch (error) {
  // ❌ 每个组件都要手动处理
  console.error('Failed to load videos:', error)
  setVideos([])
  // ❌ 没有统一的错误提示
}
```

**修复建议：**

#### 10.1 创建统一的 API 错误处理系统

**创建 `frontend/src/services/errorHandler.ts`：**
```typescript
import { AxiosError } from 'axios'
import { ApiResponse } from '@/types'

// ============================================================================
// 错误类型定义
// ============================================================================

/**
 * API 错误类型
 */
export type ApiErrorType = 
  | 'network_error'      // 网络错误（无法连接服务器）
  | 'timeout_error'      // 请求超时
  | 'client_error'       // 客户端错误（4xx）
  | 'server_error'       // 服务端错误（5xx）
  | 'business_error'     // 业务错误（接口返回非成功 code）
  | 'unknown_error'      // 未知错误

/**
 * 标准化的 API 错误
 */
export interface ApiError {
  /** 错误类型 */
  type: ApiErrorType
  /** HTTP 状态码 */
  status?: number
  /** 错误消息（用户友好） */
  message: string
  /** 详细错误消息（调试用） */
  detail?: string
  /** 业务错误码 */
  code?: number
  /** 原始错误对象 */
  originalError?: unknown
}

// ============================================================================
// 错误消息映射
// ============================================================================

/**
 * HTTP 状态码到错误消息的映射
 */
const HTTP_STATUS_MESSAGES: Record<number, string> = {
  400: '请求参数错误，请检查输入',
  401: '未登录或登录已过期，请重新登录',
  403: '没有权限执行此操作',
  404: '请求的资源不存在',
  408: '请求超时，请稍后重试',
  409: '资源冲突，可能已被修改',
  422: '请求数据验证失败',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误，请稍后重试',
  502: '网关错误，请稍后重试',
  503: '服务暂时不可用，请稍后重试',
  504: '网关超时，请稍后重试',
}

/**
 * 获取用户友好的错误消息
 */
function getErrorMessage(status?: number, defaultMessage: string = '操作失败'): string {
  if (status && HTTP_STATUS_MESSAGES[status]) {
    return HTTP_STATUS_MESSAGES[status]
  }
  return defaultMessage
}

// ============================================================================
// 错误转换器
// ============================================================================

/**
 * 将 Axios 错误转换为标准化的 ApiError
 */
export function convertAxiosError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  // 网络错误（无法连接服务器）
  if (error.code === 'ECONNABORTED' || error.message === 'Network Error') {
    return {
      type: 'network_error',
      message: '网络连接失败，请检查网络后重试',
      detail: error.message,
      originalError: error,
    }
  }
  
  // 请求超时
  if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
    return {
      type: 'timeout_error',
      message: '请求超时，请稍后重试',
      detail: error.message,
      originalError: error,
    }
  }
  
  // 有响应的错误
  if (error.response) {
    const { status, data } = error.response
    
    // 客户端错误（4xx）
    if (status >= 400 && status < 500) {
      // 检查是否是业务错误（有业务 code 和 message）
      if (data && typeof data.code === 'number' && data.message) {
        return {
          type: 'business_error',
          status,
          code: data.code,
          message: data.message,
          detail: JSON.stringify(data),
          originalError: error,
        }
      }
      
      return {
        type: 'client_error',
        status,
        message: getErrorMessage(status, '请求参数错误'),
        detail: JSON.stringify(data),
        originalError: error,
      }
    }
    
    // 服务端错误（5xx）
    if (status >= 500 && status < 600) {
      return {
        type: 'server_error',
        status,
        message: getErrorMessage(status, '服务器内部错误'),
        detail: JSON.stringify(data),
        originalError: error,
      }
    }
  }
  
  // 未知错误
  return {
    type: 'unknown_error',
    message: '发生未知错误，请稍后重试',
    detail: error.message,
    originalError: error,
  }
}

// ============================================================================
// 错误处理器
// ============================================================================

/**
 * 错误处理回调函数类型
 */
export type ErrorHandlerCallback = (error: ApiError) => void

/**
 * 全局错误处理器
 * 可以注册回调函数来处理不同类型的错误
 */
class ErrorHandler {
  private handlers: Map<ApiErrorType, ErrorHandlerCallback[]> = new Map()
  private globalHandlers: ErrorHandlerCallback[] = []
  
  /**
   * 注册特定类型错误的处理器
   */
  on(type: ApiErrorType, callback: ErrorHandlerCallback): () => void {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, [])
    }
    this.handlers.get(type)!.push(callback)
    
    // 返回取消注册的函数
    return () => {
      const handlers = this.handlers.get(type)
      if (handlers) {
        const index = handlers.indexOf(callback)
        if (index > -1) {
          handlers.splice(index, 1)
        }
      }
    }
  }
  
  /**
   * 注册全局错误处理器
   */
  onAny(callback: ErrorHandlerCallback): () => void {
    this.globalHandlers.push(callback)
    return () => {
      const index = this.globalHandlers.indexOf(callback)
      if (index > -1) {
        this.globalHandlers.splice(index, 1)
      }
    }
  }
  
  /**
   * 处理错误
   */
  handle(error: ApiError): void {
    // 调用全局处理器
    this.globalHandlers.forEach((handler) => {
      try {
        handler(error)
      } catch (e) {
        console.error('Error handler failed:', e)
      }
    })
    
    // 调用特定类型的处理器
    const typeHandlers = this.handlers.get(error.type)
    if (typeHandlers) {
      typeHandlers.forEach((handler) => {
        try {
          handler(error)
        } catch (e) {
          console.error('Error handler failed:', e)
        }
      })
    }
    
    // 控制台输出（开发环境）
    if (import.meta.env.MODE === 'development') {
      console.group('🔴 API Error')
      console.log('Type:', error.type)
      console.log('Status:', error.status)
      console.log('Message:', error.message)
      console.log('Detail:', error.detail)
      console.log('Original:', error.originalError)
      console.groupEnd()
    }
  }
}

// 导出单例
export const errorHandler = new ErrorHandler()

// ============================================================================
// 便捷方法
// ============================================================================

/**
 * 处理 API 错误的便捷函数
 * 可以直接在 catch 块中使用
 */
export function handleApiError(
  error: unknown,
  options?: {
    /** 自定义错误消息 */
    customMessage?: string
    /** 是否显示错误提示（默认 true） */
    showToast?: boolean
    /** 回调函数 */
    onError?: (apiError: ApiError) => void
  }
): ApiError {
  const { customMessage, showToast = true, onError } = options || {}
  
  // 转换错误
  let apiError: ApiError
  
  if (error && typeof error === 'object' && 'isAxiosError' in error) {
    apiError = convertAxiosError(error as AxiosError<ApiResponse<unknown>>)
  } else if (error instanceof Error) {
    apiError = {
      type: 'unknown_error',
      message: customMessage || error.message || '操作失败',
      detail: error.stack,
      originalError: error,
    }
  } else {
    apiError = {
      type: 'unknown_error',
      message: customMessage || '操作失败',
      originalError: error,
    }
  }
  
  // 应用自定义消息
  if (customMessage) {
    apiError = { ...apiError, message: customMessage }
  }
  
  // 调用回调
  onError?.(apiError)
  
  // 显示错误提示
  if (showToast) {
    // 这里可以集成 toast 通知
    // 例如：toast.error(apiError.message)
    console.error('API Error:', apiError.message)
  }
  
  return apiError
}

/**
 * 创建带错误处理的异步函数包装器
 */
export function withErrorHandling<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  options?: {
    customMessage?: string
    showToast?: boolean
  }
): (...args: Parameters<T>) => Promise<ReturnType<T> | null> {
  return async (...args: Parameters<T>) => {
    try {
      return await fn(...args)
    } catch (error) {
      handleApiError(error, options)
      return null
    }
  }
}
```

#### 10.2 更新 API 配置

**修改 `frontend/src/services/api.ts`：**
```typescript
import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ApiResponse } from '@/types'
import { convertAxiosError, errorHandler, ApiError } from './errorHandler'
import { env } from '@/lib/env'

// ============================================================================
// 创建 Axios 实例
// ============================================================================

const api: AxiosInstance = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: env.apiTimeout,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ============================================================================
// 请求拦截器
// ============================================================================

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 可以在这里添加认证 token
    // const token = localStorage.getItem('token')
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`
    // }
    
    // 添加请求时间戳（用于调试）
    if (import.meta.env.MODE === 'development') {
      console.log(`📤 [Request] ${config.method?.toUpperCase()} ${config.url}`)
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ============================================================================
// 响应拦截器
// ============================================================================

api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    // 开发环境日志
    if (import.meta.env.MODE === 'development') {
      console.log(`📥 [Response] ${response.status} ${response.config.url}`)
    }
    
    // 检查业务状态码
    const data = response.data
    if (data && typeof data.code === 'number') {
      // 假设 code = 0 或 200 表示成功
      if (data.code !== 0 && data.code !== 200) {
        // 业务错误，创建一个错误对象
        const businessError: ApiError = {
          type: 'business_error',
          status: response.status,
          code: data.code,
          message: data.message || '操作失败',
          detail: JSON.stringify(data),
        }
        
        // 处理错误
        errorHandler.handle(businessError)
        
        // 拒绝 Promise，让调用者可以 catch
        return Promise.reject(businessError)
      }
    }
    
    return response
  },
  (error) => {
    // 转换并处理错误
    const apiError = convertAxiosError(error)
    errorHandler.handle(apiError)
    
    // 拒绝 Promise，让调用者可以 catch
    return Promise.reject(apiError)
  }
)

// ============================================================================
// 便捷方法
// ============================================================================

/**
 * 获取响应数据的便捷方法
 * 自动处理 ApiResponse 包装
 */
export async function getResponseData<T>(response: AxiosResponse<ApiResponse<T>>): Promise<T> {
  const data = response.data
  
  // 如果有 data 字段，返回它
  if (data && 'data' in data) {
    return data.data
  }
  
  // 否则返回整个响应体
  return data as unknown as T
}

/**
 * 发送 GET 请求并直接返回数据
 */
export async function get<T>(url: string, config?: object): Promise<T> {
  const response = await api.get<ApiResponse<T>>(url, config)
  return getResponseData(response)
}

/**
 * 发送 POST 请求并直接返回数据
 */
export async function post<T>(url: string, data?: object, config?: object): Promise<T> {
  const response = await api.post<ApiResponse<T>>(url, data, config)
  return getResponseData(response)
}

/**
 * 发送 PUT 请求并直接返回数据
 */
export async function put<T>(url: string, data?: object, config?: object): Promise<T> {
  const response = await api.put<ApiResponse<T>>(url, data, config)
  return getResponseData(response)
}

/**
 * 发送 DELETE 请求并直接返回数据
 */
export async function del<T>(url: string, config?: object): Promise<T> {
  const response = await api.delete<ApiResponse<T>>(url, config)
  return getResponseData(response)
}

export default api
```

#### 10.3 在应用入口注册全局错误处理器

**修改 `frontend/src/App.tsx` 或创建初始化文件：**
```typescript
// frontend/src/lib/initErrorHandler.ts
import { errorHandler, ApiError, ApiErrorType } from '@/services/errorHandler'
import { useToastStore } from '@/store'

/**
 * 初始化全局错误处理器
 * 在应用启动时调用
 */
export function initErrorHandler() {
  // 获取 toast store（需要在组件外使用时可以用 getState）
  const getToastStore = () => useToastStore.getState()
  
  // 1. 全局错误处理器：显示 toast 提示
  errorHandler.onAny((error: ApiError) => {
    const { error: toastError } = getToastStore()
    
    // 根据错误类型决定是否显示 toast
    // 某些错误（如 401）可能需要特殊处理，不显示 toast
    if (error.type !== 'client_error' || error.status !== 401) {
      toastError(error.message)
    }
  })
  
  // 2. 401 未授权错误：跳转到登录页
  const handleUnauthorized = (error: ApiError) => {
    if (error.status === 401) {
      // 清除本地存储的认证信息
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      
      // 跳转到登录页（如果有）
      // window.location.href = '/login'
      
      console.warn('用户未授权，需要重新登录')
    }
  }
  
  errorHandler.on('client_error', handleUnauthorized)
  
  // 3. 网络错误：特殊提示
  errorHandler.on('network_error', (error: ApiError) => {
    console.warn('网络连接失败，请检查网络')
    // 可以在这里添加离线状态管理
  })
  
  // 4. 服务端错误：记录日志
  errorHandler.on('server_error', (error: ApiError) => {
    // 可以在这里上报错误到监控服务
    console.error('服务端错误:', error)
  })
  
  console.log('✅ 错误处理器初始化完成')
}
```

**在 `main.tsx` 中初始化：**
```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'
import { initErrorHandler } from '@/lib/initErrorHandler'

// 初始化错误处理器
initErrorHandler()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

#### 10.4 在组件中使用

**示例：重构 `Home.tsx` 中的数据获取**
```tsx
import { useEffect, useCallback } from 'react'
import { useVideoStore, useToastStore } from '@/store'
import { videoService } from '@/services'
import { handleApiError } from '@/services/errorHandler'

export default function Home() {
  const { videos, loading, setVideos, setLoading, setPagination } = useVideoStore()
  const { error: toastError } = useToastStore()
  
  const loadVideos = useCallback(async () => {
    setLoading(true)
    try {
      const response = await videoService.getVideos({ page: 1, size: 20 })
      setVideos(response.list)
      setPagination(response)
    } catch (error) {
      // 使用统一的错误处理
      const apiError = handleApiError(error, {
        customMessage: '加载视频列表失败',
        showToast: true,
      })
      
      // 可以在这里添加额外的错误处理
      setVideos([])
    } finally {
      setLoading(false)
    }
  }, [setVideos, setLoading, setPagination])
  
  useEffect(() => {
    loadVideos()
  }, [loadVideos])
  
  // 其余代码...
}
```

**使用 `withErrorHandling` 包装器：**
```tsx
import { withErrorHandling } from '@/services/errorHandler'
import { videoService } from '@/services'

// 创建带错误处理的函数
const safeGetVideos = withErrorHandling(
  (params: { page: number; size: number }) => videoService.getVideos(params),
  { customMessage: '加载视频失败' }
)

// 使用
const response = await safeGetVideos({ page: 1, size: 20 })
if (response) {
  // 成功
  setVideos(response.list)
}
// 错误已经被处理，不需要 catch
```

#### 10.5 错误处理的最佳实践

1. **分层错误处理**：
   - **拦截器层**：统一转换错误格式，处理通用逻辑
   - **全局处理器**：处理 401、403 等通用错误
   - **组件层**：处理特定业务场景的错误

2. **错误分类处理**：
   - **网络错误**：提示检查网络，提供重试按钮
   - **超时错误**：提示请求超时，提供重试按钮
   - **401 错误**：清除认证信息，跳转登录页
   - **403 错误**：提示无权限，引导联系管理员
   - **404 错误**：提示资源不存在，引导返回
   - **5xx 错误**：提示服务端错误，提供重试按钮

3. **用户友好的错误消息**：
   - 避免技术术语（如 "404 Not Found"）
   - 使用用户能理解的语言（如 "您访问的页面不存在"）
   - 提供明确的操作指引（如 "请检查网络连接"、"请稍后重试"）

4. **错误记录和监控**：
   - 开发环境：详细的控制台日志
   - 生产环境：上报到错误监控服务（如 Sentry）
   - 记录错误上下文：用户操作、请求参数、响应状态

**修复后的收益：**
1. **统一的错误处理**：所有 API 错误经过统一转换和处理
2. **更好的用户体验**：友好的错误提示，明确的操作指引
3. **更容易调试**：详细的错误日志，分类的错误类型
4. **更少的重复代码**：错误处理逻辑集中管理
5. **更好的可维护性**：添加新的错误处理逻辑只需修改一处
6. **更好的扩展性**：支持注册自定义错误处理器

---

## 🟢 Positive 值得肯定的地方

### 1. 状态管理清晰，使用 Zustand

**描述：**
项目使用 Zustand 进行状态管理，状态定义清晰，操作简单。

**优点：**
- 轻量级，无 Provider 嵌套
- API 简单直观，易于学习和使用
- 支持 TypeScript，类型安全
- 可以在组件外部使用 store
- 中间件支持（如 persist、devtools）

**代码示例：**
```typescript
// frontend/src/store/videoStore.ts
interface VideoState {
  videos: Video[]
  currentVideo: Video | null
  loading: boolean
  // ...
  
  setVideos: (videos: Video[]) => void
  setCurrentVideo: (video: Video | null) => void
  // ...
}

export const useVideoStore = create<VideoState>()((set) => ({
  videos: [],
  currentVideo: null,
  loading: false,
  // ...
  
  setVideos: (videos) => set({ videos }),
  setCurrentVideo: (video) => set({ currentVideo: video }),
  // ...
}))
```

**使用方式：**
```tsx
// 在组件中使用
const { videos, loading, setVideos } = useVideoStore()

// 选择特定的状态，避免不必要的重新渲染
const currentVideo = useVideoStore((state) => state.currentVideo)
```

---

### 2. 类型定义完善

**描述：**
项目有完整的 TypeScript 类型定义，包括所有数据模型、API 请求和响应类型。

**优点：**
- 编译时类型检查，减少运行时错误
- 更好的 IDE 支持（自动补全、类型提示）
- 代码自文档化，类型定义即文档
- 更容易重构和维护
- 更好的团队协作

**类型定义示例：**
```typescript
// frontend/src/types/index.ts
export interface Video {
  id: number
  title: string
  description: string | null
  filePath: string
  fileName: string
  fileSize: number
  duration: number | null
  format: string | null
  resolution: string | null
  coverPath: string | null
  sourceType: 'UPLOADED' | 'SCANNED'
  isFavorite: boolean
  createdAt: string
  updatedAt: string
  tags: Tag[]
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}
```

**tsconfig.json 配置：**
```json
{
  "compilerOptions": {
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    // ...
  }
}
```

---

### 3. 工具函数组织良好

**描述：**
项目将工具函数组织在 `utils/` 目录下，按功能分类，便于复用和维护。

**优点：**
- 代码复用，避免重复实现
- 集中管理，易于维护和测试
- 功能分类清晰，易于查找
- 可以单独进行单元测试

**代码示例：**
```typescript
// frontend/src/utils/format.ts
export function formatDuration(seconds: number | null | undefined): string {
  if (!seconds || seconds <= 0) return '00:00'
  
  const hrs = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)
  
  if (hrs > 0) {
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (!bytes || bytes <= 0) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let unitIndex = 0
  let size = bytes
  
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  
  return `${size.toFixed(unitIndex > 0 ? 2 : 0)} ${units[unitIndex]}`
}

export function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return ''
  
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
```

**使用方式：**
```tsx
import { formatDuration, formatFileSize, formatDate } from '@/utils/format'

function VideoInfo({ video }: { video: Video }) {
  return (
    <div>
      <p>时长: {formatDuration(video.duration)}</p>
      <p>大小: {formatFileSize(video.fileSize)}</p>
      <p>创建时间: {formatDate(video.createdAt)}</p>
    </div>
  )
}
```

---

### 4. Vite 配置合理

**描述：**
Vite 配置文件设置了路径别名、代理等，符合现代前端项目配置最佳实践。

**优点：**
- 路径别名简化导入语句
- API 代理解决开发环境跨域问题
- 配置清晰，易于理解和修改
- 使用 TypeScript 配置，类型安全

**配置示例：**
```typescript
// frontend/src/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
```

**路径别名的好处：**
```tsx
// 之前
import { Button } from '../../components/ui/button'
import { formatDate } from '../../utils/format'

// 之后
import { Button } from '@/components/ui/button'
import { formatDate } from '@/utils/format'
```

---

### 5. 项目结构清晰

**描述：**
项目按照功能模块组织目录结构，符合现代前端项目最佳实践。

**目录结构：**
```
frontend/src/
├── components/          # 组件
│   ├── layout/         # 布局组件
│   │   └── MainLayout.tsx
│   └── ui/             # UI 组件（ShadCN）
│       ├── button.tsx
│       ├── card.tsx
│       ├── input.tsx
│       └── ...
├── pages/              # 页面组件
│   ├── Home.tsx
│   ├── Player.tsx
│   ├── Favorites.tsx
│   └── ...
├── services/           # API 服务
│   ├── api.ts
│   ├── videoService.ts
│   ├── tagService.ts
│   └── ...
├── store/              # 状态管理
│   ├── index.ts
│   ├── videoStore.ts
│   ├── playerStore.ts
│   └── ...
├── types/              # 类型定义
│   └── index.ts
├── utils/              # 工具函数
│   ├── cn.ts
│   ├── format.ts
│   └── ...
├── App.tsx             # 根组件
├── main.tsx            # 入口文件
└── router.tsx          # 路由配置
```

**优点：**
- 按功能分层，职责清晰
- 易于理解和维护
- 符合约定优于配置的原则
- 团队成员可以快速上手
- 便于代码审查和测试

---

## 修复优先级建议

### 高优先级（建议本周内修复）
1. **🔴 缺少 React Query 集成**
   - 影响：性能问题、用户体验差
   - 工作量：中等（需要重构所有数据获取逻辑）
   - 建议：分阶段实施，先改造主要页面

2. **🔴 可访问性问题严重**
   - 影响：合规风险、部分用户无法使用
   - 工作量：较大（需要检查所有交互组件）
   - 建议：先修复关键页面（首页、播放页），再逐步完善

3. **🟠 MainLayout 组件过于庞大**
   - 影响：可维护性差、测试困难
   - 工作量：中等（需要拆分组件）
   - 建议：立即拆分，这是后续开发的基础

### 中优先级（建议本月内修复）
4. **🟠 缺少代码分割和懒加载**
   - 影响：首屏加载慢
   - 工作量：小（配置简单）
   - 建议：可以快速实施，效果明显

5. **🟠 缺少环境变量类型验证**
   - 影响：类型不安全
   - 工作量：小
   - 建议：快速实施，提高代码质量

6. **🟡 缺少错误边界**
   - 影响：健壮性差
   - 工作量：小到中等
   - 建议：先添加全局错误边界，再逐步细化

7. **🟡 API 响应处理不够健壮**
   - 影响：错误处理分散
   - 工作量：中等
   - 建议：创建统一的错误处理系统

### 低优先级（建议后续迭代中修复）
8. **🟡 组件内部定义子组件**
   - 影响：性能问题（轻微）
   - 工作量：小
   - 建议：在重构其他代码时一并修复

9. **🟡 事件处理函数缺少明确类型**
   - 影响：代码可读性
   - 工作量：小
   - 建议：在开发新功能时遵循最佳实践

10. **🟡 没有使用 `cn()` 函数合并类名**
    - 影响：类名可能冲突
    - 工作量：小到中等
    - 建议：逐步重构，新代码使用 `cn()`

---

## 总结

这个前端项目整体质量良好，特别是：
- ✅ Zustand 状态管理清晰
- ✅ TypeScript 类型定义完善
- ✅ 工具函数组织良好
- ✅ Vite 配置合理
- ✅ 项目结构清晰

需要重点改进的方面：
- 🔴 集成 React Query 或类似的数据获取库
- 🔴 改进可访问性，符合 WCAG 标准
- 🟠 拆分大型组件，提高可维护性
- 🟠 添加代码分割和懒加载
- 🟡 添加错误边界和统一错误处理

建议按照优先级逐步修复，先解决高优先级问题，再处理中低优先级问题。每次修复后进行充分测试，确保不会引入新的问题。

[10.5]