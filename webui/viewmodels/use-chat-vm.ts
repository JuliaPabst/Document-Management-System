"use client"

import { useState, useCallback } from "react"
import type { ChatMessage, ChatRequest, ChatResponse } from "@/lib/types"
import { apiClient } from "@/api/client"

export function useChatVM() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sessionId] = useState<string>(() => {
    // Generate a unique session ID for this chat session
    return `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
  })

  const sendMessage = useCallback(async (messageContent: string) => {
    if (!messageContent.trim()) return

    setIsLoading(true)
    setError(null)

    // Add user message to the conversation
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: "user",
      content: messageContent.trim(),
      timestamp: new Date(),
      sessionId,
    }

    setMessages((prev) => [...prev, userMessage])

    try {
      // Save user message to database
      await apiClient.saveChatMessage({
        role: "user",
        content: messageContent.trim(),
        sessionId,
      })

      const requestBody: ChatRequest = {
        message: messageContent.trim(),
        conversationHistory: messages,
      }

      const data = await apiClient.generateChatCompletion(requestBody)

      // Add assistant message to the conversation
      const assistantMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: data.message,
        timestamp: new Date(),
        sessionId,
      }

      setMessages((prev) => [...prev, assistantMessage])

      // Save assistant message to database
      await apiClient.saveChatMessage({
        role: "assistant",
        content: data.message,
        sessionId,
      })
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Failed to send message"
      setError(errorMessage)
      
      // Add error message as assistant response
      const errorMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: `Sorry, I encountered an error: ${errorMessage}`,
        timestamp: new Date(),
        sessionId,
      }
      setMessages((prev) => [...prev, errorMsg])
    } finally {
      setIsLoading(false)
    }
  }, [messages, sessionId])

  const clearMessages = useCallback(() => {
    setMessages([])
    setError(null)
  }, [])

  return {
    messages,
    isLoading,
    error,
    sendMessage,
    clearMessages,
  }
}
