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

### Run only integration tests
```bash
./mvnw test -Dtest="*IT"
```

### Run specific test class
```bash
./mvnw test -Dtest=FileMetadataRepositoryIT
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
**Example**: `FileMetadataControllerIT.java`

**Key Features**:
- Tests full HTTP request/response cycle
- Validates JSON responses using JsonPath
- Tests validation, and error handling
- Uses MockMvc to avoid starting real HTTP server

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
