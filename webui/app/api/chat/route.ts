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

    // Fetch all file metadata to provide as context
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL 
    let filesContext = ''
    
    try {
      // Get the base URL from the request
      const baseUrl = request.headers.get('origin') || 'http://localhost:3000'
      const filesResponse = await fetch(`${baseUrl}${apiBaseUrl}/v1/files`, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      
      if (filesResponse.ok) {
        const files = await filesResponse.json()
        filesContext = `\n\nYou have access to the following documents in the system:\n${JSON.stringify(files, null, 2)}\n\nUse this information to answer questions about the documents.`
      }
    } catch (error) {
      console.error('Failed to fetch file metadata:', error)
      // Continue without file context
    }

    // Build messages for OpenAI API
    const messages = [
      {
        role: 'system',
        content: `You are a helpful assistant for a document management system called PaperlessWebUI. You help users find information about their documents, answer questions about file metadata, and provide assistance with document management tasks.${filesContext}
        
When answering questions:
- Be concise and helpful
- Reference specific documents when relevant
- Help users find documents by name, author, or type
- Provide statistics about their document collection when asked
- Suggest useful actions they can take in the system`
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
        temperature: 0.7,
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
