"use client"

import { useState, useCallback } from "react"
import useSWR from "swr"
import { apiClient } from "@/api/client"
import type { FileMetadata } from "@/lib/types"

export function useDocumentVM(id: number) {
  const [isEditing, setIsEditing] = useState(false)
  const [optimisticData, setOptimisticData] = useState<FileMetadata | null>(null)

  const { data, error, isLoading, mutate } = useSWR(["document", id], () => apiClient.getFileById(id), {
    revalidateOnFocus: false,
  })

  const document = optimisticData || data

  const updateMetadata = useCallback(
    async (updates: { author?: string }) => {
      if (!data) return

      const optimistic: FileMetadata = {
        ...data,
        ...updates,
        lastEdited: new Date().toISOString(),
      }
      setOptimisticData(optimistic)

      try {
        const updated = await apiClient.updateFile(id, updates)
        setOptimisticData(null)
        mutate(updated, false)
        setIsEditing(false)
      } catch (err) {
        setOptimisticData(null)
        throw err
      }
    },
    [data, id, mutate],
  )

  const deleteDocument = useCallback(async () => {
    await apiClient.deleteFile(id)
  }, [id])

  return {
    document,
    isLoading,
    error: error?.message,
    isEditing,
    setIsEditing,
    updateMetadata,
    deleteDocument,
    refresh: mutate,
  }
}
