"use client"

import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ChevronDown, ChevronUp } from "lucide-react"

const MOCK_OCR_TEXT = `This is a sample OCR text extracted from the document. In a real implementation, this would contain the actual text content extracted from the document using OCR technology.`

export function DocumentOCRText() {
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-lg">OCR Text</CardTitle>
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
      </CardHeader>
      {isExpanded && (
        <CardContent>
          <div className="rounded-lg border border-border bg-muted/30 p-4">
            <pre className="whitespace-pre-wrap font-mono text-xs text-foreground leading-relaxed">{MOCK_OCR_TEXT}</pre>
          </div>
        </CardContent>
      )}
    </Card>
  )
}
