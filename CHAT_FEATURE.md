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
Edit the `.env` file in the root directory:

```bash
OPENAI_API_KEY=sk-your-actual-api-key-here
```

**Important:** Never commit your API key to version control. The `.env` file should be in `.gitignore`.

## Features

### 1. **Context-Aware Conversations**
- The AI has access to all file metadata in your system
- Answers questions about specific documents
- Provides statistics about your document collection

### 2. **Natural Language Queries**
Users can ask questions like:
- "How many documents do I have?"
- "Find all PDFs by Max"
- "What documents were uploaded today?"
- "Show me the largest files"
- "Who are the most active authors?"

### 3. **Persistent Conversation**
- Maintains conversation history during the session
- Understands context from previous messages
- Can be cleared with the "Clear Chat" button

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

6. **`api/client.ts`**
   - REST API adapter
   - `generateChatCompletion()` method calls `/api/v1/chat`
   - Handles HTTP communication

### Backend API (Spring Boot REST Service)

1. **`controller/ChatController.java`**
   - REST endpoint: `POST /api/v1/chat`
   - Accepts chat requests from frontend
   - Returns AI-generated responses

2. **`service/OpenAIService.java`**
   - Calls OpenAI Chat Completions API
   - Fetches file metadata from database
   - Builds context-aware system prompts
   - Includes document statistics and lists

3. **`dto/ChatCompletionRequestDto.java`**
   - Request DTO with message and conversation history

4. **`dto/ChatCompletionResponseDto.java`**
   - Response DTO with AI-generated message

5. **Database Integration**
   - Fetches all file metadata via `FileMetadataService`
   - Provides complete document context to AI
   - No direct SQL - uses JPA repositories

## API Configuration

### OpenAI Model Settings
The chat uses `gpt-4.1-mini` with:
- **Temperature:** 0.3 (high accuracy, less creativity)
- **Max Tokens:** 500 (concise responses)

You can modify these in `rest/src/main/resources/application.properties`:

```properties
openai.api.key=${OPENAI_API_KEY:}
openai.api.url=https://api.openai.com/v1/chat/completions
openai.model=gpt-4.1-mini
openai.temperature=0.3
openai.max.tokens=500
```