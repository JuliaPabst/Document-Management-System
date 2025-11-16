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
- Integrated RabbitMQ messaging: REST service sends file metadata to worker queues on document upload
- Configured bidirectional message flow: workers process messages and send results back to result queues
- Implemented result listeners in REST service to receive and process worker responses
- Externalized all credentials to .env file for secure configuration management