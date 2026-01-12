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

## Next Steps

- [x] Add Spring Batch dependencies
- [ ] Define XML schema for access logs
- [ ] Implement batch job components
- [ ] Add PostgreSQL integration
- [ ] Implement scheduling
