"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { FileMetadata } from "@/lib/types"
import { formatFileSize, formatDateTime } from "@/lib/utils/format"
import { Edit2, Save, X } from "lucide-react"

interface DocumentMetadataProps {
  document: FileMetadata
  isEditing: boolean
  onEdit: () => void
  onSave: (updates: { author: string }) => Promise<void>
  onCancel: () => void
}

export function DocumentMetadata({ document, isEditing, onEdit, onSave, onCancel }: DocumentMetadataProps) {
  const [author, setAuthor] = useState(document.author)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSave = async () => {
    setError(null)
    setIsSaving(true)

    try {
      await onSave({ author })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update metadata")
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancel = () => {
    setAuthor(document.author)
    setError(null)
    onCancel()
  }

  return (
    <Card>
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
        <div className="grid gap-4">
          <div className="space-y-2">
            <Label className="text-muted-foreground">Filename</Label>
            <p className="text-sm font-medium text-foreground">{document.filename}</p>
          </div>

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
              />
            ) : (
              <p className="text-sm font-medium text-foreground">{document.author}</p>
            )}
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
        </div>

        {isEditing && (
          <div className="flex gap-3 pt-2">
            <Button onClick={handleSave} disabled={isSaving || !author.trim()} className="flex-1 gap-2">
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
