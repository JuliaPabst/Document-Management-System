"use client"

import { SearchBar } from "@/components/search-bar"
import { SearchFilters } from "@/components/search-filters"
import { DocumentList } from "@/components/document-list"
import { SearchResultsHeader } from "@/components/search-results-header"
import { useSearchVM } from "@/viewmodels/use-search-vm"
import { Card, CardContent } from "@/components/ui/card"

export default function SearchPage() {
  const {
    documents,
    isLoading,
    error,
    searchParams,
    authors,
    fileTypes,
    updateSearch,
    updateAuthor,
    updateFileType,
    clearFilters,
    hasActiveFilters,
  } = useSearchVM()

  return (
    <div className="container mx-auto max-w-6xl py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Search Documents</h1>
        <p className="mt-2 text-muted-foreground">Find documents by name, author, or file type</p>
      </div>

      <div className="space-y-6">
        <Card>
          <CardContent className="p-6 space-y-4">
            <SearchBar value={searchParams.search || ""} onChange={updateSearch} />

            <SearchFilters
              author={searchParams.author || "all"}
              fileType={searchParams.fileType || "all"}
              authors={authors}
              fileTypes={fileTypes}
              onAuthorChange={(value) => updateAuthor(value === "all" ? "" : value)}
              onFileTypeChange={(value) => updateFileType(value === "all" ? "" : value)}
              onClearFilters={clearFilters}
              hasActiveFilters={hasActiveFilters}
            />
          </CardContent>
        </Card>

        {error && (
          <div className="rounded-lg border border-destructive bg-destructive/10 p-4">
            <p className="text-sm text-destructive">{error}</p>
          </div>
        )}

        <SearchResultsHeader count={documents.length} isLoading={isLoading} />

        <DocumentList documents={documents} searchQuery={searchParams.search} isLoading={isLoading} />
      </div>
    </div>
  )
}
