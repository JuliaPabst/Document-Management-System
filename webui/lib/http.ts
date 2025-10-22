import axios, { AxiosInstance, AxiosError } from 'axios'
import type { ErrorResponse } from './types'

export class ApiError extends Error {
  status: number
  details?: ErrorResponse

  constructor(message: string, status: number, details?: ErrorResponse) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

export class HttpClient {
  private client: AxiosInstance

  constructor(baseUrl: string) {
    this.client = axios.create({
      baseURL: baseUrl,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ErrorResponse>) => {
        if (error.response) {
          const errorData = error.response.data
          const message = errorData?.message || error.response.statusText
          throw new ApiError(message, error.response.status, errorData)
        }
        throw new Error(error.message)
      }
    )
  }

  async get<T>(path: string, params?: Record<string, string>): Promise<T> {
    const response = await this.client.get<T>(path, { params })
    return response.data
  }

  async post<T>(path: string, body: FormData | object): Promise<T> {
    const isFormData = body instanceof FormData
    const response = await this.client.post<T>(path, body, {
      headers: isFormData ? { 'Content-Type': 'multipart/form-data' } : undefined,
    })
    return response.data
  }

  async patch<T>(path: string, body: FormData | object): Promise<T> {
    const isFormData = body instanceof FormData
    const response = await this.client.patch<T>(path, body, {
      headers: isFormData ? { 'Content-Type': 'multipart/form-data' } : undefined,
    })
    return response.data
  }

  async delete(path: string): Promise<void> {
    await this.client.delete(path)
  }
}
