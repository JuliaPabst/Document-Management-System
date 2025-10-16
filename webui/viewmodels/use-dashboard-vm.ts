"use client"

import useSWR from "swr"
import { apiClient } from "@/api/client"
import type { FileMetadata } from "@/lib/types"

interface DashboardStats {
  totalDocuments: number
  totalSize: number
  fileTypes: Record<string, number>
  recentDocuments: FileMetadata[]
}

function calculateStats(documents: FileMetadata[]): DashboardStats {
  const totalDocuments = documents.length
  const totalSize = documents.reduce((sum, doc) => sum + doc.size, 0)

  const fileTypes: Record<string, number> = {}
  documents.forEach((doc) => {
    fileTypes[doc.fileType] = (fileTypes[doc.fileType] || 0) + 1
  })

  const recentDocuments = documents.slice(0, 5)

  return {
    totalDocuments,
    totalSize,
    fileTypes,
    recentDocuments,
  }
}

export function useDashboardVM() {
  const { data, error, isLoading } = useSWR("dashboard", () => apiClient.getAllFiles(), {
    revalidateOnFocus: false,
  })

  const stats = data ? calculateStats(data) : null

  return {
    stats,
    isLoading,
    error: error?.message,
  }
}
