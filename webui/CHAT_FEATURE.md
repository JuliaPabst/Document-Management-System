# Chat Feature - AI Document Assistant

## Overview
The chat feature integrates ChatGPT into your Document Management System, allowing users to interact with an AI assistant that has access to all document metadata. Users can ask questions about their documents, search for files, and get insights about their document collection.

## Setup

### 1. Get OpenAI API Key
1. Visit [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in or create an account
3. Create a new API key
4. Copy the API key

### 2. Configure Environment Variable
Edit the `.env.local` file in the `webui` directory:

```bash
OPENAI_API_KEY=sk-your-actual-api-key-here
```

**Important:** Never commit your API key to version control. The `.env.local` file should be in `.gitignore`.

## Features

### 1. **Context-Aware Conversations**
- The AI has access to all file metadata in your system
- Answers questions about specific documents
- Provides statistics about your document collection

### 2. **Natural Language Queries**
Users can ask questions like:
- "How many documents do I have?"
- "Find all PDFs by John Doe"
- "What documents were uploaded today?"
- "Show me the largest files"
- "Who are the most active authors?"

### 3. **Persistent Conversation**
- Maintains conversation history during the session
- Understands context from previous messages
- Can be cleared with the "Clear Chat" button

### 4. **User-Friendly Interface**
- Clean chat UI with message bubbles
- User and assistant avatars
- Timestamps on messages
- Loading indicators
- Auto-scroll to latest messages

## Architecture

### Frontend Components

1. **`app/chat/page.tsx`**
   - Main chat page
   - Integrates all chat components
   - Manages overall layout

2. **`components/chat-messages.tsx`**
   - Displays list of messages
   - Auto-scrolls to new messages
   - Shows empty state

3. **`components/chat-message.tsx`**
   - Individual message component
   - Different styling for user/assistant
   - Shows timestamp

4. **`components/chat-input.tsx`**
   - Message input form
   - Send button
   - Handles form submission

5. **`viewmodels/use-chat-vm.ts`**
   - Chat state management
   - Message sending logic
   - Error handling
   - Conversation history

### Backend API

**`app/api/chat/route.ts`**
- Next.js API route (Edge runtime)
- Fetches file metadata from REST API
- Calls OpenAI Chat Completions API
- Provides file context to ChatGPT
- Returns formatted responses

### Data Flow

```
User Input
    ↓
Chat Input Component
    ↓
useChatVM Hook
    ↓
POST /api/chat
    ↓
Fetch File Metadata (/api/v1/files)
    ↓
Build Context with Files
    ↓
OpenAI API Call (gpt-3.5-turbo)
    ↓
Response to Frontend
    ↓
Display in Chat Messages
```

## API Configuration

### OpenAI Model Settings
The chat uses `gpt-3.5-turbo` with:
- **Temperature:** 0.7 (balanced creativity/accuracy)
- **Max Tokens:** 500 (concise responses)

You can modify these in `app/api/chat/route.ts`:

```typescript
{
  model: 'gpt-3.5-turbo',  // or 'gpt-4' for better quality
  messages,
  temperature: 0.7,        // 0.0-2.0 (lower = more focused)
  max_tokens: 500,         // adjust for longer/shorter responses
}
```

## System Prompt

The AI is configured with the following system prompt:

```
You are a helpful assistant for a document management system called PaperlessWebUI. 
You help users find information about their documents, answer questions about file 
metadata, and provide assistance with document management tasks.

[Document metadata is injected here]

When answering questions:
- Be concise and helpful
- Reference specific documents when relevant
- Help users find documents by name, author, or type
- Provide statistics about their document collection when asked
- Suggest useful actions they can take in the system
```

## Security Considerations

### API Key Protection
- ✅ API key is stored server-side only (`.env.local`)
- ✅ Never exposed to client-side code
- ✅ API route runs on Edge runtime (serverless)
- ✅ `.env.local` should be in `.gitignore`

### Rate Limiting
Consider implementing rate limiting to prevent abuse:
- Limit messages per user per minute
- Implement cost tracking for OpenAI API usage

### Data Privacy
- Document metadata is sent to OpenAI
- Actual file contents are NOT sent
- Only metadata (filename, author, size, type, dates) is shared
- Review OpenAI's data usage policy

## Cost Management

### Pricing (as of 2024)
- **GPT-3.5-turbo:** ~$0.002 per 1K tokens
- Average conversation: ~500-1000 tokens
- Cost per message: ~$0.001-0.002

### Tips to Reduce Costs
1. Use `gpt-3.5-turbo` instead of `gpt-4`
2. Limit `max_tokens` to needed length
3. Implement message caching where possible
4. Monitor usage via OpenAI dashboard

## Usage Example

### Example Conversations

**User:** "How many documents do I have?"
**Assistant:** "You currently have 42 documents in your system."

**User:** "Find PDFs by Alice Johnson"
**Assistant:** "I found 3 PDF documents by Alice Johnson:
1. Report_2024.pdf (2.5 MB)
2. Presentation.pdf (1.2 MB)  
3. Summary.pdf (856 KB)"

**User:** "What's the largest file?"
**Assistant:** "The largest file is 'Video_Tutorial.mp4' by Bob Smith at 45.2 MB, uploaded on October 15, 2024."

## Troubleshooting

### Common Issues

#### 1. "OpenAI API key is not configured"
**Solution:** Add your API key to `.env.local`:
```bash
OPENAI_API_KEY=sk-your-key-here
```
Restart the dev server after adding.

#### 2. "Failed to get response from ChatGPT"
**Possible causes:**
- Invalid API key
- Insufficient API credits
- Network connectivity issues
- OpenAI service downtime

**Solution:** Check OpenAI dashboard for API status and usage limits.

#### 3. No file context in responses
**Possible causes:**
- REST API not accessible
- Network error fetching files

**Solution:** Ensure REST API is running and accessible at the configured URL.

#### 4. Chat not accessible
**Solution:** Verify you're using the correct URL: `http://localhost:8080/chat` (or your deployed URL)

## Files Created

### Frontend
- `app/chat/page.tsx` - Main chat page
- `components/chat-messages.tsx` - Messages container
- `components/chat-message.tsx` - Individual message
- `components/chat-input.tsx` - Input form
- `viewmodels/use-chat-vm.ts` - Chat state management

### Backend
- `app/api/chat/route.ts` - Chat API endpoint

### Configuration
- `.env.local` - Environment variables (local)
- `.env.example` - Environment template
- `lib/types.ts` - Added chat types

### Navigation
- `app/layout.tsx` - Added chat link

## Future Enhancements

1. **Message Persistence**
   - Save conversation history to database
   - Resume conversations across sessions

2. **Document Actions**
   - Allow AI to suggest document operations
   - Direct links to documents in responses

3. **File Content Search**
   - OCR text integration for full-text search
   - Search within document content

4. **Advanced Features**
   - Voice input/output
   - Multi-language support
   - Custom AI personalities
   - Document recommendations

5. **Analytics**
   - Track common questions
   - Usage statistics
   - Cost monitoring dashboard

## Development

### Running Locally
```bash
cd webui
pnpm install
pnpm dev
```

Access the chat at: `http://localhost:3000/chat`

### Testing the API Endpoint
```bash
curl -X POST http://localhost:3000/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How many documents do I have?",
    "conversationHistory": []
  }'
```

## Support

For issues or questions:
1. Check OpenAI API status
2. Verify environment configuration
3. Review server logs for errors
4. Check browser console for client errors
