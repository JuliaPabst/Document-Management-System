"use client"

import Link from "next/link"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import type { FileMetadata } from "@/lib/types"
import { formatFileSize, formatDate, getFileIcon } from "@/lib/utils/format"
import { FileText, User, Calendar } from "lucide-react"

interface DocumentCardProps {
  document: FileMetadata
  highlight?: string
}

export function DocumentCard({ document, highlight }: DocumentCardProps) {
  const highlightText = (text: string) => {
    if (!highlight) return text

    const parts = text.split(new RegExp(`(${highlight})`, "gi"))
    return parts.map((part, i) =>
      part.toLowerCase() === highlight.toLowerCase() ? (
        <mark key={i} className="bg-yellow-200 dark:bg-yellow-900/50">
          {part}
        </mark>
      ) : (
        part
      ),
    )
  }

  return (
    <Link href={`/documents/${document.id}`}>
      <Card className="transition-all hover:border-primary hover:shadow-md">
        <CardContent className="p-6">
          <div className="flex items-start gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 text-2xl">
              {getFileIcon(document.fileType)}
            </div>

            <div className="flex-1 min-w-0 space-y-3">
              <div>
                <h3 className="font-semibold text-foreground truncate text-balance">
                  {highlightText(document.filename)}
                </h3>
                <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground">
                  <div className="flex items-center gap-1.5">
                    <User className="h-3.5 w-3.5" />
                    <span>{highlightText(document.author)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>{formatDate(document.uploadTime)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <FileText className="h-3.5 w-3.5" />
                    <span>{formatFileSize(document.size)}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Badge variant="secondary">{document.fileType}</Badge>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
