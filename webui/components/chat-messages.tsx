"use client"

import { useRef, useEffect } from "react"
import { ChatMessage } from "./chat-message"
import type { ChatMessage as ChatMessageType } from "@/lib/types"
import { Loader2 } from "lucide-react"

interface ChatMessagesProps {
  messages: ChatMessageType[]
  isLoading: boolean
}

export function ChatMessages({ messages, isLoading }: ChatMessagesProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  if (messages.length === 0 && !isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center p-8">
        <h3 className="text-lg font-semibold text-foreground mb-2">Start a conversation</h3>
        <p className="text-sm text-muted-foreground max-w-md">
          Ask me anything about your documents! I can help you find files, get statistics, or answer questions about
          your document collection.
        </p>
      </div>
    )
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-4">
      {messages.map((message) => (
        <ChatMessage key={message.id} message={message} />
      ))}
      {isLoading && (
        <div className="flex items-center gap-2 text-muted-foreground">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span className="text-sm">Thinking...</span>
        </div>
      )}
      <div ref={messagesEndRef} />
    </div>
  )
}
