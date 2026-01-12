# Batch Service

A microservice for batch processing of access log statistics.

## Current Status

- Basic Spring Boot application structure  
- Actuator endpoints for health monitoring  
- Prometheus metrics integration  
- Docker support  
- CI/CD pipeline integration  

## Running the Service

### Docker
```bash
docker compose up -d batch-service
```

## Endpoints

- Health Check: `http://localhost:8086/actuator/health`
- Metrics: `http://localhost:8086/actuator/prometheus`
- Manual Trigger: `POST http://localhost:8086/api/v1/batch/trigger`
- Batch Health: `GET http://localhost:8086/api/v1/batch/health`

### Manual Job Trigger

```bash
curl -X POST http://localhost:8086/api/v1/batch/trigger
```

Response:
```json
{
  "status": "success",
  "message": "Batch job triggered successfully"
}
```

## XML Format

The batch service processes XML files with daily access statistics:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<AccessLogReport>
    <ReportDate>2026-01-11</ReportDate>
    <System>External-CMS</System>
    <GeneratedAt>2026-01-12T00:30:00Z</GeneratedAt>
    <DocumentAccesses>
        <DocumentAccess>
            <DocumentId>1</DocumentId>
            <AccessCount>15</AccessCount>
            <LastAccessTime>2026-01-11T23:45:00Z</LastAccessTime>
        </DocumentAccess>
        <!-- More DocumentAccess elements -->
    </DocumentAccesses>
</AccessLogReport>
```

Sample file: `sample-data/access-log-2026-01-11.xml`

## Database Schema

The service creates a `document_access_statistics` table:

```sql
CREATE TABLE document_access_statistics (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    access_date DATE NOT NULL,
    access_count INTEGER NOT NULL,
    source_file VARCHAR(255),
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_document_access UNIQUE (document_id, access_date)
);
```

**Features:**
- One record per document per day (unique constraint)
- Accumulates counts if same document+date appears multiple times
- Audit trail via `source_file` and `processed_at`

Migration script: `src/main/resources/db/migration/V1__create_document_access_statistics.sql`

## Scheduling

The batch job runs automatically on a schedule:

**Default Schedule:** Daily at 01:00 AM  
**Cron Expression:** `0 0 1 * * ?`

### Cron Expression Examples

| Expression | Description |
|------------|-------------|
| `0 0 1 * * ?` | Daily at 01:00 AM (default) |
| `0 0 2 * * ?` | Daily at 02:00 AM |
| `0 0 */6 * * ?` | Every 6 hours |
| `0 */30 * * * ?` | Every 30 minutes (testing) |

Configure via environment variable:
```bash
BATCH_SCHEDULE_CRON=0 0 2 * * ?
```

## Next Steps

- [x] Add Spring Batch dependencies
- [x] Define XML schema for access logs
- [x] Implement batch job components
- [x] Add PostgreSQL integration
- [x] Implement scheduling

## Steps taken during the development process

The batch service is now fully functional:
- Scheduled execution (daily at 01:00 AM)
- Manual triggering via REST API
- XML file processing
- PostgreSQL integration
- Error handling and logging
- Prometheus metrics
- Docker support
