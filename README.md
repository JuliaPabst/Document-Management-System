# Document-Management-System
A Document management system for archiving documents in a FileStore, with automatic OCR (queue for OC-recognition), automatic summary generation (using Gen-AI), tagging and full text search (ElasticSearch).

## Sprint 1
- Scaffolded the REST API project structure.
- Implemented initial endpoints for document management.
- Integrated PostgreSQL as the database and configured connection settings.
- Added Docker support for the REST service and database, including a docker-compose.yml for easy local development.
- Verified database connectivity and basic CRUD operations.
- Prepared the REST branch for merging by ensuring stable builds and successful integration tests.

### Additional information 
- Used Springboot, Lombok and Mapstruct frameworks
- Decided to use Codefirst approach 

## Sprint 2
- Add webui service and nginx service as proxy between ui and rest service
- Add webui with functionality that is described in README.md in webui folder
- Add special feature: AI Chatbot Integration: Look at CHAT_FEATURE.md file to integrate chat functionality locally by adding your own OPENAI API key

## Sprint 3
- Implemented RabbitMQ as a standalone component for asynchronous message processing
- Created message queuing infrastructure with 4 queues: ocr-worker-queue, genai-worker-queue, ocr-result-queue, genai-result-queue
- Built paperlessWorkers Spring Boot application with separate OCR and GenAI worker services
- Integrated RabbitMQ messaging: REST service sends file metadata to worker queues on document upload (POST) and file replacement (PATCH with file, skipped for metadata-only updates)
- Configured bidirectional message flow: workers process messages and send results back to result queues
- Implemented result listeners in REST service to receive and process worker responses
- Added logging at critical positions (message sending, receiving, processing)
- Layer-specific exception handling with GlobalExceptionHandler
- Externalized all credentials to .env file for secure configuration management

## Sprint 4: Worker Services (OCR, MinIO)
- **Queue Architecture Refactoring**: Optimized message flow from 4 queues to 3 queues
  - **Original Design (Sprint 3)**: 4 separate queues (ocr-worker-queue, genai-worker-queue, ocr-result-queue, genai-result-queue)
  - **Refactored Design (Sprint 4)**: 3 queues with streamlined flow
    - `ocr-worker-queue`: REST → OCR Worker
    - `genai-worker-queue`: OCR Worker → GenAI Worker (eliminated ocr-result-queue, direct worker-to-worker communication)
    - `genai-result-queue`: GenAI Worker → REST (only final results go back to REST)
- **MinIO Integration**: MinIO object storage
  - Configured MinIO service in docker-compose.yml with ports 9000 (API) and 9090 (Console)
  - Implemented MinIOFileStorage service for CRUD operations
  - Integrated MinIO SDK 8.5.17 with bucket "documents"
  - Updated REST service to store all PDF documents in MinIO
- **OCR Worker Service**: OCR processing service in paperlessWorkers application
  - Integrated Tesseract OCR v5.13.0 with Ghostscript for PDF-to-image conversion
  - Configured multi-language support (English + German + OSD)
  - Implemented TesseractOcrService with PDF handling at 300 DPI resolution
  - OcrWorker listens to ocr-worker-queue, downloads files from MinIO, performs OCR, sends results directly to genai-worker-queue
- **Docker Configuration**: Extended docker-compose.yml to include MinIO and OCR-worker containers
- **Logging**: Added logging at critical points (upload, OCR processing, queue communication)
- **TODO**: Add unit tests for OCR service and MinIO integration

## Sprint 5: Generative AI Integration
- **GenAI Worker Service**: GenAI processing service in paperlessWorkers application
  - Implemented OpenAIService for AI summary generation using OpenAI Chat Completions API
  - Configured to use gpt-4o-mini model with temperature 0.3 and max tokens 300
  - GenAIWorker receives OCR text from genai-worker-queue, calls OpenAI API, sends summary to genai-result-queue
  - Implemented fallback mechanism with placeholder summaries when API key not configured
- **Database Integration**: Extended FileMetadata entity with summary field
  - GenAIResultListener in REST service receives summary from result queue
  - FileMetadataService.updateSummary() saves AI-generated summaries to database
  - Summary cleared on file replacement to trigger regeneration
- **UI Enhancements**: 
  - DocumentSummary component with AI-generated summary display
  - Implemented auto-refresh every 5 seconds when summary is pending
  - Added advanced markdown rendering: bold (**text**), italic (*text*), inline code (`code`), headings, numbered lists, bullet points
- **Configuration**: All OpenAI settings externalized to application.properties and .env file
- **Error Handling & Logging**: Exception handling for API failures, logging for GenAI processing, API calls, and database updates
- **TODO**: Add unit tests for GenAI service and summary generation

## Architecture Overview
```
Upload PDF → REST API → MinIO Storage
           ↓
       OCR Queue → OCR Worker (Tesseract + Ghostscript)
           ↓
     GenAI Queue → GenAI Worker (OpenAI API)
           ↓
  GenAI Result Queue → REST API → Database (Summary saved)
           ↓
         UI (Auto-refresh until summary available)
```

## Technology Stack
- **Storage**: MinIO 8.5.17 (Object Storage)
- **OCR**: Tesseract v5.13.0 + Ghostscript (PDF processing)
- **AI**: OpenAI API
- **Message Queue**: RabbitMQ
- **Database**: PostgreSQL
- **Backend**: Spring Boot 3.5.4
- **Frontend**: Next.js with TypeScript
- **Container**: Docker + docker-compose