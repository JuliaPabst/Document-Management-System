import { UploadForm } from "@/components/upload-form"

export default function UploadPage() {
  return (
    <div className="container mx-auto max-w-4xl py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Upload Documents</h1>
        <p className="mt-2 text-muted-foreground">
          Add new documents to your paperless system with drag and drop or file selection
        </p>
      </div>

      <UploadForm />
    </div>
  )
}
