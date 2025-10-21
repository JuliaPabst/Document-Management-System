"use client"

import { useState, useCallback } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { FileMetadata } from "@/lib/types"
import { formatFileSize, formatDateTime } from "@/lib/utils/format"
import { Edit2, Save, X, Upload, FileText } from "lucide-react"
import { cn } from "@/lib/utils"

interface DocumentMetadataProps {
  document: FileMetadata
  isEditing: boolean
  onEdit: () => void
  onSave: (updates: { author?: string; file?: File }) => Promise<void>
  onCancel: () => void
}

export function DocumentMetadata({ document, isEditing, onEdit, onSave, onCancel }: DocumentMetadataProps) {
  const [author, setAuthor] = useState(document.author)
  const [file, setFile] = useState<File | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!isSaving) {
      setIsDragging(true)
    }
  }, [isSaving])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)

    if (isSaving) return

    const files = Array.from(e.dataTransfer.files)
    if (files.length > 0) {
      setFile(files[0])
    }
  }, [isSaving])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    if (selectedFile) {
      setFile(selectedFile)
    }
  }

  const handleSave = async () => {
    setError(null)
    setIsSaving(true)

    try {
      const updates: { author?: string; file?: File } = {}
      
      // Only include author if it changed
      if (author.trim() !== document.author) {
        updates.author = author.trim()
      }
      
      // Include file if one was selected
      if (file) {
        updates.file = file
      }

      // Make sure at least one field is updated
      if (!updates.author && !updates.file) {
        setError("No changes to save")
        setIsSaving(false)
        return
      }

      await onSave(updates)
      setFile(null) // Reset file after successful save
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update metadata")
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    setAuthor(document.author)
    setFile(null)
    setError(null)
    onCancel()
  }

  return (
    <Card className="w-full">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-lg">Document Metadata</CardTitle>
        {!isEditing && (
          <Button variant="outline" size="sm" onClick={onEdit} className="gap-2 bg-transparent">
            <Edit2 className="h-4 w-4" />
            Edit
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-4 w-full">
          <div className="space-y-2">
            <Label htmlFor="author" className="text-muted-foreground">
              Author
            </Label>
            {isEditing ? (
              <Input
                id="author"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                disabled={isSaving}
                placeholder="Enter author name"
                className="w-full"
              />
            ) : (
              <p className="text-sm font-medium text-foreground break-words">{document.author}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label className="text-muted-foreground">Filename</Label>
            <p className="text-sm font-medium text-foreground break-words">{document.filename}</p>
          </div>

          <div className="space-y-2">
            <Label className="text-muted-foreground">File Type</Label>
            <p className="text-sm font-medium text-foreground">{document.fileType}</p>
          </div>

          <div className="space-y-2">
            <Label className="text-muted-foreground">Size</Label>
            <p className="text-sm font-medium text-foreground">{formatFileSize(document.size)}</p>
          </div>

          <div className="space-y-2">
            <Label className="text-muted-foreground">Uploaded</Label>
            <p className="text-sm font-medium text-foreground">{formatDateTime(document.uploadTime)}</p>
          </div>

          <div className="space-y-2">
            <Label className="text-muted-foreground">Last Edited</Label>
            <p className="text-sm font-medium text-foreground">{formatDateTime(document.lastEdited)}</p>
          </div>

          {isEditing && (
            <div className="space-y-2 pt-2 w-full overflow-hidden">
              <Label className="text-muted-foreground">
                Replace File (Optional)
              </Label>
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={cn(
                  "relative flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 transition-colors w-full",
                  isDragging && !isSaving && "border-primary bg-primary/5",
                  !isDragging && !isSaving && "border-border hover:border-primary/50 hover:bg-accent/50",
                  isSaving && "cursor-not-allowed opacity-60",
                  file && "border-primary bg-primary/5"
                )}
              >
                <input
                  id="file-replace"
                  type="file"
                  className="sr-only"
                  onChange={handleFileChange}
                  disabled={isSaving}
                  accept=".pdf,.doc,.docx,.txt,.xlsx,.xls,.pptx,.ppt"
                />

                {file ? (
                  <div className="flex w-full items-center gap-3 min-w-0">
                    <div className="rounded-full bg-primary/10 p-2 flex-shrink-0">
                      <FileText className="h-5 w-5 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0 overflow-hidden">
                      <p className="text-sm font-medium text-foreground truncate w-full">{file.name}</p>
                      <p className="text-xs text-muted-foreground">{formatFileSize(file.size)}</p>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setFile(null)}
                      disabled={isSaving}
                      className="h-8 w-8 p-0 flex-shrink-0"
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-2 text-center w-full">
                    <div className="rounded-full bg-primary/10 p-2">
                      <Upload className="h-5 w-5 text-primary" />
                    </div>
                    <div className="space-y-1">
                      <label htmlFor="file-replace" className="cursor-pointer text-sm font-medium text-primary hover:underline">
                        Choose a file
                      </label>
                      <span className="text-sm text-muted-foreground"> or drag and drop</span>
                      <p className="text-xs text-muted-foreground">PDF, DOC, DOCX, TXT, XLS, XLSX, PPT, PPTX</p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {isEditing && (
          <div className="flex gap-3 pt-2">
            <Button 
              onClick={handleSave} 
              disabled={isSaving || (!author.trim() && !file)} 
              className="flex-1 gap-2"
            >
              <Save className="h-4 w-4" />
              {isSaving ? "Saving..." : "Save Changes"}
            </Button>
            <Button variant="outline" onClick={handleCancel} disabled={isSaving} className="gap-2 bg-transparent">
              <X className="h-4 w-4" />
              Cancel
            </Button>
          </div>
        )}

        {error && (
          <div className="rounded-lg border border-destructive bg-destructive/10 p-3">
            <p className="text-sm text-destructive">{error}</p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
