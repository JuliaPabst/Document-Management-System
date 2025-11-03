"use client"

import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import type { FileMetadata } from "@/lib/types"
import { formatDate, formatFileSize, getFileIcon } from "@/lib/utils/format"
import { ArrowRight } from "lucide-react"

interface RecentDocumentsProps {
  documents: FileMetadata[]
}

export function RecentDocuments({ documents }: RecentDocumentsProps) {
  if (documents.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Recent Documents</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">No documents yet</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-lg">Recent Documents</CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link href="/search" className="gap-2">
            View All
            <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {documents.map((doc) => (
            <Link key={doc.id} href={`/documents/${doc.id}`}>
              <div className="flex items-center gap-3 rounded-lg border border-border p-3 transition-colors hover:border-primary hover:bg-accent/50">
                <div className="text-2xl">{getFileIcon(doc.fileType)}</div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{doc.filename}</p>
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span>{doc.author}</span>
                    <span>•</span>
                    <span>{formatDate(doc.uploadTime)}</span>
                    <span>•</span>
                    <span>{formatFileSize(doc.size)}</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
