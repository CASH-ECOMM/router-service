# Router Service

REST API gateway service that provides HTTP endpoints and connects to  via gRPC.

## Quick Start

### 1. Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for containerized deployment)
- Downstream services running, configured in `application.yaml`

### 2. Running with Maven

**Build the application:**
```bash
./mvnw clean package -DskipTests
```

**Run the service:**
```bash
./mvnw spring-boot:run
```

Service will start on `http://localhost:8080`

### 3. Running with Docker

**Build and run:**
```bash
docker compose up
```

This builds the application inside the container and starts the service.

**Stop the service:**
```bash
docker compose down
```

## API Documentation

Interactive API documentation is available via Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:
```
http://localhost:8080/v3/api-docs
```

## Tech Stack

- **Spring Boot 3.5.6** - REST API framework
- **gRPC Client** - Communication with User Service
- **Protocol Buffers** - Data serialization
- **SpringDoc OpenAPI** - API documentation
- **Maven** - Build tool
- **Docker** - Containerization