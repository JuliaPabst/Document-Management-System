import axios, { AxiosInstance, AxiosError } from 'axios'

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
      (error: AxiosError) => {
        const message = error.response 
          ? `HTTP ${error.response.status}: ${error.response.statusText}`
          : error.message
        throw new Error(message)
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
