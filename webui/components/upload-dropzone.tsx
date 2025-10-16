"use client"

import type React from "react"

import { useCallback, useState } from "react"
import { Upload, CheckCircle2, XCircle, Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"
import type { UploadProgress } from "@/lib/types"

interface UploadDropzoneProps {
  onFileSelect: (file: File) => void
  uploadProgress: UploadProgress
  disabled?: boolean
}

export function UploadDropzone({ onFileSelect, uploadProgress, disabled }: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false)

  const handleDragOver = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      if (!disabled) {
        setIsDragging(true)
      }
    },
    [disabled],
  )

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)

      if (disabled) return

      const files = Array.from(e.dataTransfer.files)
      if (files.length > 0) {
        onFileSelect(files[0])
      }
    },
    [onFileSelect, disabled],
  )

  const handleFileInput = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files
      if (files && files.length > 0) {
        onFileSelect(files[0])
      }
    },
    [onFileSelect],
  )

  const isUploading = uploadProgress.status === "uploading" || uploadProgress.status === "processing"
  const isSuccess = uploadProgress.status === "success"
  const isError = uploadProgress.status === "error"

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      className={cn(
        "relative flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-12 transition-colors",
        isDragging && !disabled && "border-primary bg-primary/5",
        !isDragging && !disabled && "border-border hover:border-primary/50 hover:bg-accent/50",
        disabled && "cursor-not-allowed opacity-60",
        isSuccess && "border-green-500 bg-green-500/5",
        isError && "border-destructive bg-destructive/5",
      )}
    >
      <input
        type="file"
        id="file-upload"
        className="sr-only"
        onChange={handleFileInput}
        disabled={disabled}
        accept=".pdf,.doc,.docx,.txt,.xlsx,.xls,.pptx,.ppt"
      />

      <div className="flex flex-col items-center gap-4 text-center">
        {isUploading && (
          <>
            <Loader2 className="h-12 w-12 animate-spin text-primary" />
            <div className="space-y-2">
              <p className="text-sm font-medium text-foreground">{uploadProgress.message}</p>
              <div className="h-2 w-64 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full bg-primary transition-all duration-300"
                  style={{ width: `${uploadProgress.progress}%` }}
                />
              </div>
            </div>
          </>
        )}

        {isSuccess && (
          <>
            <CheckCircle2 className="h-12 w-12 text-green-500" />
            <p className="text-sm font-medium text-green-600 dark:text-green-400">{uploadProgress.message}</p>
          </>
        )}

        {isError && (
          <>
            <XCircle className="h-12 w-12 text-destructive" />
            <p className="text-sm font-medium text-destructive">{uploadProgress.message}</p>
          </>
        )}

        {!isUploading && !isSuccess && !isError && (
          <>
            <div className="rounded-full bg-primary/10 p-4">
              <Upload className="h-8 w-8 text-primary" />
            </div>
            <div className="space-y-2">
              <label htmlFor="file-upload" className="cursor-pointer text-sm font-medium text-primary hover:underline">
                Choose a file
              </label>
              <span className="text-sm text-muted-foreground"> or drag and drop</span>
              <p className="text-xs text-muted-foreground">PDF, DOC, DOCX, TXT, XLS, XLSX, PPT, PPTX up to 50MB</p>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
