// API Types based on OpenAPI spec

export interface FileMetadata {
  id: number
  filename: string
  author: string
  fileType: string
  size: number
  uploadTime: string
  lastEdited: string
  summary?: string
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
  searchField?: string
}

export interface SearchRequest {
  query: string
  author?: string
  fileType?: string
  searchField?: string
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: string
}

export interface SearchResult {
  documentId: number
  filename: string
  author: string
  fileType: string
  size: number
  objectKey: string
  uploadTime: string
  summary?: string
  score?: number
  highlightedText?: string
}

export interface SearchResponse {
  results: SearchResult[]
  totalHits: number
  page: number
  size: number
  totalPages: number
  searchTimeMs: number
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

// Chat types
export type MessageRole = "user" | "assistant" | "system"

export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  timestamp: Date
  sessionId?: string
}

export interface ChatRequest {
  message: string
  conversationHistory: ChatMessage[]
}

export interface ChatResponse {
  message: string
  error?: string
}

export interface ChatMessageRequestDto {
  role: string
  content: string
  sessionId?: string
}

export interface ChatMessageResponseDto {
  id: number
  role: string
  content: string
  sessionId: string | null
  timestamp: string
}
