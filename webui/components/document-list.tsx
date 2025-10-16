"use client"

import { DocumentCard } from "@/components/document-card"
import type { FileMetadata } from "@/lib/types"
import { FileX } from "lucide-react"

interface DocumentListProps {
  documents: FileMetadata[]
  searchQuery?: string
  isLoading?: boolean
}

export function DocumentList({ documents, searchQuery, isLoading }: DocumentListProps) {
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-32 animate-pulse rounded-lg bg-muted" />
        ))}
      </div>
    )
  }

  if (documents.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <div className="rounded-full bg-muted p-6">
          <FileX className="h-12 w-12 text-muted-foreground" />
        </div>
        <h3 className="mt-4 text-lg font-semibold text-foreground">No documents found</h3>
        <p className="mt-2 text-sm text-muted-foreground">
          {searchQuery ? "Try adjusting your search or filters" : "Upload your first document to get started"}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {documents.map((doc) => (
        <DocumentCard key={doc.id} document={doc} highlight={searchQuery} />
      ))}
    </div>
  )
}
