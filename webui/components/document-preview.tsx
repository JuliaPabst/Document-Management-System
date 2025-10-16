"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import type { FileMetadata } from "@/lib/types"
import { getFileIcon } from "@/lib/utils/format"

interface DocumentPreviewProps {
  document: FileMetadata
}

export function DocumentPreview({ document }: DocumentPreviewProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Preview</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex aspect-[4/3] items-center justify-center rounded-lg border border-border bg-muted/30">
          <div className="flex flex-col items-center gap-4 text-center">
            <div className="text-6xl">{getFileIcon(document.fileType)}</div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-foreground">{document.filename}</p>
              <p className="text-xs text-muted-foreground">Preview not available</p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
