"use client"

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { X } from "lucide-react"

interface SearchFiltersProps {
  author: string
  fileType: string
  authors: string[]
  fileTypes: string[]
  onAuthorChange: (value: string) => void
  onFileTypeChange: (value: string) => void
  onClearFilters: () => void
  hasActiveFilters: boolean
}

export function SearchFilters({
  author,
  fileType,
  authors,
  fileTypes,
  onAuthorChange,
  onFileTypeChange,
  onClearFilters,
  hasActiveFilters,
}: SearchFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select value={author} onValueChange={onAuthorChange}>
        <SelectTrigger className="w-[180px]">
          <SelectValue placeholder="Filter by author" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">All Authors</SelectItem>
          {authors.map((a) => (
            <SelectItem key={a} value={a}>
              {a}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={fileType} onValueChange={onFileTypeChange}>
        <SelectTrigger className="w-[180px]">
          <SelectValue placeholder="Filter by type" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">All Types</SelectItem>
          {fileTypes.map((type) => (
            <SelectItem key={type} value={type}>
              {type}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {hasActiveFilters && (
        <Button variant="ghost" size="sm" onClick={onClearFilters} className="gap-2">
          <X className="h-4 w-4" />
          Clear Filters
        </Button>
      )}
    </div>
  )
}
