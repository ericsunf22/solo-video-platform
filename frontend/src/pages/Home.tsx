import { useEffect, useCallback, useMemo } from 'react'
import { Grid, List, Eye, Clock, FileVideo, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useVideoStore, useUIStore } from '@/store'
import type { SortBy, SortOrder } from '@/store/videoStore'
import { videoService } from '@/services'
import { formatDuration, formatFileSize, formatDate } from '@/utils/format'
import type { Video, PageResponse } from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function getCoverUrl(coverPath?: string): string | null {
  if (!coverPath) return null
  return `${API_BASE_URL}/api/covers/${encodeURIComponent(coverPath)}`
}

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

const VideoCard = ({ video, onClick }: VideoCardProps) => {
  const coverUrl = getCoverUrl(video.coverPath)
  
  return (
    <Card 
      className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group"
      onClick={onClick}
      role="button"
      tabIndex={0}
      aria-label={`播放视频: ${video.title}`}
    >
      <div className="relative aspect-video bg-gray-900">
        {coverUrl ? (
          <img 
            src={coverUrl} 
            alt={video.title}
            className="w-full h-full object-cover"
            onError={(e) => {
              const target = e.target as HTMLImageElement
              target.style.display = 'none'
              const parent = target.parentElement
              if (parent) {
                parent.querySelector('.fallback-icon')?.classList.remove('hidden')
              }
            }}
          />
        ) : null}
        <div className={`absolute inset-0 flex items-center justify-center ${coverUrl ? 'hidden fallback-icon' : ''}`}>
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
}

interface VideoRowProps {
  video: Video
  onClick?: () => void
}

const VideoRow = ({ video, onClick }: VideoRowProps) => {
  const coverUrl = getCoverUrl(video.coverPath)
  
  return (
    <div 
      className="flex items-center gap-4 p-4 bg-white rounded-lg border hover:border-blue-300 transition-colors cursor-pointer"
      onClick={onClick}
      role="button"
      tabIndex={0}
      aria-label={`播放视频: ${video.title}`}
    >
      <div className="relative w-40 h-24 bg-gray-900 rounded overflow-hidden flex-shrink-0">
        {coverUrl ? (
          <img 
            src={coverUrl} 
            alt={video.title}
            className="w-full h-full object-cover"
            onError={(e) => {
              const target = e.target as HTMLImageElement
              target.style.display = 'none'
              const parent = target.parentElement
              if (parent) {
                parent.querySelector('.fallback-icon')?.classList.remove('hidden')
              }
            }}
          />
        ) : null}
        <div className={`absolute inset-0 flex items-center justify-center ${coverUrl ? 'hidden fallback-icon' : ''}`}>
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
}

interface PaginationProps {
  pagination: PageResponse<Video>
  currentPage: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange: (size: number) => void
}

function Pagination({ pagination, currentPage, pageSize, onPageChange, onPageSizeChange }: PaginationProps) {
  const { total, totalPages } = pagination
  
  const pageNumbers = useMemo(() => {
    const pages: (number | string)[] = []
    const maxVisiblePages = 5
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2))
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1)
    
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1)
    }
    
    if (startPage > 1) {
      pages.push(1)
      if (startPage > 2) {
        pages.push('...')
      }
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i)
    }
    
    if (endPage < totalPages) {
      if (endPage < totalPages - 1) {
        pages.push('...')
      }
      pages.push(totalPages)
    }
    
    return pages
  }, [currentPage, totalPages])

  const pageSizeOptions = [12, 20, 40, 60, 100]

  return (
    <div className="flex items-center justify-between mt-8 py-4 border-t">
      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-500">
          共 {total} 条视频，第 {currentPage} / {totalPages} 页
        </span>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">每页显示:</span>
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="h-8 px-2 border border-gray-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {pageSizeOptions.map((size) => (
              <option key={size} value={size}>
                {size} 条
              </option>
            ))}
          </select>
        </div>
      </div>
      
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(1)}
          disabled={currentPage === 1}
          aria-label="第一页"
        >
          <ChevronsLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          aria-label="上一页"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        
        {pageNumbers.map((page, index) => (
          <Button
            key={index}
            variant={page === currentPage ? 'default' : 'outline'}
            size="icon"
            className="h-8 w-8"
            onClick={() => typeof page === 'number' && onPageChange(page)}
            disabled={typeof page !== 'number'}
            aria-label={typeof page === 'number' ? `第 ${page} 页` : undefined}
          >
            {page}
          </Button>
        ))}
        
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          aria-label="下一页"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-8 w-8"
          onClick={() => onPageChange(totalPages)}
          disabled={currentPage === totalPages}
          aria-label="最后一页"
        >
          <ChevronsRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}

interface SortOption {
  value: SortBy
  label: string
}

const sortOptions: SortOption[] = [
  { value: 'createdAt', label: '创建日期' },
  { value: 'title', label: '视频名称' },
  { value: 'duration', label: '视频时长' },
  { value: 'fileSize', label: '文件大小' },
]

function SortControl() {
  const { sortBy, sortOrder, setSortBy, setSortOrder } = useVideoStore()

  const toggleSortOrder = () => {
    setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')
  }

  const SortIcon = sortOrder === 'asc' ? ArrowUp : ArrowDown

  return (
    <div className="flex items-center gap-2">
      <div className="flex items-center gap-1">
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as SortBy)}
          className="h-9 px-3 border border-gray-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {sortOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      <Button
        variant="outline"
        size="icon"
        className="h-9 w-9"
        onClick={toggleSortOrder}
        aria-label={sortOrder === 'asc' ? '切换为降序' : '切换为升序'}
      >
        <SortIcon className="h-4 w-4" />
      </Button>
    </div>
  )
}

export default function Home() {
  const { 
    videos = [], 
    loading, 
    setVideos, 
    setLoading, 
    pagination,
    setPagination,
    currentPage,
    pageSize,
    sortBy,
    sortOrder,
    setCurrentPage,
    setPageSize,
    needRefresh, 
    markRefreshed 
  } = useVideoStore()
  const { viewMode, setViewMode } = useUIStore()

  const loadVideos = useCallback(async (page: number, size: number, sort: SortBy, order: SortOrder) => {
    setLoading(true)
    try {
      const response = await videoService.getVideos({ 
        page, 
        size,
        sortBy: sort,
        sortOrder: order,
      })
      setVideos(response.list)
      setPagination(response)
    } catch (error) {
      console.error('Failed to load videos:', error)
      setVideos([])
    } finally {
      setLoading(false)
    }
  }, [setVideos, setLoading, setPagination])

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page)
    loadVideos(page, pageSize, sortBy, sortOrder)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [setCurrentPage, loadVideos, pageSize, sortBy, sortOrder])

  const handlePageSizeChange = useCallback((size: number) => {
    setPageSize(size)
    setCurrentPage(1)
    loadVideos(1, size, sortBy, sortOrder)
  }, [setPageSize, setCurrentPage, loadVideos, sortBy, sortOrder])

  useEffect(() => {
    loadVideos(currentPage, pageSize, sortBy, sortOrder)
  }, [sortBy, sortOrder])

  useEffect(() => {
    if (needRefresh) {
      loadVideos(1, pageSize, sortBy, sortOrder)
      markRefreshed()
    }
  }, [needRefresh, loadVideos, markRefreshed, pageSize, sortBy, sortOrder])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  const showPagination = pagination && pagination.totalPages > 1

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">视频库</h1>
        <div className="flex items-center gap-4">
          <SortControl />
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
      </div>

      {videos.length === 0 ? (
        <div className="text-center py-16">
          <FileVideo className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无视频</h3>
          <p className="text-gray-500">上传视频或扫描本地文件夹开始使用</p>
        </div>
      ) : (
        <>
          {viewMode === 'grid' ? (
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

          {showPagination && (
            <Pagination
              pagination={pagination!}
              currentPage={currentPage}
              pageSize={pageSize}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
            />
          )}
        </>
      )}
    </div>
  )
}
