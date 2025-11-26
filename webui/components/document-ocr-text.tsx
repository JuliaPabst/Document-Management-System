"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ChevronDown, ChevronUp, Sparkles } from "lucide-react"
import type { FileMetadata } from "@/lib/types"

interface DocumentSummaryProps {
  document: FileMetadata
}

function renderMarkdown(text: string) {
  // Split by **text** pattern and render bold
  const parts = text.split(/\*\*(.*?)\*\*/g)
  return parts.map((part, index) => {
    // Odd indices are the content between ** **
    if (index % 2 === 1) {
      return <strong key={index}>{part}</strong>
    }
    return part
  })
}

export function DocumentSummary({ document }: DocumentSummaryProps) {
  const [isExpanded, setIsExpanded] = useState(false)

  const hasSummary = document.summary && document.summary.trim().length > 0

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-primary" />
          <CardTitle className="text-lg">AI-Generated Summary</CardTitle>
        </div>
        {hasSummary && (
          <Button variant="ghost" size="sm" onClick={() => setIsExpanded(!isExpanded)} className="gap-2">
            {isExpanded ? (
              <>
                <ChevronUp className="h-4 w-4" />
                Collapse
              </>
            ) : (
              <>
                <ChevronDown className="h-4 w-4" />
                Expand
              </>
            )}
          </Button>
        )}
      </CardHeader>
      {isExpanded && hasSummary && document.summary && (
        <CardContent>
          <div className="rounded-lg border border-border bg-muted/30 p-4">
            <div className="text-sm text-foreground leading-relaxed space-y-2">
              {document.summary.split('\n').map((line, index) => (
                <p key={index} className="whitespace-pre-wrap">
                  {renderMarkdown(line)}
                </p>
              ))}
            </div>
          </div>
        </CardContent>
      )}
      {!hasSummary && (
        <CardContent>
          <div className="rounded-lg border border-dashed border-border bg-muted/20 p-4 text-center">
            <p className="text-sm text-muted-foreground">Summary is being generated...</p>
            <p className="text-xs text-muted-foreground mt-1">This may take a few seconds after upload.</p>
          </div>
        </CardContent>
      )}
    </Card>
  )
}
