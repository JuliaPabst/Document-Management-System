"use client"

import { CheckCircle2, Circle, Loader2 } from "lucide-react"
import { cn } from "@/lib/utils"
import type { UploadStatus } from "@/lib/types"

interface TimelineStep {
  label: string
  status: "complete" | "active" | "pending"
}

interface UploadStatusTimelineProps {
  currentStatus: UploadStatus
}

export function UploadStatusTimeline({ currentStatus }: UploadStatusTimelineProps) {
  const steps: TimelineStep[] = [
    {
      label: "Uploaded",
      status: currentStatus === "idle" ? "pending" : currentStatus === "uploading" ? "active" : "complete",
    },
    {
      label: "Processing",
      status:
        currentStatus === "idle" || currentStatus === "uploading"
          ? "pending"
          : currentStatus === "processing"
            ? "active"
            : "complete",
    },
    {
      label: "Complete",
      status: currentStatus === "success" ? "complete" : currentStatus === "error" ? "pending" : "pending",
    },
  ]

  if (currentStatus === "idle" || currentStatus === "error") {
    return null
  }

  return (
    <div className="w-full max-w-2xl">
      <div className="flex items-center justify-between">
        {steps.map((step, index) => (
          <div key={step.label} className="flex flex-1 items-center">
            <div className="flex flex-col items-center gap-2">
              <div
                className={cn(
                  "flex h-10 w-10 items-center justify-center rounded-full border-2 transition-colors",
                  step.status === "complete" && "border-primary bg-primary text-primary-foreground",
                  step.status === "active" && "border-primary bg-background text-primary",
                  step.status === "pending" && "border-border bg-background text-muted-foreground",
                )}
              >
                {step.status === "complete" && <CheckCircle2 className="h-5 w-5" />}
                {step.status === "active" && <Loader2 className="h-5 w-5 animate-spin" />}
                {step.status === "pending" && <Circle className="h-5 w-5" />}
              </div>
              <span
                className={cn(
                  "text-xs font-medium",
                  step.status === "complete" && "text-primary",
                  step.status === "active" && "text-primary",
                  step.status === "pending" && "text-muted-foreground",
                )}
              >
                {step.label}
              </span>
            </div>

            {index < steps.length - 1 && (
              <div
                className={cn(
                  "mx-2 h-0.5 flex-1 transition-colors",
                  steps[index + 1].status === "complete" || steps[index + 1].status === "active"
                    ? "bg-primary"
                    : "bg-border",
                )}
              />
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
