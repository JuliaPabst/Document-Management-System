"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"

interface FileTypeChartProps {
  fileTypes: Record<string, number>
}

export function FileTypeChart({ fileTypes }: FileTypeChartProps) {
  const entries = Object.entries(fileTypes).sort((a, b) => b[1] - a[1])
  const total = entries.reduce((sum, [, count]) => sum + count, 0)

  if (entries.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">File Types</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">No documents yet</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">File Types</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {entries.map(([type, count]) => {
          const percentage = ((count / total) * 100).toFixed(0)
          return (
            <div key={type} className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">{type}</Badge>
                  <span className="text-muted-foreground">{count} files</span>
                </div>
                <span className="font-medium text-foreground">{percentage}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-secondary">
                <div className="h-full bg-primary transition-all" style={{ width: `${percentage}%` }} />
              </div>
            </div>
          )
        })}
      </CardContent>
    </Card>
  )
}
