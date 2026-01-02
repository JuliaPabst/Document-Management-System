"use client"

import { useState, useCallback, useEffect } from "react"
import useSWR from "swr"
import { apiClient } from "@/api/client"
import type { SearchParams, SearchRequest, SearchResult } from "@/lib/types"
import { debounce } from "@/lib/utils/debounce"

export function useSearchVM() {
  const [searchParams, setSearchParams] = useState<SearchParams>({})
  const [debouncedParams, setDebouncedParams] = useState<SearchParams>({})

  // Debounced search to avoid excessive API calls
  const debouncedSetParams = useCallback(
    debounce((params: SearchParams) => {
      setDebouncedParams(params)
    }, 300),
    [],
  )

  useEffect(() => {
    debouncedSetParams(searchParams)
  }, [searchParams, debouncedSetParams])

  // Use Elasticsearch
  const buildSearchRequest = (params: SearchParams): SearchRequest => {
    // If no search query, use wildcard to get all documents
    const query = params.search && params.search.trim() !== "" ? params.search : "*"

    return {
      query: query,
      author: params.author || undefined,
      fileType: params.fileType || undefined,
      searchField: params.searchField || undefined,
      page: 0,
      size: 100, // Get more documents
      sortBy: "uploadTime",
      sortOrder: "desc",
    }
  }

  const searchRequest = buildSearchRequest(debouncedParams)

  const { data, error, isLoading, mutate } = useSWR(
    ["elasticsearch-search", searchRequest],
    () => apiClient.searchDocuments(searchRequest),
    {
      revalidateOnFocus: false,
      keepPreviousData: true,
    },
  )

  // Convert SearchResult[] to FileMetadata-like structure for compatibility
  const documents = data?.results.map((result: SearchResult) => ({
    id: result.documentId,
    filename: result.filename,
    author: result.author,
    fileType: result.fileType,
    size: result.size,
    uploadTime: result.uploadTime,
    lastEdited: result.uploadTime, // uploadTime as fallback
    summary: result.summary,
    highlightedText: result.highlightedText,
    score: result.score,
  })) || []

  const updateSearch = useCallback((query: string) => {
    setSearchParams((prev) => ({ ...prev, search: query || undefined }))
  }, [])

  const updateAuthor = useCallback((author: string) => {
    setSearchParams((prev) => ({ ...prev, author: author || undefined }))
  }, [])

  const updateFileType = useCallback((fileType: string) => {
    setSearchParams((prev) => ({ ...prev, fileType: fileType || undefined }))
  }, [])

  const updateSearchField = useCallback((searchField: string) => {
    setSearchParams((prev) => ({ ...prev, searchField: searchField || undefined }))
  }, [])

  const clearFilters = useCallback(() => {
    setSearchParams({})
  }, [])

  const hasActiveFilters = Boolean(searchParams.search || searchParams.author || searchParams.fileType || searchParams.searchField)

  // Extract unique authors and file types from search results
  const authors = Array.from(new Set(documents.map((doc) => doc.author))).sort()
  const fileTypes = Array.from(new Set(documents.map((doc) => doc.fileType))).sort()

  return {
    documents,
    isLoading,
    error: error?.message,
    searchParams,
    authors,
    fileTypes,
    totalHits: data?.totalHits || 0,
    searchTimeMs: data?.searchTimeMs || 0,
    updateSearch,
    updateAuthor,
    updateFileType,
    updateSearchField,
    clearFilters,
    hasActiveFilters,
    refresh: mutate,
  }
}
