# Integration Tests Documentation

This document describes the integration testing setup and best practices for the REST service.

## Overview

The integration tests use the following technologies and patterns:
- **Spring Boot Test** - For loading application context
- **Testcontainers** - For running real PostgreSQL, RabbitMQ, MinIO, and optionally OCR worker containers in Docker
- **MockMvc** - For simulating HTTP requests
- **AssertJ** - For fluent assertions
- **JUnit 5** - Test framework
- **Awaitility** - For asynchronous messaging assertions

## Running Tests

### Prerequisites
- Docker must be running (for Testcontainers)
- Java 21
- Maven 3.8+

### Run all tests
```bash
cd rest
./mvnw test
```

### Run specific test
```bash
# Run file upload E2E test with simulated OCR (fast)
./mvnw test -Dtest=FileUploadPipelineE2EIT

# Run file upload E2E test with real OCR worker (full integration)
./mvnw test -Dtest=FileUploadPipelineE2EIT -Dtest.ocr.enabled=true

# Run chat conversation E2E test
./mvnw test -Dtest=ChatConversationE2EIT
```

## Test Categories

### 1. End-to-End Pipeline Tests (@SpringBootTest + @AutoConfigureMockMvc)

**Purpose**: Test complete document processing workflow from upload to summary generation  
**Example**: `FileUploadPipelineE2EIT.java`

**What it tests**:
1. **File upload** via REST API (multipart form data)
2. **File storage** in MinIO (Testcontainers)
3. **Message queuing** - Verifies message sent to OCR queue
4. **OCR processing** - Real Tesseract OCR (when enabled) or simulated
5. **Message flow** - OCR result sent to GenAI queue
6. **GenAI processing** - Summary generation (simulated)
7. **Database updates** - Final state with summary
8. **API retrieval** - Verify complete document metadata

**Key Features**:
- **Two modes of operation**:
  - **Simulated OCR** (default): Fast execution, no Docker build required
  - **Real OCR** (`-Dtest.ocr.enabled=true`): Full integration with actual Tesseract OCR container
- Uses **real infrastructure** via Testcontainers:
  - PostgreSQL for database operations
  - RabbitMQ for message queuing
  - MinIO for object storage
  - OCR Worker container (when enabled) - automatically built from paperlessWorkers/Dockerfile
- Tests **asynchronous workflows** using Awaitility
- Validates **message passing** between services
- Verifies **end-to-end data flow** from HTTP request to database persistence

**OCR Worker Container**:
- Automatically built and started by Testcontainers when `-Dtest.ocr.enabled=true`
- Built from `paperlessWorkers/Dockerfile` (includes Tesseract OCR)
- Image name: `paperless-workers-test:latest`
- First run takes several minutes for Docker build
- Subsequent runs reuse built image (faster)
- Container configuration in `TestcontainersConfiguration.java`:
  - Connects to same network as RabbitMQ and MinIO
  - Auto-configured environment variables
  - Waits for application startup before tests proceed

**When to use each mode**:
- **Simulated OCR**: Fast feedback during development, CI/CD pipelines
- **Real OCR**: Final validation before deployment, testing OCR accuracy

### 2. Chat Conversation E2E Test (@SpringBootTest + @AutoConfigureMockMvc)

**Purpose**: Test complete chat conversation workflow with full application context  
**Example**: `ChatConversationE2EIT.java`

**What it tests**:
1. **User message creation** - Save chat message via REST API
2. **Chat completion** - Send message to OpenAI with conversation history
3. **Assistant response** - Receive and save OpenAI response to database
4. **Conversation history** - Retrieve messages by session ID
5. **Multi-turn conversations** - Follow-up messages with context
6. **Session management** - Delete conversation (new chat)
7. **Multiple sessions** - Independent concurrent chat sessions
8. **Input validation** - Missing fields, constraints

**Key Features**:
- Tests full HTTP request/response cycle via MockMvc
- Uses **real PostgreSQL** via Testcontainers for persistence
- **Mocks OpenAI service** to avoid external API costs and ensure deterministic tests
- Validates JSON responses using JsonPath (Hamcrest)
- Verifies database state with AssertJ assertions
- Tests conversation sequences and concurrent sessions
- Tests session-based message filtering and retrieval

**Test Flow Example** (from `completeConversationFlow` test):
```
1. User: "Hello! How many documents do I have?" → Saved to DB
2. Send to OpenAI (mocked) → "You have 5 documents in your system."
3. Assistant response saved to DB
4. User: "Can you summarize them?" → Saved to DB
5. Retrieve conversation history (3 messages)
6. Send to OpenAI with history → "Your documents include: 2 invoices..."
7. Assistant response saved to DB
8. Verify complete conversation (4 messages) in DB
9. Delete conversation
10. Verify new conversation can start
```

**How Chat Feature Works**:
1. User opens chat page → Frontend loads previous conversation from database (via GET `/api/v1/chat-messages/session/{sessionId}`)
2. User sends message → Saved to DB (POST `/api/v1/chat-messages`) → Sent to OpenAI (POST `/api/v1/chat`) → Response saved to DB
3. All messages persist across sessions using localStorage session ID
4. "New Chat" button → Deletes current conversation (DELETE `/api/v1/chat-messages/session/{sessionId}`) and creates new session

**API Endpoints Tested**:
- `POST /api/v1/chat-messages` - Save chat message (user or assistant)
- `GET /api/v1/chat-messages/session/{sessionId}` - Retrieve conversation history
- `DELETE /api/v1/chat-messages/session/{sessionId}` - Delete conversation
- `POST /api/v1/chat` - Generate chat completion via OpenAI

## Testcontainers Setup

The `TestcontainersConfiguration` class provides the following containers:

### Core Infrastructure (Always Running)
- **PostgreSQL** - Real database for testing persistence
- **RabbitMQ** - Message broker for asynchronous communication
- **MinIO** - Object storage for file uploads

### Optional Containers
- **OCR Worker** - Starts only when `-Dtest.ocr.enabled=true`
  - Automatically built from `paperlessWorkers/Dockerfile`
  - Includes Tesseract OCR for text extraction
  - First build takes several minutes, then cached

**Container Features**:
- All containers connected via shared Docker network
- Container reuse enabled (`.withReuse(true)`) for faster test execution
- `@ServiceConnection` auto-configures Spring Boot properties
- Dynamic property registration for container ports
- Automatic cleanup after test execution

**Example from TestcontainersConfiguration**:
```java
@Bean
GenericContainer<?> ocrWorkerContainer(Network network, ...) {
    boolean ocrEnabled = Boolean.parseBoolean(
        System.getProperty("test.ocr.enabled", "false")
    );
    
    if (!ocrEnabled) return null; // Skip container creation
    
    String imageName = "paperless-workers-test:latest";
    GenericContainer<?> container;
    
    try {
        // Try to use existing image first
        container = new GenericContainer<>(DockerImageName.parse(imageName));
    } catch (Exception e) {
        // Build from paperlessWorkers directory if not exists
        ImageFromDockerfile ocrWorkerImage = 
            new ImageFromDockerfile(imageName, false)
                .withDockerfile(paperlessWorkersPath.resolve("Dockerfile"))
                .withFileFromPath(".", paperlessWorkersPath);
        container = new GenericContainer<>(ocrWorkerImage);
    }
    
    return container
            .withNetwork(network)
            .withEnv("RABBITMQ_HOST", "rabbitmq")
            // ... additional configuration
            .waitingFor(Wait.forLogMessage(".*Started.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));
}
```

**Benefits**:
- Real infrastructure ensures production-like behavior
- Tests are isolated (each test gets fresh schema)
- No manual Docker commands needed
- Automatic container lifecycle management

## Assertion Libraries

### AssertJ (Recommended for Domain Logic)
```java
assertThat(metadata.getFilename()).isEqualTo("test.pdf");
assertThat(ocrResult.getExtractedText())
    .as("OCR should extract some text from the PDF")
    .isNotEmpty();
```

### Awaitility (For Asynchronous Operations)
```java
// Wait for async message processing
Awaitility.await()
    .atMost(10, TimeUnit.SECONDS)
    .pollInterval(500, TimeUnit.MILLISECONDS)
    .untilAsserted(() -> {
        FileMetadata updated = repository.findById(id).orElseThrow();
        assertThat(updated.getSummary()).isNotNull();
    });
```

### Hamcrest (for MockMvc JSON Path Assertions)
```java
mockMvc.perform(get("/api/v1/files/{id}", documentId))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.filename").value("test.pdf"))
    .andExpect(jsonPath("$.summary").value(notNullValue()));
```

## Testing Asynchronous Message Flows

The E2E test demonstrates how to test RabbitMQ message passing:

```java
// Helper method to wait for messages in queue
private <T> T awaitMessageFromQueue(
    String queueName, 
    Class<T> messageType, 
    int timeoutSeconds
) {
    return Awaitility.await()
        .atMost(timeoutSeconds, TimeUnit.SECONDS)
        .pollInterval(200, TimeUnit.MILLISECONDS)
        .until(() -> {
            Object message = rabbitTemplate.receiveAndConvert(queueName, 100);
            return message != null ? messageType.cast(message) : null;
        }, msg -> msg != null);
}

// Usage in test
FileMessageDto ocrMessage = awaitMessageFromQueue(
    ocrQueue, 
    FileMessageDto.class, 
    5
);
assertThat(ocrMessage.getId()).isEqualTo(documentId);
```

## Test Data Management

### Queue Cleanup
Always clean queues in `@BeforeEach` to ensure test isolation:

```java
@BeforeEach
void setUp() {
    fileMetadataRepository.deleteAll();
    // Drain queues before each test
    rabbitTemplate.receive(ocrQueue, 100);
    rabbitTemplate.receive(genaiQueue, 100);
    rabbitTemplate.receive(genaiResultQueue, 100);
}
```

## Best Practices

### 1. Test Isolation
- Each test should be independent and not rely on execution order
- Use `@BeforeEach` to reset state (database, queues)
- Avoid hardcoded IDs - extract them from responses

### 2. Container Management
- Use simulated mode for fast feedback loops
- Use real OCR mode for pre-deployment validation
- Let Testcontainers handle container lifecycle (no manual cleanup needed)

### 3. Meaningful Assertions
- Test both happy path and error cases
- Verify all integration points (API → Storage → Queue → Processing → Database)
- Use descriptive assertion messages: `.as("OCR should extract text")`

### 4. Performance
- Simulated OCR: ~5-10 seconds per test
- Real OCR: ~30-60 seconds (includes container startup on first run)
- Container reuse reduces overhead for subsequent tests

## Troubleshooting

### Testcontainers not starting
**Problem**: Docker daemon not running  
**Solution**: Ensure Docker Desktop is running

### OCR worker container build fails
**Problem**: Cannot find paperlessWorkers directory  
**Solution**: Ensure test is run from `rest/` directory (not project root)

### Port conflicts
**Problem**: Container port already in use  
**Solution**: Testcontainers automatically assigns random ports - no manual configuration needed

### Slow test execution
**Problem**: Containers starting for each test  
**Solution**: Container reuse is already enabled in `TestcontainersConfiguration` with `.withReuse(true)`

### OCR test timeout
**Problem**: Real OCR worker not processing messages  
**Solution**: 
- Check container logs (Docker Desktop)
- Verify RabbitMQ and MinIO are accessible from OCR container
- Increase timeout in test if processing legitimately takes longer

### Tests fail locally but pass in CI
**Problem**: Database or queue state not cleaned between tests  
**Solution**: Always use `@BeforeEach` to reset state and drain queues

### First OCR test run is very slow
**Problem**: Docker building OCR worker image from scratch  
**Expected**: First run takes 2-3 minutes to build image with Tesseract  
**Solution**: This is normal - subsequent runs reuse the built image and are much faster
