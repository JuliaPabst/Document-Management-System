"use client"

import { useState, useCallback, useEffect } from "react"
import type { ChatMessage, ChatRequest, ChatResponse } from "@/lib/types"
import { apiClient } from "@/api/client"

const SESSION_STORAGE_KEY = "chat-session-id"

export function useChatVM() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [isLoadingHistory, setIsLoadingHistory] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [sessionId, setSessionId] = useState<string>(() => {
    // Try to restore previous session from localStorage
    if (typeof window !== "undefined") {
      const stored = localStorage.getItem(SESSION_STORAGE_KEY)
      if (stored) return stored
    }
    // Generate a new session ID
    const newSessionId = `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
    if (typeof window !== "undefined") {
      localStorage.setItem(SESSION_STORAGE_KEY, newSessionId)
    }
    return newSessionId
  })

  // Load conversation history on mount
  useEffect(() => {
    const loadHistory = async () => {
      try {
        setIsLoadingHistory(true)
        const history = await apiClient.getChatMessages(sessionId)
        
        // Convert DTO to ChatMessage format
        const loadedMessages: ChatMessage[] = history.map((dto) => ({
          id: dto.id.toString(),
          role: dto.role as "user" | "assistant",
          content: dto.content,
          timestamp: new Date(dto.timestamp),
          sessionId: dto.sessionId || undefined,
        }))
        
        setMessages(loadedMessages)
      } catch (err) {
        console.error("Failed to load chat history:", err)
        // Don't show error to user, just start with empty chat
      } finally {
        setIsLoadingHistory(false)
      }
    }

    loadHistory()
  }, [sessionId])

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

  const clearMessages = useCallback(async () => {
    try {
      // Delete messages from database
      await apiClient.deleteChatMessages(sessionId)
      
      // Clear local state
      setMessages([])
      setError(null)
      
      // Generate new session ID
      const newSessionId = `session-${Date.now()}-${Math.random().toString(36).substring(7)}`
      setSessionId(newSessionId)
      
      // Update localStorage
      if (typeof window !== "undefined") {
        localStorage.setItem(SESSION_STORAGE_KEY, newSessionId)
      }
    } catch (err) {
      console.error("Failed to clear chat history:", err)
      setError("Failed to clear conversation history")
    }
  }, [sessionId])

  return {
    messages,
    isLoading,
    isLoadingHistory,
    error,
    sessionId,
    sendMessage,
    clearMessages,
  }
}
