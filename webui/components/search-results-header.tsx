"use client"

import { FileText } from "lucide-react"

interface SearchResultsHeaderProps {
  count: number
  isLoading: boolean
}

export function SearchResultsHeader({ count, isLoading }: SearchResultsHeaderProps) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-border bg-muted/50 p-4">
      <FileText className="h-5 w-5 text-muted-foreground" />
      <div>
        <p className="text-sm font-medium text-foreground">
          {isLoading ? "Searching..." : `${count} ${count === 1 ? "document" : "documents"} found`}
        </p>
      </div>
    </div>
  )
}
