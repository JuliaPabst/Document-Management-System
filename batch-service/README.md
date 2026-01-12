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

## Next Steps

- [x] Add Spring Batch dependencies
- [x] Define XML schema for access logs
- [ ] Implement batch job components
- [ ] Add PostgreSQL integration
- [ ] Implement scheduling
