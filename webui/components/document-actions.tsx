"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Download, Trash2 } from "lucide-react"
import type { FileMetadata } from "@/lib/types"

interface DocumentActionsProps {
  document: FileMetadata
  onDelete: () => Promise<void>
}

export function DocumentActions({ document, onDelete }: DocumentActionsProps) {
  const router = useRouter()
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isDownloading, setIsDownloading] = useState(false)

  const handleDownload = async () => {
    setIsDownloading(true)
    try {
      console.log("Starting download for document:", document.id, document.filename)
      const { apiClient } = await import("@/api/client")
      await apiClient.downloadFile(document.id, document.filename)
      console.log("Download completed successfully")
    } catch (err) {
      console.error("Download failed:", err)
      alert(`Download failed: ${err instanceof Error ? err.message : 'Unknown error'}`)
    } finally {
      setIsDownloading(false)
    }
  }

  const handleDelete = async () => {
    setIsDeleting(true)
    try {
      await onDelete()
      router.push("/")
    } catch (err) {
      setIsDeleting(false)
    }
  }

  return (
    <>
      <div className="flex gap-3">
        <Button 
          variant="outline" 
          className="flex-1 gap-2 bg-transparent"
          onClick={handleDownload}
          disabled={isDownloading}
        >
          <Download className="h-4 w-4" />
          {isDownloading ? "Downloading..." : "Download"}
        </Button>
        <Button variant="destructive" onClick={() => setShowDeleteDialog(true)} className="gap-2">
          <Trash2 className="h-4 w-4" />
          Delete
        </Button>
      </div>

      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Document</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete "{document.filename}"? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}