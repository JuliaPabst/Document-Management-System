// API Types based on OpenAPI spec

export interface FileMetadata {
  id: number
  filename: string
  author: string
  fileType: string
  size: number
  uploadTime: string
  lastEdited: string
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

export interface UploadRequest {
  file: File
  author: string
}

export interface SearchParams {
  search?: string
  author?: string
  fileType?: string
}

export interface UpdateRequest {
  file?: File
  author?: string
}

// UI-specific types
export type UploadStatus = "idle" | "uploading" | "processing" | "success" | "error"

export interface UploadProgress {
  status: UploadStatus
  progress: number
  message?: string
}

export interface DuplicateFileInfo {
  existingFileId: number
  filename: string
  author: string
}
