"use client"

import { use } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { DocumentMetadata } from "@/components/document-metadata"
import { DocumentPreview } from "@/components/document-preview"
import { DocumentActions } from "@/components/document-actions"
import { DocumentOCRText } from "@/components/document-ocr-text"
import { useDocumentVM } from "@/viewmodels/use-document-vm"
import { ArrowLeft, Loader2 } from "lucide-react"
import Link from "next/link"

export default function DocumentDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const { document, isLoading, error, isEditing, setIsEditing, updateMetadata, deleteDocument } = useDocumentVM(
    Number.parseInt(id),
  )

  if (isLoading) {
    return (
      <div className="container mx-auto max-w-6xl py-8 px-4">
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    )
  }

  if (error || !document) {
    return (
      <div className="container mx-auto max-w-6xl py-8 px-4">
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <h2 className="text-2xl font-bold text-foreground">Document Not Found</h2>
          <p className="mt-2 text-muted-foreground">{error || "The document you're looking for doesn't exist."}</p>
          <Button asChild className="mt-6">
            <Link href="/search">Back to Search</Link>
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto max-w-6xl py-8 px-4">
      <div className="mb-6">
        <Button variant="ghost" onClick={() => router.back()} className="gap-2 -ml-2">
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
      </div>

      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground text-balance">{document.filename}</h1>
        <p className="mt-2 text-muted-foreground">View and manage document details</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <DocumentPreview document={document} />
          <DocumentOCRText />
        </div>

        <div className="space-y-6">
          <DocumentMetadata
            document={document}
            isEditing={isEditing}
            onEdit={() => setIsEditing(true)}
            onSave={updateMetadata}
            onCancel={() => setIsEditing(false)}
          />
          <DocumentActions document={document} onDelete={deleteDocument} />
        </div>
      </div>
    </div>
  )
}
