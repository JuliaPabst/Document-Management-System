import { HttpClient } from "@/lib/http"
import type { 
  FileMetadata, 
  SearchParams, 
  UploadRequest, 
  UpdateRequest,
  ChatMessageRequestDto,
  ChatMessageResponseDto 
} from "@/lib/types"

interface ApiAdapter {
  getAllFiles(params?: SearchParams): Promise<FileMetadata[]>
  getFileById(id: number): Promise<FileMetadata>
  uploadFile(request: UploadRequest): Promise<FileMetadata>
  updateFile(id: number, request: UpdateRequest): Promise<FileMetadata>
  deleteFile(id: number): Promise<void>
  saveChatMessage(request: ChatMessageRequestDto): Promise<ChatMessageResponseDto>
  getChatMessages(sessionId?: string): Promise<ChatMessageResponseDto[]>
}

class RestAdapter implements ApiAdapter {
  private http: HttpClient

  constructor(baseUrl: string) {
    this.http = new HttpClient(baseUrl)
  }

  async getAllFiles(params?: SearchParams): Promise<FileMetadata[]> {
    return this.http.get<FileMetadata[]>("/v1/files", params as Record<string, string>)
  }

  async getFileById(id: number): Promise<FileMetadata> {
    return this.http.get<FileMetadata>(`/v1/files/${id}`)
  }

  async uploadFile(request: UploadRequest): Promise<FileMetadata> {
    const formData = new FormData()
    formData.append("file", request.file)
    formData.append("author", request.author)
    return this.http.post<FileMetadata>("/v1/files", formData)
  }

  async updateFile(id: number, request: UpdateRequest): Promise<FileMetadata> {
    const formData = new FormData()
    if (request.file) formData.append("file", request.file)
    if (request.author) formData.append("author", request.author)
    return this.http.patch<FileMetadata>(`/v1/files/${id}`, formData)
  }

  async deleteFile(id: number): Promise<void> {
    return this.http.delete(`/v1/files/${id}`)
  }

  async saveChatMessage(request: ChatMessageRequestDto): Promise<ChatMessageResponseDto> {
    return this.http.post<ChatMessageResponseDto>("/v1/chat-messages", request)
  }

  async getChatMessages(sessionId?: string): Promise<ChatMessageResponseDto[]> {
    const params = sessionId ? { sessionId } : undefined
    return this.http.get<ChatMessageResponseDto[]>("/v1/chat-messages", params)
  }
}

function createAdapter(): ApiAdapter {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL
  if (!apiBaseUrl) {
    throw new Error("API base URL is not defined. Please set NEXT_PUBLIC_API_BASE_URL in your environment variables.")
  }

  console.info("Running in REST MODE - Connected to:", apiBaseUrl)
  return new RestAdapter(apiBaseUrl)
}

export const apiClient = createAdapter()
