# Integration Tests Documentation

This document describes the integration testing setup and best practices for the REST service.

## Overview

The integration tests use the following technologies and patterns:
- **Spring Boot Test** - For loading application context
- **Testcontainers** - For running real PostgreSQL and RabbitMQ instances in Docker
- **MockMvc** - For simulating HTTP requests
- **AssertJ** - For fluent assertions
- **JUnit 5** - Test framework
- **Mockito** - For mocking external dependencies (e.g., MinIO storage)

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

## Test Categories

### 1. Repository Tests (@DataJpaTest)

**Purpose**: Test database operations with real PostgreSQL  
**Example**: `FileMetadataRepositoryIT.java`


**Key Features**:
- Uses real PostgreSQL via Testcontainers
- Automatically rolls back transactions
- Only loads JPA components (faster than full context)
- Tests custom queries and lifecycle callbacks

### 2. Controller Tests (@SpringBootTest + @AutoConfigureMockMvc)

**Purpose**: Test REST endpoints with full application context  
**Examples**: 
- `FileMetadataControllerIT.java` - Tests file upload, retrieval, and search endpoints
- `ChatMessageControllerIT.java` - Tests chat message persistence and retrieval
- `ChatControllerIT.java` - Tests chat completion endpoint (with mocked OpenAI)

**Key Features**:
- Tests full HTTP request/response cycle
- Validates JSON responses using JsonPath
- Tests validation, and error handling
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

The `TestcontainersConfiguration` class provides PostgreSQL and RabbitMQ containers:

**Benefits**:
- Real database ensures production-like behavior
- Tests are isolated (each test gets fresh schema)
- Container reuse speeds up test execution
- `@ServiceConnection` auto-configures Spring Boot properties

## Assertion Libraries

### AssertJ (Recommended)

### Hamcrest (for MockMvc)

## Mocking External Dependencies

Some services (like MinIO storage) are mocked to avoid external dependencies

## Troubleshooting

### Testcontainers not starting
**Problem**: Docker daemon not running  
**Solution**: Ensure Docker Desktop is running

### Port conflicts
**Problem**: Container port already in use  
**Solution**: Testcontainers automatically assigns random ports

### Slow test execution
**Problem**: Containers starting for each test  
**Solution**: Use `.withReuse(true)` in TestcontainersConfiguration

### Tests fail locally but pass in CI
**Problem**: Database state not cleaned between tests  
**Solution**: Always use `@BeforeEach` to reset state
