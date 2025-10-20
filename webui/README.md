# PaperlessWebUI

A modern document management system built with Next.js, TypeScript, and the MVVM architecture pattern.

## Features

- **Upload Documents**: Drag & drop interface with progress tracking
- **Search & Filter**: Full-text search with filters for author, file type, and date
- **Document Management**: View, edit metadata, and manage documents
- **Mock Mode**: Runs without backend API for development and preview

## Architecture

This project follows the **MVVM (Model-View-ViewModel)** pattern:

- **Model** (`/lib/types.ts`, `/api/client.ts`): API types and data layer
- **ViewModel** (`/viewmodels/*`): Hooks that encapsulate business logic and state
- **View** (`/components/*`, `/app/*`): Presentational components

### Connecting to Real API

To connect to the Paperless REST API:

1. Set the environment variable:
   ```bash
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
   ```

2. The app will automatically switch to REST mode and use the real API

## Project Structure

```
/app                    # Next.js App Router pages
  /page.tsx            # Dashboard
  /search/page.tsx     # Search page
  /upload/page.tsx     # Upload page
  /documents/[id]/page.tsx  # Document detail

/components            # Presentational components
  /upload-dropzone.tsx
  /search-bar.tsx
  /document-card.tsx
  /document-list.tsx
  ...

/viewmodels           # Business logic hooks
  /use-upload-vm.ts
  /use-search-vm.ts
  /use-document-vm.ts

/api                  # API client layer
  /client.ts         # REST and Mock adapters

/mocks               # Mock data for preview
  /documents.ts

/lib                 # Utilities
  /types.ts         # TypeScript types
  /http.ts          # HTTP client
  /utils/           # Helper functions
```

## API Integration

The API client (`/api/client.ts`) uses an adapter pattern:

- **RestAdapter**: Connects to real Paperless API
- **MockAdapter**: In-memory mock for development

To add OpenAPI-generated types later:
1. Generate types from `openapi.yaml`
2. Replace types in `/lib/types.ts`
3. Update adapters if needed

## Tech Stack

- **Next.js 15** (App Router)
- **React 18**
- **TypeScript**
- **Tailwind CSS v4**
- **shadcn/ui** components
- **SWR** for data fetching

## Development

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build
```

## Environment Variables

- `NEXT_PUBLIC_API_BASE_URL`: Base URL for Paperless API (optional, defaults to mock mode)

## License

MIT
