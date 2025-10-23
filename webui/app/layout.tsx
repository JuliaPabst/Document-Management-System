import type React from "react"
import type { Metadata } from "next"
import { GeistSans } from "geist/font/sans"
import { GeistMono } from "geist/font/mono"
import "./globals.css"
import Link from "next/link"
import { FileText, Search, Upload, LayoutDashboard, MessageSquare } from "lucide-react"
import { Suspense } from "react"

export const metadata: Metadata = {
  title: "Paperless - Document Management System",
  description: "Modern document management system built with Next.js",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`font-sans ${GeistSans.variable} ${GeistMono.variable}`}>
        <Suspense fallback={<div>Loading...</div>}>
          <header className="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
            <div className="container mx-auto flex h-16 items-center px-4">
              <Link href="/" className="flex items-center gap-2 font-semibold text-foreground">
                <FileText className="h-6 w-6 text-primary" />
                <span className="text-lg">Paperless</span>
              </Link>

              <nav className="ml-auto flex items-center gap-1">
                <Link
                  href="/"
                  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                >
                  <LayoutDashboard className="h-4 w-4" />
                  Dashboard
                </Link>
                <Link
                  href="/search"
                  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                >
                  <Search className="h-4 w-4" />
                  Search
                </Link>
                <Link
                  href="/upload"
                  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                >
                  <Upload className="h-4 w-4" />
                  Upload
                </Link>
                <Link
                  href="/chat"
                  className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
                >
                  <MessageSquare className="h-4 w-4" />
                  Chat
                </Link>
              </nav>
            </div>
          </header>
        </Suspense>

        <main className="min-h-[calc(100vh-4rem)]">{children}</main>
      </body>
    </html>
  )
}
