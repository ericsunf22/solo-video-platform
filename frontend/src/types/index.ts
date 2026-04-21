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

export interface Tag {
  id: number
  name: string
  color: string
  description: string | null
  createdAt: string
  updatedAt: string
  videoCount?: number
}

export interface PlayHistory {
  id: number
  videoId: number
  video: Video
  progress: number
  playCount: number
  totalPlayTime: number
  lastPlayedAt: string
  createdAt: string
  updatedAt: string
}

export interface VideoTag {
  id: number
  videoId: number
  tagId: number
  createdAt: string
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

export interface VideoUploadRequest {
  file: File
  title?: string
  description?: string
}

export interface FolderScanRequest {
  folderPath: string
  recursive?: boolean
  updateExisting?: boolean
}

export interface ScanResultResponse {
  newVideos: number
  updatedVideos: number
  skippedVideos: number
  totalVideos: number
}

export interface PlayProgressRequest {
  videoId: number
  progress: number
  duration?: number
}

export interface TagCreateRequest {
  name: string
  color?: string
  description?: string
}

export interface VideoUpdateRequest {
  title?: string
  description?: string
}
