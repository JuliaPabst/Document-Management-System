import Link from "next/link"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Upload, Search, FileText } from "lucide-react"

export function QuickActions() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Quick Actions</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <Button asChild className="w-full justify-start gap-3" size="lg">
          <Link href="/upload">
            <Upload className="h-5 w-5" />
            Upload Document
          </Link>
        </Button>
        <Button asChild variant="outline" className="w-full justify-start gap-3 bg-transparent" size="lg">
          <Link href="/search">
            <Search className="h-5 w-5" />
            Search Documents
          </Link>
        </Button>
        <Button asChild variant="outline" className="w-full justify-start gap-3 bg-transparent" size="lg">
          <Link href="/search">
            <FileText className="h-5 w-5" />
            Browse All
          </Link>
        </Button>
      </CardContent>
    </Card>
  )
}
