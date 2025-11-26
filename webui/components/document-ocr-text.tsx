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
  // Handle multiple markdown patterns
  let processed: (string | JSX.Element)[] = [text]

  // Bold: **text**
  processed = processed.flatMap((item, itemIndex) => {
    if (typeof item !== 'string') return item
    const parts = item.split(/\*\*(.*?)\*\*/g)
    return parts.map((part, index) => 
      index % 2 === 1 ? <strong key={`${itemIndex}-b-${index}`}>{part}</strong> : part
    )
  })

  // Italic: *text*
  processed = processed.flatMap((item, itemIndex) => {
    if (typeof item !== 'string') return item
    const parts = item.split(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g)
    return parts.map((part, index) => 
      index % 2 === 1 ? <em key={`${itemIndex}-i-${index}`}>{part}</em> : part
    )
  })

  // Inline code: `code`
  processed = processed.flatMap((item, itemIndex) => {
    if (typeof item !== 'string') return item
    const parts = item.split(/`([^`]+)`/g)
    return parts.map((part, index) => 
      index % 2 === 1 ? (
        <code key={`${itemIndex}-c-${index}`} className="px-1.5 py-0.5 rounded bg-muted font-mono text-xs">
          {part}
        </code>
      ) : part
    )
  })

  return processed
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
              {document.summary.split('\n').map((line, index) => {
                // Handle headings (# Heading)
                const headingMatch = line.match(/^(#{1,3})\s+(.+)$/)
                if (headingMatch) {
                  const level = headingMatch[1].length
                  const text = headingMatch[2]
                  const HeadingTag = `h${Math.min(level + 2, 6)}` as keyof JSX.IntrinsicElements
                  return (
                    <HeadingTag key={index} className="font-semibold text-base mt-3 first:mt-0">
                      {renderMarkdown(text)}
                    </HeadingTag>
                  )
                }

                // Handle unordered lists (- item or * item)
                const listMatch = line.match(/^[\s]*[-*]\s+(.+)$/)
                if (listMatch) {
                  return (
                    <li key={index} className="ml-4 list-disc">
                      {renderMarkdown(listMatch[1])}
                    </li>
                  )
                }

                // Handle numbered lists (1. item)
                const numberedMatch = line.match(/^[\s]*\d+\.\s+(.+)$/)
                if (numberedMatch) {
                  return (
                    <li key={index} className="ml-4 list-decimal">
                      {renderMarkdown(numberedMatch[1])}
                    </li>
                  )
                }

                // Regular paragraph
                return line.trim() ? (
                  <p key={index} className="whitespace-pre-wrap">
                    {renderMarkdown(line)}
                  </p>
                ) : (
                  <div key={index} className="h-2" />
                )
              })}
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
