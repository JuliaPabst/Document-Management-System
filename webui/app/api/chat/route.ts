import { NextRequest, NextResponse } from 'next/server'
import type { ChatRequest, ChatResponse } from '@/lib/types'

export const runtime = 'edge'

export async function POST(request: NextRequest) {
  try {
    const { message, conversationHistory }: ChatRequest = await request.json()

    const apiKey = process.env.OPENAI_API_KEY
    if (!apiKey) {
      return NextResponse.json(
        { error: 'OpenAI API key is not configured' } as ChatResponse,
        { status: 500 }
      )
    }

    // Always fetch file metadata to provide current context
    // This ensures the AI always has up-to-date information
    let systemMessage = `You are a helpful assistant for a document management system called Paperless. You help users find information about their documents, answer questions about file metadata, and provide assistance with document management tasks.`
    
    // Fetch all file metadata to provide as context
    // Use nginx as reverse proxy (same as client.ts does from browser)
    const nginxUrl = process.env.NGINX_URL || 'http://nginx'
    
    try {
      const filesResponse = await fetch(`${nginxUrl}/api/v1/files`, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      
      if (filesResponse.ok) {
        const files = await filesResponse.json()
          
          // Calculate statistics from the data
          const totalFiles = files.length
          const uniqueAuthors = [...new Set(files.map((f: any) => f.author))].sort()
          const fileTypeGroups = files.reduce((acc: any, f: any) => {
            acc[f.fileType] = (acc[f.fileType] || 0) + 1
            return acc
          }, {})
          const totalSize = files.reduce((sum: number, f: any) => sum + f.size, 0)
          
          // Format the context in a structured way
          const filesContext = `

=== DOCUMENT DATABASE INFORMATION ===
This information represents the COMPLETE and ACCURATE state of the document database. You MUST use ONLY this data to answer questions. DO NOT make up or hallucinate any information.

STATISTICS:
- Total Documents: ${totalFiles}
- Total Authors: ${uniqueAuthors.length}
- Total Storage Used: ${(totalSize / (1024 * 1024)).toFixed(2)} MB

AUTHORS LIST (Complete):
${uniqueAuthors.map((author, i) => `${i + 1}. ${author}`).join('\n')}

FILE TYPES DISTRIBUTION:
${Object.entries(fileTypeGroups).map(([type, count]) => `- ${type}: ${count} file(s)`).join('\n')}

COMPLETE DOCUMENT LIST:
${files.map((f: any, i: number) => 
  `${i + 1}. "${f.filename}" by ${f.author} (${f.fileType}, ${(f.size / 1024).toFixed(2)} KB, uploaded: ${new Date(f.uploadTime).toLocaleDateString()})`
).join('\n')}

CRITICAL INSTRUCTIONS:
- You have READ-ONLY access to this data
- ALWAYS base your answers on the data above - DO NOT hallucinate or make up information
- If a user asks about documents, authors, or statistics, count and reference the data above
- When asked "how many", count from the lists above
- When asked about specific files or authors, search in the lists above
- If information is not in the data above, say "I don't have information about that in the current database"
- You CANNOT execute SQL queries or modify the database
- All data is pre-fetched and sanitized for security

Remember this information for the entire conversation. Use it to answer all questions about documents.`
          
          systemMessage += filesContext
        }
      } catch (error) {
        console.error('Failed to fetch file metadata:', error)
        systemMessage += '\n\nNote: Could not fetch document database information. Please try again later.'
      }

    // Build messages for OpenAI API
    const messages = [
      {
        role: 'system',
        content: systemMessage
      },
      ...conversationHistory.map(msg => ({
        role: msg.role,
        content: msg.content
      })),
      {
        role: 'user',
        content: message
      }
    ]

    // Call OpenAI API
    const openaiResponse = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: 'gpt-3.5-turbo',
        messages,
        temperature: 0.3, 
        max_tokens: 500,
      }),
    })

    if (!openaiResponse.ok) {
      const error = await openaiResponse.text()
      console.error('OpenAI API error:', error)
      return NextResponse.json(
        { error: 'Failed to get response from ChatGPT' } as ChatResponse,
        { status: 500 }
      )
    }

    const data = await openaiResponse.json()
    const assistantMessage = data.choices[0]?.message?.content || 'No response generated'

    return NextResponse.json({
      message: assistantMessage
    } as ChatResponse)

  } catch (error) {
    console.error('Chat API error:', error)
    return NextResponse.json(
      { error: 'An error occurred while processing your request' } as ChatResponse,
      { status: 500 }
    )
  }
}
