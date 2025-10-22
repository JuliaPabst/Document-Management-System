"use client"

import { useState, useCallback } from "react"
import { apiClient } from "@/api/client"
import type { FileMetadata, UploadProgress, DuplicateFileInfo } from "@/lib/types"
import { ApiError } from "@/lib/http"

export function useUploadVM() {
  const [uploadProgress, setUploadProgress] = useState<UploadProgress>({
    status: "idle",
    progress: 0,
  })
  const [uploadedFile, setUploadedFile] = useState<FileMetadata | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [duplicateInfo, setDuplicateInfo] = useState<DuplicateFileInfo | null>(null)

  const uploadFile = useCallback(async (file: File, author: string, replaceExisting: boolean = false) => {
    setError(null)
    setUploadedFile(null)
    setDuplicateInfo(null)

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
      if (err instanceof ApiError && err.status === 409) {
        // Duplicate file detected - try to find the existing file
        try {
          const allFiles = await apiClient.getAllFiles({ search: file.name, author })
          const existingFile = allFiles.find(f => f.filename === file.name && f.author === author)
          
          const duplicateData: DuplicateFileInfo = {
            existingFileId: existingFile?.id || 0,
            filename: file.name,
            author: author,
          }
          setDuplicateInfo(duplicateData)
          setError("A file with this name already exists for this author.")
          setUploadProgress({ status: "error", progress: 0, message: "Duplicate file detected" })
        } catch (searchErr) {
          // If we can't find the file, still show the duplicate error
          const duplicateData: DuplicateFileInfo = {
            existingFileId: 0,
            filename: file.name,
            author: author,
          }
          setDuplicateInfo(duplicateData)
          setError("A file with this name already exists for this author.")
          setUploadProgress({ status: "error", progress: 0, message: "Duplicate file detected" })
        }
      } else {
        const message = err instanceof Error ? err.message : "Upload failed"
        setError(message)
        setUploadProgress({ status: "error", progress: 0, message })
      }
      throw err
    }
  }, [])

  const replaceFile = useCallback(async (file: File, author: string, existingFileId: number) => {
    setError(null)
    setUploadedFile(null)
    setDuplicateInfo(null)

    try {
      // Uploading phase
      setUploadProgress({ status: "uploading", progress: 30, message: "Replacing file..." })

      const result = await apiClient.updateFile(existingFileId, { file, author })

      // Processing phase (simulated)
      setUploadProgress({ status: "processing", progress: 70, message: "Processing document..." })

      // Small delay to show processing state
      await new Promise((resolve) => setTimeout(resolve, 500))

      // Success
      setUploadProgress({ status: "success", progress: 100, message: "File replaced successfully!" })
      setUploadedFile(result)

      return result
    } catch (err) {
      const message = err instanceof Error ? err.message : "Replace failed"
      setError(message)
      setUploadProgress({ status: "error", progress: 0, message })
      throw err
    }
  }, [])

  const reset = useCallback(() => {
    setUploadProgress({ status: "idle", progress: 0 })
    setUploadedFile(null)
    setError(null)
    setDuplicateInfo(null)
  }, [])

  return {
    uploadProgress,
    uploadedFile,
    error,
    duplicateInfo,
    uploadFile,
    replaceFile,
    reset,
  }
}
