# PaperlessWebUI

A modern document management system built with Next.js, TypeScript, and the MVVM architecture pattern.

## Features

- **Upload Documents**: Drag & drop interface with progress tracking
- **Search & Filter**: Full-text search with filters for author, file type, and date
- **Document Management**: View, edit metadata, and manage documents
- **Responsive Design**: Modern UI built with radix/ui components
- **Unique File Validation**: Prevents duplicate files with same name and author
  - If a file with the same name from the same author already exists, the REST server returns an error
  - Frontend detects the error and provides user feedback: option to replace the existing file or select a new file

## Architecture

This project follows the **MVVM (Model-View-ViewModel)** pattern:

- **Model** (`/lib/types.ts`, `/api/client.ts`): API types and data layer
- **ViewModel** (`/viewmodels/*`): Hooks that encapsulate business logic and state
- **View** (`/components/*`, `/app/*`): Presentational components

### Connecting to the API

The application requires a connection to the backend REST API. When running with Docker Compose, the API connection is configured automatically through nginx reverse proxy.

For standalone development, set the environment variable:
```bash
NEXT_PUBLIC_API_BASE_URL=/api
```

The application will connect to the REST API and use real data.

## Project Structure

```
/app                        # Next.js App Router pages
  /page.tsx                 # Dashboard
  /search/page.tsx          # Search page
  /upload/page.tsx          # Upload page
  /documents/[id]/page.tsx  # Document detail

/components                 # Presentational components
  /upload-dropzone.tsx
  /search-bar.tsx
  /document-card.tsx
  /document-list.tsx
  ...

/viewmodels                 # Business logic hooks
  /use-upload-vm.ts
  /use-search-vm.ts
  /use-document-vm.ts

/api                        # API client layer
  /client.ts                # REST adapter

/lib                        # Utilities
  /types.ts                 # TypeScript types
  /http.ts                  # HTTP client
  /utils/                   # Helper functions
```

## API Integration

The API client (`/api/client.ts`) uses the RestAdapter pattern to connect to the backend REST API. All HTTP requests are handled through the `HttpClient` wrapper with proper error handling.

To add OpenAPI-generated types:
1. Generate types from `openapi.yaml` in the REST service
2. Replace types in `/lib/types.ts`
3. Update the adapter if needed

## Tech Stack

- **Next.js 15** (App Router)
- **React 18**
- **TypeScript**
- **Tailwind CSS v4**
- **radix-ui** components
- **axios** for data fetching

## Development

```bash
# Install dependencies (using pnpm)
pnpm install

# Run development server
pnpm dev

# Build for production
pnpm build

# Start production server
pnpm start
```

## Running with Docker Compose

The recommended way to run the application is using Docker Compose from the project root:

```bash
docker compose up
```

This will start:
- PostgreSQL database
- REST API backend
- Web UI (this application)
- Nginx reverse proxy
- Lucide SVG icon components

Access the application at: http://localhost:8080

## Environment Variables

- `NEXT_PUBLIC_API_BASE_URL`: Base URL for the REST API (required, default: `/api`)
