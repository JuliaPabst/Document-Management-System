"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { UploadDropzone } from "@/components/upload-dropzone"
import { UploadStatusTimeline } from "@/components/upload-status-timeline"
import { useUploadVM } from "@/viewmodels/use-upload-vm"
import { formatFileSize } from "@/lib/utils/format"
import { FileText } from "lucide-react"
import Link from "next/link"

export function UploadForm() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [author, setAuthor] = useState("")
  const { uploadProgress, uploadedFile, error, uploadFile, reset } = useUploadVM()

  const handleFileSelect = (file: File) => {
    setSelectedFile(file)
    reset()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedFile || !author.trim()) return

    try {
      await uploadFile(selectedFile, author.trim())
    } catch (err) {
      // Error is handled in the view model
    }
  }

  const handleReset = () => {
    setSelectedFile(null)
    setAuthor("")
    reset()
  }

  const isUploading = uploadProgress.status === "uploading" || uploadProgress.status === "processing"
  const isSuccess = uploadProgress.status === "success"

  return (
    <div className="space-y-6">
      {!(isSuccess && uploadedFile) && (
        <Card>
          <CardHeader>
            <CardTitle>Upload Document</CardTitle>
            <CardDescription>Upload a new document to the paperless system</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <UploadDropzone onFileSelect={handleFileSelect} uploadProgress={uploadProgress} disabled={isUploading} />

            {selectedFile && !isSuccess && (
              <div className="flex items-center gap-3 rounded-lg border border-border bg-muted/50 p-4">
                <FileText className="h-8 w-8 text-muted-foreground" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{selectedFile.name}</p>
                  <p className="text-xs text-muted-foreground">{formatFileSize(selectedFile.size)}</p>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="author">Author Name</Label>
                <Input
                  id="author"
                  type="text"
                  placeholder="Enter your name"
                  value={author}
                  onChange={(e) => setAuthor(e.target.value)}
                  disabled={isUploading}
                  required
                />
              </div>

              <div className="flex gap-3">
                <Button type="submit" disabled={!selectedFile || !author.trim() || isUploading} className="flex-1">
                  {isUploading ? "Uploading..." : "Upload Document"}
                </Button>
                {(selectedFile || uploadProgress.status !== "idle") && (
                  <Button type="button" variant="outline" onClick={handleReset} disabled={isUploading}>
                    Reset
                  </Button>
                )}
              </div>
            </form>

            {error && (
              <div className="rounded-lg border border-destructive bg-destructive/10 p-4">
                <p className="text-sm text-destructive">{error}</p>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {uploadProgress.status !== "idle" && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Upload Progress</CardTitle>
          </CardHeader>
          <CardContent>
            <UploadStatusTimeline currentStatus={uploadProgress.status} />
          </CardContent>
        </Card>
      )}

      {isSuccess && uploadedFile && (
        <Card className="border-green-500 bg-green-500/5">
          <CardHeader>
            <CardTitle className="text-base text-green-600 dark:text-green-400">Upload Successful!</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Filename:</span>
                <span className="font-medium text-foreground">{uploadedFile.filename}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Author:</span>
                <span className="font-medium text-foreground">{uploadedFile.author}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">File Type:</span>
                <span className="font-medium text-foreground">{uploadedFile.fileType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Size:</span>
                <span className="font-medium text-foreground">{formatFileSize(uploadedFile.size)}</span>
              </div>
            </div>
            <div className="flex gap-3">
              <Button asChild className="flex-1">
                <Link href={`/documents/${uploadedFile.id}`}>View Document</Link>
              </Button>
              <Button variant="outline" onClick={handleReset}>
                Upload Another
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
