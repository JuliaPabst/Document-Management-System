'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { FileText, Search, Upload, LayoutDashboard, MessageSquare, Menu, X } from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/search', label: 'Search', icon: Search },
  { href: '/upload', label: 'Upload', icon: Upload },
  { href: '/chat', label: 'Chat', icon: MessageSquare },
]

export function NavMenu() {
  const [isOpen, setIsOpen] = useState(false)
  const pathname = usePathname()

  const isActive = (href: string) => {
    if (href === '/') {
      return pathname === '/'
    }
    return pathname.startsWith(href)
  }

  return (
    <>
      {/* Desktop Navigation */}
      <nav className="ml-auto hidden items-center gap-1 md:flex">
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-foreground",
                isActive(item.href)
                  ? "bg-accent text-foreground"
                  : "text-muted-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </Link>
          )
        })}
      </nav>

      {/* Mobile Menu Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="ml-auto flex items-center justify-center rounded-md p-2 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground md:hidden"
        aria-label="Toggle menu"
      >
        {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </button>

      {/* Mobile Navigation Overlay */}
      {isOpen && (
        <>
          <div
            className="fixed inset-0 top-16 z-40 bg-background/80 backdrop-blur-sm md:hidden"
            onClick={() => setIsOpen(false)}
          />
          <nav className="fixed left-0 right-0 top-16 z-50 border-b border-border bg-background shadow-lg md:hidden">
            <div className="container mx-auto flex flex-col p-4">
              {navItems.map((item) => {
                const Icon = item.icon
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setIsOpen(false)}
                    className={cn(
                      "flex items-center gap-3 rounded-md px-4 py-3 text-base font-medium transition-colors hover:bg-accent hover:text-foreground",
                      isActive(item.href)
                        ? "bg-accent text-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <Icon className="h-5 w-5" />
                    {item.label}
                  </Link>
                )
              })}
            </div>
          </nav>
        </>
      )}
    </>
  )
}
