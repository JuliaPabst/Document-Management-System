"use client"

import { useState, useCallback } from "react"
import { apiClient } from "@/api/client"
import type { FileMetadata, UploadProgress } from "@/lib/types"

export function useUploadVM() {
  const [uploadProgress, setUploadProgress] = useState<UploadProgress>({
    status: "idle",
    progress: 0,
  })
  const [uploadedFile, setUploadedFile] = useState<FileMetadata | null>(null)
  const [error, setError] = useState<string | null>(null)

  const uploadFile = useCallback(async (file: File, author: string) => {
    setError(null)
    setUploadedFile(null)

    try {
      // Uploading phase
      setUploadProgress({ status: "uploading", progress: 30, message: "Uploading file..." })

      const result = await apiClient.uploadFile({ file, author })

      // Processing phase (simulated)
      setUploadProgress({ status: "processing", progress: 70, message: "Processing document..." })

      // Small delay to show processing state
      await new Promise((resolve) => setTimeout(resolve, 500))

      // Success
      setUploadProgress({ status: "success", progress: 100, message: "Upload complete!" })
      setUploadedFile(result)

      return result
    } catch (err) {
      const message = err instanceof Error ? err.message : "Upload failed"
      setError(message)
      setUploadProgress({ status: "error", progress: 0, message })
      throw err
    }
  }, [])

  const reset = useCallback(() => {
    setUploadProgress({ status: "idle", progress: 0 })
    setUploadedFile(null)
    setError(null)
  }, [])

  return {
    uploadProgress,
    uploadedFile,
    error,
    uploadFile,
    reset,
  }
}
