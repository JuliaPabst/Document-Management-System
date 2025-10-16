"use client"

import { useState, useCallback, useEffect } from "react"
import useSWR from "swr"
import { apiClient } from "@/api/client"
import type { SearchParams } from "@/lib/types"
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

  const { data, error, isLoading, mutate } = useSWR(
    ["search", debouncedParams],
    () => apiClient.getAllFiles(debouncedParams),
    {
      revalidateOnFocus: false,
      keepPreviousData: true,
    },
  )

  const updateSearch = useCallback((query: string) => {
    setSearchParams((prev) => ({ ...prev, search: query || undefined }))
  }, [])

  const updateAuthor = useCallback((author: string) => {
    setSearchParams((prev) => ({ ...prev, author: author || undefined }))
  }, [])

  const updateFileType = useCallback((fileType: string) => {
    setSearchParams((prev) => ({ ...prev, fileType: fileType || undefined }))
  }, [])

  const clearFilters = useCallback(() => {
    setSearchParams({})
  }, [])

  const hasActiveFilters = Boolean(searchParams.search || searchParams.author || searchParams.fileType)

  // Extract unique authors and file types from all documents
  const authors = Array.from(new Set((data || []).map((doc) => doc.author))).sort()
  const fileTypes = Array.from(new Set((data || []).map((doc) => doc.fileType))).sort()

  return {
    documents: data || [],
    isLoading,
    error: error?.message,
    searchParams,
    authors,
    fileTypes,
    updateSearch,
    updateAuthor,
    updateFileType,
    clearFilters,
    hasActiveFilters,
    refresh: mutate,
  }
}
