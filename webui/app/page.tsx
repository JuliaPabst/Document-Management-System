"use client"

import { StatCard } from "@/components/stat-card"
import { FileTypeChart } from "@/components/file-type-chart"
import { RecentDocuments } from "@/components/recent-documents"
import { QuickActions } from "@/components/quick-actions"
import { useDashboardVM } from "@/viewmodels/use-dashboard-vm"
import { FileText, HardDrive, Loader2 } from "lucide-react"
import { formatFileSize } from "@/lib/utils/format"

export default function DashboardPage() {
  const { stats, isLoading, error } = useDashboardVM()

  if (isLoading) {
    return (
      <div className="container mx-auto max-w-7xl py-8 px-4">
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    )
  }

  if (error || !stats) {
    return (
      <div className="container mx-auto max-w-7xl py-8 px-4">
        <div className="rounded-lg border border-destructive bg-destructive/10 p-4">
          <p className="text-sm text-destructive">{error || "Failed to load dashboard"}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto max-w-7xl py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Dashboard</h1>
        <p className="mt-2 text-muted-foreground">Overview of your paperless document system</p>
      </div>

      <div className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <StatCard
            title="Total Documents"
            value={stats.totalDocuments.toString()}
            icon={FileText}
            description="Documents in your system"
          />
          <StatCard
            title="Total Storage"
            value={formatFileSize(stats.totalSize)}
            icon={HardDrive}
            description="Space used by documents"
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <RecentDocuments documents={stats.recentDocuments} />
            <FileTypeChart fileTypes={stats.fileTypes} />
          </div>

          <div>
            <QuickActions />
          </div>
        </div>
      </div>
    </div>
  )
}
