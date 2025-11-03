"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { ChatMessages } from "@/components/chat-messages"
import { ChatInput } from "@/components/chat-input"
import { useChatVM } from "@/viewmodels/use-chat-vm"
import { Trash2 } from "lucide-react"

export default function ChatPage() {
  const { messages, isLoading, sendMessage, clearMessages } = useChatVM()

  return (
    <div className="container mx-auto max-w-5xl py-8 px-4 h-[calc(100vh-8rem)]">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">Document Assistant</h1>
          <p className="mt-2 text-muted-foreground">
            Chat with AI about your documents and get instant answers
          </p>
        </div>
        {messages.length > 0 && (
          <Button variant="outline" size="sm" onClick={clearMessages} className="gap-2">
            <Trash2 className="h-4 w-4" />
            Clear Chat
          </Button>
        )}
      </div>

      <Card className="flex flex-col h-[calc(100%-6rem)]">
        <CardHeader className="border-b border-border">
          <CardTitle className="text-lg">Chat</CardTitle>
          <CardDescription>
            Ask questions about your documents, search for files, or get statistics
          </CardDescription>
        </CardHeader>
        <CardContent className="flex-1 flex flex-col p-0 overflow-hidden">
          <ChatMessages messages={messages} isLoading={isLoading} />
          <ChatInput onSendMessage={sendMessage} isLoading={isLoading} />
        </CardContent>
      </Card>
    </div>
  )
}
