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
# Run E2E test with simulated OCR (fast)
./mvnw test -Dtest=FileUploadPipelineE2EIT

# Run E2E test with real OCR worker (full integration)
./mvnw test -Dtest=FileUploadPipelineE2EIT -Dtest.ocr.enabled=true
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
- First run takes longer (~2-3 minutes for Docker build)
- Subsequent runs reuse built image (much faster)
- Container configuration in `TestcontainersConfiguration.java`:
  - Connects to same network as RabbitMQ and MinIO
  - Auto-configured environment variables
  - Waits for application startup before tests proceed

**When to use each mode**:
- **Simulated OCR**: Fast feedback during development, CI/CD pipelines
- **Real OCR**: Final validation before deployment, testing OCR accuracy

### 2. Chat Feature Tests (@SpringBootTest + @AutoConfigureMockMvc)

**Purpose**: Test chat functionality with full application context  
**Examples**: 
- `ChatMessageControllerIT.java` - Tests chat message persistence and retrieval
- `ChatControllerIT.java` - Tests chat completion endpoint (with mocked OpenAI)

**Key Features**:
- Tests full HTTP request/response cycle
- Validates JSON responses using JsonPath
- Tests validation and error handling
- Uses MockMvc to avoid starting real HTTP server
- Verifies database state after operations 

**Chat Feature Integration Tests**:
- **ChatMessageControllerIT**: Tests full workflow → HTTP Request → Controller → Service → Repository → Database
  - Verifies both HTTP responses AND database persistence
  - Tests conversation history management 
  - Tests session-based message filtering and retrieval
  - Tests conversation sequences and concurrent sessions
- **ChatControllerIT**: Tests chat completion endpoint flow
  - OpenAI service is mocked to avoid external API costs
  - Focuses on controller request/response handling
  - Tests conversation history passing and error handling
- **ChatMessageRepositoryIT**: Tests repository with real PostgreSQL
  - Custom queries and sorting
  - Session-based operations
  - Timestamp ordering

**How Chat Feature Works**:
1. User opens chat page → Frontend loads previous conversation from database (via ChatMessageController)
2. User sends message -> Saved to DB -> Sent to OpenAI (via ChatController) -> Response saved to DB
3. All messages persist across sessions using localStorage session ID
4. "New Chat" button deletes current conversation and creates new session

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
  - First build takes ~2-3 minutes, then cached

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
    
    // Build image from paperlessWorkers directory
    ImageFromDockerfile ocrWorkerImage = 
        new ImageFromDockerfile("paperless-workers-test", false)
            .withDockerfile(paperlessWorkersPath.resolve("Dockerfile"))
            .withFileFromPath(".", paperlessWorkersPath);
    
    return new GenericContainer<>(ocrWorkerImage)
            .withNetwork(network)
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
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
